package my.cute.bot.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
	private static final String LAST_MAINTENANCE_FILE_NAME = "lastmaintenance.txt";
	private static final long TIME_BETWEEN_MAINTENANCE = TimeUnit.HOURS.toMillis(24);
	
	private final String id;
	private final String parentPath;
	private final Path workingSet;
	private BufferedWriter workingSetWriter;
	/*
	 * maximum time for a line to be kept in the working set, in days
	 */
	private final long workingSetMaxAge;
	private final List<BackupRecord> backupRecords;
	private final MarkovDatabase database;
	private final LineGenerator lineGenerator;
	
	@SuppressWarnings("unused")
	private GuildDatabaseImpl() {
		this.lineGenerator = null;
		this.id = null;
		this.parentPath = null;
		this.workingSet = null;
		this.workingSetMaxAge = 0;
		this.backupRecords = null;
		this.database = null;
	};
	
	GuildDatabaseImpl(GuildDatabaseBuilder builder) {
		this.id = builder.getId();
		this.parentPath = builder.getParentPath();
		this.workingSet = Paths.get(this.parentPath + File.separator + this.id + File.separator + "workingset.txt");
		this.workingSetMaxAge = 2;
		this.backupRecords = new ArrayList<BackupRecord>(4);
		this.backupRecords.add(new BackupRecord(this.id, "daily", TimeUnit.DAYS, 1, Paths.get(this.parentPath + File.separator + this.id)));
		this.backupRecords.add(new BackupRecord(this.id, "weekly", TimeUnit.DAYS, 7, Paths.get(this.parentPath + File.separator + this.id)));
		this.backupRecords.add(new BackupRecord(this.id, "monthly", TimeUnit.DAYS, 31, Paths.get(this.parentPath + File.separator + this.id)));
		if(builder.isPrioritizeSpeed()) {
			this.database = new MarkovDatabaseBuilder(this.id, this.parentPath)
					.shardCacheSize(800)
					.build();
		} else {
			this.database = new MarkovDatabaseBuilder(this.id, this.parentPath)
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
			logger.error(this + ": exception in constructor when during workingset setup, no workingset saved for this session! "
					+ "exception: " + e.getMessage());
		}
	}

	@Override
	public boolean processLine(String line) {
		if(this.database.processLine(tokenize(line))) {
			try {
				this.workingSetWriter.append(getDateStamp() + line);
				this.workingSetWriter.newLine();
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
	public String generateLine() {
		return this.lineGenerator.generateLine();
	}

	@Override
	public String generateLine(String startWord) {
		return this.lineGenerator.generateLine(startWord);
	}

	@Override
	public boolean removeLine(String line) {
		try {
			return this.database.removeLine(tokenize(line));
		} catch (FollowingWordRemovalException e) {
			logger.error(this.toString() + ": exception thrown during line removal. line: '" + line
				+ "', ex: " + e.getMessage());
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
	
	/*
	 * TODO 
	 * this is called in other thread
	 * make sure theres no concurrency issue
	 */
	@Override
	public void maintenance() {
		this.save();
		
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
		this.backupRecords.forEach(record ->
		{
			if(record.needsMaintenance()) {
				try {
					this.saveBackup(record.getName());
					record.maintenance();
				} catch (IOException e) {
					logger.error(this + ": exception in maintenance() when trying to save backup '" 
							+ record.getName() + "': " + e.getMessage());
				}
			}
		});
		//TODO problem with updating this when potentially encountering exceptions?
		//maybe shouldnt update last maint time in that case?
		this.updateLastMaintenanceTime();
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
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(this.parentPath + File.separator + this.id + File.separator 
				+ LAST_MAINTENANCE_FILE_NAME), StandardCharsets.UTF_8)) {
			return Duration.between(ZonedDateTime.parse(reader.readLine(), DateTimeFormatter.ISO_DATE_TIME), 
					ZonedDateTime.now(TIMEZONE)).toMillis() >= TIME_BETWEEN_MAINTENANCE;
		} catch (NoSuchFileException e) {
			//probably first run. create file
			this.updateLastMaintenanceTime();
			return false;
		} catch (IOException e) {
			logger.error(this + ": exception when checking if it's time for maintenance: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		
	}
	
	private void updateLastMaintenanceTime() {
		Path lastMaintenanceFile = Paths.get(this.parentPath + File.separator + this.id + File.separator 
				+ LAST_MAINTENANCE_FILE_NAME);
		try {
			Files.write(lastMaintenanceFile, ZonedDateTime.now(TIMEZONE).format(DateTimeFormatter.ISO_DATE_TIME)
					.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.error(this + ": exception when updating last maintenance time: " + e.getMessage());
		}
	}
	
	@Override
	public void exportToText() {
		this.database.exportToTextFile();
	}
	
	@Override
	public void shutdown() {
		this.save();
		try {
			this.workingSetWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
	}
}
