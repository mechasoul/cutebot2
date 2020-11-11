package my.cute.bot.commands;

import java.io.IOException;

import gnu.trove.map.TLongLongMap;

class DefaultGuildDatabaseImpl implements DefaultGuildDatabase {
	
	private final TLongLongMap defaultGuilds;
	
	public DefaultGuildDatabaseImpl() throws IOException {
		this.defaultGuilds = null;
		
	}

	@Override
	public String getDefaultGuildId(String userId) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String setDefaultGuildId(String userId, String guildId) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String clearDefaultGuildId(String userId) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
