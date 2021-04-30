package my.cute.bot.commands;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

/**
 * changes the target guild's automatic response time, to control how frequently
 * cutebot responds to user messages without being prompted
 * 
 * <p>use: !auto minutes|'off' [guild id]
 * 
 * <p>parameter 1 is either the word "off", which turns off automatic response, or 
 * a number of minutes in [1, 525600] (shoutouts 2 rent)
 */
public class PrivateChannelAutoCommand extends PrivateChannelCommandTargeted {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelAutoCommand.class);
	final static String NAME = "auto";
	private final static String DESCRIPTION = "enable or disable automatic cutebot messages, or change the approximate "
			+ "time in between them";

	private final Map<String, GuildPreferences> allPrefs;
	
	public PrivateChannelAutoCommand(Map<String, GuildPreferences> prefs) {
		super(NAME, DESCRIPTION, PermissionLevel.ADMIN, 1, 2);
		this.allPrefs = prefs;
	}
	

	@Override
	public void execute(Message message, String[] params, Guild targetGuild) {
		GuildPreferences prefs = this.allPrefs.get(targetGuild.getId());
		
		/*
		 * should be impossible since any existing valid guild id should
		 * have a prefs file? but i guess i'll keep this for now maybe it's
		 * possible somehow that i cant think of
		 */
		if(prefs == null) {
			logger.warn(this + ": couldn't find prefs for valid guild id '" + targetGuild + "'");
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
			return;
		}

		try {
			if (params[1].equalsIgnoreCase("off")) {
				synchronized(prefs) {
					prefs.setAutomaticResponseTime(0);
				}
				message.getChannel().sendMessage("disabled automatic messages for server " 
						+ MiscUtils.getGuildString(targetGuild)).queue();
			} else {
				try {
					int minutes = Integer.parseInt(params[1]);
					if(minutes < 1 || minutes > 525600) {
						message.getChannel().sendMessage(StandardMessages.invalidAutoResponseTime(params[1])).queue();
						return;
					}
					synchronized(prefs) {
						prefs.setAutomaticResponseTime(minutes);
					}
					message.getChannel().sendMessage("set automatic message time for server " + MiscUtils.getGuildString(targetGuild)
					+ " to " + params[1] + " min").queue();
				} catch (NumberFormatException e) {
					message.getChannel().sendMessage(StandardMessages.invalidAutoResponseTime(params[1])).queue();
				}
			}
		} catch (IOException e) {
			logger.warn(this + ": unknown IOException during command execution", e);
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
		}
	}
	
	@Override
	public String toString() {
		return "PrivateChannelAutoCommand";
	}

}
