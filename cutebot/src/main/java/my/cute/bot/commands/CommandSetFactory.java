package my.cute.bot.commands;

public class CommandSetFactory {

	public static CommandSet newDefaultTextChannelSet() {
		CommandSet set = new CommandSetImpl();
		set.put("quote", new TextChannelQuoteCommand("quote"));
		return set;
	}
}
