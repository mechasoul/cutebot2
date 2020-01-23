package my.cute.bot.handlers;

import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.database.GuildDatabase;
import my.cute.bot.database.GuildDatabaseBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GuildMessageReceivedHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildMessageReceivedHandler.class);
	private static final Pattern BOT_NAME = Pattern.compile(".*(?:cutebot prime|cbp).*", Pattern.CASE_INSENSITIVE);
	
	
	@SuppressWarnings("unused")
	private final JDA jda;
	private final long id;
	private final GuildDatabase database;
	
	public GuildMessageReceivedHandler(Guild guild, JDA jda) {
		this.jda = jda;
		this.id = guild.getIdLong();
		this.database = new GuildDatabaseBuilder(guild)
				.build();
		this.database.load();
	}
	
	public void handle(GuildMessageReceivedEvent event) {
		
		String content = event.getMessage().getContentRaw();
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
