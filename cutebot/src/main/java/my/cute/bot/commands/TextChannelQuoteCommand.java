package my.cute.bot.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class TextChannelQuoteCommand extends TextChannelCommand {
	
	private static final Logger logger = LoggerFactory.getLogger(TextChannelQuoteCommand.class);

	private static final Path QUOTES_FILE = Paths.get("./twitchchat.txt");
	private static final Path QUOTE_INFO_FILE = Paths.get("./quotes_info.txt");
	
	TextChannelQuoteCommand(String name) {
		super(name);
	}

	@Override
	public void execute(MessageReceivedEvent event) {
		String dailyQuote = null;
		try (BufferedReader reader = Files.newBufferedReader(QUOTE_INFO_FILE, StandardCharsets.UTF_8)) {
			if(!isExpired(reader.readLine())) {
				//no update needed. return current day's quote
				dailyQuote = this.getDailyQuote(Integer.parseInt(reader.readLine()));
			}
		} catch (IOException e) {
			logger.error("exception thrown in TextChannelQuoteCommand.execute(): " + e.getMessage(), e);
			dailyQuote = "the quotes broke so im taking the day off";
		}
		if(dailyQuote == null) {
			//need to update quote info file and get new quote
			dailyQuote = this.updateQuoteInfoFile();
		}
		event.getChannel().sendMessage(new MessageBuilder().append("today's Twitch Chat:tm: Quote Of The Day (!quote)")
				.setEmbed(new EmbedBuilder().setDescription(dailyQuote).build()).build()).queue();
	}

	@Override
	public String getDescription() {
		return "displays today's Twitch Chat:tm: Quote Of The Day. a good conversation starter";
	}

	@Override
	public String getHelp() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * takes a String representing a date (as defined by 
	 * LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)), and determines 
	 * if that date is "expired" or not. a date is expired if it has been at least
	 * one whole day since that date
	 * 
	 * returns true if the date is expired (there is at least 1 whole day between the 
	 * given date and LocalDate.now()), and false if not
	 */
	private boolean isExpired(String dateStamp) {
		return ChronoUnit.DAYS.between(LocalDate.parse(dateStamp, DateTimeFormatter.BASIC_ISO_DATE), LocalDate.now()) >= 1;
	}
	
	private String getDailyQuote(int lineNum) {
		String quote = null;
		try (BufferedReader quoteReader = Files.newBufferedReader(QUOTES_FILE, StandardCharsets.UTF_8)) {
			while(lineNum >= 0) {
				quote = quoteReader.readLine();
				lineNum--;
			}
		} catch (IOException e) {
			logger.error("exception encountered in TextChannelQuoteCommand.getDailyQuote(): " + e.getMessage(), e);
		}
		if(quote != null) {
			return quote;
		} else {
			logger.error("getDailyQuote() returned a null quote!");
			return "the quotes broke so im taking the day off";
		}
	}
	
	private String updateQuoteInfoFile() {
		try (BufferedWriter writer = Files.newBufferedWriter(QUOTE_INFO_FILE, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
			writer.newLine();
			//get total number of quotes and randomly assign one as daily quote
			List<String> quotes = Files.readAllLines(QUOTES_FILE, StandardCharsets.UTF_8);
			int quoteNumber = new Random().nextInt(quotes.size());
			writer.append(""+quoteNumber);
			return quotes.get(quoteNumber);
		} catch (IOException e) {
			logger.error("exception thrown in TextChannelQuoteCommand.updateQuoteInfoFile(): " + e.getMessage(), e);
			return "the quotes broke so im taking the day off";
		}
	}
}
