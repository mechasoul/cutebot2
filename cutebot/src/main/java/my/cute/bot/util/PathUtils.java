package my.cute.bot.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathUtils {

	private static final String DATABASE_PARENT_DIRECTORY = "." + File.separator + "cutebot2db";
	
	public static String getDatabaseParentPath() {
		return DATABASE_PARENT_DIRECTORY;
	}
	
	public static Path getDatabaseScrapeDirectory(String databaseId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + databaseId + File.separator + "~scrape");
	}
	
	public static Path getPreferencesFile(String guildId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + "preferences.ini");
	}
	
}
