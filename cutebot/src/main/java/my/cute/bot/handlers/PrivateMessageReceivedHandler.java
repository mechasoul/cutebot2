package my.cute.bot.handlers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.database.GuildDatabase;
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
		} else if (event.getAuthor().getId().equals("115618938510901249") && event.getMessage().getContentDisplay().startsWith("!rebuild")) {
			String[] words = event.getMessage().getContentDisplay().split("\\s");
			if(words.length < 2 || words.length > 3) {
				event.getChannel().sendMessage("syntax error").queue();
				return;
			}
			String id = words[1];
			GuildPreferences guildPrefs = this.bot.getPreferences(id);
			if(guildPrefs != null) {
				if(words.length == 3) {
					try {
						guildPrefs.setDatabaseAge(Integer.parseInt(words[2]));
						guildPrefs.save();
					} catch (NumberFormatException e) {
						event.getChannel().sendMessage("error parsing new database age '" + words[2] + "'").queue();
						return;
					}
				}
				this.executor.execute(new GuildDatabaseSetupTask(this.jda, id, guildPrefs, this.bot.getDatabase(id)));
				event.getChannel().sendMessage("rebuilding database for guild " + this.jda.getGuildById(id)).queue();
			} else {
				event.getChannel().sendMessage("no such guild id found").queue();
			}
		} else if (event.getAuthor().getId().equals("115618938510901249") && event.getMessage().getContentDisplay().startsWith("!export")) {
			String[] words = event.getMessage().getContentDisplay().split("\\s");
			if(words.length != 2) {
				event.getChannel().sendMessage("syntax error").queue();
				return;
			}
			GuildDatabase db = this.bot.getDatabase(words[1]);
			if(db != null) {
				event.getChannel().sendMessage("exporting database to txt for guild '" + this.jda.getGuildById(words[1]) + "'").queue();
				db.exportToText();
			} else {
				event.getChannel().sendMessage("no such guild id found").queue();
			}
		} else if (event.getAuthor().getId().equals("115618938510901249") && event.getMessage().getContentDisplay().startsWith("!linetest")) {
			String[] words = event.getMessage().getContentDisplay().split("\\s");
			if(words.length != 3) {
				event.getChannel().sendMessage("syntax error").queue();
				return;
			}
			GuildDatabase db = this.bot.getDatabase(words[1]);
			if(db != null) {
				int iterations;
				try {
					iterations = Integer.parseInt(words[2]);
				} catch (NumberFormatException e) {
					event.getChannel().sendMessage("error parsing number of lines '" + words[2] + "'").queue();
					return;
				}
				this.executor.execute(() ->
				{
					boolean manyLines = iterations > 100;
					if(manyLines) db.prioritizeSpeed();
					try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("./testlinetest.txt"), StandardCharsets.UTF_8, 
							StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
						for(int i=0; i < iterations; i++) {
							writer.append(db.generateLine());
							writer.newLine();
						}
					} catch (IOException e) {
						event.getChannel().sendMessage("exception when writing test lines: " + e.getMessage()).queue();
						e.printStackTrace();
					}
					if(manyLines) db.prioritizeMemory();
					event.getChannel().sendMessage("line generation finished").queue();
				});
				event.getChannel().sendMessage("generating " + iterations + " lines from guild '" 
						+ this.jda.getGuildById(words[1]) + "'").queue();
			} else {
				event.getChannel().sendMessage("no such guild id found").queue();
			}
		} else {
			event.getChannel().sendMessage("??").queue();
		}
	}
	
	public ExecutorService getExecutor() {
		return this.executor;
	}
}
