package my.cute.bot.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.cache.CacheView;

public class MiscUtils {
	
	public static final ZoneId TIMEZONE = ZoneId.of("America/Vancouver");
	private static final Logger logger = LoggerFactory.getLogger(MiscUtils.class);
	private static final String NEW_LINE_TOKEN = "<_NL>";
	private static final Random RAND = new Random();
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

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
		return message.getContentRaw().trim().split("\\s+");
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
	
}
