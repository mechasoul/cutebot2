package my.cute.bot.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableList;

import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;

class GuildCommandSetImpl extends CommandSetImpl<TextChannelCommand> implements GuildCommandSet {

	private static final int MAX_ROLE_COMMANDS = 16; 
	
	private final JDA jda;
	private final String id;
	private final ConcurrentMap<String, RoleCommandDatabase> roleCommandDbs;
	
	GuildCommandSetImpl(JDA jda, String guildId, ConcurrentMap<String, RoleCommandDatabase> dbs) throws IOException {
		this.jda = jda;
		this.id = guildId;
		this.roleCommandDbs = dbs;
		Files.createDirectories(PathUtils.getGeneratedRoleCommandsDirectory(this.id));
	}
	
//	@Override
//	public TextChannelCommand put(String name, TextChannelCommand command) {
//		return this.commandSet.putIfAbsent(name, command);
//	}
//	
//	@Override
//	public TextChannelCommand remove(String commandName) {
//		return this.commandSet.remove(commandName);
//	}
//
//	@Override
//	public boolean execute(String name, Message message, String[] params) {
//		TextChannelCommand command = this.commandSet.get(name);
//		if(command != null) {
//			command.execute(message, params);
//			return true;
//		} else {
//			return false;
//		}
//	}
	
	/*
	 * note PrivateChannelRoleCommand.execute() (under create case) is already making a check
	 * to ensure the created command doesn't share a name with any other command, so the use
	 * of putIfAbsent() here is maybe redundant? but i guess it's good practice anyway
	 */
	@Override
	public boolean createRoleCommand(String name, List<Role> roles) throws IOException {
		if(this.roleCommandDbs.size() < MAX_ROLE_COMMANDS) {
			RoleCommandDatabase db = RoleCommandDatabaseFactory.create(this.id, name);
			if(this.roleCommandDbs.putIfAbsent(name, db) == null) {
				db.add(roles);
				this.put(name, new GeneratedTextChannelRoleCommand(name, db, this.jda, this.id));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean createRoleCommand(String name, Role role) throws IOException {
		return this.createRoleCommand(name, List.of(role));
	}

	@Override
	public boolean deleteRoleCommand(String name) throws IOException {
		RoleCommandDatabase db = this.roleCommandDbs.remove(name);
		if(db != null) {
			db.delete();
			this.remove(name);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isRoleCommand(String name) {
		return this.roleCommandDbs.containsKey(name);
	}

	@Override
	public RoleCommandDatabase getRoleCommandDatabase(String name) {
		return this.roleCommandDbs.get(name);
	}

	@Override
	public ImmutableList<RoleCommandDatabase> getRoleCommandDatabases() {
		return ImmutableList.copyOf(this.roleCommandDbs.values());
	}

}
