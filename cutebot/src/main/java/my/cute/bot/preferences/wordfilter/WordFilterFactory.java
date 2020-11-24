package my.cute.bot.preferences.wordfilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.PathUtils;

public class WordFilterFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(WordFilterFactory.class);

	public static WordFilter load(String id) {
		Path path = PathUtils.getWordFilterFile(id);
		if(Files.exists(path)) {
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				WordFilter.Type filterType = WordFilter.Type.valueOf(reader.readLine());
				String roleId = reader.readLine();
				String[] words = reader.readLine().split(",");
				String pattern = reader.readLine();
				EnumSet<FilterResponseAction> actions = EnumSet.copyOf(Arrays.stream(reader.readLine().split(","))
						.map(responseName -> FilterResponseAction.valueOf(responseName)).collect(Collectors.toList()));
				return new WordFilterImpl(id, path, filterType, roleId, words, pattern, actions);
			} catch (IOException e) {
				/*
				 * maybe shouldnt swallow this
				 * but if something is really wrong im sure itll appear in a more fatal spot somewhere else
				 */
				logger.warn("WordFilterFactory: IOException thrown when trying to load wordfilter for id '" + id
						+ "', loading default wordfilter instead", e);
				return new WordFilterImpl(id, path);
			}
		} else {
			return new WordFilterImpl(id, path);
		}
		
	}
}
