package my.cute.bot.util;

import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class StandardMessages {
	
	public static String unknownCommand(String givenCommand) {
		return "invalid command: '" + givenCommand + "'. try !help for a list of commands";
	}
	
	public static String noTargetGuild() {
		return "error: invalid server. either give a target server or set a default "
				+ "server for your commands (see !default)";
	}
	
	public static String invalidGuild(String guildId) {
		return "invalid server: '" + guildId + "'";
	}
	
	public static String invalidMember(String userId, String guildId) {
		return "no member found with id '" + userId + "' in server '" + guildId + "'";
	}
	
	public static String unknownError() {
		return "an unknown error has occurred. please call an adult";
	}
	
	public static String invalidSyntax(String commandName) {
		return "invalid syntax. try !help " + commandName;
	}

	public static String invalidAutoResponseTime(String givenMinutes) {
		return "invalid automatic response time: '" + givenMinutes + "'. please use a number from 1 to 525600";
	}
	
	public static String wordfilterModified() {
		return "wordfilter has been successfully modified";
	}
	
	public static String invalidRole(User user, String givenRole) {
		return user.getAsMention() + " error: '" + givenRole + "' is not a valid role for that command";
	}
	
	public static String missingPermissionsToModifyRole(User user) {
		return user.getAsMention() + " error: unable to modify role due to missing permissions. please contact your local administrator";
	}
	
	public static String commandNameAlreadyExists(String name) {
		return "error: a command already exists with the name '" + name + "'";
	}
	
	public static String invalidRoleCommand(String name) {
		return "error: no role command exists with the name '" + name + "'";
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
		return "error: unable to find any role matching '" + MiscUtils.getWords(message, paramsToIgnore+1) + "'";
	}
	
	public static String createdRoleCommand(String commandName, Role role) {
		return "successfully created command '" + commandName + "' with role '"
				+ role.getName() + "'";
	}
	
	public static String createdRoleCommand(String commandName, List<Role> roles) {
		return "successfully created command '" + commandName + "' with "
				+ (roles.size() > 1 ? "roles" : "role")
				+ " '" + roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))
				+ "'";
	}
	
	public static String addedRoleToCommand(String commandName, Role role) {
		return "successfully added role '" + role.getName() + "' to command '"
				+ commandName + "'";
	}
	
	public static String addedRolesToCommand(String commandName, List<Role> roles) {
		if(roles.size() == 1) {
			return StandardMessages.addedRoleToCommand(commandName, roles.get(0));
		} else {
			return "successfully added roles '" 
					+ roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))
					+ "' to command '" + commandName + "'";
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
			return "found roles '"
					+ roles.stream().map(role -> role.getName()).collect(Collectors.joining(", "))
					+ "' but failed to add any to command '" + commandName + "' (already exist?)";
		} else {
			//1 element in roles (should be nonempty)
			return "found role '" 
					+ roles.get(0).getName()
					+ "' but failed to add it to command '" + commandName + "' (already exists?)";
		}
	}
}
