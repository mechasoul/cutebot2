package my.cute.bot.handlers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.commands.CommandSet;
import my.cute.bot.commands.CommandFactory;
import my.cute.bot.commands.DefaultGuildDatabase;
import my.cute.bot.commands.GuildCommandSet;
import my.cute.bot.commands.PermissionLevel;
import my.cute.bot.commands.PermissionManager;
import my.cute.bot.commands.PrivateChannelCommand;
import my.cute.bot.commands.PrivateChannelCommandTargeted;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.wordfilter.WordFilter;
import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class PrivateMessageReceivedHandler {

	private final static Logger logger = LoggerFactory.getLogger(PrivateMessageReceivedHandler.class);
	
	private final MyListener bot;
	private final JDA jda;
	private final CommandSet<PrivateChannelCommand> commands;
	private final PermissionManager permissions;
	private final DefaultGuildDatabase defaultGuilds;
	//used for multithreaded tasks in some commands
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	public PrivateMessageReceivedHandler(MyListener bot, JDA jda, Map<String, GuildPreferences> allPrefs, 
			Map<String, WordFilter> allFilters, Map<String, GuildCommandSet> allCommands, PermissionManager permissions) throws IOException {
		this.bot = bot;
		this.jda = jda;
		this.defaultGuilds = DefaultGuildDatabase.Loader.createOrLoad();
		this.permissions = permissions;
		this.commands = CommandFactory.newDefaultPrivateChannelSet(this.bot, this.defaultGuilds, allPrefs, allFilters, 
				allCommands, permissions, this.executor);
	}
	
	/*
	 * all command handling occurs here
	 * attempts to find a command that matches the user's message and execute it
	 * 
	 * note that some of the stuff here around default guilds requires GUILD_MEMBERS
	 * intent. if we don't use this intent, we lose: automatically determining 
	 * default guild when someone is in only one guild, validating guilds (eg 
	 * making sure they're in the guild they have registered as default, theyre
	 * in the guild they provide as target for command, etc)
	 */
	public void handle(PrivateMessageReceivedEvent event) {
		String[] params = MiscUtils.getWords(event.getMessage());
		PrivateChannelCommand command = null;
		if(params[0].startsWith("!")) {
			String commandName = params[0].substring(1).toLowerCase();
			System.out.println("command name: " + commandName);
			command = this.commands.get(commandName);
		}
		
		if(command == null) {
			System.out.println("null cmd");
			event.getChannel().sendMessage("?? try !help").queue();
			return;
		}
		
		if(command instanceof PrivateChannelCommandTargeted) {
			/*
			 * attempt to determine target guild
			 * 1. check for provided guild id
			 * 2. check for provided guild name
			 * 3. check for default guild
			 */
			Guild targetGuild = this.tryGetTargetGuildById(params);
			if(targetGuild == null) {
				targetGuild = this.tryGetTargetGuildByName(event.getMessage().getContentRaw());
			}
			
			String targetGuildId;
			if(targetGuild == null) {
				try {
					//check for default guild
					targetGuildId = this.defaultGuilds.getDefaultGuildId(event.getAuthor());
					if(targetGuildId == null) {
						//no found default guild. check if they're in only one guild
						targetGuildId = this.tryRegisterSingleServerDefaultGuild(event.getAuthor());
						if(targetGuildId == null) {
							//no target guild found, can't continue
							event.getChannel().sendMessage(StandardMessages.noTargetGuild()).queue();
							return;
						} 
					} 
					targetGuild = this.jda.getGuildById(targetGuildId);
					//validate the stored default guild
					if(targetGuild == null) {
						event.getChannel().sendMessage(StandardMessages.invalidGuild(targetGuildId)).queue();
						this.defaultGuilds.clearDefaultGuildId(event.getAuthor());
						return;
					}
				} catch (IOException e) {
					logger.warn(this + ": general IOException thrown during check for default guild! author: '" + event.getAuthor() 
						+ "', message: '" + event.getMessage().getContentRaw() + "'", e);
					event.getChannel().sendMessage(StandardMessages.unknownError()).queue();
					return;
				}
			} else {
				targetGuildId = targetGuild.getId();
			}
			
			//final validity check, ensure they're in the given guild
			//requires GUILD_MEMBERS gateway intent i think
			if(!targetGuild.isMember(event.getAuthor()) && !this.permissions.hasPermission(event.getAuthor(), PermissionLevel.DEVELOPER)) {
				event.getChannel().sendMessage(StandardMessages.invalidGuild(targetGuildId)).queue();
				return;
			}
			/*
			 * target guild acquired, now do permissions check. if no permission, send generic unknown
			 * command error message in order to not reveal hidden commands
			 * TODO these hasPermission checks can throw IllegalArgumentException in rare cases maybe?
			 * (when targetGuild was acquired above but not found in permission manager)
			 * 
			 * also any other general command checks?
			 */
			if(this.permissions.hasPermission(event.getAuthor().getId(), targetGuildId, command.getRequiredPermissionLevel())) {
				if(command.hasCorrectParameterCount(params)) {
					((PrivateChannelCommandTargeted) command).execute(event.getMessage(), params, targetGuild);
				} else {
					event.getChannel().sendMessage(StandardMessages.invalidSyntax(command.getName())).queue();
				}
			} else {
				event.getChannel().sendMessage(StandardMessages.unknownCommand(params[0])).queue();
			}
		} else {
			if(this.permissions.hasPermission(event.getAuthor(), command.getRequiredPermissionLevel())) {
				if(command.hasCorrectParameterCount(params)) {
					command.execute(event.getMessage(), params);
				} else {
					event.getChannel().sendMessage(StandardMessages.invalidSyntax(command.getName())).queue();
				}
			} else {
				event.getChannel().sendMessage(StandardMessages.unknownCommand(params[0])).queue();
			}
		}
	}

	/**
	 * given an input String array (assumed to be a user command, already processed
	 * into parameters), attempts to extract a valid target guild by id. specifying
	 * target guild by id can be done by placing the guild id at the end of the 
	 * message; consequently, this method takes the last parameter and attempts to
	 * find a guild with that id. if no guild is found or if that parameter can't
	 * be properly formatted as a long, returns null
	 * @param params the input String array to attempt to find the target guild id in
	 * @return the guild with the id specified by the input String array, or null if
	 * no such guild exists
	 */
	private Guild tryGetTargetGuildById(String[] params) {
		String targetGuildId = params[params.length - 1];
		Guild targetGuild;
		try {
			targetGuild = this.jda.getGuildById(targetGuildId);
		} catch (NumberFormatException e) {
			targetGuild = null;
		}
		return targetGuild;
	}
	
	/**
	 * given an input String (assumed to be a user command), attempts to extract
	 * a valid target guild by name. specifying target guild by name can be done
	 * by placing the guild name in quotation marks at the end of the message; 
	 * consequently, this method extracts the text within the last two quotation
	 * marks in the input String and attempts to find a guild with that name. if
	 * no guild is found, returns null.
	 * <p>
	 * <b>note</b> this method ignores case when looking for guilds by name
	 * @param message the input String to attempt to find a target guild name in
	 * @return a guild whose name matches the provided guild name in the input String,
	 * or null if no such guild exists. if multiple such guilds exist, no guarantee
	 * is made about which guild is returned
	 */
	private Guild tryGetTargetGuildByName(String message) {
		String potentialName = MiscUtils.extractLastQuotationMarks(message);
		if(potentialName == null) return null;
		List<Guild> guilds = this.jda.getGuildsByName(potentialName, true);
		if(guilds.isEmpty()) {
			return null;
		} else {
			return guilds.get(0);
		}
	}
	
	public ExecutorService getExecutor() {
		return this.executor;
	}
	
	/*
	 * requires GUILD_MEMBERS gateway intent
	 */
	private String tryRegisterSingleServerDefaultGuild(User user) throws IOException {
		String guildId = null;
		List<Guild> mutualGuilds = this.jda.getMutualGuilds(user);
		if(mutualGuilds.size() == 1) {
			this.defaultGuilds.setDefaultGuildId(user, mutualGuilds.get(0));
			guildId = mutualGuilds.get(0).getId();
		}
		return guildId;
	}

	@Override
	public String toString() {
		return "PrivateMessageReceivedHandler";
	}
}
