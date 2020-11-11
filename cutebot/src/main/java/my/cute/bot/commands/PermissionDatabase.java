package my.cute.bot.commands;

import java.io.IOException;

public interface PermissionDatabase {

	/*
	 * a database for permissions checking for a specific set
	 * each database should refer to a specified id
	 */
	
	/*
	 * gives the specified user id the specified permission level
	 * after calling this, hasPermission(userId, permission) should return true
	 * returns true if a permission level was newly added for a user as a result
	 * of this call, and false otherwise (eg userId invalid, they already have
	 * the given permission, etc)
	 * 
	 * throws IOException if an IOException occurred while updating database file
	 * 
	 * maybe not correct to have this throw ioexception? possible that implementations
	 * might not update the on-disk file after every operation, but then i guess we'd
	 * need to have a public save() method or something so it'd be a different enough
	 * design that this is probably fine
	 */
	public boolean add(String userId, PermissionLevel permission) throws IOException;
	
	/*
	 * removes the specified permission level from the specified user id
	 * after calling this, hasPermission(userId, permission) should return false
	 * returns true if a permission level was removed as a result of this call,
	 * and false otherwise (eg the given user doesn't have the given permission)
	 * 
	 * throws IOException if an IOException occurred while updating database file
	 */
	public boolean remove(String userId, PermissionLevel permission) throws IOException;

	/*
	 * checks to see if the given user has the given permission
	 * returns true if an entry exists in the database for the specified user and
	 * permission, and false otherwise
	 * 
	 * should this throw IOException for consistency too? might want to keep permissions
	 * solely on-disk and check them every time its needed or something, in which case
	 * checking permission would also need to use disk
	 */
	public boolean hasPermission(String userId, PermissionLevel permission);
	
	/*
	 * return the string associated with this database
	 */
	public String getId();
	
}
