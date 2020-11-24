package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Message;

/**
 * for managing a server's wordfilter, an object that can be configured to perform
 * some collection of actions when a user says any of a collection of flagged phrases
 * <p>
 * <b>use</b>: !filter mode args [guild id]
 * <p>
 * <b>modes</b>: add, remove, clear, set, regex, action, view
 * <p>
 * <b>add</b>: args should be a comma-separated list of words to add to the filter. adds those
 * words in addition to whatever words already exist in the filter<br>
 * <b>remove</b>: args should be a comma-separated list of words to remove from the filter. removes
 * any words present in the list from the filter, leaving the remainder<br>
 * <b>clear</b>: no args. clears the filter, removing all words. after this, the filter won't match
 * on anything. note this does not change the set filter response actions<br>
 * <b>set</b>: args should be a comma-separated list of words to use for the filter, or a regex string
 * if the filter is in regex mode (see regex mode). discards any words that existed in the 
 * filter prior to executing the command
 * <b>regex</b>: args should either be "on" or "off". "on" switches the filter to regex mode - add and
 * remove will no longer have any effect and the set list of words will be ignored. instead, 
 * an explicit regex filter can be set via "set" mode, for more advanced filter use<br>
 * <b>action</b>: args should be a single word consisting only of numbers - each number indicates a
 * filter response action to use TODO put mapping here<br>
 * <b>view</b>: no args. view the current filter (the word list if in basic mode, the regex
 * if in regex mode) and the current filter response actions
 *
 */
public class PrivateChannelFilterCommand extends PrivateChannelCommandTargeted {

	PrivateChannelFilterCommand(String name, PermissionLevel permission, int min, int max) {
		super(name, permission, min, max);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void execute(Message message, String[] params, String targetGuild) {
		// TODO Auto-generated method stub

	}

}
