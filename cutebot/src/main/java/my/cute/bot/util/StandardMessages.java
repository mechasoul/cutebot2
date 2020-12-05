package my.cute.bot.util;

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
	
}
