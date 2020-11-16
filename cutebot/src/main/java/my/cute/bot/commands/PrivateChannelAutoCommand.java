package my.cute.bot.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.ErrorMessages;
import net.dv8tion.jda.api.entities.Message;

/*
 * changes the target guild's automatic response time, to control how frequently
 * cutebot responds to user messages without being prompted
 * 
 * use: !auto <minutes>|off [guild id]
 * 
 * parameter 1 is either the word "off", which turns off automatic response, or 
 * a number of minutes in [1, 525600] (shoutouts 2 rent)
 * parameter 2 is the target guild id (can be omitted to use default guild)
 */
public class PrivateChannelAutoCommand extends PrivateChannelCommandTargeted {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelAutoCommand.class);

	private final MyListener bot;
	
	public PrivateChannelAutoCommand(MyListener bot) {
		super("auto", PermissionLevel.ADMIN, 1, 2);
		this.bot = bot;
	}
	
	/*
	 * TODO fix this to accommodate everything in description above
	 * need to update stored time until next message, otherwise eg can't turn on
	 * auto response if its turned off
	 */
	@Override
	public void execute(Message message, String[] params, String targetGuild) {
		GuildPreferences prefs = this.bot.getPreferences(targetGuild);
		if(prefs != null) {
			if (params[1].equals("off")) {
				synchronized(prefs) {
					prefs.setAutomaticResponseTime(0);
					prefs.save();
				}
				message.getChannel().sendMessage("disabled automatic messages for server " 
						+ this.bot.getGuildString(targetGuild)).queue();
				
			} else {
				try {
					int minutes = Integer.parseInt(params[1]);
					if(minutes < 1 || minutes > 525600) {
						message.getChannel().sendMessage(ErrorMessages.invalidAutoResponseTime(params[1])).queue();
						return;
					}
					synchronized(prefs) {
						prefs.setAutomaticResponseTime(minutes);
						prefs.save();
					}
					message.getChannel().sendMessage("set automatic message time for server " + bot.getGuildString(targetGuild)
					+ " to " + params[1] + " min").queue();
				} catch (NumberFormatException e) {
					message.getChannel().sendMessage(ErrorMessages.invalidAutoResponseTime(params[1])).queue();
				}
			}
		} else {
			/*
			 * should be impossible since any existing valid guild id should
			 * have a prefs file? but i guess i'll keep this for now maybe it's
			 * possible somehow that i cant think of
			 */
			logger.warn(this + ": couldn't find prefs for valid guild id '" + targetGuild + "'");
			message.getChannel().sendMessage(ErrorMessages.unknownError()).queue();
		}
	}
	
	@Override
	public String toString() {
		return "PrivateChannelAutoCommand";
	}

}
