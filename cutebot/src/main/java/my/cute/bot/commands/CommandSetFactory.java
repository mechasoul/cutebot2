package my.cute.bot.commands;

import my.cute.bot.MyListener;
import net.dv8tion.jda.api.JDA;

public class CommandSetFactory {

	public static CommandSet newDefaultTextChannelSet() {
		CommandSet set = new CommandSetImpl();
		set.put("quote", new TextChannelQuoteCommand("quote"));
		return set;
	}
	
	public static CommandSet newDefaultPrivateChannelSet(MyListener bot, JDA jda) {
		CommandSet set = new CommandSetImpl();
		set.put("exit", new PrivateChannelExitCommand("exit", bot));
		return set;
	}
}
