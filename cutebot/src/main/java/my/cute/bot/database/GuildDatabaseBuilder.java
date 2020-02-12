package my.cute.bot.database;

import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.entities.Guild;

public class GuildDatabaseBuilder {
	
	private final String id;
	private final String parentPath;
	
	private boolean prioritizeSpeed = false;
	private int databaseAge = 2;

	public GuildDatabaseBuilder(Guild guild) {
		this.id = guild.getId().intern();
		this.parentPath = PathUtils.getDatabaseParentPath();
	}
	
	public GuildDatabaseBuilder(String id) {
		this.id = id.intern();
		this.parentPath = PathUtils.getDatabaseParentPath();
	}
	
	public GuildDatabaseBuilder prioritizeSpeed(boolean enabled) {
		this.prioritizeSpeed = enabled;
		return this;
	}
	
	public GuildDatabaseBuilder databaseAge(int age) {
		this.databaseAge = age;
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
	
	public int getDatabaseAge() {
		return this.databaseAge;
	}
}
