package my.cute.bot.commands;

public enum PermissionLevel {

	USER(1),
	ADMIN(2),
	DEVELOPER(3);
	
	private int level;
	
	PermissionLevel(int level) {
		this.level = level;
	}
	
	public boolean greaterThanOrEquals(PermissionLevel other) {
		return this.level >= other.level;
	}
}
