package my.cute.bot.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

/*
 * sets the user's default guild for commands. any commands that require 
 * a target guild can omit the target guild parameter in order to use the
 * set default guild instead
 * 
 * use: !default <guild id>|view|clear
 * 
 * parameter 1 is either the guild id of the guild to use (must be a valid
 * cutebot guild that the user is a member in), or the word "view", in 
 * which case cutebot will simply echo their set default guild
 */
public class PrivateChannelDefaultCommand extends PrivateChannelCommand {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelDefaultCommand.class);
	final static String NAME = "default";
	private final static String DESCRIPTION = "set a default server for your commands, so you don't "
			+ "have to explicitly provide the server every time";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("sets a default server for your commands. some commands require you to specify"
					+ " a target server for the command, which can be done in one of three ways:"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "**1) provide the target server's ID.** this can be done by placing the server ID at "
					+ "the end of the command, eg `!commandname parameters 1111111111` to specify server ID "
					+ "`1111111111` as the target server"
					+ System.lineSeparator()
					+ "**2) provide the target server's name.** this can be done by placing the server's name "
					+ "in quotation marks at the end of the command, eg `!commandname parameters \"cute server\"` "
					+ "to specify the server named `cute server` as the target server"
					+ System.lineSeparator()
					+ "**3) set a default server.** this allows you to omit the target server from any commands "
					+ "requiring one, and your default server will automatically be used instead. you can set "
					+ "a default server by using this command. continue reading to learn how")
			.addField("use:", "`!default <options> [<target server>]`", false)
			.addField("options:", "options should be one of `view`, which will display your current default server, "
					+ "`clear`, which will clear your current default server, or a target server as specified above "
					+ "(either as a server ID or a server name in quotation marks), which will set that server as"
					+ "your default server", false)
			.addField("examples", "`!default view`"
					+ System.lineSeparator()
					+ "display your current default server"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!default clear`"
					+ System.lineSeparator()
					+ "clear your current default server"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!default 3333333333`"
					+ System.lineSeparator()
					+ "set your default server to server ID `3333333333`"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!default \"cute server\"`"
					+ System.lineSeparator()
					+ "set your default server to server name `cute server`", false));
	
	private final DefaultGuildDatabase defaultGuilds;

	protected PrivateChannelDefaultCommand(DefaultGuildDatabase guilds) {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.USER, 1, 1);
		this.defaultGuilds = guilds;
	}

	@Override
	public void execute(Message message, String[] params) {
		try {
			if(params[1].equalsIgnoreCase("view")) {
				String defaultGuildId = this.defaultGuilds.getDefaultGuildId(message.getAuthor());
				if(defaultGuildId == null) {
					message.getChannel().sendMessage(StandardMessages.noDefaultGuildSet()).queue();
				} else {
					Guild guild = message.getJDA().getGuildById(defaultGuildId);
					if(guild != null) {
						//requires GUILD_MEMBERS intent?
						try {
							guild.retrieveMember(message.getAuthor()).queue(member -> {
								message.getChannel().sendMessage("your default server is currently set to `" + 
										MiscUtils.getGuildString(guild) + "`").queue();
							}, error -> {
								try {
									this.defaultGuilds.clearDefaultGuildId(message.getAuthor());
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
								message.getChannel().sendMessage(StandardMessages.noDefaultGuildSet()).queue();
							});
						} catch (UncheckedIOException e) {
							throw e.getCause();
						}
					} else {
						this.defaultGuilds.clearDefaultGuildId(message.getAuthor());
						message.getChannel().sendMessage(StandardMessages.noDefaultGuildSet()).queue();
					}
				}
			} else if (params[1].equalsIgnoreCase("clear")) {
				String defaultGuildId = this.defaultGuilds.getDefaultGuildId(message.getAuthor());
				if(defaultGuildId == null) {
					message.getChannel().sendMessage(StandardMessages.noDefaultGuildSet()).queue();
				} else {
					this.defaultGuilds.clearDefaultGuildId(message.getAuthor());
					message.getChannel().sendMessage("your default server has been cleared").queue();
				}
			} else {
				Guild guild;
				try {
					guild = message.getJDA().getGuildById(params[1]);
				} catch (NumberFormatException e) {
					guild = null;
				}
				if(guild != null) {
					this.trySetDefaultGuild(message, guild);
				} else {
					guild = tryGetTargetGuildByName(message);
					if(guild != null) {
						this.trySetDefaultGuild(message, guild);
					} else {
						message.getChannel().sendMessage(StandardMessages.invalidGuild(params[1])).queue();
					}
				}
			}
		} catch (IOException e) {
			logger.warn("unknown IOException thrown when trying to set default server. user: '" + message.getAuthor() 
				+ "', message: '" + message.getContentRaw() + "'");
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
			
		}
	}

	private void trySetDefaultGuild(Message message, Guild guild) throws IOException {
		try {
			guild.retrieveMember(message.getAuthor()).queue(member -> {
				try {
					this.defaultGuilds.setDefaultGuildId(message.getAuthor().getId(), guild.getId());
					message.getChannel().sendMessage("your default server has been set to `" + MiscUtils
							.getGuildString(guild) + "`. if you use a "
									+ "command that requires a target server but don't "
									+ "provide one, it will automatically use this server").queue();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}, error -> {
				message.getChannel().sendMessage(StandardMessages.invalidGuild(guild.getId())).queue();
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}
	
	
	
	/**
	 * given an input Message (assumed to be a user command), attempts to extract
	 * a valid target guild by name. specifying target guild by name can be done
	 * by placing the guild name in quotation marks at the end of the message; 
	 * consequently, this method extracts the text within the last two quotation
	 * marks in the input Message and attempts to find a guild with that name. if
	 * no guild is found, returns null.
	 * <p>
	 * <b>note</b> this method ignores case when looking for guilds by name
	 * <p>
	 * i copied and pasted this from PrivateMessageReceivedHandler so that probably
	 * means this method should be localized somewhere. miscutils...?
	 * @param message the input Message to attempt to find a target guild name in
	 * @return a guild whose name matches the provided guild name in the input Message,
	 * or null if no such guild exists. if multiple such guilds exist, no guarantee
	 * is made about which guild is returned
	 */
	private static Guild tryGetTargetGuildByName(Message message) {
		String potentialName = MiscUtils.extractLastQuotationMarks(message.getContentRaw());
		if(potentialName == null) return null;
		List<Guild> guilds = message.getJDA().getGuildsByName(potentialName, true);
		if(guilds.isEmpty()) {
			return null;
		} else {
			return guilds.get(0);
		}
	}
	
	@Override
	public String toString() {
		return "PrivateChannelDefaultCommand";
	}

}
