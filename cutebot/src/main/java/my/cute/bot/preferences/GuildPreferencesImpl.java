package my.cute.bot.preferences;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class GuildPreferencesImpl implements GuildPreferences {

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
	
	public GuildPreferencesImpl(String id) {
		this.id = id;
		this.commandPrefix = "!";
		this.databaseAge = 2;
		this.discussionChannels = null;
	}
	
	@Override
	public void load() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getPrefix() {
		return this.commandPrefix;
	}

	@Override
	public void setPrefix(String prefix) {
		this.commandPrefix = prefix;
	}

	@Override
	public int getDatabaseAge() {
		return this.databaseAge;
	}

	@Override
	public void setDatabaseAge(int age) {
		this.databaseAge = age;
	}
	
	@Override
	public boolean isDiscussionChannel(String channelId) {
		if(this.discussionChannels == null) {
			return true;
		} else {
			return this.discussionChannels.contains(channelId);
		}
	}

	@Override
	public void setDiscussionChannels(List<String> discussionChannels) {
		if(discussionChannels == null || discussionChannels.isEmpty()) {
			this.discussionChannels = null;
		} else {
			this.discussionChannels = ImmutableList.copyOf(discussionChannels);
		}
	}
	
}
