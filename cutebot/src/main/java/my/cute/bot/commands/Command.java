package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Message;

public interface Command {

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
	 * 
	 * can take basically any action
	 */
	void execute(Message message, String[] params);

	String getName();

	String getDescription();

	String getHelp();

	PermissionLevel getRequiredPermissionLevel();
	
	/*
	 * syntax check
	 * should pass in all the user's words as given by MiscUtils.getWords(Message)
	 */
	boolean hasCorrectParameterCount(String[] words);

}