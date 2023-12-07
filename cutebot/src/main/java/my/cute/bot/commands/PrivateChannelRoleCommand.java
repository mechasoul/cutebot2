package my.cute.bot.commands;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

//maybe deprecate this. lot of maintenance and not sure if it'd ever be used anymore

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
 * add roles to, params should be the roles to add, same syntax as with create: check for roles
 * surrounded by quotation marks, and if so, check for the text inside the quotation marks as 
 * an exact role name - if that doesn't exist, check the text inside the quotation marks as a
 * comma-separated list of role names. if no pair of quotation marks is found, then the first
 * word in the params list is checked as a role id, and then as a role name.
 * if adding roles to a single-role command, it will make that command a multi-role command<br>
 * <b>remove</b>: remove roles from an existing command. commandname should be the name of the 
 * command to remove roles from, params should be the role or roles to remove, surrounded by
 * quotation marks and comma-separated if more than one. if all roles but one are removed from
 * a multi-role command, it will become a single-role command. note all roles can't be removed
 * from a command, use delete mode instead<br>
 * <b>alias</b>: define an alias for a given role in an existing multi-role command. commandname
 * should be the name of the command to define an alias for, and params should be the role name
 * in quotation marks, followed by a comma, and then the alias. aliases cannot contain spaces, 
 * and at most one alias can exist for a single role in a single command<br>
 * <b>view</b>: get information about a defined command, or all defined commands. commandname 
 * should be the name of the command to view defined roles for, and can be omitted to get 
 * information about all commands. no params
 * <p>
 * all user-defined commands are <b>case insensitive</b>
 */
final class PrivateChannelRoleCommand extends PrivateChannelCommandTargeted {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelRoleCommand.class);
	final static String NAME = "role";
	private final static String DESCRIPTION = "modify or view admin-defined role commands, "
			+ "to allow server users to toggle some roles via cutebot";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("deprecated"));
	
	/*
	 * pattern for matching role/alias syntax
	 * proper syntax is:
	 * the start of the line, a quotation mark, any characters (this is the role name),
	 * a quotation mark, a comma, any amount of whitespace, at least one word character
	 * (this is the alias), and then whitespace or the end of the line
	 * eg:
	 * "role name",ALIAS
	 */
	private final static Pattern ALIAS = Pattern.compile("^\".*\",\\s*\\w+(?:\\s+|$)");
	private final static int MAX_COMMAND_NAME_LENGTH = 30;
	
	private final Map<String, GuildCommandSet> allCommands;
	
	PrivateChannelRoleCommand(Map<String, GuildCommandSet> commands) {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.ADMIN, 1, Integer.MAX_VALUE);
		this.allCommands = commands;
	}

	@Override
	public void execute(Message message, String[] params, Guild targetGuild) {
		
		//shouldnt be possible to have a valid guild without a commandset but check anyway
		GuildCommandSet commandSet = this.allCommands.get(targetGuild.getId());
		if(commandSet == null) {
			logger.warn(this + ": target guild '" + targetGuild + "' had no corresponding guild "
					+ " commandset?");
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
			return;
		}
		
		try {
			if(params[1].equalsIgnoreCase("create")) {
				if(params.length >= 4) {
					if(!commandSet.contains(params[2])) {
						if(params[2].matches("[A-Za-z0-9]+") && params[2].length() <= MAX_COMMAND_NAME_LENGTH) {
							//!role create commandname givenRoles
							ImmutableList<Role> givenRoles = MiscUtils.parseRoles(targetGuild, message, 3);
							if(givenRoles.size() >= 1) {
								if(commandSet.createRoleCommand(params[2], givenRoles)) {
									message.getChannel().sendMessage(StandardMessages.createdRoleCommand(params[2], givenRoles)).queue();
								} else {
									message.getChannel().sendMessage("failed to create command '" + params[2] + "' (max commands reached?)").queue();
								}
							} else  {
								message.getChannel().sendMessage(StandardMessages.failedToFindRoles(message, 3)).queue();
							}
						} else {
							message.getChannel().sendMessage(StandardMessages.invalidCommandName(params[2], MAX_COMMAND_NAME_LENGTH)).queue();
						}
					} else {
						message.getChannel().sendMessage(StandardMessages.commandNameAlreadyExists(params[2])).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("delete")) {
				if(params.length >= 3) {
					if(commandSet.isRoleCommand(params[2])) {
						commandSet.deleteRoleCommand(params[2]);
						message.getChannel().sendMessage("successfully deleted command '" + params[2] + "'").queue();
					} else {
						message.getChannel().sendMessage(StandardMessages.invalidRoleCommand(params[2])).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("add")) {
				if(params.length >= 4) {
					if(commandSet.isRoleCommand(params[2])) {
						//!role add commandname givenRoles
						ImmutableList<Role> givenRoles = MiscUtils.parseRoles(targetGuild, message, 3);
						if(givenRoles.size() >= 1) {
							List<Role> addedRoles = commandSet.getRoleCommandDatabase(params[2]).add(givenRoles);
							if(!addedRoles.isEmpty()) {
								message.getChannel().sendMessage(StandardMessages.addedRolesToCommand(params[2], addedRoles)).queue();
							} else {
								message.getChannel().sendMessage(StandardMessages.failedToAddRolesToCommand(params[2], givenRoles)).queue();
							}
						} else {
							message.getChannel().sendMessage(StandardMessages.failedToFindRoles(message, 3)).queue();
						}
					} else {
						message.getChannel().sendMessage(StandardMessages.invalidRoleCommand(params[2])).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("remove")) {
				if(params.length >= 4) {
					RoleCommandDatabase db = commandSet.getRoleCommandDatabase(params[2]);
					if(db != null) {
						synchronized(db) {
							//!role remove commandName givenRoles
							String roleText = MiscUtils.getWords(message, 4)[3];
							String extractedText = MiscUtils.extractQuotationMarks(roleText);
							if(extractedText != null) {
								roleText = extractedText;
								//check for single role in quotation marks
								if(db.remove(roleText)) {
									message.getChannel().sendMessage(StandardMessages.removedRoleFromCommand(params[2], roleText)).queue();
								} else {
									//try comma-separated list
									List<String> removedRoleNames = db.removeByName(roleText.split(",\\s*"));
									if(!removedRoleNames.isEmpty()) {
										message.getChannel().sendMessage(StandardMessages.removedRoleNamesFromCommand(params[2], removedRoleNames)).queue();
									} else {
										message.getChannel().sendMessage(StandardMessages.failedToFindRoles(message, 3)).queue();
									}
								}
							} else {
								//attempt to remove single role
								roleText = MiscUtils.getWords(roleText)[0];
								Role role = MiscUtils.tryRoleById(targetGuild, roleText);
								if(role != null) 
									roleText = role.getName();
								if(db.remove(roleText)) {
									message.getChannel().sendMessage(StandardMessages.removedRoleFromCommand(params[2], roleText)).queue();
								} else {
									message.getChannel().sendMessage(StandardMessages.failedToFindRoles(message, 3)).queue();
								}
							}
							if(db.isEmpty()) {
								commandSet.deleteRoleCommand(db.getName());
								message.getChannel().sendMessage("automatically deleted role command '" + db.getName()
										+ "' since all roles had been removed").queue();
							}
						}
					} else {
						message.getChannel().sendMessage(StandardMessages.invalidRoleCommand(params[2])).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("alias")) {
				if(params.length >= 4) {
					if(commandSet.isRoleCommand(params[2])) {
						//!role alias commandName "<role name>",<alias>
						//extract the "<role name>",<alias> part
						Matcher matcher = ALIAS.matcher(MiscUtils.getWords(message, 4)[3]);
						if(matcher.find()) {
							String aliasText = matcher.group();
							String roleName = MiscUtils.extractQuotationMarks(aliasText.substring(0, aliasText.lastIndexOf(',')));
							aliasText = aliasText.substring(aliasText.lastIndexOf(',') + 1).split("\\s+")[0];
							Role role = MiscUtils.getRoleByName(targetGuild, roleName);
							if(role != null) {
								if(commandSet.getRoleCommandDatabase(params[2]).addAlias(aliasText, role)) {
									message.getChannel().sendMessage("successfully added alias '" + aliasText + "' for the role '"
											+ role.getName() + "'").queue();
								} else {
									message.getChannel().sendMessage("failed to add alias '" + aliasText + "' for the role '"
											+ role.getName() + "' (role not added to command?)").queue();
								}
							} else {
								message.getChannel().sendMessage("error: unable to find any role matching '" 
										+ roleName + "'").queue();
							}
						} else {
							message.getChannel().sendMessage("syntax error: unable to parse role name and alias. try !help role").queue();
						}
					} else {
						message.getChannel().sendMessage(StandardMessages.invalidRoleCommand(params[2])).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("view")) {
				if(params.length >= 3) {
					RoleCommandDatabase db = commandSet.getRoleCommandDatabase(params[2]);
					if(db != null) {
						message.getChannel().sendMessage(db.getFormattedString()).queue();
					} else {
						message.getChannel().sendMessage(StandardMessages.unknownCommand(params[2])).queue();
					}
				} else {
					MiscUtils.sendMessages(message.getChannel(), this.getRoleCommandsAsMessages(targetGuild, commandSet));
				}
			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} catch (IOException e) {
			logger.warn(this + ": unknown IOException during command execution! message: " + message + ", guild: " 
					+ MiscUtils.getGuildString(targetGuild), e);
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
		}
	}
	
	private Queue<Message> getRoleCommandsAsMessages(Guild guild, GuildCommandSet commandSet) {
		MessageBuilder mb = new MessageBuilder();
		mb.append("current role commands for server " + MiscUtils.getGuildString(guild));
		mb.append(System.lineSeparator());
		mb.append(System.lineSeparator());
		mb.append("(format: `command name - 'role 1' (alias 1), 'role 2' (alias 2), ...`)");
		mb.append(System.lineSeparator());
		mb.append(commandSet.getRoleCommandDatabases().stream()
				.map(db -> db.getFormattedString())
				.collect(Collectors.joining(System.lineSeparator())));
		return mb.buildAll(SplitPolicy.ANYWHERE);
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
	
}
