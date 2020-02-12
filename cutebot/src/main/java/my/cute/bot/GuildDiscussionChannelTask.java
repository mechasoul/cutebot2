package my.cute.bot;

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

import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.PathUtils;

public final class GuildDiscussionChannelTask implements Runnable {

	//shouldn't store references to jda entities but this task should be short-lived so it's ok
	private final String guildId;
	private final GuildPreferences prefs;
	
	public GuildDiscussionChannelTask(String id) {
		this.guildId = id;
		this.prefs = null;
	}
	
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				lineCounts.put(channelId, lineCount);
			});
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		
		System.out.println(discussionChannels);
		this.prefs.setDiscussionChannels(discussionChannels);
	}

}
