package my.cute.bot.commands;

import java.awt.Color;
import java.util.Comparator;

import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

class PrivateChannelCommandsCommand extends PrivateChannelCommand {
	
	final static String NAME = "commands";
	private final static String DESCRIPTION = "view the list of available commands";
	private static final Color EMBED_COLOR = Color.getHSBColor(0.58f, 0.36f, 0.54f);
	
	private final CommandSet<PrivateChannelCommand> commands;
	private final PermissionManager permissions;

	protected PrivateChannelCommandsCommand(CommandSet<PrivateChannelCommand> commands, PermissionManager perms) {
		super(NAME, DESCRIPTION, PermissionLevel.USER, 0, 0);
		this.commands = commands;
		this.permissions = perms;
	}

	@Override
	public void execute(Message message, String[] params) {
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
		this.commands.stream().filter(command -> highestLevel.greaterThanOrEquals(command.getRequiredPermissionLevel()))
				.sorted(Comparator.comparing(command -> command.getName()))
				.forEachOrdered(command -> embed.addField(command.getName(), command.getDescription(), false));
		embed.addBlankField(false);
		embed.setFooter(System.lineSeparator() + MiscUtils.getSignature(), 
				"https://cdn.discordapp.com/attachments/668188089474088980/837729441785970688/mothyes.png");
		embed.setColor(EMBED_COLOR);
		message.getChannel().sendMessage(embed.build()).queue();
	}

}
