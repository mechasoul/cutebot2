package my.cute.bot.database;

import java.io.File;

import net.dv8tion.jda.api.entities.Guild;

public class GuildDatabaseBuilder {
	
	private static final String DATABASE_PARENT_DIRECTORY = "." + File.separator + "cutebot2db";
	
	private final String id;
	
	private boolean prioritizeSpeed = false;

	public GuildDatabaseBuilder(Guild guild) {
		this.id = guild.getId().intern();
	}
	
	public GuildDatabaseBuilder(String id) {
		this.id = id;
	}
	
	public GuildDatabaseBuilder prioritizeSpeed(boolean enabled) {
		this.prioritizeSpeed = enabled;
		return this;
	}

	public GuildDatabase build() {
		return new GuildDatabaseImpl(this);
	}
	
	public String getId() {
		return this.id;
	}
	
	public boolean isPrioritizeSpeed() {
		return this.prioritizeSpeed;
	}
	
	public String getParentPath() {
		return DATABASE_PARENT_DIRECTORY;
	}
}
