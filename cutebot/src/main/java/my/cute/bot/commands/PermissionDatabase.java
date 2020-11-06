package my.cute.bot.commands;

public interface PermissionDatabase {

	/*
	 * gives the specified user id the specified permission level
	 * after calling this, hasPermission(userId, permission) should return true
	 * returns true if a permission level was newly added for a user as a result
	 * of this call, and false otherwise (eg userId invalid, they already have
	 * the given permission, etc)
	 */
	public boolean add(long userId, PermissionLevel permission);
	
	/*
	 * removes the specified permission level from the specified user id
	 * after calling this, hasPermission(userId, permission) should return false
	 * returns true if a permission level was removed as a result of this call,
	 * and false otherwise (eg the given user doesn't have the given permission)
	 */
	public boolean remove(long userId, PermissionLevel permission);
	
	/*
	 * checks to see if the given user has the given permission
	 * returns true if an entry exists in the database for the specified user and
	 * permission, and false otherwise
	 */
	public boolean hasPermission(long userId, PermissionLevel permission);
	
}
