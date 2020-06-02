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
	 * returns a CompletableFuture that completes normally when the database was set up
	 * without any problems
	 * 
	 * uh i'm not 100% on the way i'm doing this with returning a completablefuture and whatever
	 * because if i do like CompletableFuture.runAsync(new GuildDatabaseSetupTask(...), executor)
	 * the returned completablefuture has this completablefuture as its result which feels really
	 * really weird and wrong, but if i don't return that future (eg just make this runnable) then 
	 * CompletableFuture.runAsync(new GuildDatabaseSetupTask(...), executor) returns a future that
	 * will basically just be trivially completed, since all the actual processing here happens 
	 * within futures, and access to these futures is lost if i don't specifically return the future
	 * so basically i need GuildMessageScrapeTask to return a future that represents all the scrapes
	 * so i can do things when they're finished, and i need to return the eventual future that's 
	 * chained off of that so i can do things when the entire setup task is finished
	 * but then in order to do something with it from another thread i end up doing like
	 * CompletableFuture.supplyAsync(() -> { return new GuildDatabaseSetupTask(..).call(); })
	 * then using the result of that future which is the actual future returned by call()
	 * it feels insanely convoluted and unnecessary but i'm not sure how to avoid going through all
	 * of this while still retaining the ability to do things when the setup task completes / know 
	 * if the setup task completed successfully (also see PrivateChannelRebuildCommand.execute() for 
	 * use case)
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
