package my.cute.bot.commands;

abstract class TextChannelCommand extends Command {
	
	TextChannelCommand(String name, PermissionLevel permission, int min, int max) {
		super(name, permission, min, max);
	}

}
