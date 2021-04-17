package my.cute.bot.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.cache.CacheView;

public class MiscUtils {
	
	public static final ZoneId TIMEZONE = ZoneId.of("America/Vancouver");
	private static final Logger logger = LoggerFactory.getLogger(MiscUtils.class);
	private static final String NEW_LINE_TOKEN = "<_NL>";
	private static final Random RAND = new Random();
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private final static Pattern QUOTATION_MARKS = Pattern.compile("^\".*\"(?:\\s+|$)");

	public static String replaceNewLinesWithTokens(String line) {
		/*
		 * i think theres something to say here about not stripping the newline token
		 * from messages so people could deliberately insert it into messages
		 * but in the end it'd just get replaced with a new line and i don't think 
		 * it really matters at all?
		 */
		return line.trim().replaceAll("\\R", NEW_LINE_TOKEN);
	}
	
	public static String replaceNewLineTokens(String line) {
		return line.replace(NEW_LINE_TOKEN, System.lineSeparator());
	}
	
	public static Emote getRandomEmoteFromCache(JDA jda) {
		CacheView<Emote> emoteCache =jda.getEmoteCache();
		return emoteCache.applyStream(stream -> 
		{
			try {
				return stream.skip(RAND.nextInt((int) emoteCache.size())).findFirst().orElseThrow();
			} catch (NoSuchElementException e) {
				logger.warn("MiscUtils: exception when trying to get random emote, size: " + emoteCache.size() + ", ex: "
						+ e.getMessage(), e);
				//mothyes
				return jda.getEmoteById("242763939631333378");
			}
		});
	}

	public static List<String> tokenize(String line) {
		return Arrays.asList(StringUtils.split(line, null));
	}
	
	public static String sanitize(String string) {
		return WHITESPACE.matcher(string).replaceAll(" ").trim().toLowerCase();
	}

	public static String getDateStamp() {
		return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
	}
	
	public static String[] getWords(Message message) {
		return WHITESPACE.split(message.getContentRaw().trim());
	}
	
	/**
	 * takes a string and extracts words from it. splits the given string on whitespace, with 
	 * leading and trailing whitespace trimmed first
	 * @param message the string to split into words
	 * @return an array consisting of the individual words in the given string
	 */
	public static String[] getWords(String message) {
		return WHITESPACE.split(message.trim());
	}
	
	public static String[] getWords(Message message, int limit) {
		return WHITESPACE.split(message.getContentRaw().trim(), limit);
	}
	
	public static String getGuildString(Guild guild) {
		return guild.getName() + " (id=" + guild.getId() + ")";
	}
	
	public static String getUserString(User user) {
		return user.getName() + user.getDiscriminator() + " (id=" + user.getId() + ")";
	}
	
	public static String getSignature() {
		StringBuilder builder = new StringBuilder();
		builder.append(System.lineSeparator());
		builder.append(System.lineSeparator());
		
		int randomNum = RAND.nextInt(5);
		if(randomNum == 0) {
			builder.append("yours truly,");
		} else if (randomNum == 1) {
			builder.append("sincerely,");
		} else if (randomNum == 2) {
			builder.append("best wishes,");
		} else if (randomNum == 3) {
			builder.append("warmest regards,");
		} else {
			builder.append("hope this helps,");
		}
		builder.append(System.lineSeparator());
		builder.append("cutebot");
		return builder.toString();
	}
	
	/**
	 * TODO i think this sucks? does it work?
	 * i wanted to do this because i thought maybe doing 
	 * messages.forEach(message -> channel.sendMessage(message).queue())
	 * could have the messages arrive at discord servers out of order or something
	 * and by chaining callbacks we could avoid it 
	 * but i feel like it sucks
	 * @param channel the channel to send the messages to
	 * @param messages the messages to send
	 */
	public static void sendMessages(MessageChannel channel, Queue<Message> messages) {
		if(messages.isEmpty()) return;
		sendMessages(channel, channel.sendMessage(messages.poll()), messages);
	}
	
	private static void sendMessages(MessageChannel channel, MessageAction previous, Queue<Message> messages) {
		Message message = messages.poll();
		if(message == null) {
			previous.queue();
		} else {
			previous.queue(success -> sendMessages(channel, channel.sendMessage(message), messages),
						failure -> channel.sendMessage((messages.size() + 1) + " message(s) not sent because something went wrong").queue());
		}
	}
	
	/**
	 * attempts to retrieve a role or roles matching the specified text from the given 
	 * guild. first checks if the given text is surrounded by quotation marks - if so,
	 * looks for a role matching exactly what's inside the quotation marks, then for
	 * roles matching a comma-separated list inside the quotation marks. if the given
	 * text is not surrounded by quotation marks, instead a check is made to see if 
	 * the first word in the text is an id matching a role, and then a check is made
	 * for a role whose name matches the first word in the text
	 * @param guild the guild to check for roles
	 * @param text text to check for roles. most common use case will be as parameters
	 * for a command, with the previous parameters stripped. should be a role name or
	 * comma-separated list of role names surrounded by quotation marks, or a role's id,
	 * or the single-word name of a role
	 * @return a possibly-empty immutable list of roles matching the given text in the
	 * given guild
	 */
	public static ImmutableList<Role> parseRoles(final Guild guild, String text) {
		String extractedText = extractQuotationMarks(text);
		if(extractedText != null) {
			
			//test for an exact role, then comma-separated
			Role singleRole = MiscUtils.getRoleByName(guild, text);
			if(singleRole != null) {
				return ImmutableList.of(singleRole);
			} else {
				//not exact role. try comma-separated list
				return MiscUtils.getRolesFromCommaSeparatedList(guild, text);
			}
		} else {
			//no quotation marks. simply check the first word in the given text
			text = MiscUtils.getWords(text)[0];
			//first check id, then role name
			Role singleRole = MiscUtils.tryRoleById(guild, text);
			if(singleRole != null) {
				return ImmutableList.of(singleRole);
			} else {
				singleRole = MiscUtils.getRoleByName(guild, text);
				if(singleRole != null) {
					return ImmutableList.of(singleRole);
				} else {
					return ImmutableList.of();
				}
			}
		}
	}
	
	/**
	 * see {@link #parseRoles(Guild, String)}, but taking on some extra message parsing.
	 * eg, message with content:
	 * <p>
	 * !role create region "North America,South America,Europe"
	 * <p>
	 * could call parseRole(guild, message, 3) to attempt to find roles matching the list.
	 * or if you'd prefer, simply shorthand for <br>
	 * parseRole(guild, getWords(message, paramsToIgnore+1)[paramsToIgnore])
	 * @param guild the guild to check for roles
	 * @param message the message to strip roles from (probably a user-submitted command)
	 * @param paramsToIgnore the number of words to ignore in the message until the roles
	 * should begin
	 * @return an immutable list of roles matching the text starting at the specified word,
	 * in the given guild
	 */
	public static ImmutableList<Role> parseRoles(final Guild guild, final Message message, int paramsToIgnore) {
		return parseRoles(guild, getWords(message, paramsToIgnore+1)[paramsToIgnore]);
	}
	
	/**
	 * returns a Role from the given guild with the given name. will first check for roles
	 * that match the given name exactly, then case insensitive. if multiple roles exist 
	 * with the given name, returns the oldest one (subject to sudden change if 
	 * JDA changes its sorting). if no roles exist with the given name, returns null
	 * @param guild the guild to check for a role 
	 * @param name the name of the role to return (case insensitive)
	 * @return a role with the given name in the given guild. if multiple roles exist with the
	 * given name, returns the oldest (subject to change if JDA changes). if none exist with 
	 * the given name, returns null
	 */
	public static Role getRoleByName(Guild guild, String name) {
		if(name.isBlank()) return null;
		List<Role> roles = guild.getRolesByName(name, true);
		if(roles.size() > 1) {
			//more than one matching role. check for one that matches case sensitive
			for(Role role : roles) {
				if(role.getName().equals(name)) return role;
			}
			//otherwise return any
			return roles.get(0);
		} else if (roles.size() == 1) {
			return roles.get(0);
		} else {
			return null;
		}
	}
	
	private static ImmutableList<Role> getRolesFromCommaSeparatedList(Guild guild, String listOfRoles) {
		return Stream.of(listOfRoles.split(","))
				.filter(name -> !name.isEmpty())
				.map(name -> MiscUtils.getRoleByName(guild, name))
				.filter(role -> role != null)
				.collect(ImmutableList.toImmutableList());
	}
	
	public static boolean hasQuotationMarks(String input) {
		return QUOTATION_MARKS.matcher(input).find();
	}
	
	/**
	 * given an input string, returns the substring of everything inside the outermost two 
	 * quotation marks in the input string. ie, returns the part of that string that starts
	 * immediately after the first quotation mark, and ends immediately before the last 
	 * quotation mark (quotation marks not included in the returned string). if the text 
	 * does not contain at least two quotation marks, null is returned
	 * @param string the string to extract text from
	 * @return the substring of the given string that starts immediately after the first 
	 * quotation mark and ends immediately before the last quotation mark, or null if the
	 * string does not contain at least two quotation marks
	 */
	public static String extractQuotationMarks(String string) {
		if(!hasQuotationMarks(string)) return null;
		
		string = string.split("\"", 2)[1];
		return string.substring(0, string.lastIndexOf('"'));
	}
	
	/**
	 * simple wrapper for Guild.getRoleById(String) that returns null instead of throwing
	 * NumberFormatException when the given id can't be parsed by Long.parseLong(String)
	 * @param guild the guild to try to retrieve a role from
	 * @param id the id of the role to try to retrieve
	 * @return the role with the given id in the given guild if it exists, or null if
	 * either no such role exists or the given id can't be parsed by Long.parseLong(String)
	 */
	public static Role tryRoleById(Guild guild, String id) {
		try {
			return guild.getRoleById(id);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	/**
	 * attempts to match a Pattern against a given input String and return the match, with a 
	 * set maximum execution time. if no match is found, null is returned, and if the execution
	 * time is exceeded, an exception is thrown
	 * @param pattern the regex to search for in the given text
	 * @param input the text to search in
	 * @param timeout the amount of time to look for
	 * @param unit the unit for the timeout
	 * @return the part of the input text that matched the pattern if a match was found, or null
	 * if no match was found
	 * @throws TimeoutException if the time spent searching the text for the given pattern exceeded
	 * the given timeout duration
	 */
	public static String findMatchWithTimeout(Pattern pattern, String input, long timeout, TimeUnit unit) throws TimeoutException {
		Matcher m = pattern.matcher(input);
		CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
			if(m.matches()) {
				return m.group();
			} else {
				return null;
			}
		});
		try {
			return future.get(timeout, unit);
		} catch (InterruptedException | ExecutionException e) {
			//i think these shouldn't happen with this future?
			throw new AssertionError(e);
		}
	}
	
	/**
	 * searches a string for any text surrounded by quotation marks. if two quotation marks are found,
	 * the text inside the quotation marks is split by commas, and the result is returned as an array. eg,
	 * calling this method with the text 
	 * <p>
	 * hello i am well but i want to say "how, are you doing today, my, fellow?" to you
	 * <p>
	 * would return an array with<br>
	 * array[0].equals("how")<br>
	 * array[1].equals("are you doing today")<br>
	 * array[2].equals("my")<br>
	 * array[3].equals("fellow?")
	 * <p>
	 * this is used especially for extracting words to use for wordfilter (eg see PrivateChannelFilterCommand), 
	 * hence the name of the method
	 * <p>
	 * note if the text does not contain two quotation marks, an empty array is returned
	 * @param text the text to parse as specified above
	 * @return an array containing the comma-separated strings inside the quotation marks in the given text
	 */
	public static String[] parseWordsToFilter(String text) {
		if((text = extractQuotationMarks(text)) == null) {
			return new String[0];
		}
		
		return text.split(",");
	}
	
}
