package my.cute.bot.util;

import java.util.List;
import java.util.stream.Collectors;

import my.cute.bot.preferences.wordfilter.WordFilter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class StandardMessages {
	
	public static String unknownCommand(String givenCommand) {
		return "error: invalid command: '" + givenCommand + "'. try `!help` for a list of commands";
	}
	
	public static String noTargetGuild() {
		return "error: invalid server. either provide a target server or set a default "
				+ "server for your commands (see `!help default`)";
	}
	
	public static String invalidGuild(String guildId) {
		return "error: invalid server: `" + guildId + "`";
	}
	
	public static String invalidMember(String userId, Guild guild) {
		return "error: no member found with id `" + userId + "` in server " + MiscUtils.getGuildString(guild);
	}
	
	public static String unknownError() {
		return "error: an unknown error has occurred. please call an adult";
	}
	
	public static String invalidSyntax(String commandName) {
		return "error: invalid syntax. try `!help " + commandName + "`";
	}

	public static String invalidAutoResponseTime(String givenMinutes) {
		return "error: invalid automatic response time: `" + givenMinutes + "`. please use a number from 1 to 525600";
	}
	
	public static String wordfilterModified() {
		return "wordfilter has been successfully modified";
	}
	
	public static String failedToFindWordfilterWords() {
		return "error: invalid syntax. make sure your word list is surrounded by quotation marks";
	}
	
	public static String wordfilterStrike(Guild guild, WordFilter.Type type) {
		String guildString = MiscUtils.getGuildString(guild);
		StringBuilder sb = new StringBuilder();
		sb.append("dear valued user,");
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append("the wordfilter for server ");
		sb.append(guildString);
		sb.append(" was found to be prohibitively slow, and so ");
		if(type == WordFilter.Type.BASIC) 
			sb.append("it has been cleared. ");
		else /* type == WordFilter.Type.REGEX */ 
			sb.append("the custom regex has been cleared. ");
		sb.append("if this happens again, the wordfilter may be permanently disabled. ");
		sb.append("you are receiving this message because you have authority to modify the wordfilter ");
		sb.append("for server '");
		sb.append(guildString);
		sb.append("'. if you don't know what any of this means or don't care, then ignore it or ");
		sb.append("contact someone who does or something");
		sb.append(MiscUtils.getSignature());
		return sb.toString();
	}
	
	public static String wordfilterDisabled(Guild guild) {
		String guildString = MiscUtils.getGuildString(guild);
		StringBuilder sb = new StringBuilder();
		sb.append("dear valued user,");
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append("the wordfilter for server ");
		sb.append(guildString);
		sb.append(" has repeatedly been found to be prohibitively slow, and so ");
		sb.append("it has been permanently disabled. ");
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append("*with great power comes great responsibility* - canadian prime minister william lyon mackenzie king");
		sb.append(MiscUtils.getSignature());
		return sb.toString();
	}
	
	public static String invalidRole(User user, String givenRole) {
		return user.getAsMention() + " error: `" + givenRole + "` is not a valid role for that command";
	}
	
	public static String missingPermissionsToModifyRole(User user) {
		return user.getAsMention() + " error: unable to modify role due to missing permissions. please contact your local administrator";
	}
	
	public static String missingPermissionsToApplyFilterRole(Role role) {
		return "missing permissions to apply role `" + role.getName() + " (id=" + role.getId()
			+ ")` to user who triggered the wordfilter";
	}
	
	public static String missingPermissionsToKick() {
		return "missing permissions to kick users";
	}
	
	public static String missingPermissionsToBan() {
		return "missing permissions to ban users";
	}
	
	public static String missingPermissionsToDeleteMessages() {
		return "missing permissions to delete messages";
	}
	
	public static String noDefaultGuildSet() {
		return "you currently have no default server set";
	}
	
	public static String commandNameAlreadyExists(String name) {
		return "error: a command already exists with the name `" + name + "`";
	}
	
	public static String invalidCommandName(String name, int maxNameLength) {
		if(name.length() > maxNameLength) {
			return "error: command names cannot be longer than " + maxNameLength + " characters";
		} else {
			return "error: `" + name + "` is not a valid command name (command names must be alphanumeric)";
		}
	}
	
	public static String invalidRoleCommand(String name) {
		return "error: no role command exists with the name `" + name + "`";
	}
	
	/**
	 * used when a user-provided role or list of roles returns no results
	 * @param message the user's message, probably a command
	 * @param paramsToIgnore the number of words (probably parameters) to 
	 * ignore before the start of the role list
	 * @return an error message indicating that no roles could be found
	 * matching the user's text
	 */
	public static String failedToFindRoles(Message message, int paramsToIgnore) {
		return "error: unable to find any role matching `" + MiscUtils.getWords(message, paramsToIgnore+1) + "`";
	}
	
	public static String createdRoleCommand(String commandName, Role role) {
		return "successfully created command `" + commandName + "` with role `"
				+ role.getName() + "`";
	}
	
	public static String createdRoleCommand(String commandName, List<Role> roles) {
		return "successfully created command `" + commandName + "` with "
				+ (roles.size() > 1 ? "roles" : "role")
				+ " `" + roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))
				+ "`";
	}
	
	public static String addedRoleToCommand(String commandName, Role role) {
		return "successfully added role `" + role.getName() + "` to command `"
				+ commandName + "`";
	}
	
	public static String addedRolesToCommand(String commandName, List<Role> roles) {
		if(roles.size() == 1) {
			return StandardMessages.addedRoleToCommand(commandName, roles.get(0));
		} else {
			return "successfully added roles `" 
					+ roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))
					+ "` to command `" + commandName + "`";
		}
	}
	
	public static String removedRoleFromCommand(String commandName, Role role) {
		return "successfully removed role `" + role.getName() + "` from command `"
				+ commandName + "`";
	}
	
	public static String removedRoleFromCommand(String commandName, String roleName) {
		return "successfully removed role `" + roleName + "` from command `"
				+ commandName + "`";
	}
	
	public static String removedRolesFromCommand(String commandName, List<Role> roles) {
		if(roles.size() == 1) {
			return StandardMessages.removedRoleFromCommand(commandName, roles.get(0));
		} else {
			return "successfully removed roles `" 
					+ roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))
					+ "` from command `" + commandName + "`";
		}
	}
	
	public static String removedRoleNamesFromCommand(String commandName, List<String> roleNames) {
		if(roleNames.size() == 1) {
			return StandardMessages.removedRoleFromCommand(commandName, roleNames.get(0));
		} else {
			return "successfully removed roles `" 
					+ String.join(", ", roleNames)
					+ "` from command `" + commandName + "`";
		}
	}
	
	/**
	 * return a message indicating that the given roles could not be added to
	 * the given command
	 * @param commandName the name of the command
	 * @param roles nonempty list of roles that could not be added
	 * @return an error message indicating that the given roles could not be 
	 * added to the given command
	 */
	public static String failedToAddRolesToCommand(String commandName, List<Role> roles) {
		if(roles.size() > 1) {
			return "found roles `"
					+ roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))
					+ "` but failed to add any to command `" + commandName + "` (already exist?)";
		} else {
			//1 element in roles (should be nonempty)
			return "found role `" 
					+ roles.get(0).getName()
					+ "` but failed to add it to command `" + commandName + "` (already exists?)";
		}
	}

	
}
