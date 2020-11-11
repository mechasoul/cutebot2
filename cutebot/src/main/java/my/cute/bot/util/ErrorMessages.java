package my.cute.bot.util;

public class ErrorMessages {
	
	public static String unknownCommand(String givenCommand) {
		return "invalid command: '" + givenCommand + "'. try !help for a list of commands";
	}
	
}
