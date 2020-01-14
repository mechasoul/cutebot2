package my.cute.bot.handlers;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import my.cute.bot.database.GuildDatabase;
import my.cute.bot.database.GuildDatabaseBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GuildMessageReceivedHandler {
	
	private final JDA bot;
	private final GuildDatabase database;
	
	private static final Pattern BOT_NAME = Pattern.compile(".*(?:cutebot prime|cbp).*", Pattern.CASE_INSENSITIVE);
	
	public GuildMessageReceivedHandler(Guild guild, JDA bot) {
		this.bot = bot;
		this.database = new GuildDatabaseBuilder(guild)
				.build();
	}
	
	public void handle(GuildMessageReceivedEvent event) {
		
		String content = event.getMessage().getContentRaw();
		this.database.processLine(content);
		
		if(BOT_NAME.matcher(content).matches()) {
			event.getChannel().sendMessage(this.database.generateLine().toUpperCase()).queue();
		}
	}
	
	public void maintenance() {
		this.database.maintenance();
	}
	
	
}
