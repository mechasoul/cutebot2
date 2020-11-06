package my.cute.bot.handlers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.commands.CommandSet;
import my.cute.bot.commands.CommandSetFactory;
import my.cute.bot.commands.PrivateChannelCommand;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class PrivateMessageReceivedHandler {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(PrivateMessageReceivedHandler.class);
	
	private final MyListener bot;
	//TODO remove?
	private final JDA jda;
	private final CommandSet<PrivateChannelCommand> commands;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	public PrivateMessageReceivedHandler(MyListener bot, JDA jda) {
		this.bot = bot;
		this.jda = jda;
		this.commands = CommandSetFactory.newDefaultPrivateChannelSet(this.bot);
	}
	
	/*
	 * TODO this
	 */
	public void handle(PrivateMessageReceivedEvent event) {
		String[] words = MiscUtils.getWords(event.getMessage());
		PrivateChannelCommand command = null;
		if(words[0].startsWith("!")) {
			String commandName = words[0].substring(1);
			command = this.commands.get(commandName);
		}
		
		/*
		 * TODO need to check if user has required permission to use the command if it exists
		 * if not, return same response as if they sent a command that doesnt exist
		 * to check required permission, need to know target server for command and check if
		 * they have admin permission on target server
		 * -> commands need a hastargetserver boolean or something, need a method to get it
		 * from sent message (check last word to see if its valid guild id, if not, check to 
		 * see if theyre only in one server, if not check to see if they have a set "default"
		 * server for commands)
		 * --> implement everything needed for above process (can check old cutebot v2 for ref)
		 */
		//if(command == null || command.hasRequiredPermission(permission))
	}
	
	public ExecutorService getExecutor() {
		return this.executor;
	}

	@Override
	public String toString() {
		return "PrivateMessageReceivedHandler";
	}
}
