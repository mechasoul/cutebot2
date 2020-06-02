package my.cute.bot.commands;

abstract class PrivateChannelCommand extends Command {

	/* 
	 * some commands sent to cutebot via private channel will affect a specific guild
	 * (especially commands used by admins), so for user convenience, users can specify
	 * a default guild to be used if they omit the guild ID from a command (see 
	 * PrivateChannelDefaultCommand for details). this flag indicates whether the command
	 * works this way; if canUseDefaultGuild is true, then when obtaining the target guild
	 * for the given command, code should first check to see if a guild was specifically 
	 * provided by the user, and if not, their default guild should be used (if no default 
	 * guild is provided then return an error).
	 * 
	 * default guilds go where? user preferences, separate permissions file?
	 */
	private final boolean canUseDefaultGuild;
	
	PrivateChannelCommand(String name, PermissionLevel permission, int min, int max, boolean targetGuild) {
		super(name, permission, min, max);
		this.canUseDefaultGuild = targetGuild;
	}
	
	public boolean canUseDefaultGuild() {
		return this.canUseDefaultGuild;
	}

}
