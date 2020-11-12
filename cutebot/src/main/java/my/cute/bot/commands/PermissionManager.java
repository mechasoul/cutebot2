package my.cute.bot.commands;

import java.io.IOException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public interface PermissionManager {

	/*
	 * top-level permissions management for cutebot commands
	 * an instance of this class should be used for all permissions checking
	 * not sure if it's correct to declare IOException in all these methods
	 * see PermissionDatabase for more on that because i already wrote it all
	 * there
	 * i guess its either this or have a save() that throws IOException or 
	 * something
	 */
	
	//TODO add methods for adding/removing permission databases for guild join/leave
	
	/*
	 * gives the specified user id the specified permission level, globally
	 * after calling this, hasPermission(userId, permission) should return true
	 * returns true if a permission level was newly added for a user as a result
	 * of this call, and false otherwise (eg userId invalid, they already have
	 * the given permission, etc)
	 */
	public boolean add(String userId, PermissionLevel permission) throws IOException;
	
	public boolean add(User user, PermissionLevel permission) throws IOException;
	
	/*
	 * same as above, but gives per-guild permissions
	 */
	public boolean add(String userId, String guildId, PermissionLevel permission) throws IOException;
	
	public boolean add(User user, Guild guild, PermissionLevel permission) throws IOException;
	
	/*
	 * removes the specified global permission level from the specified user id
	 * after calling this, hasPermission(userId, permission) should return false
	 * returns true if a permission level was removed as a result of this call,
	 * and false otherwise (eg the given user doesn't have the given permission)
	 */
	public boolean remove(String userId, PermissionLevel permission) throws IOException;
	
	public boolean remove(User user, PermissionLevel permission) throws IOException;
	
	/*
	 * same as above, but removes per-guild permissions
	 */
	public boolean remove(String userId, String guildId, PermissionLevel permission) throws IOException;
	
	public boolean remove(User user, Guild guild, PermissionLevel permission) throws IOException;
	
	/*
	 * checks to see if the given user has the given global permission
	 * returns true if an entry exists in the database for the specified user and
	 * permission, and false otherwise
	 */
	public boolean hasPermission(String userId, PermissionLevel permission);
	
	public boolean hasPermission(User user, PermissionLevel permission);
	
	/*
	 * same as above, but checks per-guild permissions
	 */
	public boolean hasPermission(String userId, String guildId, PermissionLevel permission);
	
	public boolean hasPermission(User user, Guild guild, PermissionLevel permission);
}
