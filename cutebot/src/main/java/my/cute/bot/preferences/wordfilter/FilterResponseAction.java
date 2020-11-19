package my.cute.bot.preferences.wordfilter;

import java.util.EnumSet;

enum FilterResponseAction {

	SKIP_PROCESS, SEND_RESPONSE_GUILD, SEND_RESPONSE_PRIVATE, DELETE_MESSAGE, 
	ROLE, KICK, BAN;
	
	public static final EnumSet<FilterResponseAction> DEFAULT = EnumSet.of(FilterResponseAction.SKIP_PROCESS,
			FilterResponseAction.SEND_RESPONSE_GUILD, FilterResponseAction.DELETE_MESSAGE);
}
