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
import my.cute.bot.commands.CommandSetFactory;
import my.cute.bot.commands.DefaultGuildDatabase;
import my.cute.bot.commands.PermissionManager;
import my.cute.bot.commands.PermissionManagerImpl;
import my.cute.bot.commands.PrivateChannelCommand;
import my.cute.bot.commands.PrivateChannelCommandTargeted;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.wordfilter.WordFilter;
import my.cute.bot.util.StandardMessages;
import my.cute.bot.util.MiscUtils;
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
			Map<String, WordFilter> allFilters) throws IOException {
		this.bot = bot;
		this.jda = jda;
		this.defaultGuilds = DefaultGuildDatabase.Loader.createOrLoad();
		this.commands = CommandSetFactory.newDefaultPrivateChannelSet(this.jda, this.bot, this.defaultGuilds, allPrefs, allFilters, this.executor);
		this.permissions = new PermissionManagerImpl(this.jda);
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
			String commandName = params[0].substring(1);
			command = this.commands.get(commandName);
		}
		
		if(command == null) {
			event.getChannel().sendMessage(StandardMessages.unknownCommand(params[0])).queue();
			return;
		}
		
		if(command instanceof PrivateChannelCommandTargeted) {
			//check for explicitly provided guild
			String targetGuild = params[params.length - 1];
			if(this.jda.getGuildById(targetGuild) == null) {
				try {
					//check for default guild
					targetGuild = this.defaultGuilds.getDefaultGuildId(event.getAuthor());
					if(targetGuild == null) {
						//no found default guild. check if they're in only one guild
						targetGuild = this.registerSingleServerDefaultGuild(event.getAuthor());
						if(targetGuild == null) {
							//no target guild found, can't continue
							event.getChannel().sendMessage(StandardMessages.noTargetGuild()).queue();
							return;
						} 
					} 
					//validate the stored default guild
					if(this.jda.getGuildById(targetGuild) == null) {
						event.getChannel().sendMessage(StandardMessages.invalidGuild(targetGuild)).queue();
						this.defaultGuilds.clearDefaultGuildId(event.getAuthor());
					}
				} catch (IOException e) {
					logger.warn(this + ": general IOException thrown during check for default guild! author: '" + event.getAuthor() 
						+ "', message: '" + event.getMessage().getContentRaw() + "'", e);
					event.getChannel().sendMessage(StandardMessages.unknownError()).queue();
				}
			}
			
			//final validity check, ensure they're in the given guild
			//requires GUILD_MEMBERS gateway intent i think
			if(!this.jda.getGuildById(targetGuild).isMember(event.getAuthor())) {
				event.getChannel().sendMessage(StandardMessages.invalidGuild(targetGuild)).queue();
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
			if(this.permissions.hasPermission(event.getAuthor().getId(), targetGuild, command.getRequiredPermissionLevel())) {
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
	
	public ExecutorService getExecutor() {
		return this.executor;
	}
	
	/*
	 * requires GUILD_MEMBERS gateway intent
	 */
	private String registerSingleServerDefaultGuild(User user) throws IOException {
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
