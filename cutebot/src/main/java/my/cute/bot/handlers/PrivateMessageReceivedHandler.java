package my.cute.bot.handlers;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.commands.CommandSet;
import my.cute.bot.commands.CommandSetFactory;
import my.cute.bot.commands.PermissionManager;
import my.cute.bot.commands.PermissionManagerImpl;
import my.cute.bot.commands.PrivateChannelCommand;
import my.cute.bot.commands.PrivateChannelCommandTargeted;
import my.cute.bot.util.ErrorMessages;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class PrivateMessageReceivedHandler {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(PrivateMessageReceivedHandler.class);
	
	private final MyListener bot;
	private final JDA jda;
	private final CommandSet<PrivateChannelCommand> commands;
	private final PermissionManager permissions;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	public PrivateMessageReceivedHandler(MyListener bot, JDA jda) throws IOException {
		this.bot = bot;
		this.jda = jda;
		this.commands = CommandSetFactory.newDefaultPrivateChannelSet(this.bot);
		this.permissions = new PermissionManagerImpl(this.jda);
	}
	
	public void handle(PrivateMessageReceivedEvent event) {
		String[] params = MiscUtils.getWords(event.getMessage());
		PrivateChannelCommand command = null;
		if(params[0].startsWith("!")) {
			String commandName = params[0].substring(1);
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
		if(command == null) {
			event.getChannel().sendMessage(ErrorMessages.unknownCommand(params[0].substring(1))).queue();
			return;
		}
		
		if(command instanceof PrivateChannelCommandTargeted) {
			String targetGuild = params[params.length - 1];
			if(this.jda.getGuildById(targetGuild) == null) {
				//TODO do default guild stuff here
				//if still no targetGuild then error msg + return
				/*
				 * check default guild db
				 * if no entry, check to see if theyre only in 1 cutebot guild
				 * if so, record it as default and proceed. if not, error
				 * if have defauult guild now, check to see if its valid for them?
				 */
				
			}
			/*
			 * target guild acquired, now do permissions check. if no permission, send generic unknown
			 * command error message in order to not reveal hidden commands
			 * TODO these hasPermission checks can throw IllegalArgumentException in rare cases maybe?
			 * (when targetGuild was acquired above but not found in permission manager)
			 */
			if(this.permissions.hasPermission(event.getAuthor().getId(), targetGuild, command.getRequiredPermissionLevel())) {
				((PrivateChannelCommandTargeted) command).execute(event.getMessage(), params, targetGuild);
			} else {
				event.getChannel().sendMessage(ErrorMessages.unknownCommand(params[0].substring(1))).queue();
			}
		} else {
			if(this.permissions.hasPermission(event.getAuthor().getId(), command.getRequiredPermissionLevel())) {
				command.execute(event.getMessage(), params);
			} else {
				event.getChannel().sendMessage(ErrorMessages.unknownCommand(params[0].substring(1))).queue();
			}
		}
	}
	
	public ExecutorService getExecutor() {
		return this.executor;
	}

	@Override
	public String toString() {
		return "PrivateMessageReceivedHandler";
	}
}
