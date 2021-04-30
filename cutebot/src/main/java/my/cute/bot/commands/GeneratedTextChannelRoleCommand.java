package my.cute.bot.commands;

import java.io.IOException;

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
	
	private static final Logger logger = LoggerFactory.getLogger(GeneratedTextChannelRoleCommand.class);
	
	private static final String DESCRIPTION = "used for changing roles";

	private final RoleCommandDatabase database;
	private GuildCommandSet parent;
	
	GeneratedTextChannelRoleCommand(String name, RoleCommandDatabase db, JDA jda, String id, GuildCommandSet parent) {
		super(name, DESCRIPTION, PermissionLevel.USER, 0, Integer.MAX_VALUE, jda, id);
		this.database = db;
		this.parent = parent;
	}
	
	@Override
	public void execute(Message message, String[] params) {
		
		//really shouldnt be possible for this to be null
		Guild guild = this.jda.getGuildById(this.guildId);
		String targetRoleName=null;
		String specifiedRole = MiscUtils.getWords(message, 2)[1];
		
		if(this.database.isSingleRole()) {
			targetRoleName = this.database.getSingleRoleName();
		} else if(this.database.contains(specifiedRole)) {
			//multiple roles in db. first check input as role name
			targetRoleName = specifiedRole;
		} else {
			//no role found with given name. check input as alias
			targetRoleName = this.database.getRoleNameByAlias(specifiedRole);
		}
		
		if(targetRoleName != null) {
			Role role = MiscUtils.getRoleByName(guild, targetRoleName);
			if(role != null) {
				this.applyRole(guild, role, message);
			} else {
				/*
				 * role name that exists in command no longer exists in server
				 * remove role from command
				 */
				try {
					this.database.remove(role);
					if(this.database.isEmpty()) {
						//no roles left in command. delete command
						this.parent.deleteRoleCommand(this.getName());
						this.parent = null;
						message.getChannel().sendMessage(StandardMessages.unknownCommand(this.getName())).queue();
					} else {
						message.getChannel().sendMessage(StandardMessages.invalidRole(message.getAuthor(), specifiedRole)).queue();
					}
				} catch (IOException e) {
					//unknown error when trying to remove role
					logger.warn(this + ": ioexception when trying to remove role '" + role + "'", e);
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
	
	@Override
	public String toString() {
		return "GeneratedTextChannelRoleCommand-" + this.guildId + "-" + this.getName();
	}

}
