package my.cute.bot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Command {

	public void execute(MessageReceivedEvent event);
	
	public String getName();
	
	public String getDescription();
	
	public String getHelp();
}
