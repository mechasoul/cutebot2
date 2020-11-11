package my.cute.bot.commands;

import java.io.IOException;

interface DefaultGuildDatabase {

	String getDefaultGuildId(String userId) throws IOException;
	
	String setDefaultGuildId(String userId, String guildId) throws IOException;
	
	String clearDefaultGuildId(String userId) throws IOException;
	
}
