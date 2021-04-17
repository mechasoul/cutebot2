package my.cute.bot.preferences.wordfilter;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.TimeoutException;

/**
 * for managing a filter of banned words, including modifying the filter,
 * checking a string against the filter, etc
 */
public interface WordFilter {
	
	public enum Type {
		BASIC, REGEX
	}
	
	/**
	 * checks a given string against the wordfilter
	 * 
	 * @return the string if a match was found, or returns null
	 * 		if no match was found or if the wordfilter is disabled
	 * 		(see {@link#isEnabled()})
	 * @throws TimeoutException if a timeout occurs while matching
	 */
	public String check(String input) throws TimeoutException;
	
	/**
	 * adds words to the wordfilter. every word added will trigger the
	 * wordfilter after this call returns
	 * @param words the array of words to add to the wordfilter
	 * @return true if the wordfilter was changed as a result of this
	 * 		call, false otherwise
	 * @throws IOException 
	 */
	public boolean add(String[] words) throws IOException;
	
	/**
	 * removes words from the wordfilter. every word removed will no longer
	 * trigger the wordfilter after this call returns
	 * @param words the array of words to remove from the wordfilter
	 * @return true if the wordfilter was changed as a result of this call,
	 * 		false otherwise
	 * @throws IOException 
	 */
	public boolean remove(String[] words) throws IOException;
	
	/**
	 * removes all words from the wordfilter. the wordfilter will be empty 
	 * and won't trigger on anything after this call returns
	 * @throws IOException 
	 */
	public void clear() throws IOException;
	
	/**
	 * sets the wordfilter to filter only the words given. the exact syntax of the 
	 * given string is up to implementation
	 * <p>
	 * for us, assume the given String is a comma-separated list of words, or an 
	 * explicit regex string (depends on mode)
	 * <p>
	 * note no validation on the given String is performed, so do
	 * that before calling this if allowing explicit regex
	 * @param words the words to filter
	 * @throws IOException 
	 */
	public void set(String words) throws IOException;
	
	/**
	 * gets a string representation of the filtered words - eg a comma-separated
	 * list or similar. the result should work with set(String), eg, the following
	 * sequence should be valid and result in absolutely no change to the filter:
	 * <p><pre>
	 * WordFilter filter = getSomeFilter();
	 * String stringRepresentation = filter.get();
	 * filter.set(stringRepresentation);</pre>
	 * @return the string representation of the filtered words
	 */
	public String get();
	
	/**
	 * should be called whenever a timeout occurs during check(String). does any
	 * necessary tasks to fix the (presumably dangerous) filter
	 * @param input the string that triggered a timeout
	 * @return the type of the wordfilter when the timeout occurred 
	 * @throws IOException 
	 */
	public WordFilter.Type handleTimeout(String input) throws IOException;
	
	/**
	 * sets the action to take when a user triggers the wordfilter
	 * @param actions the set of actions to take when a user triggers the wordfilter
	 * @throws IOException 
	 */
	public void setActions(EnumSet<FilterResponseAction> actions) throws IOException;
	
	/**
	 * gets the set of actions that are taken when a user triggers the wordfilter
	 * @return the set of actions
	 */
	public EnumSet<FilterResponseAction> getActions();
	
	/**
	 * gets the id of the guild with which this wordfilter is associated
	 * @return the guild id
	 */
	public String getId();
	
	public WordFilter.Type getType();
	
	void setType(Type type) throws IOException;
	
	/**
	 * check whether or not the wordfilter is enabled. if the wordfilter is disabled,
	 * check() should never return a match
	 * @return true if the wordfilter is enabled, false if disabled
	 */
	public boolean isEnabled();
	
	/**
	 * sets whether the wordfilter is enabled or disabled (if the wordfilter is 
	 * disabled, check() should never return a match)
	 * @param enabled true to enable the wordfilter, false to disable it
	 */
	public void setEnabled(boolean enabled);
	
	/**
	 * since this class has methods for users to enter arbitrary regex, there's 
	 * room for users to really mess things up. this method, along with 
	 * {@link#getStrikes()} and {@link#resetStrikes()}, are a framework for 
	 * tracking when that happens, in case we want to disable some functionality
	 * if users are creating problems too frequently
	 * <p>
	 * this method should add a single instance of a strike to the filter
	 */
	public void addStrike();
	
	/**
	 * this method can be used to get the current number of strikes on this filter.
	 * see {@link#addStrike()} for explanation on strikes
	 * @return the number of strikes recorded on this filter
	 */
	public int getStrikes();
	
	/**
	 * used to reset the current number of strikes. see {@link#addStrike()} for more
	 * on strikes
	 */
	public void resetStrikes();
	
	public static int getStrikesToDisable() {
		return 3;
	}
	
	/**
	 * sets the wordfilter role id. one option for a FilterResponseAction is to apply
	 * a role to users who trigger the wordfilter (can be used to eg apply a "mute" role),
	 * so this should be used to set the id of that role
	 * @param id the id of the role to apply 
	 * @throws IOException 
	 */
	public void setRoleId(String id) throws IOException;
	
	/**
	 * gets the set wordfilter role id. one option for a FilterResponseAction is to apply
	 * a role to users who trigger the wordfilter (can be used to eg apply a "mute" role),
	 * so this should be used to get the id of that role.
	 * @return the id of the role to apply
	 */
	public String getRoleId();
	
	/*
	 * i dont like that i have save() in the implementation class and load() in a 
	 * factory class even though they're incredibly coupled. maybe should just have
	 * a dedicated seralizer/deserializer class and do it with gson or something
	 */
	/**
	 * saves the wordfilter to disk. the first line should be the type of the wordfilter
	 * as given by WordFilter.Type.name(), and then it's up to the implementation
	 * @throws IOException 
	 */
	public void save() throws IOException;

	

}
