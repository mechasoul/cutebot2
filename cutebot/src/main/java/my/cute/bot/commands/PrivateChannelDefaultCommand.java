package my.cute.bot.commands;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.StandardMessages;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

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
			if(params[1].equalsIgnoreCase("view")) {
				String defaultGuildId = this.defaultGuilds.getDefaultGuildId(message.getAuthor());
				if(defaultGuildId == null) {
					message.getChannel().sendMessage("you currently have no default server set").queue();
				} else {
					Guild guild = this.jda.getGuildById(defaultGuildId);
					if(guild != null) {
						try {
							guild.retrieveMember(message.getAuthor()).queue(member -> {
								message.getChannel().sendMessage("your default server is currently set to '" + 
										MiscUtils.getGuildString(guild) + "'").queue();
							}, error -> {
								try {
									this.defaultGuilds.clearDefaultGuildId(message.getAuthor());
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
								message.getChannel().sendMessage("you currently have no default server set").queue();
							});
						} catch (UncheckedIOException e) {
							throw e.getCause();
						}
					} else {
						this.defaultGuilds.clearDefaultGuildId(message.getAuthor());
						message.getChannel().sendMessage("you currently have no default server set").queue();
					}
				}
			} else {
				Guild guild = this.jda.getGuildById(params[1]);
				if(guild != null) {
					try {
						guild.retrieveMember(message.getAuthor()).queue(member -> {
							try {
								this.defaultGuilds.setDefaultGuildId(message.getAuthor().getId(), params[1]);
								message.getChannel().sendMessage("your default server has been set to '" + MiscUtils
										.getGuildString(guild) + "'").queue();
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
						}, error -> {
							message.getChannel().sendMessage(StandardMessages.invalidGuild(params[1])).queue();
						});
					} catch (UncheckedIOException e) {
						throw e.getCause();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidGuild(params[1])).queue();
				}
			}
		} catch (IOException e) {
			logger.warn("unknown IOException thrown when trying to set default server. user: '" + message.getAuthor() 
				+ "', message: '" + message.getContentRaw() + "'");
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
			
		}
	}
	
	@Override
	public String toString() {
		return "PrivateChannelDefaultCommand";
	}

}
