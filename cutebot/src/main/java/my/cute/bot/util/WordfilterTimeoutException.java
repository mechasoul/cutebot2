package my.cute.bot.util;

import my.cute.bot.preferences.wordfilter.WordFilter;

public class WordfilterTimeoutException extends Exception {

	//the type of the wordfilter that threw the exception
	private final WordFilter.Type type;
	
	public WordfilterTimeoutException(Throwable cause, WordFilter.Type type) {
		super(cause);
		this.type = type;
	}
	
	public WordFilter.Type getType() {
		return this.type;
	}

	private static final long serialVersionUID = 1L;

}
