package my.cute.bot.preferences;

public interface GuildPreferences {

	public void load();
	
	/*
	 * nonempty nonwhitespace
	 */
	public String getPrefix();
	
	public void setPrefix(String prefix);
}