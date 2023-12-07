package my.cute.bot.tasks;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.database.GuildDatabase;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;

public class GuildDatabaseSetupTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(GuildDatabaseSetupTask.class);
	private final JDA jda;
	private final Guild guild;
	private final GuildPreferences prefs;
	private final GuildDatabase db;
	
	public GuildDatabaseSetupTask(JDA jda, Guild guild, GuildPreferences prefs, GuildDatabase db) {
		this.jda = jda;
		this.guild = guild;
		this.prefs = prefs;
		this.db = db;
	}
	

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GuildDatabaseSetupTask-");
		builder.append(this.guild.getId());
		return builder.toString();
	}

	/*
	 * sets up a new guild's database
	 * scrapes all messages from the specified period (as given from the guild's preferences)
	 * in all visible channels and stores them locally, uses the local files to determine 
	 * designated "discussion channels", then discards the existing database and processes
	 * all lines from the local scraped message files for designated discussion channels
	 * 
	 * returns a CompletableFuture that completes normally when the database was set up
	 * without any problems
	 * 
	 * TODO consider having this class use whatever executor it's run with when starting new
	 * GuildMessageScrapeTask? currently just running everything in common forkjoinpool but
	 * may want to specify an executor instead for this task and subtasks
	 */
	@Override
	public void run() {
		boolean doingNothing = this.jda.getPresence().getActivity() == null;
		if(doingNothing) this.jda.getPresence().setActivity(Activity.playing("busy"));
		Set<String> cuteChannels = this.guild.getChannels().stream().filter(channel -> channel.getName() != null && channel.getName().contains("cute"))
				.map(channel -> channel.getId()).collect(Collectors.toSet());
		CompletableFuture.runAsync(new GuildMessageScrapeTask(this.guild, PathUtils.getDatabaseScrapeDirectory(this.guild.getId()), 
				this.prefs.getDatabaseAge()))
				.thenRunAsync(new GuildDiscussionChannelTask(this.guild.getId(), this.prefs, cuteChannels))
				.thenRunAsync(new GuildDatabaseRebuildTask(this.guild.getId(), this.db, this.prefs))
				.whenCompleteAsync((result, throwable) -> {
					if(throwable == null) {
						logger.info(this + ": successfully set up new guild '" + MiscUtils.getGuildString(this.guild) + "'");
					} else {
						logger.warn(this + ": encountered exception when trying to set up new guild '" 
								+ MiscUtils.getGuildString(this.guild) + "'; setup process aborted!", throwable);
					}
					if(doingNothing) this.jda.getPresence().setActivity(null);
				//call join so this task doesn't complete until all of its subtasks complete
				}).join();
	}

}
