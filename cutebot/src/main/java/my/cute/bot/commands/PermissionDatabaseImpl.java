package my.cute.bot.commands;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.entities.User;

class PermissionDatabaseImpl implements PermissionDatabase {
	
	/*
	 * permission database implementation
	 * implementation note: currently everyone has user permissions and there's no difference
	 * between admin and dev permissions (ie, dev permission is done by checking the global
	 * permissions database, and admin permission is done on a per-guild basis. there's no
	 * command that requires global admin permission but not global dev permission, which is
	 * to say global admin permission doesnt exist. similarly, no targeted commands require
	 * dev permission, ie theres no per-guild dev permission)
	 * 
	 * this could change in the future in which case our permission structure would have to
	 * be a map of id -> permission level that id possesses or something, but for now we
	 * can just use lightweight list
	 * 
	 * it also means actually all these methods could just omit the PermissionLevel parameter
	 * but again this could very easily change and i dont want to have to redo all this stuff
	 * later
	 */
	
	private final static Logger logger = LoggerFactory.getLogger(PermissionDatabaseImpl.class);

	private final String id;
	private final Path path;
	private final TLongSet users;
	
	PermissionDatabaseImpl(String id) throws IOException {
		this.id = id;
		this.path = PathUtils.getPermissionsFile(this.id);
		Files.createDirectories(this.path.getParent());
		this.users = this.load();
	}
	
	@Override
	public synchronized boolean add(String userId, PermissionLevel permission) throws IOException {
		boolean newPermission = this.users.add(Long.parseLong(userId));
		if(newPermission) this.save();
		return newPermission;
	}
	
	@Override
	public synchronized boolean add(User user, PermissionLevel permission) throws IOException {
		return this.add(user.getId(), permission);
	}

	@Override
	public synchronized boolean remove(String userId, PermissionLevel permission) throws IOException {
		boolean removedPermission = this.users.remove(Long.parseLong(userId));
		if(removedPermission) this.save();
		return removedPermission;
	}
	
	@Override
	public synchronized boolean remove(User user, PermissionLevel permission) throws IOException {
		return this.remove(user.getId(), permission);
	}

	@Override
	public synchronized boolean hasPermission(String userId, PermissionLevel permission) {
		return this.users.contains(Long.parseLong(userId));
	}
	
	@Override
	public synchronized boolean hasPermission(User user, PermissionLevel permission) {
		return this.hasPermission(user.getId(), permission);
	}

	@Override
	public String getId() {
		return this.id;
	}
	
	private synchronized void save() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE)) {
			StringBuilder data = new StringBuilder();
			this.users.forEach(id -> 
			{
				data.append(id);
				data.append(System.lineSeparator());
				return true;
			});
			writer.append(data.toString().trim());
		}
	}
	
	private synchronized TLongSet load() throws IOException {
		TLongSet users = new TLongHashSet(2);
		try (Stream<String> lines = Files.lines(this.path, StandardCharsets.UTF_8)) {
			lines.forEach(userId -> users.add(Long.parseLong(userId)));
		} catch (NoSuchFileException e) {
			logger.info(this + ": NoSuchFileException when trying to load permissions list (first run?)");
		}
		return users;
	}
	
	@Override
	public String toString() {
		return "PermissionDatabaseImpl-" + this.id;
	}

}
