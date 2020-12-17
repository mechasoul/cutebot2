package my.cute.bot.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

class GuildCommandSetImpl extends CommandSetImpl<TextChannelCommand> implements GuildCommandSet {

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

	@Override
	public boolean createRoleCommand(String name, List<Role> roles) {
		return false;
	}

	@Override
	public boolean createRoleCommand(String name, Role role) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteRoleCommand(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRoleCommand(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RoleCommandDatabase getRoleCommandDatabase(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableList<RoleCommandDatabase> getRoleCommandDatabases() {
		return ImmutableList.copyOf(this.roleCommandDbs.values());
	}

}
