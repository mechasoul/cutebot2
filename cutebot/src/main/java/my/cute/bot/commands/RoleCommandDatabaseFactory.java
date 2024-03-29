package my.cute.bot.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import gnu.trove.set.hash.THashSet;
import my.cute.bot.util.BiMapStringStringTypeAdapter;
import my.cute.bot.util.PathUtils;
import my.cute.bot.util.THashSetStringTypeAdapter;

public class RoleCommandDatabaseFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(RoleCommandDatabaseFactory.class);
	static final Gson GSON = new GsonBuilder()
			.registerTypeHierarchyAdapter(THashSet.class, new THashSetStringTypeAdapter())
			.registerTypeHierarchyAdapter(BiMap.class, new BiMapStringStringTypeAdapter())
			.create();

	public static RoleCommandDatabase load(String guildId, String commandName) throws IOException {
		Path path = PathUtils.getGeneratedRoleCommandDatabase(guildId, commandName);
		return load(guildId, commandName, path);
	}
	
	public static RoleCommandDatabase load(String guildId, String commandName, Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			Set<String> roles = GSON.fromJson(reader.readLine(), new TypeToken<THashSet<String>>(){}.getType());
			BiMap<String, String> aliases = GSON.fromJson(reader.readLine(), new TypeToken<BiMap<String, String>>(){}.getType());
			return new RoleCommandDatabaseImpl(guildId, commandName, roles, aliases);
		} catch (NoSuchFileException e) {
			logger.info("RoleCommandDatabaseFactory: missing db file for guild '" + guildId + "', command '" 
					+ commandName + "' (first load?)");
			return new RoleCommandDatabaseImpl(guildId, commandName);
		}
	}
	
	public static RoleCommandDatabase create(String guildId, String commandName) {
		return new RoleCommandDatabaseImpl(guildId, commandName);
	}
	
}
