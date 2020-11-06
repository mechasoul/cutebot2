package my.cute.bot.commands;

public abstract class TextChannelCommand extends CommandImpl {
	
	TextChannelCommand(String name, PermissionLevel permission, int min, int max) {
		super(name, permission, min, max);
	}

}
