package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelStatusCommand extends PrivateChannelCommand {

	final static String NAME = "status";
	private static final String DESCRIPTION = "change cutebot's status";
	
	public PrivateChannelStatusCommand() {
		super(NAME, DESCRIPTION, PermissionLevel.DEVELOPER, 0, Integer.MAX_VALUE);
	}
	
	/*
	 * sets the jda instance's activity to the given status
	 * commands should have the syntax
	 * !status [<status>]
	 * not including a status will reset the jda instance's current activity
	 * including a status will set the jda instance's status to whatever was given
	 * statuses can contain whitespace or whatever; everything after the initial 
	 * '!status ' will be used
	 * 
	 * also see Command.execute(Message) for general guarantees of execute(Message),
	 * preprocessing rules, etc
	 */
	@Override
	public void execute(Message message, String[] params) {
		if(params.length == 1) {
			message.getJDA().getPresence().setActivity(null);
			message.getChannel().sendMessage("resetting").queue();
		} else {
			String status = message.getContentDisplay().split("!status\\s+", 2)[1];
			message.getChannel().sendMessage("set status to '" + status + "'").queue();
			message.getJDA().getPresence().setActivity(Activity.playing(status));
		}
	}

}
