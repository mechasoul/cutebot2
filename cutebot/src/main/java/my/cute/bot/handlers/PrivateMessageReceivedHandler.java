package my.cute.bot.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.tasks.GuildDatabaseRebuildTask;
import my.cute.bot.tasks.GuildDiscussionChannelTask;
import my.cute.bot.tasks.GuildMessageScrapeTask;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class PrivateMessageReceivedHandler {

	private final static Logger logger = LoggerFactory.getLogger(PrivateMessageReceivedHandler.class);
	
	private final MyListener bot;
	private final JDA jda;
	
	public PrivateMessageReceivedHandler(MyListener bot, JDA jda) {
		this.bot = bot;
		this.jda = jda;
	}
	
	public void handle(PrivateMessageReceivedEvent event) {
		if (event.getAuthor().getId().equals("115618938510901249") && event.getMessage().getContentDisplay().equals("!exit")) {
			event.getChannel().sendMessage("ok").queue(msg -> this.bot.shutdown(), ex -> this.bot.shutdown());
		} else if (event.getAuthor().getId().equals("115618938510901249")) {
			String id = event.getMessage().getContentRaw();
			GuildPreferences guildPrefs = this.bot.getPreferences(id);
			if(guildPrefs != null) {
				new Thread(() -> 
				{
					try {
						new GuildMessageScrapeTask(this.jda.getGuildById(id), PathUtils.getDatabaseScrapeDirectory(id), 
								guildPrefs.getDatabaseAge()).call()
						.thenRun(new GuildDiscussionChannelTask(id, guildPrefs))
						.thenRun(new GuildDatabaseRebuildTask(id, this.bot.getDatabase(id), guildPrefs))
						.whenComplete((result, throwable) ->
						{
							if(throwable == null) {
								logger.info(this + ": successfully rebuilt database for guild '" + id + "'");
							} else {
								logger.warn(this + ": encountered exception when trying to rebuild guild database '" + id
									+ "'; setup process aborted! ex: " + throwable.getMessage(), throwable);
							}
						});
					} catch (Exception e) {
						logger.warn(this + ": encountered exception when trying to rebuild guild database '" + id
						+ "'; setup process aborted! ex: " + e.getMessage(), e);
					}
				}).start();
			}
		} else {
			event.getChannel().sendMessage("??").queue();
		}
	}
}
