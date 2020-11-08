package my.cute.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelGuildCommand extends PrivateChannelCommand {

private final JDA jda;
	
	public PrivateChannelGuildCommand(JDA jda) {
		super("guild", PermissionLevel.DEVELOPER, 1, 1);
		this.jda = jda;
	}
	
	@Override
	public void execute(Message message, String[] params) {
		try {
			Guild guild = this.jda.getGuildById(params[1]);
			message.getChannel().sendMessage(guild != null ? guild.toString() : "no guild found with id '" 
					+ params[1] + "'").queue();
		} catch (NumberFormatException e) {
			message.getChannel().sendMessage("error parsing guild id '" + params[1] + "'").queue();
		}
	}

}
