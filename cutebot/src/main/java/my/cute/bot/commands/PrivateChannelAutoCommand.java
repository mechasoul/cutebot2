package my.cute.bot.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelAutoCommand extends PrivateChannelCommandTargeted {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelAutoCommand.class);

	private final MyListener bot;
	
	public PrivateChannelAutoCommand(MyListener bot) {
		super("auto", PermissionLevel.ADMIN, 1, 2);
		this.bot = bot;
	}
	
	/*
	 * TODO some kind of locking on the preferences for the server in question
	 */
	@Override
	public void execute(Message message, String[] params, String targetGuild) {
		GuildPreferences prefs = this.bot.getPreferences(targetGuild);
		if(prefs != null) {
			try {
				prefs.setAutomaticResponseTime(Integer.parseInt(params[2]));
				prefs.save();
				//note new auto response time won't take effect until after the next auto response
				message.getChannel().sendMessage("set automatic message time for server " + bot.getGuildString(targetGuild)
					+ " to " + params[2] + " min").queue();
			} catch (NumberFormatException e) {
				message.getChannel().sendMessage("invalid number of minutes '" + params[2] + "'").queue();
			}
		} else {
			/*
			 * should be impossible since any existing valid guild id should
			 * have a prefs file? but i guess i'll keep this for now maybe it's
			 * possible somehow that i cant think of
			 */
			logger.warn(this + ": couldn't find prefs for valid guild id '" + targetGuild + "'");
		}
	}
	
	@Override
	public String toString() {
		return "PrivateChannelAutoCommand";
	}

}
