package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Message;

public abstract class CommandImpl implements Command {
	
	private final String name;
	private final String description;
	private final String help;
	private final PermissionLevel requiredPermissionLevel;
	/*
	 * number of parameters required for the command
	 * not including the command itself, so eg !command <parameter> would be 1
	 * some commands include optional parameters, so we have minimum and maximum
	 * parameter count
	 */
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
	
	/*
	 * note we pass in all words in the user's command, including the command itself
	 * (as given by MiscUtils.getWords(Message), but in command construction we specify
	 * parameter count as the number of actual parameters, not including the command
	 * itself. so subtract one from words.length here
	 */
	@Override
	public boolean hasCorrectParameterCount(String[] words) {
		if(words.length - 1 >= this.minParams && words.length - 1 <= this.maxParams) {
			return true;
		} else {
			return false;
		}
	}
}
