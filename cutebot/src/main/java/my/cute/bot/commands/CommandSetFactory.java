package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;

public class CommandSetFactory {

	public static CommandSet newDefaultTextChannelSet(GuildPreferences prefs) {
		CommandSet set = new CommandSetImpl();
		set.put("quote", new TextChannelQuoteCommand(prefs));
		return set;
	}
	
	public static CommandSet newDefaultPrivateChannelSet(MyListener bot) {
		CommandSet set = new CommandSetImpl();
		set.put("exit", new PrivateChannelExitCommand(bot));
		return set;
	}
}
