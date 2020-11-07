package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Message;

public abstract class CommandImpl implements Command {
	
	private final String name;
	private final String description;
	private final String help;
	private final PermissionLevel requiredPermissionLevel;
	private final int minParams;
	private final int maxParams;
	
	protected CommandImpl(String name, PermissionLevel permission, int min, int max) {
		this.name = name;
		this.description = "";
		this.help = "";
		this.requiredPermissionLevel = permission;
		this.minParams = min;
		this.maxParams = max;
	}

	/*
	 * TODO
	 * GuildMessageReceivedEvent and PrivateMessageReceivedEvent arent subclasses of
	 * MessageReceivedEvent so we can't pass them in to this...
	 * can construct a new MessageReceivedEvent since we have reference to the jda instance
	 * and can get the responsenumber and message from the existing event
	 * alternatively, is it ok to change parameter to Message? can retrieve channel from
	 * the message along with author and w/e else. if its a guild command we should have
	 * info on the guild at command object creation time anyway so we dont need that
	 * 
	 * try changing to Message and continuing. see if any issues pop up later from not 
	 * passing in the event
	 */
	/*
	 * executes the Command, using the given Message as a parameter.
	 * all required arguments for the command are to be given in the Message's content.
	 * before calling execute(Message), it should be guaranteed that
	 * the Message's content (as given by Message.getContentDisplay()) starts with 
	 * '!<command name>', where <command name> is Command.name - ie, 
	 * message.getContentDisplay().startsWith("!" + command.getName)
	 * returns true
	 */
	@Override
	public abstract void execute(Message message, String[] params);
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public String getDescription() {
		return this.description;
	}
	
	@Override
	public String getHelp() {
		return this.help;
	}
	
	@Override
	public PermissionLevel getRequiredPermissionLevel() {
		return this.requiredPermissionLevel;
	}
	
	@Override
	public boolean hasCorrectParameterCount(String[] words) {
		if(words.length - 1 >= this.minParams && words.length - 1 <= this.maxParams) {
			return true;
		} else {
			return false;
		}
	}
	
	//TODO what was i doing with this
	public boolean hasRequiredPermission(PermissionLevel permission) {
		if(permission == PermissionLevel.DEVELOPER) {
			return true;
		} else if (permission == PermissionLevel.ADMIN) {
			if(this.requiredPermissionLevel != PermissionLevel.DEVELOPER) {
				return true;
			} else {
				return false;
			}
		} else {
			if(this.requiredPermissionLevel == PermissionLevel.USER) {
				return true;
			} else {
				return false;
			}
		}
	}
}
