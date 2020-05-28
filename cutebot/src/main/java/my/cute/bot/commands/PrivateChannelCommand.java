package my.cute.bot.commands;

abstract class PrivateChannelCommand extends Command {

	private final boolean requiresTargetGuild;
	
	PrivateChannelCommand(String name, PermissionLevel permission, int min, int max, boolean targetGuild) {
		super(name, permission, min, max);
		this.requiresTargetGuild = targetGuild;
	}
	
	public boolean requiresTargetGuild() {
		return this.requiresTargetGuild;
	}

}
