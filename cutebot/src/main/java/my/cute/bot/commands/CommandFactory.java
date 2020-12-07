package my.cute.bot.commands;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.wordfilter.WordFilter;
import net.dv8tion.jda.api.JDA;

public class CommandFactory {

	//TODO load user-defined commands from a file somewhere
	public static CommandSet<TextChannelCommand> newDefaultTextChannelSet(GuildPreferences prefs, JDA jda, String id) {
		CommandSet<TextChannelCommand> set = new CommandSetImpl<TextChannelCommand>(2);
		set.put("quote", new TextChannelQuoteCommand(prefs, jda, id));
		return set;
	}
	
	public static CommandSet<PrivateChannelCommand> newDefaultPrivateChannelSet(JDA jda, MyListener bot, 
			DefaultGuildDatabase defaultGuilds, Map<String, GuildPreferences> allPrefs, Map<String, WordFilter> allFilters, ExecutorService executor) {
		CommandSet<PrivateChannelCommand> set = new CommandSetImpl<PrivateChannelCommand>(17);
		set.put(PrivateChannelAutoCommand.NAME, new PrivateChannelAutoCommand(allPrefs));
		set.put(PrivateChannelChannelCommand.NAME, new PrivateChannelChannelCommand(jda));
		set.put(PrivateChannelDefaultCommand.NAME, new PrivateChannelDefaultCommand(jda, defaultGuilds));
		set.put(PrivateChannelExitCommand.NAME, new PrivateChannelExitCommand(bot));
		set.put(PrivateChannelExportCommand.NAME, new PrivateChannelExportCommand(bot));
		set.put(PrivateChannelFilterCommand.NAME, new PrivateChannelFilterCommand(allFilters));
		set.put(PrivateChannelGuildCommand.NAME, new PrivateChannelGuildCommand(jda));
		set.put(PrivateChannelMaintCommand.NAME, new PrivateChannelMaintCommand(bot));
		set.put(PrivateChannelRebuildCommand.NAME, new PrivateChannelRebuildCommand(jda, bot, executor, allPrefs));
		set.put(PrivateChannelStatusCommand.NAME, new PrivateChannelStatusCommand(jda));
		
		return set;
	}
	
}
