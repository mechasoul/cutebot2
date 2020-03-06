package my.cute.bot.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathUtils {

	private static final String DATABASE_PARENT_DIRECTORY = "." + File.separator + "cutebot2db";
	private static final String WORKING_SET_FILE_NAME = "workingset.txt";
	private static final String LAST_MAINTENANCE_FILE_NAME = "lastmaintenance.txt";
	private static final String DATABASE_DIRECTORY_NAME = "~database";
	private static final String BACKUP_DIRECTORY_NAME = "~backups";
	private static final String SCRAPE_DIRECTORY_NAME = "~scrape";
	private static final String PREFERENCES_FILE_NAME = "preferences.ini";
	
	public static String getDatabaseParentPath() {
		return DATABASE_PARENT_DIRECTORY;
	}
	
	public static Path getDatabaseDirectory(String guildId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + DATABASE_DIRECTORY_NAME);
	}
	
	public static Path getWorkingSetFile(String guildId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + WORKING_SET_FILE_NAME);
	}
	
	public static Path getDatabaseLastMaintenanceFile(String guildId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + LAST_MAINTENANCE_FILE_NAME);
	}
	
	public static Path getBackupLastMaintenanceFile(String guildId, String backupName) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + BACKUP_DIRECTORY_NAME
				+ File.separator + backupName + "-last.txt");
	}
	
	public static Path getDatabaseScrapeDirectory(String databaseId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + databaseId + File.separator + SCRAPE_DIRECTORY_NAME);
	}
	
	public static Path getPreferencesFile(String guildId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + PREFERENCES_FILE_NAME);
	}
	
}
