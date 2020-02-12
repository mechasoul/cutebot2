package my.cute.bot.preferences;

public class GuildPreferencesFactory {

	public static GuildPreferences newDefaultGuildPreferences(String id) {
		return new GuildPreferencesImpl(id);
	}
}
