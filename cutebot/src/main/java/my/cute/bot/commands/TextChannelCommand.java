package my.cute.bot.commands;

abstract class TextChannelCommand implements Command {

	private final String name;
	private final PermissionLevel permissionLevel;
	
	TextChannelCommand(String name) {
		this.name = name;
		this.permissionLevel = PermissionLevel.USER;
	}
	
	TextChannelCommand(String name, PermissionLevel permission) {
		this.name = name;
		this.permissionLevel = permission;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public PermissionLevel getRequiredPermissionLevel() {
		return this.permissionLevel;
	}

}
