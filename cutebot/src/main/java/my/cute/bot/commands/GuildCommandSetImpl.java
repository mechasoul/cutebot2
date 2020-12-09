package my.cute.bot.commands;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;

class GuildCommandSetImpl extends CommandSetImpl<TextChannelCommand> implements GuildCommandSet {

	private final JDA jda;
	private final Map<String, RoleCommandDatabase> roleCommandDbs;
	
	GuildCommandSetImpl(int capacity, JDA jda, Map<String, RoleCommandDatabase> dbs) {
		super(capacity);
		this.jda = jda;
		this.roleCommandDbs = dbs;
	}

	@Override
	public boolean createRoleCommand(String name, List<Role> roles) {
		thi
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
	public ImmutableList<Role> getRoleCommands() {
		// TODO Auto-generated method stub
		return null;
	}

}
