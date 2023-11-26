package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

public abstract class PrivateChannelCommandTargeted extends PrivateChannelCommand {

	/* 
	 * some commands sent to cutebot via private channel will affect a specific guild
	 * (especially commands used by admins), so the user must identify a target guild
	 * in order to execute the command. these commands must be handled a bit differently
	 * (eg we need to obtain the target guild from their message, give an error if one
	 * isn't supplied, etc), so this class should be the base for such commands
	 * 
	 * note target guild is counted as a part of parameter count
	 */
	
	PrivateChannelCommandTargeted(String name, String description, PermissionLevel permission, int min, int max) {
		super(name, description, permission, min, max);
	}

	
	/*
	 * require a specific target guild on execution, so we use a slightly different execute
	 * method
	 */
	public abstract void execute(Message message, String[] params, Guild targetGuild);

	@Override
	public void execute(Message message, String[] params) {
		throw new UnsupportedOperationException("must provide a target guild for this command");
	}
}
