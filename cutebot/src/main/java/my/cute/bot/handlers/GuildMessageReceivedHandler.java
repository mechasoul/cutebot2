package my.cute.bot.handlers;

import java.util.regex.Pattern;

import my.cute.bot.database.GuildDatabase;
import my.cute.bot.database.GuildDatabaseBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GuildMessageReceivedHandler {
	
	private final JDA jda;
	private final GuildDatabase database;
	
	private static final Pattern BOT_NAME = Pattern.compile(".*(?:cutebot prime|cbp).*", Pattern.CASE_INSENSITIVE);
	
	public GuildMessageReceivedHandler(Guild guild, JDA jda) {
		this.jda = jda;
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
			this.database.maintenance();
		}
		return needsMaintenance;
	}
	
	public void maintenance() {
		this.database.maintenance();
	}
	
	public void prepareForShutdown() {
		this.database.save();
	}
}
