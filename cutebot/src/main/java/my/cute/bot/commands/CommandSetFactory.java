package my.cute.bot.commands;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.wordfilter.WordFilter;
import net.dv8tion.jda.api.JDA;

public class CommandSetFactory {

	public static CommandSet<TextChannelCommand> newDefaultTextChannelSet(GuildPreferences prefs) {
		CommandSet<TextChannelCommand> set = new CommandSetImpl<TextChannelCommand>(2);
		set.put("quote", new TextChannelQuoteCommand(prefs));
		return set;
	}
	
	public static CommandSet<PrivateChannelCommand> newDefaultPrivateChannelSet(JDA jda, MyListener bot, 
			DefaultGuildDatabase defaultGuilds, Map<String, GuildPreferences> allPrefs, Map<String, WordFilter> allFilters, ExecutorService executor) {
		CommandSet<PrivateChannelCommand> set = new CommandSetImpl<PrivateChannelCommand>(17);
		set.put("auto", new PrivateChannelAutoCommand(bot, allPrefs));
		set.put("channel", new PrivateChannelChannelCommand(jda));
		set.put("default", new PrivateChannelDefaultCommand(jda, defaultGuilds));
		set.put("exit", new PrivateChannelExitCommand(bot));
		set.put("export", new PrivateChannelExportCommand(bot));
		//TODO 
		//set.put("filter", new PrivateChannelFilterCommand());
		set.put("guild", new PrivateChannelGuildCommand(jda));
		set.put("maint", new PrivateChannelMaintCommand(bot));
		set.put("rebuild", new PrivateChannelRebuildCommand(jda, bot, executor, allPrefs));
		set.put("status", new PrivateChannelStatusCommand(jda));
		
		return set;
	}
	
}
