package my.cute.bot.commands;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

/**
 * changes the target guild's automatic response time, to control how frequently
 * cutebot responds to user messages without being prompted
 * 
 * <p>use: !auto minutes|'off'|view [guild id]
 * 
 * <p>parameter 1 is either the word "off", which turns off automatic response, or 
 * a number of minutes in [1, 525600] (shoutouts 2 rent), or the word "view", which
 * displays automatic response time
 */
public class PrivateChannelAutoCommand extends PrivateChannelCommandTargeted {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelAutoCommand.class);
	final static String NAME = "auto";
	private final static String DESCRIPTION = "enable or disable automatic cutebot responses, or change the approximate "
			+ "time in between them";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("enable or disable automatic cutebot responses, or change the approximate time in between them. "
					+ "note that because this setting can be changed on a per-server basis, this command requires "
					+ "a target server. see `!help default` for more on ways to provide a target server")
			.addField("use:", "`!auto <options> [<target server>]`", false)
			.addField("options", "options should be `off`, which disables automatic cutebot responses, "
					+ "or a number from 1 to 525600 (inclusive), which enables automatic cutebot responses and sets them "
					+ "to occur approximately that many minutes apart, or `view`, which displays the current setting "
					+ "for automatic cutebot responses", false)
			.addField("examples", "`!auto off \"cute server\"`"
					+ System.lineSeparator()
					+ "disables automatic cutebot responses for server name `cute server`"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!auto view`"
					+ System.lineSeparator()
					+ "displays the current setting for automatic cutebot responses for your default server (see `!help default`)"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!auto 60 11111111111`"
					+ System.lineSeparator()
					+ "enables automatic cutebot responses for server ID `11111111111`, and sets them to occur approximately "
					+ "every 60 minutes", false));

	private final Map<String, GuildPreferences> allPrefs;
	
	public PrivateChannelAutoCommand(Map<String, GuildPreferences> prefs) {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.ADMIN, 1, 2);
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
			if(params[1].equalsIgnoreCase("off")) {
				prefs.setAutomaticResponseTime(0);
				message.getChannel().sendMessage("disabled automatic responses for server `" 
						+ MiscUtils.getGuildString(targetGuild) + "`").queue();
			} else if(params[1].equalsIgnoreCase("view")) {
				int autoResponseTime = prefs.getAutomaticResponseTime();
				if(autoResponseTime == 0) {
					message.getChannel().sendMessage("automatic responses for server `" + MiscUtils.getGuildString(targetGuild)
						+ "` are currently disabled").queue();
				} else {
					message.getChannel().sendMessage("automatic responses for server `" + MiscUtils.getGuildString(targetGuild)
						+ "` are currently set to occur approximately every " + autoResponseTime + " minutes").queue();
				}
			} else {
				try {
					int minutes = Integer.parseInt(params[1]);
					if(minutes < 1 || minutes > 525600) {
						message.getChannel().sendMessage(StandardMessages.invalidAutoResponseTime(params[1])).queue();
						return;
					}
					prefs.setAutomaticResponseTime(minutes);
					message.getChannel().sendMessage("set automatic response time for server `" + MiscUtils.getGuildString(targetGuild)
					+ "` to approximately " + params[1] + " minutes").queue();
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
