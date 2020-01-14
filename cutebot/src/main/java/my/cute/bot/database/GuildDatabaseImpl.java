package my.cute.bot.database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.markov2.MarkovDatabase;
import my.cute.markov2.exceptions.FollowingWordRemovalException;
import my.cute.markov2.impl.MarkovDatabaseBuilder;

public class GuildDatabaseImpl implements GuildDatabase {

	private static final Logger logger = LoggerFactory.getLogger(GuildDatabaseImpl.class);
	
	private final String id;
	private final String parentPath;
	private final Path workingSet;
	private final long workingSetMaxAge;
	private final List<BackupRecord> backupRecords;
	private final MarkovDatabase database;
	
	public GuildDatabaseImpl(GuildDatabaseBuilder builder) {
		this.id = builder.getId();
		this.parentPath = builder.getParentPath();
		this.workingSet = Paths.get(this.parentPath + File.separator + this.id + File.separator + "workingset.txt");
		this.workingSetMaxAge = TimeUnit.DAYS.toMillis(2);
		this.backupRecords = new ArrayList<BackupRecord>(4);
		this.backupRecords.add(new BackupRecord("daily", TimeUnit.DAYS, 1));
		this.backupRecords.add(new BackupRecord("weekly", TimeUnit.DAYS, 7));
		this.backupRecords.add(new BackupRecord("monthly", TimeUnit.DAYS, 31));
		this.database = new MarkovDatabaseBuilder(this.id, this.parentPath)
				.shardCacheSize(0)
				.fixedCleanupThreshold(100)
				.build();
	}

	@Override
	public void processLine(String line) {
		this.database.processLine(tokenize(line));
		try {
			Files.write(this.workingSet, (getDateStamp() + line).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			logger.error(this + ": exception when trying to write line to workingset.txt: " + e.getMessage());
		}
	}

	

	@Override
	public String generateLine() {
		return this.database.generateLine();
	}

	@Override
	public String generateLine(String startWord) {
		return this.database.generateLine(startWord);
	}

	@Override
	public boolean removeLine(String line) {
		try {
			return this.database.removeLine(tokenize(line));
		} catch (FollowingWordRemovalException e) {
			logger.warn(this.toString() + ": exception thrown during line removal. line: " + line
				+ ", ex: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void save() {
		this.database.save();
	}

	@Override
	public void load() {
		this.database.load();
	}

	@Override
	public Path saveBackup(String backupName) throws IOException {
		return this.database.saveBackup(backupName);
	}

	@Override
	public void loadBackup(String backupName) throws FileNotFoundException, IOException {
		this.database.loadBackup(backupName);
	}

	@Override
	public void deleteBackup(String backupName) throws IOException {
		this.database.deleteBackup(backupName);
	}
	
	@Override
	public void maintenance() {
		/*
		 * check for automatic backup creation
		 */
		long currentTimeMillis = System.currentTimeMillis();
		this.backupRecords.forEach(record ->
		{
			if(record.shouldSaveBackup(currentTimeMillis)) {
				record.setPrevTime(currentTimeMillis);
				try {
					this.saveBackup(record.getName());
				} catch (IOException e) {
					logger.error(this + ": exception in maintenance when trying to save backup '" 
							+ record.getName() + "': " + e.getMessage());
				}
			}
		});
		
		/*
		 * update workingset.txt
		 */
		try {
			Path tempWorkingSet = Files.createTempFile(this.workingSet.getParent(), "workingset", null);
			try (BufferedWriter writer = Files.newBufferedWriter(tempWorkingSet, StandardCharsets.UTF_8, StandardOpenOption.CREATE, 
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
				 Stream<String> lines = Files.lines(this.workingSet, StandardCharsets.UTF_8)) {
				lines.filter(line -> !line.isEmpty())
					.forEach(line -> 
					{
						if(isExpired(line)) {
							this.removeLine(line.substring(8));
						} else {
							try {
								writer.append(line);
								writer.newLine();
							} catch (IOException e) {
								logger.error(this + ": exception when writing line '" + line + "' to tempWorkingSet: "
										+ e.getMessage());
							}
						}
					});
			} 
			Files.move(tempWorkingSet, this.workingSet, StandardCopyOption.REPLACE_EXISTING);
			Files.delete(tempWorkingSet);
		} catch (IOException ex) {
			logger.error(this + ": exception in maintenance() when updating workingset: "
					+ ex.getMessage());
		}
	}
	
	/*
	 * determine whether to remove a database line from the working set or not
	 * database lines in workingSet all have the date inserted at the beginning
	 * (date as defined by DateTimeFormatter.BASIC_ISO_DATE, which is of the form
	 * YYYYMMDD), so check current date against the date obtained by parsing the 
	 * first 8 characters of the line and workingSetMaxAge
	 * 
	 * returns true if the line is old enough to be considered expired (as determined
	 * by workingSetMaxAge), otherwise false
	 */
	private boolean isExpired(String databaseLine) {
		return Duration.between(LocalDateTime.parse(databaseLine.substring(0, 8), 
				DateTimeFormatter.BASIC_ISO_DATE), LocalDateTime.now()).toMillis() >= this.workingSetMaxAge;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof GuildDatabaseImpl))
			return false;
		GuildDatabaseImpl other = (GuildDatabaseImpl) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GuildDatabaseImpl [id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}

	private static List<String> tokenize(String line) {
		return Arrays.asList(StringUtils.split(line, null));
	}
	
	private static String getDateStamp() {
		return LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
	}
}
