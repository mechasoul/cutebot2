package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelGuildCommand extends PrivateChannelCommand {

	final static String NAME = "guild";
	private final static String DESCRIPTION = "view the name of a given server id";
	
	public PrivateChannelGuildCommand() {
		super(NAME, DESCRIPTION, PermissionLevel.DEVELOPER, 1, 1);
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
