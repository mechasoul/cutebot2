package my.cute.bot.database;

import java.io.File;

import net.dv8tion.jda.api.entities.Guild;

public class GuildDatabaseBuilder {
	
	private static final String DATABASE_PARENT_DIRECTORY = "." + File.separator + "cutebot2db";
	
	private final String id;

	public GuildDatabaseBuilder(Guild guild) {
		this.id = guild.getId().intern();
	}
	
	public GuildDatabaseBuilder(String id) {
		this.id = id;
	}

	public GuildDatabase build() {
		return new GuildDatabaseImpl(this);
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getParentPath() {
		return DATABASE_PARENT_DIRECTORY;
	}
}
