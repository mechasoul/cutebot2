package my.cute.bot.commands;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.api.entities.Message;

class CommandSetImpl implements CommandSet {

	private final ConcurrentHashMap<String, Command> commandSet;
	
	public CommandSetImpl() {
		//TODO params
		this.commandSet = new ConcurrentHashMap<>();
	}
	
	@Override
	public Iterator<Entry<String, Command>> iterator() {
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
	public Command get(String commandName) {
		return this.commandSet.get(commandName);
	}
	
	@Override
	public Command put(String name, Command command) {
		return this.commandSet.put(name, command);
	}
	
	@Override
	public Command remove(String commandName) {
		return this.commandSet.remove(commandName);
	}

	@Override
	public boolean execute(String name, Message message) {
		Command command = this.commandSet.get(name);
		if(command != null) {
			command.execute(message);
			return true;
		} else {
			return false;
		}
	}

	

}
