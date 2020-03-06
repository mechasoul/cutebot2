package my.cute.bot.tasks;

import java.io.BufferedReader;
import java.io.IOException;
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
			try {
				this.db.clear();
				try (Stream<Path> scrapeFiles = Files.list(PathUtils.getDatabaseScrapeDirectory(this.id))) {
					scrapeFiles.forEach(file ->
					{
						String channelId = file.getFileName().toString().split("\\.")[0].intern();
						if(this.prefs.isDiscussionChannel(channelId)) {
							try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
								/*
								 * first two lines of a scrape file are the start and end times of the scraped
								 * message period, so ignore those
								 */
								reader.readLine();
								reader.readLine();
								String line = reader.readLine();
								while(line != null) {
									//first 8 characters of each line are a datestamp. ignore
									this.db.processLine(line.substring(8));
									line = reader.readLine();
								}
							} catch (IOException e) {
								logger.error(this + ": IOException when trying to process scraped files, aborting. ex: " + e, e);
							}
						}
					});
				}
			} catch (IOException e) {
				//TODO
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
