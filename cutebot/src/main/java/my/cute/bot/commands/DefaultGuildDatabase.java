package my.cute.bot.commands;

import java.io.IOException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public interface DefaultGuildDatabase {
	
	public static class Loader {
		private Loader() {
		}
		
		public static DefaultGuildDatabase createOrLoad() throws IOException {
			return new DefaultGuildDatabaseImpl();
		}
	}

	String getDefaultGuildId(String userId) throws IOException;
	
	String getDefaultGuildId(User user) throws IOException;
	
	String setDefaultGuildId(String userId, String guildId) throws IOException;
	
	String setDefaultGuildId(User user, Guild guild) throws IOException;
	
	String clearDefaultGuildId(String userId) throws IOException;
	
	String clearDefaultGuildId(User user) throws IOException;
	
}
