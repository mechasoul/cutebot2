package my.cute.bot.preferences.wordfilter;

import java.util.EnumSet;

/**
 * an enum containing various actions that can be taken when a wordfilter is triggered.
 * each value corresponds to a number so that users can set actions via a simple 
 * string of numbers instead of having to specify enum names or something (for more 
 * on this see {@link FilterResponseAction#numbersToActions(String)})
 * <p>
 * <b>(1) SKIP_PROCESS</b>: message won't be processed into database (to avoid 
 * cutebot repeating the offending phrase)<br>
 * <b>(2) SEND_RESPONSE_GUILD</b>: message sent in the same channel as the offending
 * message, to notify the user who sent the message<br>
 * <b>(3) SEND_RESPONSE_PRIVATE</b>: private message sent to the user who sent the 
 * message, to notify them<br>
 * <b>(4) DELETE_MESSAGE</b>: offending message is deleted<br>
 * <b>(5) ROLE</b>: a user-specified role is assigned to the user who sent the 
 * message (can be used eg to apply a "mute" role)<br>
 * <b>(6) KICK</b>: user who sent the message is kicked from the server<br>
 * <b>(7) BAN</b>: user who sent the message is banned from the server 
 */
public enum FilterResponseAction {

	/* 1 */ SKIP_PROCESS,
	/* 2 */ SEND_RESPONSE_GUILD, 
	/* 3 */ SEND_RESPONSE_PRIVATE,
	/* 4 */ DELETE_MESSAGE, 
	/* 5 */ ROLE,
	/* 6 */ KICK, 
	/* 7 */ BAN;

	public static final EnumSet<FilterResponseAction> DEFAULT = EnumSet.of(FilterResponseAction.SKIP_PROCESS);
	
	public static String toDescription(FilterResponseAction action) {
		switch(action) {
		case BAN:
			return "user banned";
		case DELETE_MESSAGE:
			return "message deleted";
		case KICK:
			return "user kicked";
		case ROLE:
			return "role applied";
		case SEND_RESPONSE_GUILD:
			return "warning sent in server";
		case SEND_RESPONSE_PRIVATE:
			return "warning sent in private message";
		case SKIP_PROCESS:
			return "message not processed into cutebot database";
		default:
			throw new IllegalArgumentException("no description for " + action);
		}
	}
	
	/**
	 * converts a string of numbers into an enumset containing the corresponding 
	 * filterresponseactions. allows users to indicate a set of actions via a simple 
	 * string (eg 137) instead of having to specify exact enum names or something
	 * @param numbers a string containing only numbers corresponding to filter response 
	 * actions, as indicated in the FilterResponseAction definition
	 * @return an EnumSet containing the FilterResponseActions that match the numbers
	 * contained in the string
	 */
	public static EnumSet<FilterResponseAction> numbersToActions(String numbers) {
		EnumSet<FilterResponseAction> set = EnumSet.noneOf(FilterResponseAction.class);
		for(char c : numbers.toCharArray()) {
			set.add(numberToAction(Character.getNumericValue(c)));
		}
		return set;
	}
	
	private static FilterResponseAction numberToAction(int num) {
		switch(num) {
		case 1:
			return FilterResponseAction.SKIP_PROCESS;
		case 2:
			return FilterResponseAction.SEND_RESPONSE_GUILD;
		case 3:
			return FilterResponseAction.SEND_RESPONSE_PRIVATE;
		case 4:
			return FilterResponseAction.DELETE_MESSAGE;
		case 5:
			return FilterResponseAction.ROLE;
		case 6:
			return FilterResponseAction.KICK;
		case 7:
			return FilterResponseAction.BAN;
		default:
			throw new IllegalArgumentException("invalid number with no mapped FilterResponseAction - " + num);
		}
	}
}
