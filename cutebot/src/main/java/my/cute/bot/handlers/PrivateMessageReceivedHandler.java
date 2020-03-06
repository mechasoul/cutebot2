package my.cute.bot.handlers;

import java.util.concurrent.ConcurrentHashMap;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.tasks.GuildDiscussionChannelTask;
import my.cute.bot.tasks.GuildMessageScrapeTask;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;
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
			String id = event.getMessage().getContentRaw();
			GuildPreferences guildPrefs = this.prefs.get(id);
			if(guildPrefs != null) {
				try {
					new GuildMessageScrapeTask(this.jda.getGuildById(id), PathUtils.getDatabaseScrapeDirectory(id), 
							guildPrefs.getDatabaseAge()).call()
						.thenRunAsync(new GuildDiscussionChannelTask(id, guildPrefs));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			event.getChannel().sendMessage("??").queue();
		}
	}
	
	public void addGuildPreferencesIfAbsent(String guildId, GuildPreferences prefs) {
		this.prefs.putIfAbsent(guildId, prefs);
	}
}
