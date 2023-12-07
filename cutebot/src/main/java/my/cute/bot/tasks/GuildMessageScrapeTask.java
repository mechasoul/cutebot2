package my.cute.bot.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.utils.TimeUtil;

public class GuildMessageScrapeTask implements Runnable {
	
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
	 * 
	 * most of this method is performed async in completionstages
	 * 
	 * returns a CompletableFuture that represents all of the different scrape tasks over all
	 * the channels in the given guild. when the returned CompletableFuture completes normally,
	 * all scrape tasks have been successful. if at least one scrape task completes exceptionally
	 * (eg ioexception), the returned CompletableFuture will also complete exceptionally
	 */
	@Override
	public void run() {
		
		/*
		 * TODO
		 * 
		 * having start of period be most recent msg time and end of period be oldest message
		 * means that announcement channels are almost always included + basically any channel
		 * that gets a handful of messages in a very short time and then is dead forever
		 * but if we start from current time we likely exclude channels that were once active
		 * but closed at some point
		 * possible solns?
		 * start from current message but check that period is at least some extended time for
		 * channels that have been alive at least that long
		 * 
		 * tidy? huge method
		 */
		logger.info(this + ": starting message scrape task");
		
		final List<CompletableFuture<?>> pendingTasks = new ArrayList<>(10);
		this.scrapeDirectory.toFile().mkdirs();
		
		final ZonedDateTime oldestAcceptableDateTimeZoned = ZonedDateTime.now(MiscUtils.TIMEZONE).minusDays(this.maxMessageAge);
		
		this.guild.getTextChannelCache().forEach(channel -> {
			try {
				logger.info(this + ": starting on " + channel);
				Message latestMessage=null;
				for(Message msg : channel.getHistory().retrievePast(3).complete()) {
					if (msg != null) {
						latestMessage = msg;
						break;
					}
				}
				//no messages, go next channel
				if(latestMessage == null) return;
				
				Message oldestMessage = null;
				List<Message> oldestMessages = channel.getHistoryFromBeginning(1).complete().getRetrievedHistory();
				if(oldestMessages.isEmpty()) return;
				oldestMessage = oldestMessages.get(0);
				
				Path scrapeFile = this.scrapeDirectory.resolve(channel.getId() + ".txt");
				if(Files.exists(scrapeFile)) {

					Path oldScrapeFile = Files.createTempFile(this.scrapeDirectory, channel.getId(), null);
					Files.move(scrapeFile, oldScrapeFile, StandardCopyOption.REPLACE_EXISTING);

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
					
					/*
					 * readers/writers are closed in a CompletableFuture created by a whenComplete() call on the scrape tasks
					 * can't manage them in a try-with-resources block as far as I can tell since they'll be automatically
					 * closed when the try block ends and they go out of scope, and we need them to be open for the entirety 
					 * of the scrape task. even if the scraping throws an exception, the whenComplete() future still executes
					 * and attempts to close() the reader/writer. eclipse still gives warning tho so i suppress it
					 */
					@SuppressWarnings("resource")
					BufferedReader reader = Files.newBufferedReader(oldScrapeFile, StandardCharsets.UTF_8);
					@SuppressWarnings("resource")
					BufferedWriter writer = Files.newBufferedWriter(scrapeFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
							StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
					final OffsetDateTime oldestAcceptableDateTime = oldestAcceptableDateTimeZoned.toOffsetDateTime();
					final OffsetDateTime previousLatestMessageTime = ZonedDateTime.parse(reader.readLine(), 
							DateTimeFormatter.ISO_ZONED_DATE_TIME).toOffsetDateTime();
					final OffsetDateTime previousOldestAcceptableTime = ZonedDateTime.parse(reader.readLine(), 
							DateTimeFormatter.ISO_ZONED_DATE_TIME).toOffsetDateTime();
					
					/*
					 * recorded endTime will either be (current time - maxMessageAge) or the time of the oldest message
					 * whichever one comes after: if it's (currentTime - maxMessageAge), then the channel has been
					 * alive longer than the scraped message period and we scrape until the end of the period, and if
					 * it's the time of the oldest message, then we scrape the entire channel and record its effective
					 * creation time to more accurately get the lifetime of the channel
					 */
					ZonedDateTime endTime = oldestAcceptableDateTime.isAfter(oldestMessage.getTimeCreated()) ? 
							oldestAcceptableDateTimeZoned : oldestMessage.getTimeCreated().atZoneSameInstant(MiscUtils.TIMEZONE);
					
					writeStartAndEndTimestamps(writer, latestMessage.getTimeCreated().atZoneSameInstant(MiscUtils.TIMEZONE),
							endTime);
					
					if(previousLatestMessageTime.isBefore(oldestAcceptableDateTime)) {
						//scraped file is totally outdated. scrape only from servers
						reader.close();
						pendingTasks.add(scrapeMessagesUntil(channel, writer, oldestAcceptableDateTime)
						.whenComplete((result, throwable) -> {
							try {
								writer.close();
								Files.delete(oldScrapeFile);
							} catch (IOException e) {
								logger.warn(this + ": IOException when cleaning up old scrape file '" + oldScrapeFile 
										+ "', writer '" + writer + "' for channel '" + channel + "'! ex: " + e, e);
								throw new UncheckedIOException(e);
							}
						}));
					} else {
						//scrape from servers, file, servers
						pendingTasks.add(scrapeMessagesUntil(channel, writer, previousLatestMessageTime)
								.thenAcceptAsync(result -> {
									try {
										String line = reader.readLine();
										//note scrape file is organized from newest message to oldest message
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
									} catch (IOException e) {
										logger.warn(this + ": IOException when copying lines from old scrape file '" + oldScrapeFile
												+ "' for channel '" + channel + "'! ex: " + e.getMessage(), e);
										throw new UncheckedIOException(e);
									}
								}).thenComposeAsync(result -> scrapeMessagesBetween(channel, writer, previousOldestAcceptableTime, 
										oldestAcceptableDateTime))
								.whenComplete((result, throwable) -> {
									try {
										reader.close();
										writer.close();
										Files.delete(oldScrapeFile);
									} catch (IOException e) {
										logger.warn(this + ": IOException when cleaning up reader '" + reader + "', writer '"
												+ writer + "', old scrape file '" + oldScrapeFile + "', channel '" + channel + "'! ex: " 
												+ e, e);
										throw new UncheckedIOException(e);
									}
								}));
					}
				} else {
					//no previously scraped messages. scrape all from discord servers
					@SuppressWarnings("resource")
					BufferedWriter writer = Files.newBufferedWriter(scrapeFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
							StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
					final OffsetDateTime oldestAcceptableDateTime = oldestAcceptableDateTimeZoned.toOffsetDateTime();
					
					ZonedDateTime endTime = oldestAcceptableDateTime.isAfter(oldestMessage.getTimeCreated()) ? 
							oldestAcceptableDateTimeZoned : oldestMessage.getTimeCreated().atZoneSameInstant(MiscUtils.TIMEZONE);
					
					writeStartAndEndTimestamps(writer, latestMessage.getTimeCreated().atZoneSameInstant(MiscUtils.TIMEZONE),
							endTime);
					
					

					pendingTasks.add(scrapeMessagesUntil(channel, writer, oldestAcceptableDateTime)
							.whenComplete((result, throwable) -> {
								try {
									writer.close();
								} catch (IOException e) {
									logger.warn(this + ": IOException when closing writer '" + writer + "' for channel '"
											+ channel + "'! ex: " + e, e);
									throw new UncheckedIOException(e);
								}
							}));
				}
			} catch (InsufficientPermissionException e) {
				logger.info(this + ": missing permissions on channel '" + channel + "', proceeding to next channel. ex: "
						+ e.getMessage());
			} catch (IOException ex) {
				logger.warn(this + ": IOException during general task process in channel '" + channel + "'! ex: " 
						+ ex.getMessage(), ex);
				pendingTasks.add(CompletableFuture.failedFuture(ex));
			}
		});
		
		//call join so this task doesn't complete until all scraping and processing is done
		CompletableFuture.allOf(pendingTasks.toArray(new CompletableFuture[0])).join();
	}
	
	private CompletableFuture<?> scrapeMessagesUntil(TextChannel channel, BufferedWriter writer, OffsetDateTime endPoint) {
		return this.scrapeMessagesBetween(channel, writer, null, endPoint);
	}
	
	/*
	 * scrape discord servers
	 * both startPoint and endPoint are exclusive
	 * if startPoint is null, will start from latest message
	 * 
	 */
	private CompletableFuture<?> scrapeMessagesBetween(TextChannel channel, BufferedWriter writer, OffsetDateTime startPoint, OffsetDateTime endPoint) {
		return channel.getIterableHistory().cache(false)
				.skipTo(startPoint != null ? TimeUtil.getDiscordTimestamp(startPoint.toInstant().toEpochMilli()) : 0)
				.forEachAsync(msg -> {
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
								logger.warn(this + ": IOException when trying to scrape messages in channel '" + channel
										+ "', writer '" + writer + "', startPoint '" + startPoint + "', endPoint '" + endPoint
										+ "'! ex: " + e.getMessage(), e);
								throw new UncheckedIOException(e);
							}
						}
						return true;
					}
				});
	}
	
	private static void writeStartAndEndTimestamps(BufferedWriter writer, ZonedDateTime latestMessageTime, ZonedDateTime oldestAcceptableTime) throws IOException {
		if(latestMessageTime.isBefore(oldestAcceptableTime)) latestMessageTime = oldestAcceptableTime;
		writer.append(latestMessageTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
		writer.newLine();
		writer.append(oldestAcceptableTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GuildMessageScrapeTask-");
		builder.append(this.guild.getId());
		return builder.toString();
	}

	

}
