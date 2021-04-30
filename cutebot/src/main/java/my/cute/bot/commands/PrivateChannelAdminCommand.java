package my.cute.bot.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.RestAction;

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

/*
 * TODO
 * remove block on removing server owner. permissions db already auto adds 
 * owner if it's empty after removal, so this is redundant
 * 
 * update to allow for comma-separated list. i specified it in the docs 
 * above but the command doesnt actually check for it?
 * 
 * update getUserById in view to use retrieveUser / retrieveMember
 * need to probably change most uses of get to retrieve...
 */
class PrivateChannelAdminCommand extends PrivateChannelCommandTargeted {
	
	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelAdminCommand.class);
	final static String NAME = "admin";
	private final static String DESCRIPTION = "modify and view which users have permission to use admin-restricted commands";
	
	private final PermissionManager allPermissions;
	
	PrivateChannelAdminCommand(PermissionManager perms) {
		super(NAME, DESCRIPTION, PermissionLevel.ADMIN, 1, 3);
		this.allPermissions = perms;
	}

	@Override
	public void execute(Message message, String[] params, Guild targetGuild) {
		try {
			if(params[1].equalsIgnoreCase("add")) {
				if(params.length >= 3) {
					try {
						
						/*
						 * use retrieveMemberById() since it updates cache
						 * requires guild members gateway intent
						 */
						targetGuild.retrieveMemberById(params[2], false).queue(member ->
						{
							try {
								boolean newAdmin = this.allPermissions.add(member.getUser(), targetGuild, PermissionLevel.ADMIN);
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
			} else if(params[1].equalsIgnoreCase("remove")) {
				if(params.length >= 3) {
					try {
						targetGuild.retrieveMemberById(params[2], false).queue(member -> 
						{
							try {
								boolean removedAdmin = this.allPermissions.remove(member.getUser(), targetGuild, PermissionLevel.ADMIN);
								if(removedAdmin) {
									message.getChannel().sendMessage("removed admin privileges from user " + MiscUtils.getUserString(member.getUser()) 
									+ " in server " + MiscUtils.getGuildString(member.getGuild())).queue();
								} else {
									message.getChannel().sendMessage("unable to remove admin privileges from user " 
											+ MiscUtils.getUserString(member.getUser()) + " in server "
											+ MiscUtils.getGuildString(member.getGuild()) + " (not an admin?)").queue();
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
			} else if(params[1].equalsIgnoreCase("view")) {
				this.sendFormattedAdminListMessages(targetGuild, message.getChannel());
			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} catch (IOException e) {
			logger.warn(this + ": unknown IOException during command execution! msg: " + message, e);
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
		}
	}
	
	private void sendFormattedAdminListMessages(Guild targetGuild, MessageChannel targetChannel) throws IOException {
		MessageBuilder builder = new MessageBuilder();
		builder.append("admin list for server " + MiscUtils.getGuildString(targetGuild));
		builder.append(System.lineSeparator());
		try {
			RestAction.allOf(this.allPermissions.getAdmins(targetGuild.getId()).stream()
					.map(userId -> targetGuild.retrieveMemberById(userId).onErrorMap(throwable -> {
						//retrieve member failed; invalid member. remove them as admin
						try {
							this.allPermissions.remove(userId, targetGuild.getIdLong(), PermissionLevel.ADMIN);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
						return null;
					}))
					.collect(Collectors.toList()))
					.queue(admins -> {
						admins.forEach(admin -> {
							if(admin != null) {
								builder.append(System.lineSeparator());
								builder.append(MiscUtils.getUserString(admin.getUser()));
							}
						});
						MiscUtils.sendMessages(targetChannel, builder.buildAll());
					}, error -> {
						targetChannel.sendMessage(StandardMessages.unknownError()).queue();
					});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		
//		action.queue(admins -> {
//			admins.forEach(admin -> {
//				if(admin != null) 
//					builder.append(MiscUtils.getUserString(admin));
//				else
//					builder.append("unknown user");
//			});
//			MiscUtils.sendMessages(targetChannel, builder.buildAll());
//		}, error -> {
//			targetChannel.sendMessage(StandardMessages.unknownError()).queue();
//		});
		
//		builder.append(this.allPermissions.getAdmins(targetGuild.getId()).stream().map(userId ->
//		{
//			this.jda.retrieveUserById(userId).
//			User user = this.jda.getUserById(userId);
//			if(user != null) {
//				return MiscUtils.getUserString(user);
//			} else {
//				return "unknown user id " + userId;
//			}
//		}).collect(Collectors.joining(System.lineSeparator())));
		
	}
	
	@Override
	public String toString() {
		return "PrivateChannelAdminCommand";
	}

}
