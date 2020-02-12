package my.cute.bot.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.MiscUtils;

public class BackupRecord implements Maintainable {
	
	private static final Logger logger = LoggerFactory.getLogger(BackupRecord.class);

	private final String parentId;
	private final long duration;
	private final String name;
	private final Path lastMaintenanceFile;
	
	BackupRecord(String id, String name, TimeUnit unit, int duration, Path parentPath) {
		this.parentId = id;
		this.name = name;
		this.duration = unit.toMillis(duration);
		this.lastMaintenanceFile = parentPath.resolve("~backups" + File.separator + this.name + "-last.txt");
	}
	
	@Override
	public boolean needsMaintenance() {
		try (BufferedReader reader = Files.newBufferedReader(this.lastMaintenanceFile, StandardCharsets.UTF_8)) {
			return Duration.between(ZonedDateTime.parse(reader.readLine(), DateTimeFormatter.ISO_DATE_TIME), 
					ZonedDateTime.now(MiscUtils.TIMEZONE)).toMillis() >= this.duration;
		} catch (NoSuchFileException ex) {
			//probably first run. should create backup
			return true;
		} catch (IOException e) {
			logger.error(this + ": exception when checking needsMaintenance(): " + e.getMessage());
			return false;
		}
	}

	@Override
	public void maintenance() {
		try {
			Files.write(this.lastMaintenanceFile, ZonedDateTime.now(MiscUtils.TIMEZONE).format(DateTimeFormatter.ISO_DATE_TIME)
					.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	String getName() {
		return this.name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parentId == null) ? 0 : parentId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof BackupRecord))
			return false;
		BackupRecord other = (BackupRecord) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BackupRecord-");
		builder.append(parentId);
		builder.append("-");
		builder.append(name);
		return builder.toString();
	}
	
	

	
}
