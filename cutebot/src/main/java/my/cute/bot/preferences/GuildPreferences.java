package my.cute.bot.preferences;

import java.util.List;

public interface GuildPreferences {

	public void load();
	
	/*
	 * nonempty nonwhitespace
	 */
	public String getPrefix();
	
	public void setPrefix(String prefix);
	
	public int getDatabaseAge();
	
	public void setDatabaseAge(int age);
	
	public boolean isDiscussionChannel(String channelId);
	
	public void setDiscussionChannels(List<String> discussionChannels);
}