package my.cute.bot.preferences.wordfilter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import my.cute.bot.util.MiscUtils;

public class WordFilterImpl implements WordFilter {

	private static final String EMPTY_COMPILED_FILTER_TOKEN = "[null]";
	private static final long TIMEOUT = 1;
	private static final TimeUnit UNIT = TimeUnit.SECONDS;
	
	private final String id;
	private final Path path;
	private WordFilter.Type mode;
	private String roleId;
	private Set<String> filteredWords;
	private Pattern compiledFilter;
	private EnumSet<FilterResponseAction> responseActions;
	private boolean enabled;
	private int strikes;
	
	WordFilterImpl(String id, Path path) {
		this.id = id;
		this.path = path;
		this.mode = WordFilter.Type.BASIC;
		this.roleId = "";
		this.filteredWords = new HashSet<String>(3);
		this.compiledFilter = null;
		this.responseActions = FilterResponseAction.DEFAULT;
		this.enabled = true;
		this.strikes = 0;
	}
	
	WordFilterImpl(String id, Path path, WordFilter.Type type, String roleId, String[] words,
			String pattern, EnumSet<FilterResponseAction> actions, boolean enabled, int strikes) {
		this.id = id;
		this.path = path;
		this.mode = type;
		this.roleId = roleId;
		this.filteredWords = new HashSet<String>((words.length * 4 / 3) + 1);
		for(String word : words) {
			if(!word.isBlank()) this.filteredWords.add(word);
		}
		this.compiledFilter = pattern.equals(EMPTY_COMPILED_FILTER_TOKEN) ? null : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		this.responseActions = actions;
		this.enabled = enabled;
		this.strikes = strikes;
	}
	
	@Override
	public synchronized String check(String input) throws TimeoutException {
		if(this.compiledFilter == null || !this.isEnabled()) return null;
		try {
			return MiscUtils.findMatchWithTimeout(this.compiledFilter, input, TIMEOUT, UNIT);
		} catch (InterruptedException | ExecutionException e) {
			//TODO ?
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public synchronized String strip(String input, String replacement) throws TimeoutException {
		if(this.compiledFilter == null || !this.isEnabled()) return input;
		
		try {
			Matcher matcher = this.compiledFilter.matcher(input);
			CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> matcher.replaceAll(replacement));
			return future.get(TIMEOUT, UNIT);
		} catch (InterruptedException | ExecutionException e) {
			//TODO ?
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized boolean add(final String[] words) throws IOException {
		if(this.mode == WordFilter.Type.REGEX) return false;
		int initialNumFilteredWords = this.filteredWords.size();
		if(initialNumFilteredWords >= WordFilter.MAX_FILTERED_WORDS) return false;
		for(String word : words) {
			if(this.filteredWords.size() >= WordFilter.MAX_FILTERED_WORDS) break;
			if(word.length() <= WordFilter.MAX_WORD_LENGTH && !word.isBlank()) this.filteredWords.add(word);
		}
		boolean filterChanged = this.filteredWords.size() > initialNumFilteredWords;
		if (filterChanged) {
			this.updateCompiledFilter();
			this.save();
		}
		return filterChanged;
	}

	@Override
	public synchronized boolean remove(String[] words) throws IOException {
		if(this.mode == WordFilter.Type.REGEX) return false;
		int initialNumFilteredWords = this.filteredWords.size();
		if(this.filteredWords.isEmpty()) return false;
		for(String word : words) {
			if(this.filteredWords.isEmpty()) break;
			this.filteredWords.remove(word);
		}
		boolean filterChanged = this.filteredWords.size() < initialNumFilteredWords;
		if (filterChanged) {
			this.updateCompiledFilter();
			this.save();
		}
		return filterChanged;
	}

	@Override
	public synchronized void clear() throws IOException {
		this.mode = WordFilter.Type.BASIC;
		this.filteredWords.clear();
		this.updateCompiledFilter();
		this.save();
	}

	/**
	 * assume the given String is a comma-separated list of words, or an 
	 * explicit regex string (depends on mode)
	 * <br><br>note no validation on the given String is performed, so do
	 * that before calling this if allowing explicit regex
	 */
	@Override
	public synchronized void set(String words) throws IOException, PatternSyntaxException {
		if(this.mode == WordFilter.Type.BASIC) {
			this.filteredWords.clear();
			for(String word : words.split(",")) {
				if(this.filteredWords.size() >= MAX_FILTERED_WORDS) break;
				word = word.trim();
				if(word.length() <= MAX_WORD_LENGTH && !word.isBlank()) this.filteredWords.add(word);
			}
			this.updateCompiledFilter();
		} else /* this.mode == WordFilter.Type.REGEX */ {
			this.compiledFilter = Pattern.compile(words, Pattern.CASE_INSENSITIVE);
		}
		this.save();
	}

	@Override
	public synchronized String get() {
		if(this.mode == WordFilter.Type.BASIC) {
			return String.join(",", this.filteredWords.stream().sorted().collect(Collectors.toList()));
		} else /* this.mode == WordFilter.Type.REGEX */ {
			return (this.compiledFilter == null ? "" : this.compiledFilter.pattern());
		}
	}
	
	/**
	 * if current mode is REGEX, switch back to BASIC
	 * if current mode is BASIC, clear the filter
	 * @throws IOException 
	 */
	@Override
	public synchronized WordFilter.Type handleTimeout(String input) throws IOException {
		WordFilter.Type type;
		if(this.getType() == WordFilter.Type.REGEX) {
			type = WordFilter.Type.REGEX;
			this.setType(WordFilter.Type.BASIC);
		} else /* this.getType() == WordFilter.Type.BASIC */ {
			type = WordFilter.Type.BASIC;
			this.clear();
		}
		this.save();
		return type;
	}

	@Override
	public synchronized void setActions(EnumSet<FilterResponseAction> actions) throws IOException {
		this.responseActions = EnumSet.copyOf(actions);
		this.save();
	}

	@Override
	public synchronized EnumSet<FilterResponseAction> getActions() {
		return this.responseActions;
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	@Override
	public synchronized void setType(WordFilter.Type type) throws IOException {
		Type previousType = this.mode;
		this.mode = type;
		if(previousType == Type.REGEX && type == Type.BASIC) {
			this.updateCompiledFilter();
		}
		this.save();
	}
	
	@Override
	public synchronized WordFilter.Type getType() {
		return this.mode;
	}
	
	@Override
	public synchronized boolean isEnabled() {
		return this.enabled;
	}
	
	@Override
	public synchronized void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public synchronized void addStrike() {
		if(this.strikes < WordFilter.getStrikesToDisable()) strikes++;
	}

	@Override
	public synchronized int getStrikes() {
		return this.strikes;
	}

	@Override
	public synchronized void resetStrikes() {
		this.strikes = 0;
	}
	
	@Override
	public synchronized void setRoleId(String id) throws IOException {
		this.roleId = id;
		this.save();
	}
	
	@Override
	public synchronized void clearRoleId() throws IOException {
		this.roleId = "";
		this.save();
	}

	@Override
	public synchronized String getRoleId() {
		return this.roleId;
	}
	
	@Override
	public int getMaxWords() {
		return WordFilter.MAX_FILTERED_WORDS;
	}
	
	@Override
	public int getMaxWordLength() {
		return WordFilter.MAX_WORD_LENGTH;
	}
	
	/**
	 * compiles the Pattern object used to do the actual matching on strings. pre-compiling is good
	 * for performance i guess?
	 * generated regex will match all words in the filter, as whole words, optionally ending with an 's'
	 * for example, filter list "apple,orange,banana" is built into string 
	 * "\\W*((?:apple)|(?:orange)|(?:banana))s?\\W*"
	 * which should match any whole word: apple, apples, orange, oranges, banana, bananas
	 * the word "crabapple" would not trigger a match
	 * filter will also match any number of non-word characters at start or end
	 * eg "`apples?`" would be matched by having "apple" as a filtered word (optional 's', non-word characters '`' and '?')
	 */
	private synchronized void updateCompiledFilter() {
		if(this.filteredWords.isEmpty()) {
			this.compiledFilter = null;
		} else {
			String newFilter = "\\W*((?:" + String.join(")|(?:", this.filteredWords) + "))s?\\W*";
			this.compiledFilter = Pattern.compile(newFilter, Pattern.CASE_INSENSITIVE);
		}
	}

	@Override
	public synchronized void save() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(this.getType().name());
			writer.newLine();
			writer.append(this.roleId);
			writer.newLine();
			writer.append(String.join(",", this.filteredWords));
			writer.newLine();
			writer.append(this.compiledFilter == null ? EMPTY_COMPILED_FILTER_TOKEN : this.compiledFilter.pattern());
			writer.newLine();
			writer.append(this.responseActions.stream().map(action -> action.name()).collect(Collectors.joining(",")));
			writer.newLine();
			writer.append(""+this.enabled);
			writer.newLine();
			writer.append(""+this.strikes);
		}
	}

	@Override
	public String toString() {
		return "WordFilterImpl-" + this.id;
	}
}
