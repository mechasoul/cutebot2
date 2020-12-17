package my.cute.bot.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.MiscUtils;
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

	private final RoleCommandDatabase database;
	
	GeneratedTextChannelRoleCommand(String name, RoleCommandDatabase db, JDA jda, String id) {
		super(name, PermissionLevel.USER, 0, Integer.MAX_VALUE, jda, id);
		this.database = db;
	}
	
	@Override
	public void execute(Message message, String[] params) {
		
		//really shouldnt be possible for this to be null
		Guild guild = this.jda.getGuildById(this.guildId);
		long targetRoleId;
		String targetRoleName="";
		String specifiedRole = message.getContentRaw().trim().split("\\s+", 2)[1];
		
		if(this.database.isSingleRole()) {
			targetRoleId = this.database.getSingleRoleId();
			targetRoleName = this.database.getSingleRoleName();
		} else {
			//try input as role name, then alias
			targetRoleName = specifiedRole;
			targetRoleId = this.database.getRoleId(specifiedRole);
			if(targetRoleId == -1) {
				//no role with that name. try alias
				targetRoleName = this.database.getRoleNameByAlias(specifiedRole);
				targetRoleId = this.database.getRoleIdByAlias(specifiedRole);
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
					this.database.remove(targetRoleName);
					//TODO delete command or something if no roles left in db after removal
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
		Role role = MiscUtils.getRoleByName(guild, roleName);
		if(role != null) {
			this.database.update(roleName, role.getIdLong());
		}
		return role;
	}
	
	@Override
	public String toString() {
		return "GeneratedTextChannelRoleCommand-" + this.guildId + "-" + this.getName();
	}

}
