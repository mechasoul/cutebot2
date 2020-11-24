package my.cute.bot.preferences.wordfilter;

import java.util.EnumSet;

public enum FilterResponseAction {

	/* 1 */ SKIP_PROCESS,
	/* 2 */ SEND_RESPONSE_GUILD, 
	/* 3 */ SEND_RESPONSE_PRIVATE,
	/* 4 */ DELETE_MESSAGE, 
	/* 5 */ ROLE,
	/* 6 */ KICK, 
	/* 7 */ BAN;

	public static final EnumSet<FilterResponseAction> DEFAULT = EnumSet.of(FilterResponseAction.SKIP_PROCESS,
			FilterResponseAction.SEND_RESPONSE_GUILD, FilterResponseAction.DELETE_MESSAGE);
}
