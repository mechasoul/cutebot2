package my.cute.bot.commands;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.gson.reflect.TypeToken;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import my.cute.bot.util.PathUtils;
import net.dv8tion.jda.api.entities.Role;

class RoleCommandDatabaseImpl implements RoleCommandDatabase {
	
	private final static int MAX_ROLES = 32;

	private final String commandName;
	private final String guildId;
	private final TObjectLongMap<String> roleNames;
	private final BiMap<String, String> aliases;
	private final Path path;
	
	RoleCommandDatabaseImpl(String guildId, String name, TObjectLongMap<String> roles, BiMap<String, String> aliases) {
		this.guildId = guildId;
		this.commandName = name;
		this.roleNames = new TObjectLongHashMap<>((roles.size()+1) * 4 / 3, 0.75f, -1);
		this.roleNames.putAll(roles);
		this.aliases = HashBiMap.create(aliases.size()+1);
		this.aliases.putAll(aliases);
		this.path = PathUtils.getGeneratedRoleCommandDatabase(this.guildId, this.commandName);
	}
	
	RoleCommandDatabaseImpl(String guildId, String name) {
		this.guildId = guildId;
		this.commandName = name;
		this.roleNames = new TObjectLongHashMap<>(3, 0.75f, -1);
		this.aliases = HashBiMap.create(3);
		this.path = PathUtils.getGeneratedRoleCommandDatabase(this.guildId, this.commandName);
	}
	
	//TODO add save() call to any methods that modify db (as specified in RoleCommandDatabase)
	
	
	@Override
	public synchronized boolean add(String roleName, long roleId) throws IOException {
		roleName = roleName.toLowerCase();
		if(this.roleNames.size() >= MAX_ROLES) return false;
		
		boolean newRoleAdded = this.roleNames.putIfAbsent(roleName, roleId) == -1;
		if(newRoleAdded) this.save();
		return newRoleAdded;
	}
	
	@Override
	public synchronized boolean add(Role role) throws IOException {
		return this.add(role.getName().toLowerCase(), role.getIdLong());
	}

	private synchronized boolean addWithoutSave(Role role) {
		if(this.roleNames.size() >= MAX_ROLES) return false;
		
		return this.roleNames.putIfAbsent(role.getName().toLowerCase(), role.getIdLong()) == -1;
	}

	@Override
	public synchronized ImmutableList<Role> add(List<Role> roles) throws IOException {
		ImmutableList.Builder<Role> addedRolesBuilder = ImmutableList.builderWithExpectedSize(roles.size());
		roles.forEach(role -> {
			if(this.addWithoutSave(role)) addedRolesBuilder.add(role);
		});
		ImmutableList<Role> addedRoles = addedRolesBuilder.build();
		if(!addedRoles.isEmpty()) this.save();
		return addedRoles;
	}
	
	@Override
	public synchronized boolean update(String roleName, long roleId) throws IOException {
		roleName = roleName.toLowerCase();
		
		if(!this.roleNames.containsKey(roleName)) return false;
		
		this.roleNames.put(roleName, roleId);
		this.save();
		return true;
	}

	@Override
	public synchronized boolean remove(String roleName) throws IOException {
		roleName = roleName.toLowerCase();
		
		if(this.roleNames.remove(roleName) != -1) {
			this.aliases.inverse().remove(roleName);
			this.save();
			return true;
		} else {
			return false;
		}
	}
	
	private synchronized boolean removeWithoutSave(String roleName) {
		roleName = roleName.toLowerCase();
		
		if(this.roleNames.remove(roleName) != -1) {
			this.aliases.inverse().remove(roleName);
			return true;
		} else {
			return false;
		}
	}
	
	private synchronized boolean removeWithoutSave(Role role) {
		return this.removeWithoutSave(role.getName());
	}
	
	@Override
	public synchronized boolean remove(Role role) throws IOException {
		return this.remove(role.getName());
	}
	
	@Override
	public ImmutableList<Role> remove(List<Role> roles) throws IOException {
		ImmutableList.Builder<Role> removedRolesBuilder = ImmutableList.builderWithExpectedSize(roles.size());
		roles.forEach(role -> {
			if(this.removeWithoutSave(role)) removedRolesBuilder.add(role);
		});
		
		ImmutableList<Role> removedRoles = removedRolesBuilder.build();
		if(!removedRoles.isEmpty()) this.save();
		return removedRoles;
	}
	
	@Override
	public ImmutableList<String> removeByName(String... roleNames) throws IOException {
		ImmutableList.Builder<String> removedRoleNamesBuilder = ImmutableList.builderWithExpectedSize(roleNames.length);
		Stream.of(roleNames).forEach(roleName -> {
			if(this.removeWithoutSave(roleName)) removedRoleNamesBuilder.add(roleName);
		});
		
		ImmutableList<String> removedRoleNames = removedRoleNamesBuilder.build();
		if(!removedRoleNames.isEmpty()) this.save();
		return removedRoleNames;
	}

	@Override
	public boolean addAlias(String alias, Role role) throws IOException {
		alias = alias.toLowerCase();
		
		if(!this.roleNames.containsKey(role.getName().toLowerCase())) {
			return false;
		} else {
			this.aliases.forcePut(alias, role.getName().toLowerCase());
			this.save();
			return true;
		}
	}

	@Override
	public boolean removeAlias(String alias) throws IOException {
		alias = alias.toLowerCase();
		
		boolean successfullyRemoved = this.aliases.remove(alias) != null;
		if(successfullyRemoved) this.save();
		return successfullyRemoved;
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
	public ImmutableList<String> getRoleNames() {
		return ImmutableList.copyOf(this.roleNames.keySet());
	}
	
	@Override
	public long[] getRoleIds() {
		return this.roleNames.values();
	}
	
	@Override
	public String getFormattedString() {
		//format: command name - 'role 1' (alias 1), 'role 2' (alias 2), ...
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		sb.append(" - ");
		sb.append(this.roleNames.keySet().stream().map(roleName -> {
			StringBuilder role = new StringBuilder();
			role.append("'");
			role.append(roleName);
			role.append("'");
			String roleAlias = this.aliases.inverse().get(roleName);
			if(roleAlias != null) {
				role.append(" (");
				role.append(roleAlias);
				role.append(")");
			}
			return roleAlias.toString();
		}).collect(Collectors.joining(", ")));
		return sb.toString();
	}

	@Override
	public synchronized void save() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8)) {
			RoleCommandDatabaseFactory.GSON.toJson(this.roleNames, new TypeToken<TObjectLongMap<String>>(){}.getType(), writer);
			writer.newLine();
			RoleCommandDatabaseFactory.GSON.toJson(this.aliases, new TypeToken<BiMap<String, String>>(){}.getType(), writer);
		}
	}

	@Override
	public void delete() throws IOException {
		Files.deleteIfExists(path);
	}

}
