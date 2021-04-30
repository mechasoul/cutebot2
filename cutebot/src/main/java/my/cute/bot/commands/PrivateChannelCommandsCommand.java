package my.cute.bot.commands;

import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

class PrivateChannelCommandsCommand extends PrivateChannelCommand {
	
	final static String NAME = "commands";
	
	private final CommandSet<PrivateChannelCommand> commands;
	private final PermissionManager permissions;

	protected PrivateChannelCommandsCommand(CommandSet<PrivateChannelCommand> commands, PermissionManager perms) {
		super(NAME, PermissionLevel.USER, 0, 0);
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
				.forEach(command -> embed.addField(command.getName(), command.getDescription(), false));
		embed.setFooter(MiscUtils.getSignature());
		message.getChannel().sendMessage(embed.build()).queue();
	}

}
