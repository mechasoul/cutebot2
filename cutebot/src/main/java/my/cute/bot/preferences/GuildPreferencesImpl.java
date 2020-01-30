package my.cute.bot.preferences;

class GuildPreferencesImpl implements GuildPreferences {

	private String commandPrefix;
	
	public GuildPreferencesImpl() {
		this.commandPrefix = "!";
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

	
}
