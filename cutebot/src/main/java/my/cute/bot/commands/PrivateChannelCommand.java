package my.cute.bot.commands;

public abstract class PrivateChannelCommand implements Command {

	private final String name;
	private final boolean requiresTargetGuild;
	
	PrivateChannelCommand(String name, boolean targetGuild) {
		this.name = name;
		this.requiresTargetGuild = targetGuild;
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHelp() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean requiresTargetGuild() {
		return this.requiresTargetGuild;
	}

}
