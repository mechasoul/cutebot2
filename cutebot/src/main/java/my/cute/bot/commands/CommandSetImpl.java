package my.cute.bot.commands;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.dv8tion.jda.api.entities.Message;

class CommandSetImpl<T extends Command> implements CommandSet<T> {

	protected final ConcurrentMap<String, T> commandSet;
	
	public CommandSetImpl(int capacity) {
		this.commandSet = new ConcurrentHashMap<>(capacity);
	}
	
	public CommandSetImpl() {
		this.commandSet = new ConcurrentHashMap<>();
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
		return this.commandSet.containsKey(commandName.toLowerCase());
	}
	
	@Override
	public T get(String commandName) {
		return this.commandSet.get(commandName.toLowerCase());
	}
	
	@Override
	public T put(String name, T command) {
		return this.commandSet.putIfAbsent(name.toLowerCase(), command);
	}
	
	@Override
	public T remove(String commandName) {
		return this.commandSet.remove(commandName.toLowerCase());
	}

	@Override
	public boolean execute(String name, Message message, String[] params) {
		T command = this.commandSet.get(name.toLowerCase());
		if(command != null) {
			command.execute(message, params);
			return true;
		} else {
			return false;
		}
	}

	

}
