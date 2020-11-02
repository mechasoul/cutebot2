package my.cute.bot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelGuildCommand extends PrivateChannelCommand {

private final JDA jda;
	
	public PrivateChannelGuildCommand(JDA jda) {
		super("guild", PermissionLevel.DEVELOPER, 1, 1, false);
		this.jda = jda;
	}
	
	@Override
	public void execute(Message message) {
		String[] words = message.getContentDisplay().split("\\s");

		try {
			Guild guild = this.jda.getGuildById(words[1]);
			message.getChannel().sendMessage(guild != null ? guild.toString() : "no guild found with id '" 
					+ words[1] + "'").queue();
		} catch (NumberFormatException e) {
			message.getChannel().sendMessage("error parsing guild id '" + words[1] + "'").queue();
		}
	}

}
