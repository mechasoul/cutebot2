package my.cute.bot.preferences.wordfilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class WordFilterFactory {

	public static WordFilter load(String id, Path file) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			WordFilter.Type filterType = WordFilter.Type.valueOf(reader.readLine());
			String[] words = reader.readLine().split(",");
			String pattern = reader.readLine();
			EnumSet<FilterResponseAction> actions = EnumSet.copyOf(Arrays.stream(reader.readLine().split(","))
					.map(responseName -> FilterResponseAction.valueOf(responseName)).collect(Collectors.toList()));
			return new WordFilterImpl(id, file, filterType, words, pattern, actions);
		} catch (NoSuchFileException e) {
			//first run?
			return new WordFilterImpl(id, file);
		}
	}
}
