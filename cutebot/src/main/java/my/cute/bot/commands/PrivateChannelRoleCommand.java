package my.cute.bot.commands;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.ConcurrentFinalEntryMap;
import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

/**
 * command for defining commands to allow users to add/remove roles to themselves. there are
 * two types of commands that can be defined this way: single-role commands, and multi-role
 * commands. single role commands are used with no parameters to toggle a single role; using
 * the command without the role will add it, and using the command with the role will remove
 * it (eg '!na' to toggle a 'North America' role used for matchmaking). multi-role commands 
 * require an additional parameter of the role to toggle, but otherwise work the same. multi-
 * role commands also allow the use of aliases for user convenience (eg '!region North America'
 * or '!region na' to toggle the 'North America' role, '!region Europe' or '!region eu' to
 * toggle the 'Europe' role). this gives admins some control over how they want the user 
 * experience to work
 * <p>
 * <b>use</b>: !role mode commandname params
 * <p>
 * valid modes are:<br>
 * <b>create</b>: define a new command. commandname should be the name of the command, params 
 * should be either a single role, or a comma-separated list of roles, depending on whether 
 * the command should be a single or multi-role command. in either case, role names should be
 * exact, and the name or list of names should be surrounded by quotation marks (note if the
 * command is for a single role and that role has no spaces in its name, then quotation marks
 * can be omitted for convenience)<br>
 * <b>delete</b>: delete an existing command. commandname should be the name of the command to
 * delete. no params<br>
 * <b>add</b>: add roles to an existing command. commandname should be the name of the command to
 * add roles to, params should be the roles to add, surrounded by quotation marks and comma-
 * separated if adding more than one. if adding roles to a single-role command, it will make
 * that command a multi-role command<br>
 * <b>remove</b>: remove roles from an existing command. commandname should be the name of the 
 * command to remove roles from, params should be the role or roles to remove, surrounded by
 * quotation marks and comma-separated if more than one. if all roles but one are removed from
 * a multi-role command, it will become a single-role command. note all roles can't be removed
 * from a command, use delete mode instead<br>
 * <b>alias</b>: define an alias for a given role in an existing multi-role command. commandname
 * should be the name of the command to define an alias for, and params should be the role name
 * in quotation marks, followed by a comma, and then the alias. aliases cannot contain spaces<br>
 * <b>view</b>: get information about a defined command, or all defined commands. commandname 
 * should be the name of the command to view defined roles for, and can be omitted to get 
 * information about all commands. no params
 * <p>
 * all user-defined commands are case insensitive
 */
final class PrivateChannelRoleCommand extends PrivateChannelCommandTargeted {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelRoleCommand.class);
	final static String NAME = "role";
	private final static Pattern QUOTATION_MARKS = Pattern.compile("^\".*\"(?:\\s+|$)");
	
	private final ConcurrentFinalEntryMap<String, CommandSet<TextChannelCommand>> allCommands;
	private final JDA jda;
	
	PrivateChannelRoleCommand(ConcurrentFinalEntryMap<String, CommandSet<TextChannelCommand>> commands) {
		super(NAME, PermissionLevel.ADMIN, 1, Integer.MAX_VALUE);
		this.allCommands = commands;
	}

	@Override
	public void execute(Message message, String[] params, Guild targetGuild) {
		
		//shouldnt be possible to have a valid guild without a commandset but check anyway
		CommandSet<TextChannelCommand> commands = this.allCommands.get(targetGuild.getId());
		if(commands == null) {
			logger.warn(this + ": target guild '" + targetGuild + "' had no corresponding text "
					+ "channel commandset?");
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
			return;
		}
		
		if(params[1].equals("create")) {
			if(params.length >= 4) {
				if(commands.contains(params[2])) {
					//!role create commandname givenRoles
					String givenRoles = MiscUtils.getWords(message, 4)[3];
					if(QUOTATION_MARKS.matcher(givenRoles).matches()) {
						givenRoles = extractQuotationMarks(givenRoles);
						//test for an exact role, then comma-separated
						
					} else {
						//no quotation marks. check for simple role provided as params[3]
					}
				}
			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} else if (params[1].equals("delete")) {
			if(params.length >= 3) {
				
			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} else if (params[1].equals("add")) {
			if(params.length >= 4) {

			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} else if (params[1].equals("remove")) {
			if(params.length >= 4) {
				
			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} else if (params[1].equals("alias")) {
			if(params.length >= 4) {

			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} else if (params[1].equals("view")) {
			
		} else {
			message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
		}
		
	}

	/*
	 * put this in command help
	 *  * <b>formatting for writing role names</b>: when a command includes a single role, that role
 * can be surrounded by quotation marks - if the role name contains spaces, it <i>must</i> be 
 * surrounded by quotation marks. if you're writing a <i>list</i> of roles, then the <i>entire
 * list</i> should be surrounded by quotation marks, with role names separated by commas. role
 * complex role names that contain commas and quotation marks will likely not work properly if
 * in a list - enter them individually, or note that using the role's id is also an option<br>
 * <b>examples</b><br>
 * !role create eu Europe<br>
 * !role create na "North America"<br>
 * !role create region "Europe,South America"<br>
 * !role delete na<br>
 * !role add region "North America"<br>
 * !role remove region Europe<br>
 * !role add region "single role, with comma and spaces"<br>
 * !role alias region "South America",SA<br>
 * <p>
 * <b>results of executing all above examples</b><br>
 * '!eu' to toggle 'Europe' role<br>
 * '!region North America' to toggle 'North America' role<br>
 * '!region single role, with comma and spaces' to toggle 'single role, with comma and spaces' role<br>
 * '!region SA' or '!region South America' to toggle 'South America' role
	 */
	@Override
	public String toString() {
		return "PrivateChannelRoleCommand";
	}
	
	private static String extractQuotationMarks(String string) {
		string = string.split("\"", 2)[1];
		return string.substring(0, string.lastIndexOf('"'));
	}
}
