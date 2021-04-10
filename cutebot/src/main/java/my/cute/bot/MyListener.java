package my.cute.bot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.commands.CommandSet;
import my.cute.bot.commands.CommandFactory;
import my.cute.bot.commands.PermissionLevel;
import my.cute.bot.commands.PermissionManager;
import my.cute.bot.commands.PermissionManagerImpl;
import my.cute.bot.commands.TextChannelCommand;
import my.cute.bot.database.GuildDatabase;
import my.cute.bot.handlers.GuildMessageReceivedHandler;
import my.cute.bot.handlers.PrivateMessageReceivedHandler;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.GuildPreferencesFactory;
import my.cute.bot.preferences.wordfilter.WordFilter;
import my.cute.bot.preferences.wordfilter.WordFilterFactory;
import my.cute.bot.tasks.GuildDatabaseSetupTask;
import my.cute.bot.util.ConcurrentFinalEntryMap;
import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import my.cute.bot.util.WordfilterTimeoutException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import okhttp3.OkHttpClient;

public class MyListener extends ListenerAdapter {
	
	/*
	 * method of generating unprompted messages without relying on a recent 
	 * user message. ended up spamming messages during off-hours, maybe with
	 * a way to modify timer based on activity it would work...keeping the
	 * framework here in case i revisit it
	 */
	@SuppressWarnings("unused")
	private class AutomaticMessageTask implements Runnable {
		
		@Override
		public void run() {
//			for(String id : automaticMessageGuilds) {
//				try {
//					String line = getDatabase(id).generateLine();
//					Guild guild = jda.getGuildById(id);
//					guild.getDefaultChannel().sendMessage(line).queue();
//					logger.info("AutomaticMessageTask: sent line '" + line + "' in guild '" + jda.getGuildById(id)
//						+ "' in channel '" + guild.getDefaultChannel());
//				} catch (IOException e) {
//					logger.warn("AutomaticMessageTask: encountered IOException on guild '" + jda.getGuildById(id) + "'", e);
//				}
//			}
//			int delay = RAND.nextInt(180) + 30;
//			taskScheduler.schedule(this, delay, TimeUnit.MINUTES);
//			logger.info("AutomaticMessageTask: scheduled next automatic message in " + delay + "min");
		}
		
	}
	
	private static final Logger logger = LoggerFactory.getLogger(MyListener.class);
	private final JDA jda;
	private final ConcurrentFinalEntryMap<String, GuildPreferences> allPrefs;
	private final ConcurrentFinalEntryMap<String, WordFilter> allFilters;
	private final ConcurrentFinalEntryMap<String, CommandSet<TextChannelCommand>> guildCommands;
	private final ConcurrentFinalEntryMap<String, GuildMessageReceivedHandler> guildMessageHandlers;
	private final PermissionManager permissions;
	private final PrivateMessageReceivedHandler privateMessageHandler;
	private final ScheduledExecutorService taskScheduler;
	
	/*
	 * i think it's supposed to be bad practice to use "this" as an argument to something 
	 * else from inside a constructor because the object isn't properly initialized or 
	 * something but it's really a lot simpler this way and i'm trying to be careful to 
	 * make sure nothing is done with the reference aside from saving it
	 * 
	 * there are ways around it (eg make the privateMessageHandler field not final, have
	 * a method to create it and call that method after creating the MyListener object),
	 * but this seems generally uglier than just doing it like this (could avoid most
	 * problems by say making ctor private, having an internal factory class, and have
	 * the factory create method be public and take care of calling the initialization
	 * methods? then its just like MyListener.Factory.create() to build and all the other
	 * stuff is hidden away in the create method. but still that field has to be non-final
	 * and i dont think it really matters as long as im careful)
	 */
	
	/*
	 * TODO maybe a better way of distributing prefs and wordfilter to commands is to make like
	 * a map in ctor and populate it with the prefs/wordfilter objects, then pass it in to the
	 * privatemessagereceivedhandler ctor and have that pass it in to the commandset which passes
	 * it in to any commands that require it. could stay final and we just remove/add on guild
	 * leave/join. wordfilters could be replaced during runtime so theres possible concurrency
	 * problems, but by doing this we avoid having awkward MyListener.getPreferences(String) 
	 * methods and whatever that dont really make sense to be here (also load wordfilter here
	 * and pass it in to guildmessagereceivedhandler like with prefs)
	 */
	MyListener(JDA jda) throws IOException {
		this.jda = jda;
		int numActiveGuilds = this.jda.getGuilds().size();
		this.allPrefs = new ConcurrentFinalEntryMap<>(numActiveGuilds * 4 / 3, 0.75f);
		this.allFilters = new ConcurrentFinalEntryMap<>(numActiveGuilds * 4 / 3, 0.75f);
		this.guildCommands = new ConcurrentFinalEntryMap<>(numActiveGuilds * 4 / 3, 0.75f);
		this.permissions = new PermissionManagerImpl(this.jda.getGuilds().size() * 4 / 3);
		this.guildMessageHandlers = new ConcurrentFinalEntryMap<>(numActiveGuilds * 4 / 3, 0.75f);
		this.taskScheduler = Executors.newScheduledThreadPool(2);
		
		for(Guild guild : this.jda.getGuilds()) {
			this.registerGuild(guild);
		}
		
		this.privateMessageHandler = new PrivateMessageReceivedHandler(this, jda, this.allPrefs, this.allFilters, this.permissions);
		
		
		this.taskScheduler.scheduleWithFixedDelay(() -> 
		{ 
			this.checkMaintenance();
		}, 1, 12, TimeUnit.HOURS);
		this.taskScheduler.scheduleWithFixedDelay(() ->
		{
			this.permissionMaintenance();
		}, 1, 36, TimeUnit.HOURS);
		//shelving this for now and going back to old method of automatic messages
		//(generate message after someone else sends a message after the given time)
//		this.taskScheduler.execute(new AutomaticMessageTask());
	}
	/*
	 * TODO
	 * check for message deletion event
	 * if possible, have message deletion add the deleted content to a deletedmessages.txt file
	 * during maintenance, scan this file over the entire workingset and remove each line in deleted
	 * messages from the workingset once (and remove from database ofc). this way message deletion
	 * will be reflected in db
	 * -> message delete event predictably doesnt provide the deleted message content, just id. 
	 * possible solution is to manually cache all messages within some timeframe, eg 24 hrs, along 
	 * w/ their id; when a msg is deleted, check cache for it. manage cache during maintenance
	 * most deleted msgs that are problematic will be deleted very shortly after posting so holding
	 * them for a short timeframe like a day should keep things manageable + still be effective
	 * could combine this w/ workingset by, say, appending msg id after datestamp? i think id rather
	 * keep workingset trim tho
	 * 
	 * can psosibly do the same thing with message edits?
	 * 
	 * quick cmd to load db from backup
	 */
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if(event.getAuthor().isBot()) return;
		
		try {
			this.guildMessageHandlers.get(event.getGuild().getId()).handle(event);
		} catch (WordfilterTimeoutException e) {
			this.handleWordfilterTimeout(event.getGuild(), e);
		} catch (IOException e) {
			//an ioexception that's bubbled up this high is fatal. shutdown and require user intervention
			//also assume the details have been logged from wherever the problem started
			//TODO change that to log here instead probably? shouldnt log rethrow i think, should log
			//where its actually handled
			this.shutdown();
		}
	}
	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if(event.getAuthor().isBot()) return;
		
		this.privateMessageHandler.handle(event);
	}
	
	//note that this event may be fired mistakenly on a guild we're already in? so needs to be ok with that
	//TODO anything else to do on guuild join?
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		boolean newGuild = false;
		//preliminary check to avoid unnecessary GuildMessageReceivedHandler creation since it's slightly expensive
		if(!this.guildMessageHandlers.containsKey(event.getGuild().getId())) {
			//verify that guild hasn't been added in the meantime by other thread
			try {
				newGuild = this.registerGuild(event.getGuild());
			} catch (IOException e) {
				logger.error(this + ": encountered IOException when trying to construct GuildMessageReceivedHandler for new guild '" +
						event.getGuild() + "', can't continue!", e);
				this.shutdown();
			}
		}
		
		if(newGuild) {
			String id = event.getGuild().getId().intern();
			this.taskScheduler.submit(new GuildDatabaseSetupTask(this.jda, event.getGuild(), this.allPrefs.get(id), this.getDatabase(id)));
		}
	}
	
	//TODO add stuff to this
	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		logger.info(this + ": left guild " + event.getGuild());
		String id = event.getGuild().getId().intern();
		this.guildMessageHandlers.get(id).prepareForShutdown();
		this.guildMessageHandlers.remove(id);
	}
	
	void checkMaintenance() {
		/*
		 * TODO
		 * discussion channel updating should occur periodically in maintenance
		 * doesnt need to happen every maintenance tho and in fact should not
		 */
		this.guildMessageHandlers.forEach((id, handler) -> handler.checkMaintenance());
	}
	
	/*
	 * used to force maintenance to start on all servers
	 * checkMaintenance() should generally be used instead. this exists primarily for,
	 * eg, a developer command so a dev can force maintenance if they need to
	 */
	public void forceMaintenance() {
		this.guildMessageHandlers.forEach((id, handler) -> handler.maintenance());
	}
	
	/*
	 * same as above but for a specific server
	 * throws IllegalArgumentException if the given server id isn't a valid key in 
	 * this.guildMessageHandlers
	 */
	public void forceMaintenance(String id) {
		GuildMessageReceivedHandler handler = this.guildMessageHandlers.get(id);
		if(handler == null) throw new IllegalArgumentException("invalid guild id '" + id + "'");
		handler.maintenance();
	}
	
	private void permissionMaintenance() {
		this.jda.getGuildCache().forEach(guild -> {
			guild.retrieveOwner().queue(owner -> {
				try {
					this.permissions.add(owner.getUser(), guild, PermissionLevel.ADMIN);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			this.permissions.getAdmins(guild.getId()).forEach(id -> {
				guild.retrieveMemberById(id).queue(null, new ErrorHandler()
						.handle(EnumSet.of(ErrorResponse.UNKNOWN_USER, ErrorResponse.UNKNOWN_MEMBER), 
								ex -> {
									try {
										this.permissions.remove(id+"", guild.getId(), PermissionLevel.ADMIN);
									} catch (IOException e) {
										throw new UncheckedIOException(e);
									}
								}
						)
				);
			});
		});
	}
	
	public void shutdown() {
		this.guildMessageHandlers.forEach((id, handler) -> handler.prepareForShutdown());
		this.taskScheduler.shutdownNow();
		this.privateMessageHandler.getExecutor().shutdownNow();
		OkHttpClient client = this.jda.getHttpClient();
		client.connectionPool().evictAll();
		client.dispatcher().executorService().shutdown();
		this.jda.shutdown();
	}
	
	public GuildDatabase getDatabase(String id) {
		GuildMessageReceivedHandler handler = this.guildMessageHandlers.get(id);
		if(handler != null) {
			return handler.getDatabase();
		} else {
			return null;
		}
	}
	
	/**
	 * to be called when a new guild needs to be managed. adds all necessary resources to
	 * maps and whatever so they can be received wherever they're needed. note if an already
	 * registered guild is passed in to this, nothing will break, although it will probably
	 * cost a bit of processing time (mostly since a new GuildMessageReceivedHandler will be
	 * constructed, which requires loading that guild's database. the handler won't go in the
	 * map and will eventually be garbage collected but loading db can take some time)
	 * @param guild the new guild to manage
	 * @return true if the guild is newly registered, or false if it was already registered
	 * @throws IOException if an unknown IOException occurs when trying to load resources from 
	 * disk
	 */
	private boolean registerGuild(Guild guild) throws IOException {
		if(this.guildMessageHandlers.containsKey(guild.getId())) return false;
		
		GuildPreferences prefs = GuildPreferencesFactory.load(guild.getId());
		WordFilter filter = WordFilterFactory.load(guild.getId());
		CommandSet<TextChannelCommand> commands = CommandFactory.newDefaultTextChannelSet(this.jda, guild.getId(), prefs);
		this.allPrefs.put(guild.getId(), prefs);
		this.allFilters.put(guild.getId(), filter);
		this.guildCommands.put(guild.getId(), commands);
		this.permissions.addGuild(guild);
		/*
		 * TODO
		 * move this owner->Admin check to PermissionDatabaseFactory or PermissionManager.addGuild()?
		 * so that if the permissions file is empty, owner gets added as admin
		 * change permissionMaintenance() above to function the same way; if permissions empty, add owner
		 * & also try to prevent last person with admin permissions from being removed as admin via command
		 */
		try {
			guild.retrieveOwner(false).queue(owner -> {
				try {
					this.permissions.add(owner.getUser(), guild, PermissionLevel.ADMIN);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		return this.guildMessageHandlers.put(guild.getId(), new GuildMessageReceivedHandler(guild, this.jda, prefs, filter, this.taskScheduler, commands)) == null;
	}
	
	/**
	 * top-level handling for when a guild's wordfilter exceeds the set max execution time. 
	 * this is likely going to be caused by someone deliberately setting a problematic 
	 * explicit regex filter, rather than normal use. when this occurs, a strike is recorded
	 * against the wordfilter in question. if the number of strikes reaches the set limit 
	 * (as determined by {@link#WordFilter.getStrikesToDisable()}), the wordfilter is
	 * disabled. in any case, a message is sent (or attempted to be sent) to all users with
	 * cutebot admin permissions in the relevant guild
	 * @param guild the guild that had a WordfilterTimeoutException occur
	 * @param e the exception that was thrown
	 */
	private void handleWordfilterTimeout(Guild guild, WordfilterTimeoutException e) {
		WordFilter filter = this.allFilters.get(guild.getId());
		//shouldnt happen
		if(filter == null) throw new AssertionError("guild '" + MiscUtils.getGuildString(guild) + "' threw a "
				+ "WordfilterTimeoutException, but couldn't find wordfilter with that id?", e);
		
		filter.addStrike();
		String message;
		if(filter.getStrikes() >= WordFilter.getStrikesToDisable()) {
			filter.setEnabled(false);
			message = StandardMessages.wordfilterDisabled(guild);
		} else {
			message = StandardMessages.wordfilterStrike(guild, e.getType());
		}
		this.permissions.getAdmins(guild.getId()).forEach(id -> {
			this.jda.openPrivateChannelById(id).flatMap(channel -> channel.sendMessage(message)).queue();
		});
	}
	
	public String getGuildString(String id) {
		return this.jda.getGuildById(id).toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MyListener");
		return builder.toString();
	}
}
