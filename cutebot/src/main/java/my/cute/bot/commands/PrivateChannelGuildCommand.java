package my.cute.bot.commands;

import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelGuildCommand extends PrivateChannelCommand {

	final static String NAME = "guild";
	private final static String DESCRIPTION = "view the name of a given server ID";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("view the name of a given server ID. in order for this command to work, "
					+ "cutebot must be a member of the server")
			.addField("use:", "`!guild <options>`", false)
			.addField("options", "`<options>` should be a single server ID", false)
			.addField("examples", "`!guild 1111111111`"
					+ System.lineSeparator()
					+ "view the name of the discord server with ID `1111111111`", false));
	
	public PrivateChannelGuildCommand() {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.DEVELOPER, 1, 1);
	}
	
	@Override
	public void execute(Message message, String[] params) {
		try {
			Guild guild = message.getJDA().getGuildById(params[1]);
			message.getChannel().sendMessage(guild != null ? guild.toString() : "no guild found with id '" 
					+ params[1] + "'").queue();
		} catch (NumberFormatException e) {
			message.getChannel().sendMessage("error parsing guild id '" + params[1] + "'").queue();
		}
	}

}
