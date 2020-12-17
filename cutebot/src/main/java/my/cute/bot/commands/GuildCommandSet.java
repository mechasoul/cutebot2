package my.cute.bot.commands;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import gnu.trove.map.TObjectLongMap;
import net.dv8tion.jda.api.entities.Role;

/**
 * holds all commands for a single guild, extending the functionality of a commandset
 * to include guild-specific commands like user-defined role-managing commands, and 
 * general user-defined pingpong commands
 * <p>
 * <b>note</b> all commands and parameters should be <b>case insensitive</b>
 */
public interface GuildCommandSet extends CommandSet<TextChannelCommand> {

	/**
	 * creates a new role command with the given name and roles
	 * @param name the name of the new command 
	 * @param roles the list of roles which should be able to be used with the new
	 * command
	 * @return true if a new command was created (ie, the commandset was modified as
	 * a result of this call), false otherwise (for example, if a command with that
	 * name already exists)
	 */
	public boolean createRoleCommand(String name, List<Role> roles);
	
	/**
	 * creates a new role command with the given name and single role
	 * @param name the name of the new command
	 * @param role the single role to be used with the new command
	 * @return true if a new command was created (ie, the commandset was modified as
	 * a result of this call), false otherwise (for example, if a command with that
	 * name already exists)
	 */
	public boolean createRoleCommand(String name, Role role);
	
	public boolean deleteRoleCommand(String name);
	
	public boolean isRoleCommand(String name);
	
	public RoleCommandDatabase getRoleCommandDatabase(String name);
	
	public ImmutableList<RoleCommandDatabase> getRoleCommandDatabases();
	
}
