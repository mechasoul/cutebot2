package my.cute.bot.commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import my.cute.bot.util.PathUtils;

class PermissionDatabaseFactory {
	
	private final static Logger logger = LoggerFactory.getLogger(PermissionDatabaseFactory.class);

	static PermissionDatabase load(String guildId) throws IOException {
		Path path = PathUtils.getPermissionsFile(guildId);
		TLongSet users = new TLongHashSet(2);
		try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
			lines.forEach(userId -> users.add(Long.parseLong(userId)));
		} catch (NoSuchFileException e) {
			logger.info("PermissionDatabaseFactory: NoSuchFileException when trying to load permissions list (first run?)");
		}
		return new PermissionDatabaseImpl(guildId, users);
	}
	
	@Override
	public String toString() {
		return "PermissionDatabaseFactory";
	}
	
}
