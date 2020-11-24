package my.cute.bot.commands;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.ErrorMessages;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

/*
 * sets the user's default guild for commands. any commands that require 
 * a target guild can omit the target guild parameter in order to use the
 * set default guild instead
 * 
 * use: !default <guild id>|view
 * 
 * parameter 1 is either the guild id of the guild to use (must be a valid
 * cutebot guild that the user is a member in), or the word "view", in 
 * which case cutebot will simply echo their set default guild
 */
public class PrivateChannelDefaultCommand extends PrivateChannelCommand {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelDefaultCommand.class);
	final static String NAME = "default";
	
	private final DefaultGuildDatabase defaultGuilds;
	private final JDA jda;

	protected PrivateChannelDefaultCommand(JDA jda, DefaultGuildDatabase guilds) {
		super(NAME, PermissionLevel.USER, 1, 1);
		this.defaultGuilds = guilds;
		this.jda = jda;
	}

	@Override
	public void execute(Message message, String[] params) {
		try {
			if(params[1].equals("view")) {
				String defaultGuildId = this.defaultGuilds.getDefaultGuildId(message.getAuthor());
				if(defaultGuildId == null) {
					message.getChannel().sendMessage("you currently have no default server set").queue();
				} else {
					if(this.isValidDefaultGuild(message.getAuthor(), defaultGuildId)) {
						message.getChannel().sendMessage("your default server is currently set to '" + 
								MiscUtils.getGuildString(this.jda.getGuildById(defaultGuildId)) + "'").queue();
					} else {
						this.defaultGuilds.clearDefaultGuildId(message.getAuthor());
						message.getChannel().sendMessage("you currently have no default server set").queue();
					}
				}
			} else {
				if(this.isValidDefaultGuild(message.getAuthor(), params[1])) {
					this.defaultGuilds.setDefaultGuildId(message.getAuthor().getId(), params[1]);
					message.getChannel().sendMessage("your default server has been set to '" + MiscUtils
							.getGuildString(this.jda.getGuildById(params[1])) + "'").queue();
				} else {
					message.getChannel().sendMessage(ErrorMessages.invalidGuild(params[1])).queue();
				}
			}
		} catch (IOException e) {
			logger.warn("unknown IOException thrown when trying to set default server. user: '" + message.getAuthor() 
				+ "', message: '" + message.getContentRaw() + "'");
			message.getChannel().sendMessage(ErrorMessages.unknownError()).queue();
			
		}
	}
	
	/*
	 * requires GUILD_MEMBERS gateway intent i think
	 */
	private boolean isValidDefaultGuild(User user, String guildId) {
		Guild guild = this.jda.getGuildById(guildId);
		if(guild != null) {
			if(guild.isMember(user)) {
				return true;
			}
		}
		return false;
	}

}
