package my.cute.bot.commands;

import java.util.Comparator;

import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

class PrivateChannelHelpCommand extends PrivateChannelCommand {
	
	final static String NAME = "help";
	private final static String DESCRIPTION = "view the list of available commands, or get detailed information about a command";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("view the list of available commands, or get detailed information about a command along with examples")
			.addField("use:", "`!help [<options>]`", false)
			.addField("options", "`<options>` should either be omitted, which will display a list of all commands along with a "
					+ "short description, or be the name of a command, which will display detailed information about the command "
					+ "and what options to provide when using it, along with examples of how to use the command properly", false)
			.addField("examples", "`!help`"
					+ System.lineSeparator()
					+ "view a list of all commands along with short descriptions"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!help default`"
					+ System.lineSeparator()
					+ "view detailed information about the `default` command", false));
			
	
	private final CommandSet<PrivateChannelCommand> commands;
	private final PermissionManager permissions;

	protected PrivateChannelHelpCommand(CommandSet<PrivateChannelCommand> commands, PermissionManager perms) {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.USER, 0, 1);
		this.commands = commands;
		this.permissions = perms;
	}

	@Override
	public void execute(Message message, String[] params) {
		if(params.length == 1) {
			//display help for all commands
			PermissionLevel highestLevel = this.permissions.getHighestPermissionLevel(message.getAuthor());
			EmbedBuilder embed = new EmbedBuilder();
			embed.setTitle("commands");
			embed.setDescription("you can use `!help <command name>` to get detailed information about any of the following commands");
			this.commands.stream().filter(command -> highestLevel.greaterThanOrEquals(command.getRequiredPermissionLevel()))
					.sorted(Comparator.comparing(command -> command.getName()))
					.forEachOrdered(command -> embed.addField(command.getName(), command.getDescription(), false));
			MiscUtils.applyFlair(embed);
			message.getChannel().sendMessageEmbeds(embed.build()).queue();
		} else {
			//display help for a specific command
			Command command = this.commands.get(params[1]);
			if(command == null) {
				message.getChannel().sendMessage(StandardMessages.unknownCommand(params[1])).queue();
			} else {
				if(!this.permissions.getHighestPermissionLevel(message.getAuthor()).greaterThanOrEquals(command.getRequiredPermissionLevel())) {
					//missing permission to view that command
					message.getChannel().sendMessage(StandardMessages.unknownCommand(params[1])).queue();
				} else {
					message.getChannel().sendMessageEmbeds(command.getHelp()).queue();
				}
			}
		}
	}

}
