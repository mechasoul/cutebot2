package my.cute.bot.util;

import java.util.List;
import java.util.stream.Collectors;

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
	
	public static String createdRoleCommand(String commandName, Role role) {
		return "successfully created command '" + commandName + "' with role '"
				+ role.getName() + "'";
	}
	
	public static String createdRoleCommand(String commandName, List<Role> roles) {
		return "successfully created command '" + commandName + "' with "
				+ (roles.size() > 1 ? "roles" : "role")
				+ " '" + String.join(", ", roles.stream().map(role -> role.getName()).collect(Collectors.toList()))
				+ "'";
	}
	
	public static String addedRoleToCommand(String commandName, Role role) {
		return "successfully added role '" + role.getName() + "' to command '"
				+ commandName + "'";
	}
	
	public static String addedRolesToCommand(String commandName, List<Role> roles) {
		return "successfully added roles '" 
				+ String.join(", ", roles.stream().map(role -> role.getName()).collect(Collectors.toList()))
				+ "' to command '" + commandName + "'";
	}
}
