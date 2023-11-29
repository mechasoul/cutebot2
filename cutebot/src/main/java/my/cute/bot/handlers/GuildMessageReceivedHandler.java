package my.cute.bot.handlers;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.CutebotTask;
import my.cute.bot.commands.GuildCommandSet;
import my.cute.bot.commands.PermissionDatabase;
import my.cute.bot.commands.PermissionLevel;
import my.cute.bot.database.GuildDatabase;
import my.cute.bot.database.GuildDatabaseBuilder;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.wordfilter.FilterResponseAction;
import my.cute.bot.preferences.wordfilter.WordFilter;
import my.cute.bot.tasks.GuildDatabaseSetupTask;
import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import my.cute.bot.util.WordfilterTimeoutException;
import my.cute.markov2.exceptions.ReadObjectException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

/*
 * TODO
 * in older code like this class, mylistener, etc i did a lot of catching IOException to
 * log and swallow or log and rethrow. should take another look at these and see if theres
 * any more appropriate action to take
 */
public class GuildMessageReceivedHandler {
	
	private class AutonomyHandler {
		private long lastAutoMessageTime=0;
		private final Random RAND = new Random();
		private float randomFactor = this.getNewRandomFactor();
		
		private boolean shouldSendAutomaticMessage() {
			int autoMessageTime = prefs.getAutomaticResponseTime();
			return autoMessageTime != 0 && 
					(System.currentTimeMillis() - this.lastAutoMessageTime) >= ((long)(autoMessageTime * this.randomFactor) * 60000L);
		}
		
		private void update() {
			this.lastAutoMessageTime = System.currentTimeMillis();
			this.randomFactor = this.getNewRandomFactor();
		}
		
		//gets a random float in [2/3, 4/3)
		private float getNewRandomFactor() {
			return (this.RAND.nextFloat() * (2f/3f)) + (2f/3f);
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(GuildMessageReceivedHandler.class);
	@SuppressWarnings("unused")
	private static final Pattern BOT_NAME = (CutebotTask.ACTIVE_TOKEN == CutebotTask.CUTEBOT_PRIME_TOKEN) ?
			Pattern.compile(".*(?:cutebot prime|cbp).*", Pattern.CASE_INSENSITIVE) : 
			Pattern.compile(".*(?:cutebot).*", Pattern.CASE_INSENSITIVE);
	
	private final JDA jda;
	private final String id;
	private final GuildDatabase database;
	private final GuildPreferences prefs;
	private final WordFilter wordFilter;
	private final PermissionDatabase perms;
	private final GuildCommandSet commands;
	private final Random random = new Random();
	private final ExecutorService executor;
	private final AutonomyHandler autonomyHandler;
	
	public GuildMessageReceivedHandler(Guild guild, JDA jda, GuildPreferences prefs, WordFilter filter, 
			PermissionDatabase perms, ExecutorService executor, GuildCommandSet commands) throws IOException {
		this.jda = jda;
		this.id = guild.getId();
		this.prefs = prefs;
		this.wordFilter = filter;
		this.perms = perms;
		this.autonomyHandler = new AutonomyHandler();
		this.executor = executor;
		this.database = new GuildDatabaseBuilder(guild)
				.databaseAge(this.prefs.getDatabaseAge())
				.build();
		this.database.load();
		this.commands = commands;
	}
	
	/*
	 * TODO this is a really big method can maybe separate it into pieces or whatever
	 * 
	 * currently calling handle() as a Runnable in ForkJoinPool.commonPool() (from MyListener)
	 * each guild has its own lock to synchronize on so can handle at most one message at a time per guild
	 * but multiple guilds should be able to handle messages concurrently
	 */
	public void handle(GuildMessageReceivedEvent event) throws IOException, WordfilterTimeoutException {
		
		String content = event.getMessage().getContentRaw();
		
		if(!StringUtils.isWhitespace(content)) {
			//message nonempty. check for command
			String[] params = MiscUtils.getWords(event.getMessage());
			String firstWord = params[0];
			if(firstWord.startsWith(this.prefs.getPrefix())) {
				//first word starts with designated command prefix. check if it's a command
				String commandName = firstWord.substring(this.prefs.getPrefix().length()).toLowerCase();
				if(this.commands.execute(commandName, event.getMessage(), params)) {
					//don't process commands into database
					return;
				}
			} else if(firstWord.startsWith("/")) {
				//discord command. don't process
				return;
			}
		}
		
		try {
			if(this.handleWordFilter(event.getMessage())) {
				//wordfilter found a match
				if(this.wordFilter.getActions().contains(FilterResponseAction.SKIP_PROCESS)) return;
			}
		} catch (TimeoutException e) {
			//problem with wordfilter
			//call low-level maintenance so wordfilter can do any necessary changes
			//then throw exception so something higher-up can handle the general issues that come from this
			WordFilter.Type type = this.wordFilter.handleTimeout(event.getMessage().getContentRaw());
			throw new WordfilterTimeoutException(e, type);
		}
		
		//don't process messages / send automatic messages in non-discussion channels
		if(!this.prefs.isDiscussionChannel(event.getChannel().getId())) return;
		
		try {
			
			this.database.processLine(content);
			
		} catch (ReadObjectException e) {
			/*
			 * TODO is it possible for this to be something other than IOException?
			 * ie, a problem with loading a shard results in some other exception somehow?
			 */
			this.recoverFromDatabaseError(event, e);
			//don't continue with line generation and whatever if the database is broken
			return;
		} catch (IOException e) {
			/*
			 * could indicate a problem with writing to workingset file, or something else unanticipated 
			 * further operation will make workingset inconsistent, so require user intervention
			 * this method is called from MyListener, which will shutdown on IOException encountered here
			 */
			logger.warn(this + ": unknown IOException thrown during line processing - possible workingset inconsistency!");
			this.database.setShouldRestoreFromBackup(true);
			throw e;
		}
			
		try {
			if(this.autonomyHandler.shouldSendAutomaticMessage()) {
				String line = this.database.generateLine();
				event.getChannel().sendMessage(line).queue();
				this.autonomyHandler.update();
				logger.info(this + ": sent automatic message '" + line + "', next automatic message scheduled in around " 
						+ this.prefs.getAutomaticResponseTime() + " mins");
			} else if(BOT_NAME.matcher(content).matches()) {
				if(isQuestion(content)) {
					event.getChannel().sendMessage(this.database.generateLine()).queue();
				} else {
					addReactionToMessage(event.getMessage());
				}
			} else if (content.contains("mothyes")) {
				addReactionToMessage(event.getMessage());
			}
		} catch (InsufficientPermissionException e) {
			/*
			 * missing permission for sendMessage() or similar
			 * do nothing
			 */
		} catch (IOException e) {
			/*
			 * line generation isn't a terribly important place for an IOException to be thrown so don't
			 * do anything. if something is wrong with the database then we can just wait for it to be 
			 * thrown from somewhere where we're writing to db
			 */
			logger.warn(this + ": encountered IOException during line generation", e);
		}
	}

	private void recoverFromDatabaseError(GuildMessageReceivedEvent event, ReadObjectException e) {
		logger.info(this + ": encountered ReadObjectException during line processing. beginning automatic database restore", e);
		if(this.database.restoreFromAutomaticBackups()) {
			try {
				this.database.clearAutomaticBackups();
				this.database.maintenance();
			} catch (IOException e1) {
				/*
				 * TODO should do something like rebuild here?
				 * database was successfully rebuilt but io error when deleting backups/performing maintenance could still
				 * result in db being in a compromised state
				 * maybe nuke and start over? or do SOMETHING instead of logging and swallowing
				 */
				logger.warn(this + ": exception when trying to clear automatic backups after restoring from backup", e);
			}
		} else {
			try {
				this.database.clearAutomaticBackups();
			} catch (IOException e1) {
				//see above. maybe do something here
				logger.warn(this + ": exception when trying to clear automatic backups after restoring from backup", e);
			}
			this.database.markForMaintenance();
			this.executor.submit(new GuildDatabaseSetupTask(this.jda, event.getGuild(), this.prefs, this.database));
		}
	}
	
	public boolean checkMaintenance() {
		boolean needsMaintenance = this.database.needsMaintenance();
		if(needsMaintenance) {
			this.maintenance();
		}
		return needsMaintenance;
	}
	
	public void maintenance() {
		this.executor.execute(() -> 
		{
			try {
				this.database.maintenance();
			} catch (IOException e) {
				//TODO do something else here?
				logger.warn(this + ": maintenance on guild " + this.id + " terminated due to IOException", e);
			}
		});
	}
	
	public void prepareForShutdown() {
		try {
			this.database.shutdown();
		} catch (IOException e) {
			logger.warn(this + ": encountered IOException during shutdown, possible database inconsistency", e);
		}
	}
	
	public GuildDatabase getDatabase() {
		return this.database;
	}
	
	public GuildPreferences getPreferences() {
		return this.prefs;
	}
	
	//TODO this sucks
	private static boolean isQuestion(String s) {
		if(s.contains("what") || s.contains("who") || s.contains("why") || s.contains("wat") || s.contains("how") || s.contains("when") || s.contains("will") || s.contains("are you") || s.contains("are u") || s.contains("can you") || s.contains("do you") || s.contains("can u") || s.contains("do u") || s.contains("where") || s.contains("?")) {
			return true;
		} else {
			return false;
		}
	}
	
	
	private void addReactionToMessage(Message message) {
		try {
			if(random.nextInt(10) == 0) {
				message.addReaction(MiscUtils.getRandomEmoteFromCache(this.jda)).queue();
			} else {
				//mothyes
				message.addReaction(this.jda.getEmoteById("242763939631333378")).queue();
			}
		} catch (IllegalArgumentException e) {
			/*
			 * thrown from Message.addReaction(Emote) if the emote can't be used in the given channel
			 * do nothing
			 */
		}
	}
	
	private boolean handleWordFilter(Message message) throws TimeoutException, IOException {
		String filteredWord;
		filteredWord = this.wordFilter.check(message.getContentRaw());
		if(filteredWord != null) {
			try {
				this.applyWordFilterActions(message, filteredWord);
			} catch (InsufficientPermissionException | HierarchyException e) {
				/*
				 * less important exceptions thrown during wordfilter action application
				 * more important ones are handled inside applyWordFilterActions(), but
				 * these can be thrown from eg a server moderator triggering the 
				 * wordfilter, someone triggering the wordfilter in a channel cutebot
				 * doesn't have permissions in when it's set to delete offending messages,
				 * etc
				 * 
				 * these edge cases could happen but don't really represent a general 
				 * problem with the wordfilter (in the same sense as eg cutebot not having
				 * permissions to kick users but being set to kick users), so we do nothing
				 */
			}
			return true;
		} else {
			return false;
		}
	}
	
	private void applyWordFilterActions(final Message message, final String filteredWord) throws IOException {
		StringBuilder errorBuilder = new StringBuilder();
		EnumSet<FilterResponseAction> actions = this.wordFilter.getActions();
		if(actions.contains(FilterResponseAction.BAN)) {
			try {
				message.getGuild().ban(message.getAuthor(), 0, "don't say '" + filteredWord + "'").queue();
			} catch (InsufficientPermissionException e) {
				errorBuilder.append(StandardMessages.missingPermissionsToBan());
				errorBuilder.append(System.lineSeparator());
			}
		} else if (actions.contains(FilterResponseAction.KICK)) {
			try {
				message.getGuild().kick(message.getAuthor().getId(), "please don't say '" + filteredWord + "'").queue();
			} catch (InsufficientPermissionException e) {
				errorBuilder.append(StandardMessages.missingPermissionsToKick());
				errorBuilder.append(System.lineSeparator());
			}
		}
		if(actions.contains(FilterResponseAction.DELETE_MESSAGE)) {
			try {
				message.delete().queue();
			} catch (InsufficientPermissionException e) {
				errorBuilder.append(StandardMessages.missingPermissionsToDeleteMessages());
				errorBuilder.append(System.lineSeparator());
			}
		}
		if(actions.contains(FilterResponseAction.SEND_RESPONSE_GUILD)) {
			MessageBuilder builder = new MessageBuilder();
			builder.mention(message.getAuthor());
			builder.append(message.getAuthor());
			builder.append(" your message contained a flagged phrase please don't do that");
			message.getChannel().sendMessage(builder.build()).queue();
		}
		if(actions.contains(FilterResponseAction.SEND_RESPONSE_PRIVATE)) {
			MessageBuilder builder = new MessageBuilder();
			builder.append("dear user,");
			builder.append(System.lineSeparator());
			builder.append(System.lineSeparator());
			builder.append("your message (");
			builder.append(message.getJumpUrl());
			builder.append(") in server '");
			builder.append(MiscUtils.getGuildString(message.getGuild()));
			builder.append("' contained the flagged phrase '");
			builder.append(filteredWord);
			builder.append("'. please don't do that");
			builder.append(MiscUtils.getSignature());
			message.getAuthor().openPrivateChannel()
					.flatMap(channel -> channel.sendMessage(builder.build())).queue();
		}
		if(actions.contains(FilterResponseAction.ROLE)) {
			/*
			 * role could be invalid either by not having been set (wordFilter.getRoleId()
			 * is empty), or by being deleted since being set (guild.getRoleById(String) 
			 * returns null)
			 */
			Role role; 
			if(this.wordFilter.getRoleId().isBlank())
				role = null;
			else 
				role = message.getGuild().getRoleById(this.wordFilter.getRoleId());
			if(role == null) {
				//stored role is invalid. remove this action and notify admins
				actions.remove(FilterResponseAction.ROLE);
				this.wordFilter.setActions(actions);
				this.wordFilter.clearRoleId();
				this.notifyCutebotAdmins("the wordfilter was set to apply a role to users who trigger it, "
						+ "but no valid role was provided. please check your wordfilter settings");
				return;
			}
			try {
				message.getGuild().addRoleToMember(message.getAuthor().getId(), role).queue();
			} catch (InsufficientPermissionException | HierarchyException e) {
				//missing permission to modify roles or to interact with the specified role
				errorBuilder.append(StandardMessages.missingPermissionsToApplyFilterRole(role));
				errorBuilder.append(System.lineSeparator());
			} 
		}
		
		String errorMessage = errorBuilder.toString();
		if(!errorMessage.isEmpty())
			this.notifyCutebotAdmins("wordfilter error: " + errorMessage);
	}
	
	private void notifyCutebotAdmins(String context) {
		StringBuilder sb = new StringBuilder();
		sb.append("dear user,");
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append("there was a problem encountered in server '");
		sb.append(MiscUtils.getGuildString(this.jda.getGuildById(this.id)));
		sb.append("': ");
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append(context);
		sb.append(System.lineSeparator());
		sb.append("you are receiving this message because you have cutebot admin permissions in this server. "
				+ "if you don't know what any of this means just ignore this or something");
		sb.append(MiscUtils.getSignature());
		String message = sb.toString();
		this.perms.getUsersWithPermission(PermissionLevel.ADMIN)
				.forEach(id -> this.jda.openPrivateChannelById(id)
						.flatMap(channel -> channel.sendMessage(message))
						.queue()
				);
	}
	
	@Override
	public String toString() {
		return "GuildMessageReceivedHandler-" + this.id;
	}
}
