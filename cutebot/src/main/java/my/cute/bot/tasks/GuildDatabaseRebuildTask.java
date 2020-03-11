package my.cute.bot.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.database.GuildDatabase;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.PathUtils;

public final class GuildDatabaseRebuildTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildDatabaseRebuildTask.class);
	
	private final String id;
	private final GuildDatabase db;
	private final GuildPreferences prefs;
	
	public GuildDatabaseRebuildTask(String id, GuildDatabase db, GuildPreferences prefs) {
		this.id = id;
		this.db = db;
		this.prefs = prefs;
	}
	
	/*
	 * TODO
	 * mark backups as out of date when rebuild occurs?
	 */

	/*
	 * replaces the contents of the given GuildDatabase with the contents of the existing
	 * scrape files for the database, using only the channels that are designated as discussion
	 * channels by the given GuildPreferences
	 */
	@Override
	public void run() {
		/*
		 * lock on db. db object can't be used for line processing, generation, maintenance,
		 * etc while rebuild is in process
		 */
		synchronized(this.db) {
			logger.info(this + ": starting");
			try {
				logger.info(this + ": clearing database");
				this.db.clear();
				logger.info(this + ": finished clearing database. constructing new database for speed");
				this.db.prioritizeSpeed();
				try (Stream<Path> scrapeFiles = Files.list(PathUtils.getDatabaseScrapeDirectory(this.id))) {
					scrapeFiles.forEach(file ->
					{
						String channelId = file.getFileName().toString().split("\\.")[0].intern();
						if(this.prefs.isDiscussionChannel(channelId)) {
							logger.info(this + ": processing channel " + channelId);
							try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
								/*
								 * first two lines of a scrape file are the start and end times of the scraped
								 * message period, so ignore those
								 */
								reader.readLine();
								reader.readLine();
								String line = reader.readLine();
								while(line != null) {
									//first 8 characters of each line are a datestamp for that message
									this.db.processLineWithDate(line.substring(8), line.substring(0, 8));
									line = reader.readLine();
								}
								logger.info(this + ": finished channel " + channelId);
							} catch (IOException e) {
								logger.warn(this + ": IOException when trying to process scraped files, aborting. ex: " + e, e);
								throw new UncheckedIOException(e);
							}
						}
					});
				}
				logger.info(this + ": finished processing. constructing new database for memory");
				this.db.prioritizeMemory();
				logger.info(this + ": complete");
			} catch (IOException e) {
				logger.warn(this + ": encountered IOException, db may be in inconsistent state! ex: " + e, e);
				throw new UncheckedIOException(e);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GuildDatabaseRebuildTask-");
		builder.append(id);
		return builder.toString();
	}

}
