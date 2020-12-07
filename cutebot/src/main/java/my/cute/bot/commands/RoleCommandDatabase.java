package my.cute.bot.commands;

/**
 * holds data for a user-generated role command
 */
interface RoleCommandDatabase {

	/**
	 * adds a role to the database for this command. does nothing if the given 
	 * role name already exists in the database
	 * @param roleName the name of the role to add
	 * @param roleId the id of the role to add
	 * @return true if the role name and id were successfully added (ie, the 
	 * database was modified as a result of this call), false otherwise
	 */
	public boolean add(String roleName, long roleId);
	
	/**
	 * updates a role already existing in the database with a new role id. does
	 * nothing if the given role name does not exist in the database already
	 * @param roleName the name of the role to update
	 * @param roleId the new id of the role
	 * @return true if the role's id was successfully updated (ie, the database
	 * was modified as a result of this call), false otherwise
	 */
	public boolean update(String roleName, long roleId);
	
	/**
	 * removes a role from the database for this command. should also remove
	 * any aliases that map to the given role if they exist
	 * @param roleName the name of the role to remove
	 * @return true if the role was successfully removed (ie, the database 
	 * was modified as a result of this call), false otherwise
	 */
	public boolean remove(String roleName);
	
	/**
	 * adds an alias to the database. aliases can be used as shorthand for
	 * a long role name
	 * @param alias the alias to use for the given role
	 * @param roleName the name of the role being given an alias
	 * @return ?
	 */
	public boolean addAlias(String alias, String roleName);
	
	/**
	 * removes an alias for a role from the database for this command
	 * @param alias the alias to remove
	 * @return true if the alias was successfully removed (ie, the database 
	 * was modified as a result of this call), false otherwise
	 */
	public boolean removeAlias(String alias);
	
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
	 * used as shorthand for a single-role database to obtain the id of 
	 * the role it contains
	 * @return the id of the role in the database, or -1 if more than one
	 * role exists
	 */
	public long getSingleRoleId();
	
	/**
	 * obtains the id in the database for the given role name
	 * @param roleName the name of the role
	 * @return the id of the given role, or -1 if no role exists in the database
	 * with that name
	 */
	public long getRoleId(String roleName);
	
	/**
	 * obtains the name of the role that the given alias is mapped to in the database
	 * @param alias the alias for the role
	 * @return the name of the role that the given alias is mapped to, or null if the
	 * given alias doesn't exist in the database
	 */
	public String getRoleNameByAlias(String alias);
	
	/**
	 * obtains the id of the role that the given alias corresponds to
	 * @param alias the alias to fetch the role id for
	 * @return the id of the role with the name that the given alias maps to, or -1 if
	 * the alias doesn't map to a role in the database
	 */
	public long getRoleIdByAlias(String alias);
	
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
	
	/**
	 * saves all data to disk
	 */
	public void save();
	
	/**
	 * deletes all data from disk and cleans up any necessary resources for gc
	 */
	public void delete();
	
}
