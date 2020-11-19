package my.cute.bot.preferences.wordfilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class WordFilterFactory {

	public static WordFilter load(String id, Path file) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			WordFilter wordFilter = null;
			WordFilter.Type filterType = WordFilter.Type.valueOf(reader.readLine());
			if(filterType == WordFilter.Type.BASIC) {
				String[] words = reader.readLine().split(",");
				String pattern = reader.readLine();
				EnumSet<FilterResponseAction> actions = EnumSet.copyOf(Arrays.stream(reader.readLine().split(","))
						.map(responseName -> FilterResponseAction.valueOf(responseName)).collect(Collectors.toList()));
				wordFilter = new WordFilterBasicImpl(id, file, words, pattern, actions);
			} else if(filterType == WordFilter.Type.REGEX) {
				String pattern = reader.readLine();
				EnumSet<FilterResponseAction> actions = EnumSet.copyOf(Arrays.stream(reader.readLine().split(","))
						.map(responseName -> FilterResponseAction.valueOf(responseName)).collect(Collectors.toList()));
				wordFilter = new WordFilterRegexImpl(id, file, pattern, actions);
			} else {
				//?
			}
			return wordFilter;
		}
	}
}
