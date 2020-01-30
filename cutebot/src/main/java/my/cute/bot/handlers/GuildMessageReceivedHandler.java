package my.cute.bot.handlers;

import java.io.File;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.commands.CommandSet;
import my.cute.bot.commands.CommandSetFactory;
import my.cute.bot.database.GuildDatabase;
import my.cute.bot.database.GuildDatabaseBuilder;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.GuildPreferencesFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GuildMessageReceivedHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildMessageReceivedHandler.class);
	private static final Pattern BOT_NAME = Pattern.compile(".*(?:cutebot prime|cbp).*", Pattern.CASE_INSENSITIVE);
	private static final String DATABASE_PARENT_DIRECTORY = "." + File.separator + "cutebot2db";
	
	@SuppressWarnings("unused")
	private final JDA jda;
	private final long id;
	private final GuildDatabase database;
	private final GuildPreferences prefs;
	private final CommandSet commands;
	
	public GuildMessageReceivedHandler(Guild guild, JDA jda) {
		this.jda = jda;
		this.id = guild.getIdLong();
		this.database = new GuildDatabaseBuilder(guild, DATABASE_PARENT_DIRECTORY)
				.build();
		this.database.load();
		this.prefs = GuildPreferencesFactory.newDefaultGuildPreferences();
		this.prefs.load();
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
}
