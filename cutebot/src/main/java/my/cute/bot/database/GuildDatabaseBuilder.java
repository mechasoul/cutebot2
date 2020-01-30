package my.cute.bot.database;

import net.dv8tion.jda.api.entities.Guild;

public class GuildDatabaseBuilder {
	
	private final String id;
	private final String parentPath;
	
	private boolean prioritizeSpeed = false;

	public GuildDatabaseBuilder(Guild guild, String parentPath) {
		this.id = guild.getId().intern();
		this.parentPath = parentPath;
	}
	
	public GuildDatabaseBuilder(String id, String parentPath) {
		this.id = id.intern();
		this.parentPath = parentPath;
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
		return this.parentPath;
	}
}
