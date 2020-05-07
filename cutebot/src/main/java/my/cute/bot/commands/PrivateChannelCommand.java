package my.cute.bot.commands;

abstract class PrivateChannelCommand implements Command {

	private final String name;
	private final boolean requiresTargetGuild;
	private final PermissionLevel permission;
	
	PrivateChannelCommand(String name, boolean targetGuild) {
		this.name = name;
		this.requiresTargetGuild = targetGuild;
		this.permission = PermissionLevel.USER;
	}
	
	PrivateChannelCommand(String name, boolean targetGuild, PermissionLevel permission) {
		this.name = name;
		this.requiresTargetGuild = targetGuild;
		this.permission = permission;
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
	
	@Override
	public PermissionLevel getRequiredPermissionLevel() {
		return this.permission;
	}
	
	public boolean requiresTargetGuild() {
		return this.requiresTargetGuild;
	}

}
