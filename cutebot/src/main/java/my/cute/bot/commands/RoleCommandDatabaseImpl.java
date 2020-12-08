package my.cute.bot.commands;

import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import net.dv8tion.jda.api.entities.Role;

class RoleCommandDatabaseImpl implements RoleCommandDatabase {
	
	private final static int MAX_ROLES = 30;

	private final String commandName;
	private final String guildId;
	private final TObjectLongMap<String> roleNames;
	private final BiMap<String, String> aliases;
	
	RoleCommandDatabaseImpl(String name, String guildId, TObjectLongMap<String> roles, BiMap<String, String> aliases) {
		this.commandName = name;
		this.guildId = guildId;
		this.roleNames = new TObjectLongHashMap<>(roles.size() * 4 / 3, 0.75f, -1);
		this.roleNames.putAll(roles);
		this.aliases = aliases;
	}
	
	
	@Override
	public synchronized boolean add(String roleName, long roleId) {
		roleName = roleName.toLowerCase();
		if(this.roleNames.size() >= MAX_ROLES) return false;
		
		return this.roleNames.putIfAbsent(roleName, roleId) == -1;
	}
	
	@Override
	public synchronized boolean add(Role role) {
		return this.add(role.getName().toLowerCase(), role.getIdLong());
	}


	@Override
	public synchronized ImmutableList<Role> add(List<Role> roles) {
		ImmutableList.Builder<Role> addedRoles = ImmutableList.builderWithExpectedSize(roles.size());
		roles.forEach(role -> {
			if(this.add(role)) addedRoles.add(role);
		});
		return addedRoles.build();
	}
	
	@Override
	public synchronized boolean update(String roleName, long roleId) {
		roleName = roleName.toLowerCase();
		
		if(!this.roleNames.containsKey(roleName)) return false;
		this.roleNames.put(roleName, roleId);
		return true;
	}

	@Override
	public synchronized boolean remove(String roleName) {
		roleName = roleName.toLowerCase();
		
		boolean existingRole = this.roleNames.remove(roleName) != -1;
		this.aliases.inverse().remove(roleName);
		return existingRole;
	}

	@Override
	public boolean addAlias(String alias, String roleName) {
		roleName = roleName.toLowerCase();
		alias = alias.toLowerCase();
		
		if(!this.roleNames.containsKey(roleName)) {
			return false;
		} else {
			this.aliases.forcePut(alias, roleName);
			return true;
		}
	}

	@Override
	public boolean removeAlias(String alias) {
		alias = alias.toLowerCase();
		
		return this.aliases.remove(alias) != null;
	}

	@Override
	public synchronized boolean isSingleRole() {
		return this.roleNames.size() == 1;
	}

	@Override
	public synchronized String getSingleRoleName() {
		if(this.isSingleRole()) {
			return this.roleNames.keys(new String[1])[0];
		} else {
			return null;
		}
	}

	@Override
	public synchronized long getSingleRoleId() {
		if(this.isSingleRole()) {
			return this.roleNames.values()[0];
		} else {
			return -1;
		}
	}

	@Override
	public synchronized long getRoleId(String roleName) {
		roleName = roleName.toLowerCase();
		
		return this.roleNames.get(roleName);
	}

	@Override
	public String getRoleNameByAlias(String alias) {
		alias = alias.toLowerCase();
		
		return this.aliases.get(alias);
	}
	
	@Override
	public synchronized long getRoleIdByAlias(String alias) {
		alias = alias.toLowerCase();
		
		String roleName = this.aliases.get(alias);
		return (roleName == null ? -1 : this.roleNames.get(roleName));
	}

	@Override
	public String getName() {
		return this.commandName;
	}

	@Override
	public String getId() {
		return this.guildId;
	}

	@Override
	public synchronized void save() {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}
}
