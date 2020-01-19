package my.cute.bot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import my.cute.bot.handlers.GuildMessageReceivedHandler;
import my.cute.bot.handlers.PrivateMessageReceivedHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.OkHttpClient;

public class MyListener extends ListenerAdapter {

	private final static boolean DISABLE_GUILDS = false;
	
	private final JDA jda;
	private final ConcurrentMap<String, GuildMessageReceivedHandler> guildMessageHandlers;
	private final PrivateMessageReceivedHandler privateMessageHandler;
	private final ScheduledExecutorService taskScheduler;
	
	MyListener(JDA jda) {
		this.jda = jda;
		this.guildMessageHandlers = new ConcurrentHashMap<>(jda.getGuilds().size() * 4 / 3, 0.75f);
		jda.getGuilds().forEach(guild -> this.guildMessageHandlers.put(guild.getId(), new GuildMessageReceivedHandler(guild, jda)));
		this.privateMessageHandler = new PrivateMessageReceivedHandler(this);
		
		this.taskScheduler = Executors.newSingleThreadScheduledExecutor();
		this.taskScheduler.scheduleWithFixedDelay(() -> 
		{ 
			checkMaintenance();
		}, 1, 12, TimeUnit.HOURS);
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if(event.getAuthor().isBot() || DISABLE_GUILDS) return;
		
		this.guildMessageHandlers.get(event.getGuild().getId()).handle(event);
	}
	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if(event.getAuthor().isBot()) return;
		
		this.privateMessageHandler.handle(event);
	}
	
	void maintenance() {
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
