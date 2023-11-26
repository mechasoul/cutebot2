package my.cute.bot.preferences;

import java.io.IOException;
import java.util.Collection;

import com.google.common.collect.ImmutableList;

public interface GuildPreferences {

	/*
	 * nonempty nonwhitespace
	 */
	public String getPrefix();
	
	public void setPrefix(String prefix) throws IOException;
	
	public int getDatabaseAge();
	
	public void setDatabaseAge(int age) throws IOException;
	
	public boolean isDiscussionChannel(String channelId);
	
	public ImmutableList<String> getDiscussionChannels();
	
	public void setDiscussionChannels(Collection<String> discussionChannels) throws IOException;
	
	public void setAutomaticResponseTime(int minutes) throws IOException;
	
	public int getAutomaticResponseTime();
}