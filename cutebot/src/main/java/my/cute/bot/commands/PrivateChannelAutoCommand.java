package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelAutoCommand extends PrivateChannelCommand {

	private final MyListener bot;
	
	public PrivateChannelAutoCommand(MyListener bot) {
		super("auto", PermissionLevel.ADMIN, 1, 2, true);
		this.bot = bot;
	}
	
	/*
	 * TODO some kind of locking on the preferences for the server in question
	 */
	@Override
	public void execute(Message message) {
		String[] words = message.getContentDisplay().split("\\s");
		
		GuildPreferences prefs = this.bot.getPreferences(words[1]);
		if(prefs != null) {
			try {
				prefs.setAutomaticResponseTime(Integer.parseInt(words[2]));
				prefs.save();
				//note new auto response time won't take effect until after the next auto response
				message.getChannel().sendMessage("set automatic message time for server " + bot.getGuildString(words[1])
					+ " to " + words[2] + " min").queue();
			} catch (NumberFormatException e) {
				message.getChannel().sendMessage("invalid number of minutes '" + words[2] + "'").queue();
			}
		} else {
			message.getChannel().sendMessage("no guild found with id " + words[1]).queue();
		}
	}

}
