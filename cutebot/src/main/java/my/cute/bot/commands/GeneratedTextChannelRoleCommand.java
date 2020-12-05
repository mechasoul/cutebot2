package my.cute.bot.commands;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.TObjectLongMap;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;

class GeneratedTextChannelRoleCommand extends TextChannelCommand {
	
	private final static Logger logger = LoggerFactory.getLogger(GeneratedTextChannelRoleCommand.class);

	private final TObjectLongMap<String> roleNames;
	private final Map<String, String> aliases;
	GeneratedTextChannelRoleCommand(String name, TObjectLongMap<String> roleIds, JDA jda, String id) {
		super(name, PermissionLevel.USER, 0, Integer.MAX_VALUE, jda, id);
		this.roleNames = roleIds;
		this.aliases = new ConcurrentHashMap<String, String>(5, 0.75f);
	}
	
	@Override
	public void execute(Message message, String[] params) {
		
		//really shouldnt be possible for this to be null
		Guild guild = this.jda.getGuildById(this.guildId);
		long targetRoleId;
		String targetRoleName="";
		String specifiedRole = message.getContentRaw().trim().split("\\s+", 2)[1];
		
		if(this.roleNames.size() == 1) {
			targetRoleId = this.roleNames.values(new long[1])[0];
			targetRoleName = this.roleNames.keySet().toArray(new String[1])[0];
		} else {
			//try input as role name, then alias
			targetRoleName = specifiedRole;
			targetRoleId = this.roleNames.get(specifiedRole);
			if(targetRoleId == -1) {
				//no role with that name. try alias
				targetRoleName = this.aliases.get(specifiedRole);
				targetRoleId = (targetRoleName == null ? -1 : this.roleNames.get(targetRoleName));
			}
		}
		
		if(targetRoleId != -1) {
			Role role = guild.getRoleById(targetRoleId);
			if(role != null) {
				this.applyRole(guild, role, message);
			} else {
				/*
				 * role id that exists in command no longer exists in server
				 * check to see if there's a different role with the same name,
				 * and if not then remove the role
				 */
				role = this.updateRole(guild, targetRoleName);
				if(role != null) {
					//successfully updated the role in internal map. apply it
					this.applyRole(guild, role, message);
				} else {
					this.removeRole(targetRoleName);
					message.getChannel().sendMessage(StandardMessages.invalidRole(message.getAuthor(), specifiedRole)).queue();
				}
			}
		} else {
			message.getChannel().sendMessage(StandardMessages.invalidRole(message.getAuthor(), specifiedRole)).queue();
		}
	}
	
	private void removeRoleFromMember(Guild guild, Member member, Role role, MessageChannel context) {
		guild.removeRoleFromMember(member, role).queue(success -> {
			context.sendMessage(member.getAsMention()
					+ " successfully removed role `" + role.getName() + "`").queue();
		}, new ErrorHandler()
			.ignore(ErrorResponse.UNKNOWN_MEMBER, ErrorResponse.UNKNOWN_ROLE)
			.handle(ErrorResponse.MISSING_PERMISSIONS, ex -> 
			context.sendMessage(StandardMessages.missingPermissionsToModifyRole(member.getUser())).queue())
		);
	}
	
	private void addRoleToMember(Guild guild, Member member, Role role, MessageChannel messageChannel) {
		guild.addRoleToMember(member, role).queue(success -> {
			messageChannel.sendMessage(member.getAsMention()
					+ " successfully added role `" + role.getName() + "`").queue();
		}, new ErrorHandler()
			.ignore(ErrorResponse.UNKNOWN_MEMBER, ErrorResponse.UNKNOWN_ROLE)
			.handle(ErrorResponse.MISSING_PERMISSIONS, ex -> 
			messageChannel.sendMessage(StandardMessages.missingPermissionsToModifyRole(member.getUser())).queue())
		);
	}
	
	private void applyRole(Guild guild, Role role, Message context) {
		guild.retrieveMember(context.getAuthor(), false).queue(member -> {
			try {
				if(member.getRoles().contains(role)) {
					this.removeRoleFromMember(guild, member, role, context.getChannel());
				} else {
					this.addRoleToMember(guild, member, role, context.getChannel());
				}
			} catch (InsufficientPermissionException | HierarchyException e) {
				context.getChannel().sendMessage(StandardMessages.missingPermissionsToModifyRole(context.getAuthor())).queue();
			} catch (Exception e) {
				logger.warn(this + ": unknown exception when trying to modify roles! msg: " + context, e);
				context.getChannel().sendMessage(StandardMessages.unknownError()).queue();
			}
		});
	}
	
	Role updateRole(Guild guild, String roleName) {
		List<Role> roles = guild.getRolesByName(roleName, false);
		if(roles.size() >= 1) {
			this.roleNames.put(roleName, roles.get(0).getIdLong());
			return roles.get(0);
		} else {
			return null;
		}
	}
	
	Role updateRole(Guild guild, long roleId) {
		Role role = guild.getRoleById(roleId);
		if(role != null) {
			this.roleNames.put(role.getName(), role.getIdLong());
			return role;
		} else {
			return null;
		}
	}
	
	boolean updateAlias(String alias, String roleName) {
		if(this.roleNames.containsKey(roleName)) {
			this.aliases.put(alias, roleName);
			return true;
		} else {
			return false;
		}
	}
	
	//TODO need to do something if no roles exist in command after this
	void removeRole(String roleName) {
		this.roleNames.retainEntries((name, id) -> !roleName.equals(name));
		while (this.aliases.values().remove(roleName));
	}
	
	@Override
	public String toString() {
		return "GeneratedTextChannelRoleCommand-" + this.guildId + "-" + this.getName();
	}

}
