package my.cute.bot.handlers;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.GuildMessageScrapeTask;
import my.cute.bot.commands.CommandSet;
import my.cute.bot.commands.CommandSetFactory;
import my.cute.bot.database.GuildDatabase;
import my.cute.bot.database.GuildDatabaseBuilder;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GuildMessageReceivedHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildMessageReceivedHandler.class);
	private static final Pattern BOT_NAME = Pattern.compile(".*(?:cutebot prime|cbp).*", Pattern.CASE_INSENSITIVE);
	
	private static int count=0;
	private static long time=0;
	
	private final JDA jda;
	private final long id;
	private final GuildDatabase database;
	private final GuildPreferences prefs;
	private final CommandSet commands;
	
	public GuildMessageReceivedHandler(Guild guild, JDA jda, GuildPreferences prefs) {
		this.jda = jda;
		this.id = guild.getIdLong();
		this.prefs = prefs;
		this.prefs.load();
		this.database = new GuildDatabaseBuilder(guild)
				.databaseAge(this.prefs.getDatabaseAge())
				.build();
		this.database.load();
		this.commands = CommandSetFactory.newDefaultTextChannelSet();
	}
	
	public void handle(GuildMessageReceivedEvent event) {
		
		String content = event.getMessage().getContentRaw();
		
		if(!StringUtils.isWhitespace(content)) {
			//message nonempty. check for command
			String firstWord = content.split("\\s")[0];
			if(firstWord.startsWith(this.prefs.getPrefix())) {
				//first word starts with designated command prefix. check if it's a command
				String commandName = firstWord.substring(this.prefs.getPrefix().length());
				if(this.commands.execute(commandName, event.getMessage())) {
					//don't process commands into database
					return;
				}
			}
		}
		
		if(content.equals("abab") && event.getAuthor().getId().equals("115618938510901249")) {
			new GuildMessageScrapeTask(event.getGuild(), PathUtils.getDatabaseScrapeDirectory(event.getGuild().getId()),
					2).run();
			return;
		}
		
		this.database.processLine(content);
		
		if(BOT_NAME.matcher(content).matches()) {
			event.getChannel().sendMessage(this.database.generateLine()).queue();
		}
	}
	
	public boolean checkMaintenance() {
		boolean needsMaintenance = this.database.needsMaintenance();
		if(needsMaintenance) {
			ForkJoinPool.commonPool().execute(() -> 
			{
				try {
					this.database.maintenance();
				} catch (Throwable th) {
					logger.error("maintenance on guild " + this.id + " terminated due to throwable: " + th.getMessage(), th);
				}
			});
		}
		return needsMaintenance;
	}
	
	public void maintenance() {
		this.database.maintenance();
	}
	
	public void prepareForShutdown() {
		this.database.shutdown();
	}
	
	static String getFormattedTime(long millis) {
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		millis -= TimeUnit.MINUTES.toMillis(minutes);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		millis -= TimeUnit.SECONDS.toMillis(seconds);
		StringBuilder sb = new StringBuilder();
		sb.append(minutes);
		sb.append(":");
		sb.append(seconds);
		sb.append(".");
		sb.append(millis);
		return sb.toString();
	}
}
