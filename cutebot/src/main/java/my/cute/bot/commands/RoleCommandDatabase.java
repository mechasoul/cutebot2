package my.cute.bot.commands;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.api.entities.Role;

/**
 * holds data for a user-generated role command: which roles can be toggled via
 * the command, and aliases for those roles.
 * <p>
 * <b>note</b> all managed role names and aliases should be <b>case insensitive</b>
 * <p>
 * <b>also note about i/o:</b> this class represents a database that is managed 
 * without transactions; any modification to the database is followed immediately
 * by an update to the saved database on disk. consequently, any methods that 
 * modify the database will (likely) perform io and thus any code that uses this
 * class should be ready to handle ioexception
 */
interface RoleCommandDatabase {

	/**
	 * adds a role to the database for this command. does nothing if the given 
	 * role name already exists in the database
	 * @param roleName the name of the role to add
	 * @return true if the role name was successfully added (ie, the 
	 * database was modified as a result of this call), false otherwise
	 * @throws IOException 
	 */
	public boolean add(String roleName) throws IOException;
	
	public boolean add(Role role) throws IOException;
	
	/**
	 * adds the given roles to the database
	 * @param roles the roles to add
	 * @return an immutable list of all roles successfully added (if no roles 
	 * were successfully added, an empty list is returned)
	 * @throws IOException 
	 */
	public ImmutableList<Role> add(List<Role> roles) throws IOException;
	
	/**
	 * removes a role from the database for this command. should also remove
	 * any aliases that map to the given role if they exist
	 * @param roleName the name of the role to remove
	 * @return true if the role was successfully removed (ie, the database 
	 * was modified as a result of this call), false otherwise
	 * @throws IOException 
	 */
	public boolean remove(String roleName) throws IOException;
	
	public boolean remove(Role role) throws IOException;
	
	public ImmutableList<Role> remove(List<Role> roles) throws IOException;
	
	public ImmutableList<String> removeByName(String... roleNames) throws IOException;
	
	/**
	 * adds an alias to the database. aliases can be used as shorthand for
	 * a long role name. only one alias can exist for a given role
	 * @param alias the alias to use for the given role
	 * @param role the role being given an alias
	 * @return true if the alias was successfully added for the given role
	 * (ie, the database was modified as a result of this call), false
	 * otherwise
	 * @throws IOException 
	 */
	public boolean addAlias(String alias, Role role) throws IOException;
	
	/**
	 * removes an alias for a role from the database for this command
	 * @param alias the alias to remove
	 * @return true if the alias was successfully removed (ie, the database 
	 * was modified as a result of this call), false otherwise
	 * @throws IOException 
	 */
	public boolean removeAlias(String alias) throws IOException;
	
	/**
	 * checks if this database contains only a single role
	 * @return true if this database contains a single role, false otherwise
	 */
	public boolean isSingleRole();
	
	/**
	 * used as shorthand for a single-role database to obtain the name of 
	 * the role it contains
	 * @return the name of the role in the database, or null if more than one
	 * role exists
	 */
	public String getSingleRoleName();
	
	/**
	 * returns true if the database contains the given role name
	 * @param roleName the name of the role to check
	 * @return true if the given name exists in the database, false otherwise
	 */
	public boolean contains(String roleName);
	
	/**
	 * obtains the name of the role that the given alias is mapped to in the database
	 * @param alias the alias for the role
	 * @return the name of the role that the given alias is mapped to, or null if the
	 * given alias doesn't exist in the database
	 */
	public String getRoleNameByAlias(String alias);
	
	/**
	 * gets the name of the command this database is used for
	 * @return the name of the command this database is used for
	 */
	public String getName();
	
	/**
	 * a generated role command is used in a single guild. this retrieves the id of the
	 * guild that this database's corresponding command is used in
	 * @return the id of the guild this database's command is used in
	 */
	public String getId();
	
	public ImmutableList<String> getRoleNames();
	
	/**
	 * get a formatted human-readable string representing the command and its roles. intended
	 * for use with functions that eg give a summary of all available role commands, and 
	 * differs from toString() in that this provides nice human-readable information rather 
	 * than developer-focused debug information
	 * @return the nicely formatted human readable string representation of this command and 
	 * its roles and their aliases
	 */
	public String getFormattedString();
	
	/**
	 * saves all data to disk
	 * @throws IOException 
	 */
	public void save() throws IOException;
	
	/**
	 * deletes all data from disk and cleans up any necessary resources for gc
	 */
	public void delete() throws IOException;
	
}
