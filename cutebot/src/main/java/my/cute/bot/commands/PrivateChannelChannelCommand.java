package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class PrivateChannelChannelCommand extends PrivateChannelCommand {

	final static String NAME = "channel";
	private final static String DESCRIPTION = "view the name of a given channel id";
	
	public PrivateChannelChannelCommand() {
		super(NAME, DESCRIPTION, PermissionLevel.DEVELOPER, 1, 1);
	}
	
	@Override
	public void execute(Message message, String[] params) {
		try {
			TextChannel channel = message.getJDA().getTextChannelById(params[1]);
			message.getChannel().sendMessage(channel != null ? channel.toString() : "no channel found with id '" 
					+ params[1] + "'").queue();
		} catch (NumberFormatException e) {
			message.getChannel().sendMessage("error parsing channel id '" + params[1] + "'").queue();
		}
	}

}
