package my.cute.bot.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.utils.cache.CacheView;

public class MiscUtils {
	
	public static final ZoneId TIMEZONE = ZoneId.of("America/Vancouver");
	private static final Logger logger = LoggerFactory.getLogger(MiscUtils.class);
	private static final String NEW_LINE_TOKEN = "<_NL>";
	private static final Random RAND = new Random();

	public static String replaceNewLinesWithTokens(String line) {
		return line.trim().replace(NEW_LINE_TOKEN, "newline").replace("\n", NEW_LINE_TOKEN);
	}
	
	public static String replaceNewLineTokens(String line) {
		return line.replace(NEW_LINE_TOKEN, "\n");
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
				return jda.getEmoteById("684796381671850111");
			}
		});
	}

	public static List<String> tokenize(String line) {
		return Arrays.asList(StringUtils.split(line, null));
	}

	public static String getDateStamp() {
		return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
	}
	
}
