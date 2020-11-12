package my.cute.bot.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class PermissionManagerImpl implements PermissionManager {
	
	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(PermissionManagerImpl.class);
	private final static String GLOBAL_KEY = "global";

	/*
	 * structure for holding cutebot permissions
	 * uses key=guild id, value = permission database for that guild id
	 * also use key GLOBAL_KEY for the global permission database
	 */
	private final Map<String, PermissionDatabase> permissions;
	private final JDA jda;
	
	public PermissionManagerImpl(JDA jda) throws IOException {
		//TODO args
		this.permissions = new ConcurrentHashMap<String, PermissionDatabase>();
		this.jda = jda;
		try {
			this.jda.getGuilds().forEach(guild -> {
				try {
					this.permissions.put(guild.getId(), new PermissionDatabaseImpl(guild.getId()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		this.permissions.put(GLOBAL_KEY, new PermissionDatabaseImpl(GLOBAL_KEY));
	}

	@Override
	public boolean add(String userId, PermissionLevel permission) throws IOException {
		return this.permissions.get(GLOBAL_KEY).add(userId, permission);
	}
	
	@Override
	public boolean add(User user, PermissionLevel permission) throws IOException {
		return this.add(user.getId(), permission);
	}

	/*
	 * some input validation should be performed before calling this to make sure
	 * guildId is a valid guild id, so in theory this.permissions.get(guildId) 
	 * should never be null - but it's maybe possible for it to happen somehow
	 * with idk guild id caching or weird guild join/leave timings or something
	 * so we check just in case
	 */
	@Override
	public boolean add(String userId, String guildId, PermissionLevel permission) throws IOException {
		PermissionDatabase permDb = this.permissions.get(guildId);
		if(permDb != null) {
			 return permDb.add(userId, permission);
		} else {
			throw new IllegalArgumentException(this + ": called add with invalid guild id. params "
					+ "userId='" + userId + "', "
					+ "guildId='" + guildId + "', permission='" + permission + "'");
		}
	}
	
	@Override
	public boolean add(User user, Guild guild, PermissionLevel permission) throws IOException {
		return this.add(user.getId(), guild.getId(), permission);
	}

	@Override
	public boolean remove(String userId, PermissionLevel permission) throws IOException {
		return this.permissions.get(GLOBAL_KEY).remove(userId, permission);
	}
	
	@Override
	public boolean remove(User user, PermissionLevel permission) throws IOException {
		return this.remove(user.getId(), permission);
	}

	/*
	 * see add(String, String, PermissionLevel) re: null checking
	 */
	@Override
	public boolean remove(String userId, String guildId, PermissionLevel permission) throws IOException {
		PermissionDatabase permDb = this.permissions.get(guildId);
		if(permDb != null) {
			 return permDb.remove(userId, permission);
		} else {
			throw new IllegalArgumentException(this + ": called remove with invalid guild id. params "
					+ "userId='" + userId + "', "
					+ "guildId='" + guildId + "', permission='" + permission + "'");
		}
	}
	
	@Override
	public boolean remove(User user, Guild guild, PermissionLevel permission) throws IOException {
		return this.remove(user.getId(), guild.getId(), permission);
	}

	@Override
	public boolean hasPermission(String userId, PermissionLevel permission) {
		return this.permissions.get(GLOBAL_KEY).hasPermission(userId, permission);
	}
	
	@Override
	public boolean hasPermission(User user, PermissionLevel permission) {
		return this.hasPermission(user.getId(), permission);
	}

	/*
	 * see add(String, String, PermissionLevel) re: null checking
	 */
	@Override
	public boolean hasPermission(String userId, String guildId, PermissionLevel permission) {
		PermissionDatabase permDb = this.permissions.get(guildId);
		if(permDb != null) {
			 return permDb.hasPermission(userId, permission);
		} else {
			throw new IllegalArgumentException(this + ": called hasPermission with invalid guild "
					+ "id. params userId='" + userId + "', "
					+ "guildId='" + guildId + "', permission='" + permission + "'");
		}
	}
	
	@Override
	public boolean hasPermission(User user, Guild guild, PermissionLevel permission) {
		return this.hasPermission(user.getId(), guild.getId(), permission);
	}

	public String toString() {
		return "PermissionManagerImpl";
	}
}
