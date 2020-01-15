package my.cute.bot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import my.cute.bot.handlers.GuildMessageReceivedHandler;
import my.cute.bot.handlers.PrivateMessageReceivedHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MyListener extends ListenerAdapter {

	private final JDA jda;
	private final ConcurrentMap<String, GuildMessageReceivedHandler> guildMessageHandlers;
	private final PrivateMessageReceivedHandler privateMessageHandler;
	
	MyListener(JDA bot) {
		this.jda = bot;
		this.guildMessageHandlers = new ConcurrentHashMap<>(bot.getGuilds().size() * 4 / 3, 0.75f);
		bot.getGuilds().forEach(guild -> this.guildMessageHandlers.put(guild.getId(), new GuildMessageReceivedHandler(guild, bot)));
		this.privateMessageHandler = new PrivateMessageReceivedHandler(this);
	}
	
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
	
	void maintenance() {
		this.guildMessageHandlers.forEach((id, handler) -> handler.maintenance());
	}
	
	public void shutdown() {
		this.guildMessageHandlers.forEach((id, handler) -> handler.prepareForShutdown());
		this.jda.shutdown();
	}
}
