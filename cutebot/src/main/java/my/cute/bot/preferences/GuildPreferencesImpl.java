package my.cute.bot.preferences;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import my.cute.bot.util.PathUtils;

class GuildPreferencesImpl implements GuildPreferences, Serializable {

	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(GuildPreferencesImpl.class);
	private static final Gson GSON = new GsonBuilder().create();
	
	private final String id;
	
	private String commandPrefix;
	/*
	 * maximum age of any line in the database, in days. any line older than this
	 * will be removed during maintenance
	 */
	private int databaseAge;
	/*
	 * list of TextChannel ids that are designated "discussion channels". a line will
	 * only be processed into the database if it's from a discussion channel. the goal
	 * is that channels with a particularly specific purpose (bot channels, game channels,
	 * announcement channels, etc) will be excluded, and only channels in which people 
	 * actually have regular conversation will be used to build database, which should
	 * mean cutebot will be more representative of how people actually talk in the guild
	 * 
	 * if this is null, every channel will be considered to be a discussion channel
	 */
	private ImmutableList<String> discussionChannels;
	private int automaticMessageTime = 0;
	
	
	public GuildPreferencesImpl(String id) {
		this.id = id;
		this.commandPrefix = "!";
		this.databaseAge = 1095;
		this.discussionChannels = null;
	}
	
	private synchronized void save() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(PathUtils.getPreferencesFile(this.id), StandardCharsets.UTF_8, 
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(GSON.toJson(this));
		} 
	}
	
	@Override
	public synchronized String getPrefix() {
		return this.commandPrefix;
	}

	@Override
	public synchronized void setPrefix(String prefix) throws IOException {
		this.commandPrefix = prefix;
		this.save();
	}

	@Override
	public synchronized int getDatabaseAge() {
		return this.databaseAge;
	}

	@Override
	public synchronized void setDatabaseAge(int age) throws IOException {
		this.databaseAge = age;
		this.save();
	}
	
	@Override
	public synchronized boolean isDiscussionChannel(String channelId) {
		if(this.discussionChannels == null) {
			return true;
		} else {
			return this.discussionChannels.contains(channelId);
		}
	}
	
	@Override
	public synchronized ImmutableList<String> getDiscussionChannels() {
		return this.discussionChannels;
	}

	@Override
	public synchronized void setDiscussionChannels(Collection<String> discussionChannels) throws IOException {
		if(discussionChannels == null || discussionChannels.isEmpty()) {
			this.discussionChannels = null;
		} else {
			this.discussionChannels = ImmutableList.copyOf(discussionChannels);
		}
		this.save();
	}
	
	@Override
	public synchronized void setAutomaticResponseTime(int minutes) throws IOException {
		this.automaticMessageTime = minutes;
		this.save();
	}

	@Override
	public synchronized int getAutomaticResponseTime() {
		return this.automaticMessageTime;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GuildPreferencesImpl [id=");
		builder.append(id);
		builder.append(", commandPrefix=");
		builder.append(commandPrefix);
		builder.append(", databaseAge=");
		builder.append(databaseAge);
		builder.append(", discussionChannels=");
		builder.append(discussionChannels);
		builder.append("]");
		return builder.toString();
	}
}
