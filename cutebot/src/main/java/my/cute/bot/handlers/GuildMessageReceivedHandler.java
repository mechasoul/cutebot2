package my.cute.bot.handlers;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.CutebotTask;
import my.cute.bot.commands.CommandSet;
import my.cute.bot.commands.CommandSetFactory;
import my.cute.bot.commands.TextChannelCommand;
import my.cute.bot.database.GuildDatabase;
import my.cute.bot.database.GuildDatabaseBuilder;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.tasks.GuildDatabaseSetupTask;
import my.cute.bot.util.MiscUtils;
import my.cute.markov2.exceptions.ReadObjectException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class GuildMessageReceivedHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildMessageReceivedHandler.class);
	@SuppressWarnings("unused")
	private static final Pattern BOT_NAME = (CutebotTask.ACTIVE_TOKEN == CutebotTask.CUTEBOT_PRIME_TOKEN) ?
			Pattern.compile(".*(?:cutebot prime|cbp).*", Pattern.CASE_INSENSITIVE) : 
			Pattern.compile(".*(?:cutebot).*", Pattern.CASE_INSENSITIVE);
	
	private final JDA jda;
	private final String id;
	private final GuildDatabase database;
	private final GuildPreferences prefs;
	private final CommandSet<TextChannelCommand> commands;
	private final Random random = new Random();
	private final ExecutorService executor;
	
	private long lastAutoMessageTime = 0;
	private long timeUntilNextAutoMessage;
	
	public GuildMessageReceivedHandler(Guild guild, JDA jda, GuildPreferences prefs, ExecutorService executor) throws IOException {
		this.jda = jda;
		this.id = guild.getId();
		this.prefs = prefs;
		this.executor = executor;
		this.database = new GuildDatabaseBuilder(guild)
				.databaseAge(this.prefs.getDatabaseAge())
				.build();
		this.database.load();
		this.commands = CommandSetFactory.newDefaultTextChannelSet(this.prefs);
		
		this.timeUntilNextAutoMessage = this.getTimeInBetweenAutoMessages(this.prefs.getAutomaticResponseTime());
	}
	
	/*
	 * TODO this is a really big method can maybe separate it into pieces or whatever
	 */
	public void handle(GuildMessageReceivedEvent event) {
		
		String content = event.getMessage().getContentRaw();
		
		if(!StringUtils.isWhitespace(content)) {
			//message nonempty. check for command
			String[] params = MiscUtils.getWords(event.getMessage());
			String firstWord = params[0];
			if(firstWord.startsWith(this.prefs.getPrefix())) {
				//first word starts with designated command prefix. check if it's a command
				String commandName = firstWord.substring(this.prefs.getPrefix().length());
				if(this.commands.execute(commandName, event.getMessage(), params)) {
					//don't process commands into database
					return;
				}
			}
		}
		
		try {
			
			this.database.processLine(content);
			
		} catch (ReadObjectException e) {
			/*
			 * TODO is it possible for this to be something other than IOException?
			 * ie, a problem with loading a shard results in some other exception somehow?
			 */
			logger.info(this + ": encountered ReadObjectException during line processing. beginning automatic database restore", e);
			
			if(this.database.restoreFromAutomaticBackups()) {
				
				try {
					this.database.clearAutomaticBackups();
				} catch (IOException e1) {
					logger.warn(this + ": exception when trying to clear automatic backups after restoring from backup", e);
				}
				//should just run maintenance immediately instead?
				this.database.markForMaintenance();
			} else {
				try {
					this.database.clearAutomaticBackups();
				} catch (IOException e1) {
					logger.warn(this + ": exception when trying to clear automatic backups after restoring from backup", e);
				}
				this.database.markForMaintenance();
				this.executor.submit(new GuildDatabaseSetupTask(this.jda, this.id, this.prefs, this.database));
			}
			//don't continue with line generation and whatever if the database is broken
			return;
		} catch (IOException e) {
			/*
			 * could indicate a problem with writing to workingset file, or something else
			 * currently just logging. should maybe throw fatal exception if workingset 
			 * isnt working properly?
			 */
			logger.warn(this + ": general IOException thrown during line processing - possible workingset inconsistency!", e);
		}
			
		try {
			if(this.shouldSendAutomaticMessage()) {
				String line = this.database.generateLine();
				event.getChannel().sendMessage(line).queue();
				this.lastAutoMessageTime = System.currentTimeMillis();
				this.timeUntilNextAutoMessage = this.getTimeInBetweenAutoMessages(this.prefs.getAutomaticResponseTime());
				logger.info(this + ": sent automatic message '" + line + "', next automatic message scheduled in " 
						+ (this.timeUntilNextAutoMessage / 60000L) + " mins");
			} else if(BOT_NAME.matcher(content).matches()) {
				if(isQuestion(content)) {
					event.getChannel().sendMessage(this.database.generateLine()).queue();
				} else {
					addReactionToMessage(event.getMessage());
				}
			} else if (content.contains("mothyes")) {
				addReactionToMessage(event.getMessage());
			}
		} catch (InsufficientPermissionException e) {
			/*
			 * missing permission for sendMessage() or similar
			 * do nothing
			 */
		} catch (IOException e) {
			/*
			 * an IOException here is technically not fatal. we can wait for it to be thrown
			 * from a more critical spot, and just continue without doing line generation here
			 */
			logger.warn(this + ": encountered IOException during line generation", e);
		}
	}
	
	public boolean checkMaintenance() {
		boolean needsMaintenance = this.database.needsMaintenance();
		if(needsMaintenance) {
			this.maintenance();
		}
		return needsMaintenance;
	}
	
	public void maintenance() {
		this.executor.execute(() -> 
		{
			try {
				this.database.maintenance();
			} catch (Throwable th) {
				logger.error(this + ": maintenance on guild " + this.id + " terminated due to throwable", th);
			}
		});
	}
	
	public void prepareForShutdown() {
		try {
			this.database.shutdown();
		} catch (IOException e) {
			logger.warn(this + ": encountered IOException during shutdown, possible database inconsistency", e);
		}
	}
	
	public GuildDatabase getDatabase() {
		return this.database;
	}
	
	public GuildPreferences getPreferences() {
		return this.prefs;
	}
	
	public void updatePreferences() {
		this.timeUntilNextAutoMessage = this.getTimeInBetweenAutoMessages(this.prefs.getAutomaticResponseTime());
	}
	
	//TODO this sucks
	private static boolean isQuestion(String s) {
		if(s.contains("what") || s.contains("who") || s.contains("why") || s.contains("wat") || s.contains("how") || s.contains("when") || s.contains("will") || s.contains("are you") || s.contains("are u") || s.contains("can you") || s.contains("do you") || s.contains("can u") || s.contains("do u") || s.contains("where") || s.contains("?")) {
			return true;
		} else {
			return false;
		}
	}
	
	private long getTimeInBetweenAutoMessages(final int autoResponseTime) {
		int minutes;
		if(autoResponseTime == 0) {
			minutes = 0;
		} else {
			minutes = this.random.nextInt(autoResponseTime) + autoResponseTime;
		}
		return ((long)minutes) * 60000L;
	}
	
	private boolean shouldSendAutomaticMessage() {
		return this.timeUntilNextAutoMessage != 0 && 
				(System.currentTimeMillis() - this.lastAutoMessageTime) >= this.timeUntilNextAutoMessage;
	}
	
	private void addReactionToMessage(Message message) {
		try {
			if(random.nextInt(10) == 0) {
				message.addReaction(MiscUtils.getRandomEmoteFromCache(this.jda)).queue();
			} else {
				//mothyes
				message.addReaction(this.jda.getEmoteById("242763939631333378")).queue();
			}
		} catch (IllegalArgumentException e) {
			/*
			 * thrown from MessageaddReaction(Emote) if the emote can't be used in the given channel
			 * do nothing
			 */
		}
	}
	
	@Override
	public String toString() {
		return "GuildMessageReceivedHandler-" + this.id;
	}
}
