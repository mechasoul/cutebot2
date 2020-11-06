package my.cute.bot.commands;

public abstract class PrivateChannelCommand extends CommandImpl {

	/* 
	 * some commands sent to cutebot via private channel will affect a specific guild
	 * (especially commands used by admins), so the user must identify a target guild
	 * in order to execute the command. these commands must be handled a bit differently
	 * (eg we need to obtain the target guild from their message, give an error if one
	 * isn't supplied, etc), so we use this to check if the command works this way
	 * 
	 * if it is such a command, we check for 1) an explicitly provided target guild
	 * (all commands that require a target guild will check for one provided in the
	 * last argument given by the user), 2) a set "default guild" for the given user -
	 * if a default guild is set, then all commands that require a target guild but 
	 * don't explicitly provide one will be directed to the default guild. if a user
	 * is only in one cutebot guild, that should be set to their default guild, 
	 * otherwise it must be set by the user
	 */
	private final boolean requiresTargetGuild;
	
	PrivateChannelCommand(String name, PermissionLevel permission, int min, int max, boolean targetGuild) {
		super(name, permission, min, max);
		this.requiresTargetGuild = targetGuild;
	}
	
	public boolean requiresTargetGuild() {
		return this.requiresTargetGuild;
	}

}
