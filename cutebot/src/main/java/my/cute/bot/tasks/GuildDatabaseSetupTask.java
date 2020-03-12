package my.cute.bot.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.database.GuildDatabase;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

public class GuildDatabaseSetupTask implements Callable<CompletableFuture<Void>> {

	private static final Logger logger = LoggerFactory.getLogger(GuildDatabaseSetupTask.class);
	private final JDA jda;
	private final String id;
	private final GuildPreferences prefs;
	private final GuildDatabase db;
	
	public GuildDatabaseSetupTask(JDA jda, String guildId, GuildPreferences prefs, GuildDatabase db) {
		this.jda = jda;
		this.id = guildId;
		this.prefs = prefs;
		this.db = db;
	}
	

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GuildDatabaseSetupTask-");
		builder.append(id);
		return builder.toString();
	}

	/*
	 * sets up a new guild's database
	 * scrapes all messages from the specified period (as given from the guild's preferences)
	 * in all visible channels and stores them locally, uses the local files to determine 
	 * designated "discussion channels", then discards the existing database and processes
	 * all lines from the local scraped message files for designated discussion channels
	 * 
	 * TODO GuildMessageScrapeTask.call() returns a future consisting of all futures 
	 * representing the channel scrapes, which happen in some other threads and then
	 * i call thenRun() on that future so i think it executes in whatever thread was 
	 * scraping? probably dont do that
	 * 
	 * returns a CompletableFuture that completes normally when the database was set up
	 * without any problems
	 */
	@Override
	public CompletableFuture<Void> call() throws Exception {
		boolean doingNothing = this.jda.getPresence().getActivity() == null;
		if(doingNothing) this.jda.getPresence().setActivity(Activity.playing("busy"));
		return new GuildMessageScrapeTask(this.jda.getGuildById(this.id), PathUtils.getDatabaseScrapeDirectory(id), 
				prefs.getDatabaseAge()).call()
		.thenRun(new GuildDiscussionChannelTask(id, prefs))
		.thenRun(new GuildDatabaseRebuildTask(id, db, prefs))
		.whenComplete((result, throwable) ->
		{
			if(throwable == null) {
				logger.info(this + ": successfully set up new guild '" + this.jda.getGuildById(this.id) + "'");
			} else {
				logger.warn(this + ": encountered exception when trying to set up new guild '" 
						+ this.jda.getGuildById(this.id) + "'; setup process aborted! ex: " 
						+ throwable.getMessage(), throwable);
			}
			if(doingNothing) this.jda.getPresence().setActivity(null);
		});
		
	}

}
