package my.cute.bot.commands;

public abstract class TextChannelCommand implements Command {

	private final String name;
	
	TextChannelCommand(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}

}
