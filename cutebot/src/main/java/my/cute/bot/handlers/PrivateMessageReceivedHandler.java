package my.cute.bot.handlers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.tasks.GuildDatabaseSetupTask;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class PrivateMessageReceivedHandler {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(PrivateMessageReceivedHandler.class);
	
	private final MyListener bot;
	private final JDA jda;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	public PrivateMessageReceivedHandler(MyListener bot, JDA jda) {
		this.bot = bot;
		this.jda = jda;
	}
	
	public void handle(PrivateMessageReceivedEvent event) {
		if (event.getAuthor().getId().equals("115618938510901249") && event.getMessage().getContentDisplay().equals("!exit")) {
			event.getChannel().sendMessage("ok").queue(msg -> this.bot.shutdown(), ex -> this.bot.shutdown());
		} else if (event.getAuthor().getId().equals("115618938510901249") && event.getMessage().getContentDisplay().startsWith("!status")) {
			String[] words = event.getMessage().getContentDisplay().split("\\s");
			if(words.length == 1) {
				this.jda.getPresence().setActivity(null);
				event.getChannel().sendMessage("resetting").queue();
			} else {
				event.getChannel().sendMessage("set status to '" + words[1] + "'").queue();
				this.jda.getPresence().setActivity(Activity.playing(words[1]));
			}
		} else if (event.getAuthor().getId().equals("115618938510901249")) {
			String id = event.getMessage().getContentRaw();
			GuildPreferences guildPrefs = this.bot.getPreferences(id);
			if(guildPrefs != null) {
				this.executor.execute(new GuildDatabaseSetupTask(this.jda, id, guildPrefs, this.bot.getDatabase(id)));
				event.getChannel().sendMessage("rebuilding database for guild " + this.jda.getGuildById(id)).queue();
			} else {
				event.getChannel().sendMessage("no such guild id found").queue();
			}
		} else {
			event.getChannel().sendMessage("??").queue();
		}
	}
}
