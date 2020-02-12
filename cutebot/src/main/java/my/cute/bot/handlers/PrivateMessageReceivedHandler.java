package my.cute.bot.handlers;

import java.util.concurrent.ConcurrentHashMap;

import my.cute.bot.GuildDiscussionChannelTask;
import my.cute.bot.GuildMessageScrapeTask;
import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class PrivateMessageReceivedHandler {

	private final MyListener bot;
	private final JDA jda;
	private final ConcurrentHashMap<String, GuildPreferences> prefs;
	
	public PrivateMessageReceivedHandler(MyListener bot, JDA jda, ConcurrentHashMap<String, GuildPreferences> prefsMap) {
		this.bot = bot;
		this.jda = jda;
		this.prefs = prefsMap;
	}
	
	public void handle(PrivateMessageReceivedEvent event) {
		if (event.getAuthor().getId().equals("115618938510901249") && event.getMessage().getContentDisplay().equals("!exit")) {
			event.getChannel().sendMessage("ok").queue(msg -> this.bot.shutdown(), ex -> this.bot.shutdown());
		} else if (event.getAuthor().getId().equals("115618938510901249")) {
			Guild guild = this.jda.getGuildById(event.getMessage().getContentRaw());
			try {
				new GuildMessageScrapeTask(guild, PathUtils.getDatabaseScrapeDirectory(guild.getId()), 60).call()
					.thenRunAsync(new GuildDiscussionChannelTask(guild.getId()));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			event.getChannel().sendMessage("??").queue();
		}
	}
	
	public void addGuildPreferences(String guildId, GuildPreferences prefs) {
		this.prefs.putIfAbsent(guildId, prefs);
	}
}
