package my.cute.bot.commands;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.api.entities.Message;

class CommandSetImpl<T extends Command> implements CommandSet<T> {

	private final ConcurrentHashMap<String, T> commandSet;
	
	public CommandSetImpl(int capacity) {
		this.commandSet = new ConcurrentHashMap<>(capacity);
	}
	
	@Override
	public Iterator<Entry<String, T>> iterator() {
		return this.commandSet.entrySet().iterator();
	}
	
	@Override
	public int size() {
		return this.commandSet.size();
	}

	@Override
	public boolean isEmpty() {
		return this.commandSet.isEmpty();
	}
	
	@Override
	public boolean contains(String commandName) {
		return this.commandSet.containsKey(commandName);
	}
	
	@Override
	public T get(String commandName) {
		return this.commandSet.get(commandName);
	}
	
	@Override
	public T put(String name, T command) {
		return this.commandSet.putIfAbsent(name, command);
	}
	
	@Override
	public T remove(String commandName) {
		return this.commandSet.remove(commandName);
	}

	@Override
	public boolean execute(String name, Message message, String[] params) {
		T command = this.commandSet.get(name);
		if(command != null) {
			command.execute(message, params);
			return true;
		} else {
			return false;
		}
	}

	

}
