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
	public void execute(Message message);
	
	public String getName();
	
	public String getDescription();
	
	public String getHelp();
}
