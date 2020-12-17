package my.cute.bot.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.GuildPreferences;
import my.cute.bot.preferences.wordfilter.WordFilter;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;

public class CommandFactory {

	//TODO load user-defined commands from a file somewhere
	public static CommandSet<TextChannelCommand> newDefaultTextChannelSet(JDA jda, String id, GuildPreferences prefs) {
		CommandSet<TextChannelCommand> set = new CommandSetImpl<TextChannelCommand>(2);
		set.put("quote", new TextChannelQuoteCommand(jda, id, prefs));
		return set;
	}
	
	public static GuildCommandSet loadGuildCommandSet(JDA jda, String guildId, GuildPreferences prefs) throws IOException {
		ConcurrentMap<String, RoleCommandDatabase> roleCommandDbs = new ConcurrentHashMap<>(3);
		
		GuildCommandSet set = new GuildCommandSetImpl(jda, guildId, roleCommandDbs);
		set.put("quote", new TextChannelQuoteCommand(jda, guildId, prefs));
		
		try (Stream<Path> stream = Files.walk(PathUtils.getGeneratedRoleCommandsDirectory(guildId))) {
			stream.forEach(file -> {
				try {
					if(file.toFile().isFile()) {
						//enforce alphanumeric command names, so the first occurrence of "." must be for file extension
						String commandName = file.getFileName().toString().split("\\.")[0];
						RoleCommandDatabase db = RoleCommandDatabaseFactory.load(guildId, commandName, file);
						set.put(commandName, new GeneratedTextChannelRoleCommand(commandName, db, jda, guildId));
						roleCommandDbs.put(commandName, db);
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		
		
		return set;
	}
	
	public static CommandSet<PrivateChannelCommand> newDefaultPrivateChannelSet(JDA jda, MyListener bot, 
			DefaultGuildDatabase defaultGuilds, Map<String, GuildPreferences> allPrefs, Map<String, WordFilter> allFilters, ExecutorService executor) {
		CommandSet<PrivateChannelCommand> set = new CommandSetImpl<PrivateChannelCommand>(17);
		set.put(PrivateChannelAutoCommand.NAME, new PrivateChannelAutoCommand(allPrefs));
		set.put(PrivateChannelChannelCommand.NAME, new PrivateChannelChannelCommand(jda));
		set.put(PrivateChannelDefaultCommand.NAME, new PrivateChannelDefaultCommand(jda, defaultGuilds));
		set.put(PrivateChannelExitCommand.NAME, new PrivateChannelExitCommand(bot));
		set.put(PrivateChannelExportCommand.NAME, new PrivateChannelExportCommand(bot));
		set.put(PrivateChannelFilterCommand.NAME, new PrivateChannelFilterCommand(allFilters));
		set.put(PrivateChannelGuildCommand.NAME, new PrivateChannelGuildCommand(jda));
		set.put(PrivateChannelMaintCommand.NAME, new PrivateChannelMaintCommand(bot));
		set.put(PrivateChannelRebuildCommand.NAME, new PrivateChannelRebuildCommand(jda, bot, executor, allPrefs));
		set.put(PrivateChannelStatusCommand.NAME, new PrivateChannelStatusCommand(jda));
		
		return set;
	}
	
}
