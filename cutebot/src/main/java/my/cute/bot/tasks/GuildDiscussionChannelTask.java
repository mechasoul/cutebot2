package my.cute.bot.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.PathUtils;

public final class GuildDiscussionChannelTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(GuildDiscussionChannelTask.class);
	
	private final String guildId;
	private final GuildPreferences prefs;
	
	public GuildDiscussionChannelTask(String id, GuildPreferences prefs) {
		this.guildId = id;
		this.prefs = prefs;
	}
	
	@Override
	public void run() {
		
		//TODO params
		ConcurrentHashMap<String, Long> lineCounts = new ConcurrentHashMap<>();
		ConcurrentHashMap<String, Long> durations = new ConcurrentHashMap<>();
		
		AtomicLong totalDuration = new AtomicLong(0);
		try (Stream<Path> files = Files.list(PathUtils.getDatabaseScrapeDirectory(this.guildId))) {
			
			files.forEach(file -> 
			{
				long lineCount=0;
				String channelId = file.getFileName().toString().split("\\.")[0].intern();
				try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8);
						BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					lineCount = lines.count() - 2;
					long duration = Duration.between(ZonedDateTime.parse(reader.readLine(), DateTimeFormatter.ISO_ZONED_DATE_TIME), 
							ZonedDateTime.parse(reader.readLine(), DateTimeFormatter.ISO_ZONED_DATE_TIME))
							.abs().toMillis();
					durations.put(channelId, duration);
					totalDuration.addAndGet(duration);
				} catch (IOException e) {
					e.printStackTrace();
					logger.error(this + ": IOException when trying to process scraped files, aborting. ex: " + e, e);
				}
				lineCounts.put(channelId, lineCount);
			});
			
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(this + ": IOException when trying to process scraped files, aborting. ex: " + e, e);
		}
		
		long totalMessageCount = lineCounts.values().stream().mapToLong(i -> i.longValue()).sum();
		final double totalMessagesPerSecond = (double)totalMessageCount / (totalDuration.get() / 1000);
		
		List<String> discussionChannels = new ArrayList<>(10);
		
		lineCounts.entrySet().forEach(entry ->
		{
			Long duration = durations.get(entry.getKey());
			if(duration != null && ((double)entry.getValue() / (duration / 1000)) >= totalMessagesPerSecond) {
				discussionChannels.add(entry.getKey());
			}
		});
		
		this.prefs.setDiscussionChannels(discussionChannels);
		this.prefs.save();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GuildDiscussionChannelTask-");
		builder.append(guildId);
		return builder.toString();
	}

}
