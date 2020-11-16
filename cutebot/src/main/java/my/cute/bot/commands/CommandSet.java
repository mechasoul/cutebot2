package my.cute.bot.commands;

import java.util.Map.Entry;

import net.dv8tion.jda.api.entities.Message;

public interface CommandSet<T extends Command> extends Iterable<Entry<String, T>> {

	public int size();
	
	public boolean isEmpty();
	
	public boolean contains(String commandName);
	
	public T get(String commandName);
	
	public T put(String name, T command);
	
	public T remove(String commandName);
	
	public boolean execute(String name, Message message, String[] params);
}
