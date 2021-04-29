package my.cute.bot.commands;

import java.io.IOException;

import com.google.common.collect.ImmutableSet;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

/**
 * top-level permissions management for cutebot commands. a single instance
 * of cutebot will hold a bunch of permissions for different users across
 * different guilds, so an instance of this class should be used to
 * manage all of that
 * <p>
 * note that any PermissionManager methods that modify permissions can throw
 * IOException if one occurs while trying to modify permissions on disk.
 * not sure if it's correct to declare IOException in all these methods since
 * it seems like more of an implementation thing? see PermissionDatabase for 
 * more on that because i already wrote it all there<br>
 * i guess its either this or have a save() that throws IOException or 
 * something
 */
public interface PermissionManager {
	
	/**
	 * gives the specified user id the specified permission level, globally. 
	 * after calling this, calling {@link #hasPermission(String, PermissionLevel)}
	 * with (userId, permission) should return true
	 * @param userId the user id to change permissions for
	 * @param permission the permission level to grant that user
	 * @return true if a permission level was newly added for a user as a result
	 * of this call, and false otherwise (eg userId invalid, they already have
	 * the given permission, etc)
	 * @throws IOException
	 */
	public boolean add(String userId, PermissionLevel permission) throws IOException;
	
	public boolean add(User user, PermissionLevel permission) throws IOException;
	
	/**
	 * same as {@link #add(String, PermissionLevel)}, but gives per-guild permissions.
	 * after calling this, calling {@link #hasPermission(String, String, PermissionLevel)} 
	 * with (userId, guildId, permission) should return true
	 * @param userId the user id to change permissions for
	 * @param guildId the guild id to change permissions for that user in
	 * @param permission the permission level to grant that user
	 * @return true if a permission level was newly added for a user as a result
	 * of this call, and false otherwise (eg userId invalid, they already have
	 * the given permission, etc)
	 * @throws IOException
	 */
	public boolean add(String userId, String guildId, PermissionLevel permission) throws IOException;
	
	public boolean add(User user, Guild guild, PermissionLevel permission) throws IOException;
	
	/*
	 * removes the specified global permission level from the specified user id
	 * after calling this, hasPermission(userId, permission) should return false
	 * returns true if a permission level was removed as a result of this call,
	 * and false otherwise (eg the given user doesn't have the given permission)
	 */
	/**
	 * removes the specified global permission level from the specified user id
	 * after calling this, calling {@link #hasPermission(String, PermissionLevel)}
	 * with the same arguments should return false
	 * @param userId the id of the user
	 * @param permission the permission level to remove
	 * @return true if a permission level was removed as a result of this call, 
	 * false otherwise (eg if the given user doesn't have the given permission). or
	 * equivalently returns true if the manager changed as a result of this call
	 * @throws IOException
	 */
	public boolean remove(String userId, PermissionLevel permission) throws IOException;
	
	public boolean remove(User user, PermissionLevel permission) throws IOException;
	
	/**
	 * see {@link #remove(String, PermissionLevel)}, but modifies permissions for a 
	 * specific guild, rather than the global permissions
	 * <p>
	 * note that if the removed user was the last user with permissions in the given
	 * guild (ie, the given guild now has no admins), an attempt will be made to 
	 * automatically add the server owner as admin
	 * @param userId the id of the user
	 * @param guildId the id of the guild
	 * @param permission the permission level to remove from the user in the guild
	 * @return true if a permission level was removed as a result of this call, 
	 * false otherwise (eg if the given user doesn't have the given permission). or
	 * equivalently returns true if the manager changed as a result of this call
	 * @throws IOException
	 */
	public boolean remove(String userId, String guildId, PermissionLevel permission) throws IOException;
	
	public boolean remove(long userId, long guildId, PermissionLevel permission) throws IOException;
	
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
	
	/**
	 * adds a new guild to manage permissions for. used eg on guild join
	 * <p>
	 * note that if the list of permissions for the guild is empty (eg if this is
	 * a guild that has never been managed in the past), an attempt will be made
	 * to automatically add the guild owner as admin
	 * @param guildId the id of the newly managed guild
	 * @return true if the guild added was new (did not already exist in the manager),
	 * false otherwise (or, true if the manager was changed as a result of this call)
	 * @throws IOException
	 */
	public boolean addGuild(String guildId) throws IOException;
	
	public boolean addGuild(Guild guild) throws IOException;
	
	/*
	 * removes a guild from the manager. clean up files on disk?
	 * use eg on guild leave
	 * 
	 * returns true if a guild was removed from the manager as a result of this call,
	 * false otherwise (eg given guild id wasn't found in manager)
	 */
	public boolean removeGuild(String guildId) throws IOException;
	
	public boolean removeGuild(Guild guild) throws IOException;
	
	public PermissionDatabase getPermissionDatabase(String guildId);
	
	/**
	 * gets an immutable set view of all admins in a given server. note this makes a 
	 * copy of the backing data structure and so is O(n)
	 * @param guildId the id of a guild
	 * @return the set of all user ids with admin permissions in the given guild
	 * @throws IllegalArgumentException if the given guild id is not a valid guild id in
	 * this PermissionManager
	 */
	public ImmutableSet<Long> getAdmins(String guildId);
	
	public int getSize(String guildId);
}
