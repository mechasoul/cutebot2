package my.cute.bot.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PathUtils {

	private static final String DATABASE_PARENT_DIRECTORY = "." + File.separator + "cutebot2db";
	private static final String WORKING_SET_FILE_NAME = "workingset.txt";
	private static final String LAST_MAINTENANCE_FILE_NAME = "lastmaintenance.txt";
	private static final String DATABASE_DIRECTORY_NAME = "~database";
	private static final String BACKUP_DIRECTORY_NAME = "~backups";
	private static final String SCRAPE_DIRECTORY_NAME = "~scrape";
	private static final String PREFERENCES_FILE_NAME = "preferences.ini";
	private static final String PERMISSIONS_FILE_NAME = "permissions.db";
	private static final String DEFAULT_GUILDS_FILE_NAME = "defaultguilds.db";
	
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
	
	public static Path getBackupDirectory(String guildId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + BACKUP_DIRECTORY_NAME);
	}
	
	public static Path getBackupLastMaintenanceFile(String guildId, String backupName) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + BACKUP_DIRECTORY_NAME
				+ File.separator + backupName + "-last.txt");
	}
	
	public static Path getBackupWorkingSetFile(String guildId, String backupName) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + BACKUP_DIRECTORY_NAME
				+ File.separator + guildId + "_" + backupName + "_workingset.txt");
	}
	
	public static Path getDatabaseScrapeDirectory(String databaseId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + databaseId + File.separator + SCRAPE_DIRECTORY_NAME);
	}
	
	public static Path getPreferencesFile(String guildId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + PREFERENCES_FILE_NAME);
	}
	
	public static Path getPermissionsFile(String guildId) {
		return Paths.get(DATABASE_PARENT_DIRECTORY + File.separator + guildId + File.separator + PERMISSIONS_FILE_NAME);
	}
	
	public static Path getDefaultGuildsFile() {
		return Paths.get(DEFAULT_GUILDS_FILE_NAME);
	}
	
	public static List<Path> listFilesNewestFirst(Path directory, Pattern regex) throws IOException {
		try (final Stream<File> matchingFiles = Arrays.stream(directory.toFile().listFiles((file, name) -> regex.matcher(name).matches()))) {
			return matchingFiles
					.collect(Collectors.toMap(file -> file, file -> file.lastModified()))
					.entrySet()
					.stream()
					.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
					.map(entry -> entry.getKey())
					.map(file -> file.toPath())
					.collect(Collectors.toList());
		}
	}
	
}
