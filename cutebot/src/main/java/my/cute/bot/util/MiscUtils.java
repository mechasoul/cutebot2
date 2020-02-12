package my.cute.bot.util;

import java.time.ZoneId;

public class MiscUtils {
	
	public static final ZoneId TIMEZONE = ZoneId.of("America/Vancouver");
	private static final String NEW_LINE_TOKEN = "<_NL>";

	public static String replaceNewLinesWithTokens(String line) {
		return line.trim().replace(NEW_LINE_TOKEN, "newline").replace("\n", NEW_LINE_TOKEN);
	}
	
	public static String replaceNewLineTokens(String line) {
		return line.replace(NEW_LINE_TOKEN, "\n");
	}
	
}
