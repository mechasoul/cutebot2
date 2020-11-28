package my.cute.bot.commands;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;

/**
 * command for managing user permissions. users have the authority to add and remove
 * users to their equivalent level of authority. this is slightly dangerous but idc
 * let people sort out their own admin permissions
 * <p>
 * <b>use</b>: !admin mode params
 * <p>
 * valid modes are:<br>
 * <b>add</b>: params should be a comma-separated list of users to add as admin for
 * the target server (adding one user is fine)<br>
 * <b>remove</b>: params should be a comma-separated list of users to remove as 
 * admin for the target server<br>
 * <b>view</b>: no params. displays a list of all users with admin rights for the 
 * specified server
 * <p>
 * note that some qualities of the PermissionDatabase are missing from this; we don't
 * specify permission level to give users (only admin), no ability to interact with the
 * global permission database, etc. this is because right now this is the only 
 * functionality required and i don't want to overcomplicate anything. global admins 
 * can be hardcoded in without any problems and i don't foresee myself adding anyone
 * else as global admin anyway
 * <p>
 * in the case that some of this functionality needs to be added it shouldnt be too hard
 * to eg add another parameter for specifying permission level to modify, etc
 */
class PrivateChannelAdminCommand extends PrivateChannelCommandTargeted {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelAdminCommand.class);
	final static String NAME = "admin";
	
	private final PermissionManager allPermissions;
	private final JDA jda;
	
	PrivateChannelAdminCommand(PermissionManager perms, JDA jda) {
		super(NAME, PermissionLevel.ADMIN, 1, 3);
		this.allPermissions = perms;
		this.jda = jda;
	}

	@Override
	public void execute(Message message, String[] params, String targetGuild) {
		try {
			if(params[1].equals("add")) {
				if(params.length >= 3) {
					//ensure given id is a member in target guild
					try {
						this.jda.getGuildById(targetGuild).retrieveMemberById(params[2], false).queue(member ->
						{
							try {
								boolean newAdmin = this.allPermissions.add(member.getId(), targetGuild, PermissionLevel.ADMIN);
								if(newAdmin) {
									message.getChannel().sendMessage("user " + MiscUtils.getUserString(member.getUser()) 
											+ " now has cutebot admin privileges in server " + MiscUtils.getGuildString(member.getGuild())).queue();
								} else {
									message.getChannel().sendMessage("user was not added as a cutebot admin in server " 
											+ MiscUtils.getGuildString(member.getGuild()) + " (already admin?)").queue();
								}
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
							
						}, 
						error -> message.getChannel().sendMessage(StandardMessages.invalidMember(params[2], targetGuild)).queue()
						);
					} catch (NumberFormatException e) {
						message.getChannel().sendMessage(StandardMessages.invalidMember(params[2], targetGuild)).queue();
					} catch (UncheckedIOException e) {
						throw e.getCause();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if(params[1].equals("remove")) {
				if(params.length >= 3) {
					
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if(params[1].equals("view")) {
				
			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} catch (IOException e) {
			logger.warn(this + ": unknown IOException during command execution", e);
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
		}
	}
	
	@Override
	public String toString() {
		return "PrivateChannelAdminCommand";
	}

}
