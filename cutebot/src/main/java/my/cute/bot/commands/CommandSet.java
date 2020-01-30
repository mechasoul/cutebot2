package my.cute.bot.commands;

import java.util.Map.Entry;

import net.dv8tion.jda.api.entities.Message;

public interface CommandSet extends Iterable<Entry<String, Command>> {

	public int size();
	
	public boolean isEmpty();
	
	public boolean contains(String commandName);
	
	public Command get(String commandName);
	
	public Command put(String name, Command command);
	
	public Command remove(String commandName);
	
	/*
	 * TODO this
	 * what param gets passed in?
	 * see Command.execute() for consideration
	 * 
	 */
	public boolean execute(String name, Message message);
}
