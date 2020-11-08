package my.cute.bot.commands;

public abstract class PrivateChannelCommand extends CommandImpl {

	/*
	 * base class for commands executed via a private channel
	 * commands that don't require a target guild parameter should use this
	 * as their base class (see PrivateChannelCommandTargeted for more)
	 */
	protected PrivateChannelCommand(String name, PermissionLevel permission, int min, int max) {
		super(name, permission, min, max);
	}

}
