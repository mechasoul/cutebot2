package my.cute.bot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.database.GuildDatabase;
import my.cute.bot.handlers.GuildMessageReceivedHandler;
import my.cute.bot.handlers.PrivateMessageReceivedHandler;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.GuildPreferencesFactory;
import my.cute.bot.tasks.GuildDatabaseSetupTask;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.OkHttpClient;

public class MyListener extends ListenerAdapter {
	
	@SuppressWarnings("unused")
	private class AutomaticMessageTask implements Runnable {
		
		@Override
		public void run() {
			for(String id : automaticMessageGuilds) {
				try {
					String line = getDatabase(id).generateLine();
					Guild guild = jda.getGuildById(id);
					guild.getDefaultChannel().sendMessage(line).queue();
					logger.info("AutomaticMessageTask: sent line '" + line + "' in guild '" + jda.getGuildById(id)
						+ "' in channel '" + guild.getDefaultChannel());
				} catch (IOException e) {
					logger.warn("AutomaticMessageTask: encountered IOException on guild '" + jda.getGuildById(id) + "'", e);
				}
			}
			int delay = RAND.nextInt(180) + 30;
			taskScheduler.schedule(this, delay, TimeUnit.MINUTES);
			logger.info("AutomaticMessageTask: scheduled next automatic message in " + delay + "min");
		}
		
	}
	
	private static final Logger logger = LoggerFactory.getLogger(MyListener.class);
	private static final Random RAND = new Random();
	
	private final JDA jda;
	private final ConcurrentMap<String, GuildMessageReceivedHandler> guildMessageHandlers;
	private final PrivateMessageReceivedHandler privateMessageHandler;
	private final ScheduledExecutorService taskScheduler;
	private final List<String> automaticMessageGuilds;
	
	MyListener(JDA jda) throws IOException {
		this.jda = jda;
		this.automaticMessageGuilds = Collections.synchronizedList(new ArrayList<>());
		this.automaticMessageGuilds.add("101153748377686016");
		this.guildMessageHandlers = new ConcurrentHashMap<>(jda.getGuilds().size() * 4 / 3, 0.75f);
		this.privateMessageHandler = new PrivateMessageReceivedHandler(this, jda);
		
		this.taskScheduler = Executors.newScheduledThreadPool(2);
		
		try {
			jda.getGuilds().forEach(guild -> {
				try {
					GuildPreferences prefs = GuildPreferencesFactory.loadGuildPreferences(guild.getId());
					this.guildMessageHandlers.put(guild.getId(), new GuildMessageReceivedHandler(guild, jda, prefs, this.taskScheduler));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		
		
		this.taskScheduler.scheduleWithFixedDelay(() -> 
		{ 
			checkMaintenance();
		}, 1, 12, TimeUnit.HOURS);
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
		
		this.guildMessageHandlers.get(event.getGuild().getId()).handle(event);
	}
	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if(event.getAuthor().isBot()) return;
		
		this.privateMessageHandler.handle(event);
	}
	
	//note that this event may be fired mistakenly on a guild we're already in? so needs to be ok with that
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		GuildPreferences prefs = GuildPreferencesFactory.newDefaultGuildPreferences(event.getGuild().getId());;
		boolean newGuild = false;
		//preliminary check to avoid unnecessary GuildMessageReceivedHandler creation since it's slightly expensive
		if(!this.guildMessageHandlers.containsKey(event.getGuild().getId())) {
			//verify that guild hasn't been added in the meantime by other thread
			try {
				newGuild = this.guildMessageHandlers.putIfAbsent(event.getGuild().getId(), 
						new GuildMessageReceivedHandler(event.getGuild(), jda, prefs, this.taskScheduler)) == null;
			} catch (IOException e) {
				logger.error(this + ": encountered IOException when trying to construct GuildMessageReceivedHandler for new guild '" +
						event.getGuild() + "', can't continue!", e);
				this.shutdown();
			}
		}
		
		if(newGuild) {
			String id = event.getGuild().getId().intern();
			this.taskScheduler.submit(new GuildDatabaseSetupTask(this.jda, id, this.getPreferences(id), this.getDatabase(id)));
		}
	}
	
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
	
	public GuildPreferences getPreferences(String id) {
		GuildMessageReceivedHandler handler = this.guildMessageHandlers.get(id);
		if(handler != null) {
			return handler.getPreferences();
		} else {
			return null;
		}
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
