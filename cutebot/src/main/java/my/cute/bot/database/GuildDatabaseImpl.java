package my.cute.bot.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.PathUtils;
import my.cute.markov2.MarkovDatabase;
import my.cute.markov2.exceptions.FollowingWordRemovalException;
import my.cute.markov2.impl.MarkovDatabaseBuilder;

public class GuildDatabaseImpl implements GuildDatabase {

	private static final Logger logger = LoggerFactory.getLogger(GuildDatabaseImpl.class);
	private static final long TIME_BETWEEN_MAINTENANCE = TimeUnit.HOURS.toMillis(12);
	
	private final String id;
	private final Path workingSet;
	private BufferedWriter workingSetWriter;
	
	private final ImmutableList<BackupRecord> backupRecords;
	private final MarkovDatabase database;
	private final LineGenerator lineGenerator;
	
	/*
	 * maximum time for a line to be kept in the working set, in days
	 */
	private int workingSetMaxAge;
	private boolean isShutdown = false;
	
	@SuppressWarnings("unused")
	private GuildDatabaseImpl() {
		this.lineGenerator = null;
		this.id = null;
		this.workingSet = null;
		this.workingSetMaxAge = 0;
		this.backupRecords = null;
		this.database = null;
	};
	
	GuildDatabaseImpl(GuildDatabaseBuilder builder) {
		this.id = builder.getId();
		this.workingSet = PathUtils.getWorkingSetFile(this.id);
		this.workingSetMaxAge = builder.getDatabaseAge();
		this.backupRecords = ImmutableList.<BackupRecord>builderWithExpectedSize(3)
				.add(new BackupRecord(this.id, "daily", TimeUnit.DAYS, 1))
				.add(new BackupRecord(this.id, "weekly", TimeUnit.DAYS, 7))
				.add(new BackupRecord(this.id, "monthly", TimeUnit.DAYS, 31))
				.build();
		if(builder.isPrioritizeSpeed()) {
			this.database = new MarkovDatabaseBuilder(this.id, PathUtils.getDatabaseParentPath())
					.shardCacheSize(800)
					.build();
		} else {
			this.database = new MarkovDatabaseBuilder(this.id, PathUtils.getDatabaseParentPath())
					.shardCacheSize(0)
					.fixedCleanupThreshold(100)
					.build();
		}
		
		this.lineGenerator = new SpookyLineGenerator(this.database);
		
		try {
			this.workingSet.toFile().createNewFile();
			this.workingSetWriter = Files.newBufferedWriter(this.workingSet, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			//TODO rethrow this. shouldnt continue when workingset is broken
			logger.error(this + ": exception in constructor when during workingset setup, no workingset saved for this session! "
					+ "exception: " + e.getMessage());
		}
	}

	@Override
	public synchronized boolean processLine(String line) throws IllegalStateException {
		if(this.isShutdown) throw new IllegalStateException("can't process lines on a shutdown database");
		
		line = MiscUtils.replaceNewLinesWithTokens(line);
		if(this.database.processLine(tokenize(line))) {
			try {
				this.workingSetWriter.append(getDateStamp() + line);
				this.workingSetWriter.newLine();
				this.workingSetWriter.flush();
			} catch (IOException e) {
				logger.error(this + ": exception when trying to write line '" + line + "' to workingset: " + e.getMessage());
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized String generateLine() {
		return MiscUtils.replaceNewLineTokens(this.lineGenerator.generateLine());
	}

	@Override
	public synchronized String generateLine(String startWord) {
		return MiscUtils.replaceNewLineTokens(this.lineGenerator.generateLine(startWord));
	}

	@Override
	public synchronized boolean removeLine(String line) {
		if(this.isShutdown) throw new IllegalStateException("can't remove line from shutdown database");
		
		//any lines passed to removeLine should come from database (eg workingset) and already be sanitized
		//so no need to call MiscUtils.replaceNewLinesWithTokens() on line before processing it
		try {
			return this.database.removeLine(tokenize(line));
		} catch (FollowingWordRemovalException e) {
			logger.error(this.toString() + ": exception thrown during line removal. line: '" + line
				+ "', ex: " + e.getMessage());
			return false;
		}
	}

	@Override
	public synchronized void save() {
		this.database.save();
	}

	@Override
	public synchronized void load() {
		this.database.load();
	}

	@Override
	public synchronized Path saveBackup(String backupName) throws IOException {
		return this.database.saveBackup(backupName);
	}

	@Override
	public synchronized void loadBackup(String backupName) throws FileNotFoundException, IOException {
		this.database.loadBackup(backupName);
	}

	@Override
	public void deleteBackup(String backupName) throws IOException {
		this.database.deleteBackup(backupName);
	}
	
	@Override
	public synchronized void maintenance() {
		if(this.isShutdown) throw new IllegalStateException("can't start maintenance on a shutdown database");
		logger.info(this + ": starting maintenance");
		this.save();
		logger.info(this + "-maint: db saved. beginning workingset maintenance");
		/*
		 * update workingset.txt
		 */
		try {
			this.workingSetWriter.close();
			Path tempWorkingSet = Files.createTempFile(this.workingSet.getParent(), "workingset", null);
			try (BufferedWriter writer = Files.newBufferedWriter(tempWorkingSet, StandardCharsets.UTF_8, StandardOpenOption.CREATE, 
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
				 Stream<String> lines = Files.lines(this.workingSet, StandardCharsets.UTF_8)) {
				lines.filter(line -> !line.isEmpty())
				.forEach(line -> 
				{
					if(isExpired(line)) {
						//first 8 characters of every line in workingset is a date stamp added in, so ignore that
						this.removeLine(line.substring(8));
					} else {
						try {
							writer.append(line);
							writer.newLine();
						} catch (IOException e) {
							logger.error(this + ": exception in maintenance() when writing line '" + line + "' to tempWorkingSet: "
									+ e.getMessage());
						}
					}
				});
			} 
			logger.info(this + "-maint: workingset processed. replacing old workingset");
			Files.move(tempWorkingSet, this.workingSet, StandardCopyOption.REPLACE_EXISTING);
			this.save();
			this.workingSetWriter = Files.newBufferedWriter(this.workingSet, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} catch (IOException ex) {
			logger.error(this + ": exception in maintenance() when updating workingset: "
					+ ex.getMessage());
			ex.printStackTrace();
		}
		
		/*
		 * check for automatic backup creation
		 */
		logger.info(this + "-maint: finished workingset maintenance. beginning backup maintenance");
		this.backupRecords.forEach(record ->
		{
			if(record.needsMaintenance()) {
				try {
					logger.info(this + "-maint: backup record '" + record.getName() + "' out of date. saving new backup");
					this.saveBackup(record.getName());
					record.maintenance();
				} catch (IOException e) {
					logger.error(this + ": exception in maintenance() when trying to save backup '" 
							+ record.getName() + "': " + e.getMessage());
				}
			}
		});
		logger.info(this + "-maint: finished backup maintenance. updating last maintenance time");
		//TODO problem with updating this when potentially encountering exceptions?
		//maybe shouldnt update last maint time in that case?
		this.updateLastMaintenanceTime();
		logger.info(this + ": finished maintenance");
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
		return ChronoUnit.DAYS.between(LocalDate.parse(databaseLine.substring(0, 8), 
				DateTimeFormatter.BASIC_ISO_DATE), LocalDate.now()) >= this.workingSetMaxAge;
	}
	
	/*
	 * should this throw IOException instead of logging and swallowing?
	 */
	@Override
	public boolean needsMaintenance() {
		try (BufferedReader reader = Files.newBufferedReader(PathUtils.getDatabaseLastMaintenanceFile(this.id), StandardCharsets.UTF_8)) {
			return Duration.between(ZonedDateTime.parse(reader.readLine(), DateTimeFormatter.ISO_DATE_TIME), 
					ZonedDateTime.now(MiscUtils.TIMEZONE)).toMillis() >= TIME_BETWEEN_MAINTENANCE;
		} catch (NoSuchFileException e) {
			//probably first run. run maintenance
			return true;
		} catch (IOException e) {
			logger.error(this + ": exception when checking if it's time for maintenance: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		
	}
	
	private void updateLastMaintenanceTime() {
		try {
			Files.write(PathUtils.getDatabaseLastMaintenanceFile(this.id), ZonedDateTime.now(MiscUtils.TIMEZONE)
					.format(DateTimeFormatter.ISO_DATE_TIME).getBytes(StandardCharsets.UTF_8), 
					StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.error(this + ": exception when updating last maintenance time: " + e.getMessage());
		}
	}
	
	@Override
	public synchronized void exportToText() {
		this.database.exportToTextFile();
	}
	
	@Override
	public synchronized void shutdown() {
		this.save();
		try {
			this.workingSetWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.isShutdown = true;
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
		builder.append("GuildDatabaseImpl-");
		builder.append(id);
		return builder.toString();
	}

	private static List<String> tokenize(String line) {
		return Arrays.asList(StringUtils.split(line, null));
	}
	
	private static String getDateStamp() {
		return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
	}
}
