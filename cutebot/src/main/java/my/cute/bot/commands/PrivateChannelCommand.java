package my.cute.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;

public abstract class PrivateChannelCommand extends CommandImpl {

	/*
	 * base class for commands executed via a private channel
	 * commands that don't require a target guild parameter should use this
	 * as their base class (see PrivateChannelCommandTargeted for more)
	 */
	protected PrivateChannelCommand(String name, String description, EmbedBuilder helpEmbed, PermissionLevel permission, int min, int max) {
		super(name, description, helpEmbed, permission, min, max);
	}

}
