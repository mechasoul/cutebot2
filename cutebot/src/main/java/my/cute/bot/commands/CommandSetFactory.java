package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;

public class CommandSetFactory {

	public static CommandSet<TextChannelCommand> newDefaultTextChannelSet(GuildPreferences prefs) {
		CommandSet<TextChannelCommand> set = new CommandSetImpl<TextChannelCommand>();
		set.put("quote", new TextChannelQuoteCommand(prefs));
		return set;
	}
	
	public static CommandSet<PrivateChannelCommand> newDefaultPrivateChannelSet(MyListener bot) {
		CommandSet<PrivateChannelCommand> set = new CommandSetImpl<PrivateChannelCommand>();
		set.put("exit", new PrivateChannelExitCommand(bot));
		return set;
	}
	
}
