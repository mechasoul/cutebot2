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

import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

/*
 * implementation for default guild database. holds a map of user ids -> guild
 * ids for users -> their set default guilds
 * 
 * map implemented via a trove map, for minimal memory footprint. considered 
 * storing it on disk but this is much simpler and memory footprint should be 
 * small enough this way that its fine
 * 
 * note that because we use trove long-long map bc of small memory usage, we 
 * need to constantly convert back and forth between string/long. this is 
 * kinda awkward but probably preferable to using a string/string map (im sure
 * neither performance benefit is actually noticeable but id def prefer lower
 * memory use to slightly faster query response)
 * 
 * also note that as a consequence of the above, basically all of these methods
 * can throw a NumberFormatException but all input for these methods should be 
 * validated already somewhere else so i'm not checking for it
 */
class DefaultGuildDatabaseImpl implements DefaultGuildDatabase {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultGuildDatabaseImpl.class);
	
	private final TLongLongMap defaultGuilds;
	private final Path path;
	
	public DefaultGuildDatabaseImpl() throws IOException {
		this.path = PathUtils.getDefaultGuildsFile();
		this.defaultGuilds = this.load();
	}

	/*
	 * gets the default guild id associated with the given user id
	 * if no such user id exists, returns null
	 */
	@Override
	public synchronized String getDefaultGuildId(String userId) throws IOException {
		return this.getDefaultGuildId(Long.parseLong(userId));
	}
	
	@Override
	public synchronized String getDefaultGuildId(User user) throws IOException {
		return this.getDefaultGuildId(user.getIdLong());
	}
	
	private synchronized String getDefaultGuildId(long userId) throws IOException {
		long guildId = this.defaultGuilds.get(userId);
		if(guildId == this.defaultGuilds.getNoEntryValue()) {
			return null;
		} else {
			return ""+guildId;
		}
	}

	/*
	 * adds a new entry to the default guild map
	 * if the given user id already exists in the map as a key, overwrites
	 * the value associated with it with the given guild id
	 * 
	 * returns the previously associated value, or null if given user id 
	 * didn't already exist in the map
	 */
	@Override
	public synchronized String setDefaultGuildId(String userId, String guildId) throws IOException {
		return this.setDefaultGuildId(Long.parseLong(userId), Long.parseLong(guildId));
	}
	
	@Override
	public synchronized String setDefaultGuildId(User user, Guild guild) throws IOException {
		return this.setDefaultGuildId(user.getIdLong(), guild.getIdLong());
	}
	
	private synchronized String setDefaultGuildId(long userId, long guildId) throws IOException {
		long previousValue = this.defaultGuilds.put(userId, guildId);
		this.save();
		if(previousValue == this.defaultGuilds.getNoEntryValue()) {
			return null;
		} else {
			return ""+previousValue;
		}
	}

	/*
	 * removes the entry associated with the given user id key from the map
	 * if no such entry exists, nothing happens
	 * 
	 * returns the guild id that was associated with the user id, or null if
	 * the user id didn't exist in the map
	 */
	@Override
	public synchronized String clearDefaultGuildId(String userId) throws IOException {
		return this.clearDefaultGuildId(Long.parseLong(userId));
	}
	
	@Override
	public synchronized String clearDefaultGuildId(User user) throws IOException {
		return this.clearDefaultGuildId(user.getIdLong());
	}
	
	private synchronized String clearDefaultGuildId(long id) throws IOException {
		long previousValue = this.defaultGuilds.remove(id);
		if(previousValue == this.defaultGuilds.getNoEntryValue()) {
			return null;
		} else {
			this.save();
			return ""+previousValue;
		}
	}
	
	private synchronized void save() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE)) {
			StringBuilder data = new StringBuilder();
			this.defaultGuilds.forEachEntry((userId, guildId) -> 
			{
				data.append(userId);
				data.append(",");
				data.append(guildId);
				data.append(System.lineSeparator());
				return true;
			});
			writer.append(data.toString().trim());
		}
	}
	
	private synchronized TLongLongMap load() throws IOException {
		TLongLongMap map = new TLongLongHashMap(10, 0.5f, -1, -1);
		try {
			try (Stream<String> lines = Files.lines(this.path, StandardCharsets.UTF_8)) {
				lines.forEach(line ->
				{
					String[] ids = line.split(",");
					map.put(Long.parseLong(ids[0]), Long.parseLong(ids[1]));
				});
			}
		} catch (NoSuchFileException e) {
			logger.info(this + ": NoSuchFileException when trying to load default guilds database (first run?)");
		}
		return map;
	}

	@Override
	public String toString() {
		return "DefaultGuildDatabaseImpl";
	}
}
