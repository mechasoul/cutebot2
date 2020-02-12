package my.cute.bot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.TimeUtil;

public class GuildMessageScrapeTask implements Callable<CompletableFuture<Void>> {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildMessageScrapeTask.class);

	//shouldn't hold references to jda entities, but this task should be shortlived
	private Guild guild;
	private Path scrapeDirectory;
	//in days
	private int maxMessageAge;
	
	public GuildMessageScrapeTask(Guild guild, Path directory, int maxAge) {
		this.guild = guild;
		this.scrapeDirectory = directory;
		this.maxMessageAge = maxAge;
	}
	
	/*
	 * scrapes messages from each channel in the given guild, saving them to disk
	 * each channel is saved as <scrapeDirectory>/<channel id>.txt
	 * only messages from the defined scraped message period are accepted. the scraped message
	 * period is defined as the interval [current time, current time - this.maxMessageAge] (where
	 * maxMessageAge is a number of days) 
	 * for each scraped file, the first two lines represent the ZonedDateTime of 
	 * each end of the scraped message period. the first line in the file is the timestamp
	 * (as defined by ZonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) of the
	 * newest message in the channel, and the second line is the timestamp of the end of the 
	 * accepted message period (ie, current time - this.maxMessageAge)
	 * in the case where the latest message is older than the end of the accepted message period,
	 * the first line (the start of the scraped message period) will instead be the end of the
	 * scraped message period, so the start and the end of the scraped message period will be the 
	 * same and the file will contain no scraped messages
	 * messages are copied from the existing scraped message file where possible to speed up the 
	 * process / avoid scraping discord servers as much as possible
	 */
	@Override
	public CompletableFuture<Void> call() throws Exception {
		
		final List<CompletableFuture<?>> pendingTasks = new ArrayList<>(10);
		this.scrapeDirectory.toFile().mkdirs();
		
		final ZonedDateTime oldestAcceptableDateTimeZoned = ZonedDateTime.now(MiscUtils.TIMEZONE).minusDays(this.maxMessageAge);
		
		this.guild.getTextChannelCache().forEach(channel ->
		{
			logger.info("starting on " + channel);
			Message latestMessage=null;
			for(Message msg : channel.getHistory().retrievePast(4).complete()) {
				if (msg != null) {
					latestMessage = msg;
					break;
				}
			}
			//no messages, go next channel
			if(latestMessage == null) return;
			
			/*
			 * TODO
			 * need to block on scraping or something
			 * because if we try to set up a task to run when this is done scraping, all the scraping in here
			 * happens async, so the subsequent task executes when run() returns at which point temp files exist,
			 * scraping has just started, etc
			 * either need to set up some kind of completablefuture that completes when all scrape tasks are
			 * complete, or block on scraping (since this is happening in other thread anyway)
			 */
			try {
				Path scrapeFile = this.scrapeDirectory.resolve(channel.getId() + ".txt");
				if(Files.exists(scrapeFile)) {

					Path oldScrapeFile = Files.createTempFile(this.scrapeDirectory, channel.getId(), null);
					Files.move(scrapeFile, oldScrapeFile, StandardCopyOption.REPLACE_EXISTING);
					
					writeStartAndEndTimestamps(scrapeFile, latestMessage.getTimeCreated().atZoneSameInstant(MiscUtils.TIMEZONE),
							oldestAcceptableDateTimeZoned);

					/*
					 * possible timeline
					 * <- start of time ... [end of scraped msg file, start of scraped msg file] ... newest message ->
					 * three steps for scraping messages:
					 * 1. scrape messages from discord servers starting from the latest message, stopping when we find
					 * 		a message that isn't newer than the newest message in the existing scraped msg file
					 * 2. copy messages from existing scraped msg file to new scraped msg file
					 * 3. scrape messages from discord servers starting from before end date of scraped msg file until 
					 * 		the start of time
					 * end each step of the process if we encounter a message that's older than the oldest acceptable
					 * message datetime. if there are never any existing messages newer than the oldest acceptable 
					 * datetime, we'll just end up with an empty scrape at each step and it's fine
					 */
					@SuppressWarnings("resource")
					BufferedReader reader = Files.newBufferedReader(oldScrapeFile, StandardCharsets.UTF_8);
					final OffsetDateTime oldestAcceptableDateTime = oldestAcceptableDateTimeZoned.toOffsetDateTime();
					final OffsetDateTime previousLatestMessageTime = ZonedDateTime.parse(reader.readLine(), 
							DateTimeFormatter.ISO_ZONED_DATE_TIME).toOffsetDateTime();
					final OffsetDateTime previousOldestAcceptableTime = ZonedDateTime.parse(reader.readLine(), 
							DateTimeFormatter.ISO_ZONED_DATE_TIME).toOffsetDateTime();
					if(previousLatestMessageTime.isBefore(oldestAcceptableDateTime)) {
						//scraped file is totally outdated. scrape only from servers
						reader.close();
						pendingTasks.add(scrapeMessagesUntil(channel, scrapeFile, oldestAcceptableDateTime)
						.thenAccept(result -> {
							try {
								Files.delete(oldScrapeFile);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}));
					} else {
						//scrape from servers, file, servers
						pendingTasks.add(scrapeMessagesUntil(channel, scrapeFile, previousLatestMessageTime)
						.thenAccept(result -> 
						{
							try (BufferedWriter writer = Files.newBufferedWriter(scrapeFile, StandardCharsets.UTF_8, 
									StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
								String line = reader.readLine();
								while (line != null) {
									if(LocalDate.parse(line.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE)
											.isBefore(oldestAcceptableDateTime.toLocalDate())) {
										break;
									} else {
										writer.newLine();
										writer.append(line);
										line = reader.readLine();
									}
								}
								reader.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} 
						}).thenAccept(result -> {
							try {
								scrapeMessagesBetween(channel, scrapeFile, previousOldestAcceptableTime, 
										oldestAcceptableDateTime);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}).thenAccept(result -> {
							try {
								Files.delete(oldScrapeFile);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}));
					}

				} else {
					//no previously scraped messages. scrape all from discord servers
					writeStartAndEndTimestamps(scrapeFile, latestMessage.getTimeCreated().atZoneSameInstant(MiscUtils.TIMEZONE),
							oldestAcceptableDateTimeZoned);
					
					final OffsetDateTime oldestAcceptableDateTime = oldestAcceptableDateTimeZoned.toOffsetDateTime();

					pendingTasks.add(scrapeMessagesUntil(channel, scrapeFile, oldestAcceptableDateTime));

				}
			} catch (IOException ex) {
				//TODO
				ex.printStackTrace();
			}
		});
		
		return CompletableFuture.allOf(pendingTasks.toArray(new CompletableFuture[0]));
	}
	
	public void run() {
		
		this.scrapeDirectory.toFile().mkdirs();
		
		final ZonedDateTime oldestAcceptableDateTimeZoned = ZonedDateTime.now(MiscUtils.TIMEZONE).minusDays(this.maxMessageAge);
		
		this.guild.getTextChannelCache().forEach(channel ->
		{
			logger.info("starting on " + channel);
			Message latestMessage=null;
			for(Message msg : channel.getHistory().retrievePast(4).complete()) {
				if (msg != null) {
					latestMessage = msg;
					break;
				}
			}
			//no messages, go next channel
			if(latestMessage == null) return;
			
			/*
			 * TODO
			 * need to block on scraping or something
			 * because if we try to set up a task to run when this is done scraping, all the scraping in here
			 * happens async, so the subsequent task executes when run() returns at which point temp files exist,
			 * scraping has just started, etc
			 * either need to set up some kind of completablefuture that completes when all scrape tasks are
			 * complete, or block on scraping (since this is happening in other thread anyway)
			 */
			try {
				Path scrapeFile = this.scrapeDirectory.resolve(channel.getId() + ".txt");
				if(Files.exists(scrapeFile)) {

					Path oldScrapeFile = Files.createTempFile(this.scrapeDirectory, channel.getId(), null);
					Files.move(scrapeFile, oldScrapeFile, StandardCopyOption.REPLACE_EXISTING);
					
					writeStartAndEndTimestamps(scrapeFile, latestMessage.getTimeCreated().atZoneSameInstant(MiscUtils.TIMEZONE),
							oldestAcceptableDateTimeZoned);

					/*
					 * possible timeline
					 * <- start of time ... [end of scraped msg file, start of scraped msg file] ... newest message ->
					 * three steps for scraping messages:
					 * 1. scrape messages from discord servers starting from the latest message, stopping when we find
					 * 		a message that isn't newer than the newest message in the existing scraped msg file
					 * 2. copy messages from existing scraped msg file to new scraped msg file
					 * 3. scrape messages from discord servers starting from before end date of scraped msg file until 
					 * 		the start of time
					 * end each step of the process if we encounter a message that's older than the oldest acceptable
					 * message datetime. if there are never any existing messages newer than the oldest acceptable 
					 * datetime, we'll just end up with an empty scrape at each step and it's fine
					 */
					@SuppressWarnings("resource")
					BufferedReader reader = Files.newBufferedReader(oldScrapeFile, StandardCharsets.UTF_8);
					final OffsetDateTime oldestAcceptableDateTime = oldestAcceptableDateTimeZoned.toOffsetDateTime();
					final OffsetDateTime previousLatestMessageTime = ZonedDateTime.parse(reader.readLine(), 
							DateTimeFormatter.ISO_ZONED_DATE_TIME).toOffsetDateTime();
					final OffsetDateTime previousOldestAcceptableTime = ZonedDateTime.parse(reader.readLine(), 
							DateTimeFormatter.ISO_ZONED_DATE_TIME).toOffsetDateTime();
					if(previousLatestMessageTime.isBefore(oldestAcceptableDateTime)) {
						//scraped file is totally outdated. scrape only from servers
						reader.close();
						scrapeMessagesUntil(channel, scrapeFile, oldestAcceptableDateTime)
						.thenAccept(result -> {
							try {
								Files.delete(oldScrapeFile);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						});
					} else {
						//scrape from servers, file, servers
						scrapeMessagesUntil(channel, scrapeFile, previousLatestMessageTime)
						.thenAccept(result -> 
						{
							try (BufferedWriter writer = Files.newBufferedWriter(scrapeFile, StandardCharsets.UTF_8, 
									StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
								String line = reader.readLine();
								while (line != null) {
									if(LocalDate.parse(line.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE)
											.isBefore(oldestAcceptableDateTime.toLocalDate())) {
										break;
									} else {
										writer.newLine();
										writer.append(line);
										line = reader.readLine();
									}
								}
								reader.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} 
						}).thenAccept(result -> {
							try {
								scrapeMessagesBetween(channel, scrapeFile, previousOldestAcceptableTime, 
										oldestAcceptableDateTime);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}).thenAccept(result -> {
							try {
								Files.delete(oldScrapeFile);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						});
					}

				} else {
					//no previously scraped messages. scrape all from discord servers
					writeStartAndEndTimestamps(scrapeFile, latestMessage.getTimeCreated().atZoneSameInstant(MiscUtils.TIMEZONE),
							oldestAcceptableDateTimeZoned);
					
					final OffsetDateTime oldestAcceptableDateTime = oldestAcceptableDateTimeZoned.toOffsetDateTime();

					scrapeMessagesUntil(channel, scrapeFile, oldestAcceptableDateTime);

				}
			} catch (IOException ex) {
				//TODO
				ex.printStackTrace();
			}
		});
	}
	
	private CompletableFuture<?> scrapeMessagesUntil(TextChannel channel, Path scrapeFile, OffsetDateTime endPoint) throws IOException {
		return this.scrapeMessagesBetween(channel, scrapeFile, null, endPoint);
	}
	
	/*
	 * both startPoint and endPoint are exclusive
	 * if startPoint is null, will start from latest message
	 */
	@SuppressWarnings("resource")
	private CompletableFuture<?> scrapeMessagesBetween(TextChannel channel, Path scrapeFile, OffsetDateTime startPoint, OffsetDateTime endPoint) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(scrapeFile, StandardCharsets.UTF_8, 
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		return channel.getIterableHistory().cache(false)
				.skipTo(startPoint != null ? TimeUtil.getDiscordTimestamp(startPoint.toInstant().toEpochMilli()) : 0)
				.forEachAsync(msg ->
				{
					OffsetDateTime msgCreationTime = msg.getTimeCreated();
					if(!msgCreationTime.isAfter(endPoint)) {
						return false;
					} else {
						if(!msg.getAuthor().isBot()) {
							try {
								writer.newLine();
								writer.append(msgCreationTime.atZoneSameInstant(MiscUtils.TIMEZONE).toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE));
								writer.append(MiscUtils.replaceNewLinesWithTokens(msg.getContentRaw()));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						return true;
					}
				}).whenComplete((result, throwable) -> {
					try {
						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
	}
	
	private void writeStartAndEndTimestamps(Path scrapeFile, ZonedDateTime latestMessageTime, ZonedDateTime oldestAcceptableTime) throws IOException {
		if(latestMessageTime.isBefore(oldestAcceptableTime)) latestMessageTime = oldestAcceptableTime;
		try (BufferedWriter writer = Files.newBufferedWriter(scrapeFile, StandardCharsets.UTF_8, 
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(latestMessageTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
			writer.newLine();
			writer.append(oldestAcceptableTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
		}
	}

	

}
