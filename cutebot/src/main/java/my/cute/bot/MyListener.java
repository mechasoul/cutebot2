package my.cute.bot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import my.cute.bot.handlers.GuildMessageReceivedHandler;
import my.cute.bot.handlers.PrivateMessageReceivedHandler;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.GuildPreferencesFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.OkHttpClient;

public class MyListener extends ListenerAdapter {
	
	private final JDA jda;
	private final ConcurrentMap<String, GuildMessageReceivedHandler> guildMessageHandlers;
	private final PrivateMessageReceivedHandler privateMessageHandler;
	private final ScheduledExecutorService taskScheduler;
	
	MyListener(JDA jda) {
		this.jda = jda;
		this.guildMessageHandlers = new ConcurrentHashMap<>(jda.getGuilds().size() * 4 / 3, 0.75f);
		ConcurrentHashMap<String, GuildPreferences> prefsMap = new ConcurrentHashMap<>(jda.getGuilds().size() * 4 / 3, 0.75f);
		jda.getGuilds().forEach(guild -> {
			GuildPreferences prefs = GuildPreferencesFactory.newDefaultGuildPreferences(guild.getId());
			prefsMap.put(guild.getId(), prefs);
			this.guildMessageHandlers.put(guild.getId(), new GuildMessageReceivedHandler(guild, jda, prefs));
		});
		this.privateMessageHandler = new PrivateMessageReceivedHandler(this, jda, prefsMap);
		
		this.taskScheduler = Executors.newSingleThreadScheduledExecutor();
		this.taskScheduler.scheduleWithFixedDelay(() -> 
		{ 
			checkMaintenance();
		}, 1, 12, TimeUnit.HOURS);
	}
	/*
	 * TODO
	 * check for message deletion event
	 * if possible, have message deletion add the deleted content to a deletedmessages.txt file
	 * during maintenance, scan this file over the entire workingset and remove each line in deleted
	 * messages from the workingset once (and remove from database ofc). this way message deletion
	 * will be reflected in db
	 * 
	 * can psosibly do the same thing with message edits?
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
		GuildPreferences prefs = GuildPreferencesFactory.newDefaultGuildPreferences(event.getGuild().getId());
		this.guildMessageHandlers.putIfAbsent(event.getGuild().getId(), new GuildMessageReceivedHandler(event.getGuild(), jda, prefs));
		this.privateMessageHandler.addGuildPreferences(event.getGuild().getId(), prefs);
		/*
		 * TODO
		 * get guild messages from all visible channels, from now to age limit
		 * use messages to determine discussion channels
		 * process all messages from discussion channels
		 * determine default channel from discussion channels
		 * save messages to directory under db parent, each channel w/ own file
		 * 		insert lines at start of channel messages file with start and end dates of scraped msg period
		 * 		mark each msg with a timestamp at start like we do w/ workingset 
		 */
//		this.taskScheduler.execute(() ->
//		{
//			Guild guild = event.getGuild();
//			guild.getTextChannelCache().forEach(channel ->
//			{
//				
//				channel.getIterableHistory().cache(false).forEachAsync(action)
//			});
//		});
	}
	
	void maintenance() {
		/*
		 * TODO
		 * discussion channel updating should occur periodically in maintenance
		 * doesnt need to happen every maintenance tho and in fact should not
		 */
		this.guildMessageHandlers.forEach((id, handler) -> handler.maintenance());
	}
	
	void checkMaintenance() {
		this.guildMessageHandlers.forEach((id, handler) -> handler.checkMaintenance());
	}
	
	public void shutdown() {
		this.guildMessageHandlers.forEach((id, handler) -> handler.prepareForShutdown());
		this.taskScheduler.shutdownNow();
		OkHttpClient client = this.jda.getHttpClient();
		client.connectionPool().evictAll();
		client.dispatcher().executorService().shutdown();
		this.jda.shutdown();
	}
}
