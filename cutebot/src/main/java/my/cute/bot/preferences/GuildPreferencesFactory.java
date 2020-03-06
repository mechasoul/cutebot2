package my.cute.bot.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import my.cute.bot.util.PathUtils;

public class GuildPreferencesFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildPreferencesFactory.class);
	private static final Gson GSON = new GsonBuilder().create();

	public static GuildPreferences newDefaultGuildPreferences(String id) {
		return new GuildPreferencesImpl(id);
	}
	
	public static GuildPreferences loadGuildPreferences(String id) {
		Path preferencesFile = PathUtils.getPreferencesFile(id);
		if(Files.exists(preferencesFile)) {
			try (BufferedReader reader = Files.newBufferedReader(preferencesFile, StandardCharsets.UTF_8)) {
				return GSON.fromJson(reader.readLine(), GuildPreferencesImpl.class);
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("GuildPreferencesFactory: IOException thrown when trying to load preferences for id '" + id
						+ "', loading default preferences instead. ex: " + e.getMessage(), e);
				return new GuildPreferencesImpl(id);
			}
		} else {
			return new GuildPreferencesImpl(id);
		}
	}
}
