package my.cute.bot.util;

public class ErrorMessages {
	
	public static String unknownCommand(String givenCommand) {
		return "invalid command: '" + givenCommand + "'. try !help for a list of commands";
	}
	
	public static String noTargetGuild() {
		return "error: invalid guild. either give a target server or set a default "
				+ "server for your commands (see !default)";
	}
	
	public static String invalidGuild(String guildId) {
		return "invalid server: '" + guildId + "'";
	}
	
	public static String unknownError() {
		return "an unknown error has occurred. please call an adult";
	}
	
	public static String invalidSyntax(String commandName) {
		return "invalid syntax. try !help " + commandName;
	}
	
}
