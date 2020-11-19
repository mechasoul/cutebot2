package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import net.dv8tion.jda.api.JDA;

public class CommandSetFactory {

	public static CommandSet<TextChannelCommand> newDefaultTextChannelSet(GuildPreferences prefs) {
		CommandSet<TextChannelCommand> set = new CommandSetImpl<TextChannelCommand>(2);
		set.put("quote", new TextChannelQuoteCommand(prefs));
		return set;
	}
	
	public static CommandSet<PrivateChannelCommand> newDefaultPrivateChannelSet(JDA jda, MyListener bot, DefaultGuildDatabase defaultGuilds) {
		CommandSet<PrivateChannelCommand> set = new CommandSetImpl<PrivateChannelCommand>(17);
		set.put("exit", new PrivateChannelExitCommand(bot));
		set.put("default", new PrivateChannelDefaultCommand(jda, defaultGuilds));
		return set;
	}
	
}
