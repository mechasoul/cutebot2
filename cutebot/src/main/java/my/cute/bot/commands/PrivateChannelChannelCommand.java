package my.cute.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class PrivateChannelChannelCommand extends PrivateChannelCommand {

	private final JDA jda;
	
	public PrivateChannelChannelCommand(JDA jda) {
		super("channel", PermissionLevel.DEVELOPER, 1, 1);
		this.jda = jda;
	}
	
	@Override
	public void execute(Message message, String[] params) {
		try {
			TextChannel channel = this.jda.getTextChannelById(params[1]);
			message.getChannel().sendMessage(channel != null ? channel.toString() : "no channel found with id '" 
					+ params[1] + "'").queue();
		} catch (NumberFormatException e) {
			message.getChannel().sendMessage("error parsing channel id '" + params[1] + "'").queue();
		}
	}

}
