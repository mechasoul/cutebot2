package my.cute.bot.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.tasks.GuildDatabaseSetupTask;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelRebuildCommand extends PrivateChannelCommand {

	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelRebuildCommand.class);
	
	private final JDA jda;
	private final MyListener bot;
	private final ExecutorService executor;
	
	public PrivateChannelRebuildCommand(JDA jda, MyListener bot, ExecutorService executor) {
		/*
		 * don't use default guild for this command
		 * rebuilding database is a substantial task and so 
		 * the dev must specify exactly what to do
		 */
		super("rebuild", PermissionLevel.DEVELOPER, 1, 2, false);
		this.jda = jda;
		this.bot = bot;
		this.executor = executor;
	}
	
	@Override
	public void execute(Message message) {
		String words[] = MiscUtils.sanitize(message.getContentDisplay()).split("\\s");
		if(words[1].equals("all")) {
			logger.info(this + ": beginning rebuild on all guilds");
			message.getChannel().sendMessage("beginning rebuild on all guilds").queue();
			
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			
			Activity previousActivity = this.jda.getPresence().getActivity();
			this.jda.getPresence().setActivity(Activity.playing("VERY busy"));
			
			for(String id : this.jda.getGuilds().stream().map(guild -> guild.getId()).collect(Collectors.toList())) {
				GuildPreferences guildPrefs = this.bot.getPreferences(id);
				if(guildPrefs != null) {
					/*
					 * this feels terrible but idk how else to do this
					 * see GuildDatabaseSetupTask.call() for reasoning / dilemma that motivated me to do it this way
					 */
					CompletableFuture.supplyAsync(() -> {
						try {
							return new GuildDatabaseSetupTask(this.jda, id, guildPrefs, this.bot.getDatabase(id)).call();
						} catch (Exception e) {
							return CompletableFuture.<Void>failedFuture(e);
						}
					}, this.executor).whenComplete((result, throwable) -> {
						futures.add(result);
					});
				} else {
					logger.warn(this + ": found id in guild list '" + id + "' with no corresponding prefs object?");
				}
			}
			
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
			.whenComplete((result, throwable) -> {
				if(throwable == null) {
					logger.info(this + ": successfully completed rebuild on all guilds");
					message.getChannel().sendMessage("successfully completed rebuild on all guilds").queue();
				} else {
					logger.info(this + ": encountered problems during rebuild. likely not successful");
					message.getChannel().sendMessage("encountered problems during rebuild. likely not successful").queue();
				}
				this.jda.getPresence().setActivity(previousActivity);
			});
		} else {
			String id = words[1];
			GuildPreferences guildPrefs = this.bot.getPreferences(id);
			if(guildPrefs != null) {
				if(words.length == 3) {
					try {
						guildPrefs.setDatabaseAge(Integer.parseInt(words[2]));
						guildPrefs.save();
					} catch (NumberFormatException e) {
						message.getChannel().sendMessage("error parsing new database age '" + words[2] + "'").queue();
						return;
					}
				}
				this.executor.submit(new GuildDatabaseSetupTask(this.jda, id, guildPrefs, this.bot.getDatabase(id)));
				message.getChannel().sendMessage("rebuilding database for guild " + this.jda.getGuildById(id)).queue();
			} else {
				message.getChannel().sendMessage("no such guild id found").queue();
			}
		}
	}

}
