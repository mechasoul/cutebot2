package my.cute.bot.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.preferences.GuildPreferences;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

class TextChannelQuoteCommand extends TextChannelCommand {
	
	/*
	 * TODO
	 * a lot of static patterns here maybe should rethink having a separate
	 * instance of this class in every guild's commandset
	 * alternatively could allow for guilds to build their own quote file 
	 * just right now everything is shared on a single quote file so this
	 * entire class could be made singleton with no issue
	 */
	
	private static final Logger logger = LoggerFactory.getLogger(TextChannelQuoteCommand.class);

	/*
	 * the file that holds quotes
	 * each line should be a single quote
	 */
	private static final Path QUOTES_FILE = Paths.get("./twitchchat.txt");
	/*
	 * the file that saves the daily quote
	 * first line is the datestamp of the saved quote 
	 * (as by LocalDate.format(DateTimeFormatter.BASIC_ISO_DATE)
	 * second line is the actual quote of the day
	 */
	private static final Path DAILY_QUOTE_FILE = Paths.get("./daily_quote.txt");
	private static final Random RAND = new Random();
	
	private final GuildPreferences prefs;
	
	TextChannelQuoteCommand(GuildPreferences prefs) {
		super("quote", PermissionLevel.USER, 0, 0);
		this.prefs = prefs;
	}

	@Override
	public void execute(Message message) {
		String dailyQuote = null;
		/*
		 * prevent possibly having two guilds try to update quote file at same time,
		 * read when quote isn't done writing, etc
		 */
		synchronized(TextChannelQuoteCommand.class) {
			try (BufferedReader reader = Files.newBufferedReader(DAILY_QUOTE_FILE, StandardCharsets.UTF_8)) {
				if(!isExpired(reader.readLine())) {
					//no update needed. return current day's quote
					dailyQuote = reader.readLine();
				}
			} catch (NoSuchFileException e) {
				//likely first run, do nothing. will continue and create quote info file and get new quote
			} catch (IOException e) {
				logger.warn(this + ": exception thrown in execute(): " + e.getMessage(), e);
				dailyQuote = "the quotes broke so im taking the day off";
			}
		}
		if(dailyQuote == null) {
			//need to update quote info file and get new quote
			dailyQuote = this.updateDailyQuoteFile();
		}
		message.getChannel().sendMessage(new MessageBuilder().append("today's Twitch Chat:tm: Quote Of The Day (" 
				+ this.prefs.getPrefix() + "quote)")
				.setEmbed(new EmbedBuilder().setDescription(dailyQuote).build()).build()).queue();
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
		try {
			return ChronoUnit.DAYS.between(LocalDate.parse(dateStamp, DateTimeFormatter.BASIC_ISO_DATE), LocalDate.now()) >= 1;
		} catch (DateTimeParseException e) {
			//something weird happened. should remake daily quote
			logger.warn(this + ": exception in parsing dateStamp '" + dateStamp + "', rebuilding "
					+ "daily quote file. ex: " + e, e);
			return true;
		}
	}
	
	/*
	 * updates the DAILY_QUOTE_FILE
	 * writes the current datestamp to the file, as given by
	 * LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE
	 * and then retrieves a random line from QUOTES_FILE, which
	 * is selected as the daily quote, and writes it to DAILY_QUOTE_FILE
	 * 
	 * returns the selected daily quote
	 */
	private String updateDailyQuoteFile() {
		synchronized(TextChannelQuoteCommand.class) {
			try (BufferedWriter writer = Files.newBufferedWriter(DAILY_QUOTE_FILE, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				writer.append(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
				writer.newLine();
				//get all quotes and randomly assign one as daily quote
				List<String> quotes = Files.readAllLines(QUOTES_FILE, StandardCharsets.UTF_8);
				String dailyQuote = quotes.get(RAND.nextInt(quotes.size()));
				writer.append(dailyQuote);
				return dailyQuote;
			} catch (IOException e) {
				logger.warn(this + ": exception thrown in updateQuoteInfoFile(): " + e.getMessage(), e);
				return "the quotes broke so im taking the day off";
			}
		}
	}
}
