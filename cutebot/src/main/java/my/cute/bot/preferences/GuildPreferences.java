package my.cute.bot.preferences;

import java.util.List;

import com.google.common.collect.ImmutableList;

public interface GuildPreferences {

	public void save();
	
	/*
	 * nonempty nonwhitespace
	 */
	public String getPrefix();
	
	public void setPrefix(String prefix);
	
	public int getDatabaseAge();
	
	public void setDatabaseAge(int age);
	
	public boolean isDiscussionChannel(String channelId);
	
	public ImmutableList<String> getDiscussionChannels();
	
	public void setDiscussionChannels(List<String> discussionChannels);
	
	public void setAutomaticResponseTime(int minutes);
	
	public int getAutomaticResponseTime();
}