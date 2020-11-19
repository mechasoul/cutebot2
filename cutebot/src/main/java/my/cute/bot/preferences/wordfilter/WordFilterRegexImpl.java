package my.cute.bot.preferences.wordfilter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WordFilterRegexImpl extends WordFilterBasicImpl {
	
	WordFilterRegexImpl(String id, Path path) {
		super(id, path);
	}
	
	WordFilterRegexImpl(String id, Path path, String pattern, EnumSet<FilterResponseAction> actions) {
		super(id, path);
		this.compiledFilter = pattern.equals(EMPTY_COMPILED_FILTER_TOKEN) ? null : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		this.responseActions = actions;
	}

	@Override
	public boolean add(String[] words) {
		throw new UnsupportedOperationException("can't add words to an explicit regex wordfilter");
	}

	@Override
	public boolean remove(String[] words) {
		throw new UnsupportedOperationException("can't remove words from an explicit regex wordfilter");
	}

	@Override
	public void clear() throws IOException {
		this.compiledFilter = null;
		this.save();
	}

	/*
	 * TODO some kind of validation - exposing this completely to arbitrary user-given
	 * regex seems dangerous
	 */
	@Override
	public void set(String words) throws IOException {
		this.compiledFilter = Pattern.compile(words, Pattern.CASE_INSENSITIVE);
		this.save();
	}

	@Override
	public String get() {
		return this.compiledFilter.pattern();
	}

	@Override
	public Type getType() {
		return WordFilter.Type.REGEX;
	}

	@Override
	public void save() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(this.getType().name());
			writer.newLine();
			writer.append(this.compiledFilter == null ? EMPTY_COMPILED_FILTER_TOKEN : this.compiledFilter.pattern());
			writer.newLine();
			writer.append(this.responseActions.stream().map(action -> action.name()).collect(Collectors.joining(",")));
		}
	}

}
