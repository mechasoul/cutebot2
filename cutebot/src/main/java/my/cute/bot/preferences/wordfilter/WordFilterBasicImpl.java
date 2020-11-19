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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WordFilterBasicImpl implements WordFilter {

	private static final int MAX_FILTERED_WORDS = 60;
	protected static final String EMPTY_COMPILED_FILTER_TOKEN = "[null]";
	
	protected final String id;
	protected final Path path;
	private Set<String> filteredWords;
	protected Pattern compiledFilter;
	protected EnumSet<FilterResponseAction> responseActions;
	
	WordFilterBasicImpl(String id, Path path) {
		this.id = id;
		this.path = path;
		this.filteredWords = new HashSet<String>(3);
		this.compiledFilter = null;
		this.responseActions = FilterResponseAction.DEFAULT;
	}
	
	WordFilterBasicImpl(String id, Path path, String[] words, String pattern, EnumSet<FilterResponseAction> actions) {
		this.id = id;
		this.path = path;
		this.filteredWords = new HashSet<String>((words.length * 4 / 3) + 1);
		for(String word : words) {
			this.filteredWords.add(word);
		}
		this.compiledFilter = pattern.equals(EMPTY_COMPILED_FILTER_TOKEN) ? null : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		this.responseActions = actions;
	}
	
	@Override
	public synchronized String check(String input) {
		if(this.compiledFilter == null) return null;
		Matcher m = this.compiledFilter.matcher(input);
		if(m.find()) {
			return m.group();
		}
		return null;
	}

	@Override
	public synchronized boolean add(final String[] words) throws IOException {
		int initialNumFilteredWords = this.filteredWords.size();
		if(initialNumFilteredWords >= MAX_FILTERED_WORDS) return false;
		for(String word : words) {
			if(this.filteredWords.size() >= MAX_FILTERED_WORDS) break;
			this.filteredWords.add(word);
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
		this.filteredWords.clear();
		this.updateCompiledFilter();
		this.save();
	}

	/**
	 * assume the given String is a comma-separated list of words
	 */
	@Override
	public synchronized void set(String words) throws IOException {
		this.filteredWords.clear();
		for(String word : words.split(",")) {
			if(this.filteredWords.size() >= MAX_FILTERED_WORDS) break;
			this.filteredWords.add(word);
		}
		this.updateCompiledFilter();
		this.save();
	}

	@Override
	public String get() {
		return String.join(",", this.filteredWords);
	}

	@Override
	public synchronized void setAction(EnumSet<FilterResponseAction> actions) throws IOException {
		this.responseActions = EnumSet.copyOf(actions);
		this.save();
	}

	@Override
	public EnumSet<FilterResponseAction> getAction() {
		return this.responseActions;
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	@Override
	public WordFilter.Type getType() {
		return WordFilter.Type.BASIC;
	}
	
	/**
	 * compiles the Pattern object used to do the actual matching on strings. pre-compiling is good
	 * for performance i guess?
	 * generated regex will match all words in the filter, as whole words, optionally ending with an 's'
	 * for example, filter list "apple,orange,banana" is built into string 
	 * "\\b((?:apple)|(?:orange)|(?:banana))s?\\b"
	 * which should match any whole word: apple, apples, orange, oranges, banana, bananas
	 * the word "crabapple" would not trigger a match
	 */
	private synchronized void updateCompiledFilter() {
		if(this.filteredWords.isEmpty()) {
			this.compiledFilter = null;
		} else {
			String newFilter = "\\b((?:" + String.join(")|(?:", this.filteredWords) + "))s?\\b";
			this.compiledFilter = Pattern.compile(newFilter, Pattern.CASE_INSENSITIVE);
		}
	}

	@Override
	public synchronized void save() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(this.getType().name());
			writer.newLine();
			writer.append(String.join(",", this.filteredWords));
			writer.newLine();
			writer.append(this.compiledFilter == null ? EMPTY_COMPILED_FILTER_TOKEN : this.compiledFilter.pattern());
			writer.newLine();
			writer.append(this.responseActions.stream().map(action -> action.name()).collect(Collectors.joining(",")));
		}
	}
}
