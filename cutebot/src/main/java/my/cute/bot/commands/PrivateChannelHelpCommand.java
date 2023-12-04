package my.cute.bot.commands;

import java.awt.Color;
import java.util.Comparator;

import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

class PrivateChannelHelpCommand extends PrivateChannelCommand {
	
	final static String NAME = "help";
	private final static String DESCRIPTION = "view the list of available commands, or get detailed information about a command";
	
	private final CommandSet<PrivateChannelCommand> commands;
	private final PermissionManager permissions;

	protected PrivateChannelHelpCommand(CommandSet<PrivateChannelCommand> commands, PermissionManager perms) {
		super(NAME, DESCRIPTION, PermissionLevel.USER, 0, 0);
		this.commands = commands;
		this.permissions = perms;
	}

	@Override
	public void execute(Message message, String[] params) {
		//TODO command-specific help (ie, !help <command name>). need support here + define help in command classes
		//TODO go through commands and place `` around formatted guild strings. looks nicer
		
		/*
		 * need to check if user has dev permissions,
		 * check if user has admin permissions on any server,
		 * then iterate over all commands, pick out ones they have permission to use,
		 * and give command and its description (via Command.getDescription())
		 * maybe use an embed for formatting?
		 */
		PermissionLevel highestLevel = this.permissions.getHighestPermissionLevel(message.getAuthor());
		EmbedBuilder embed = new EmbedBuilder();
		embed.setTitle("commands");
		embed.setDescription("you can use `!help <command name>` to get detailed information about any of the following commands");
		this.commands.stream().filter(command -> highestLevel.greaterThanOrEquals(command.getRequiredPermissionLevel()))
				.sorted(Comparator.comparing(command -> command.getName()))
				.forEachOrdered(command -> embed.addField(command.getName(), command.getDescription(), false));
		MiscUtils.applyFlair(embed);
		message.getChannel().sendMessageEmbeds(embed.build()).queue();
	}

}
