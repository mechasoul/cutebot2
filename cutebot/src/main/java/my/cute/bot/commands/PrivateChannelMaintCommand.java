package my.cute.bot.commands;

import my.cute.bot.MyListener;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelMaintCommand extends PrivateChannelCommand {
	
	final static String NAME = "maint";
	private MyListener bot;

	PrivateChannelMaintCommand(MyListener bot) {
		super(NAME, PermissionLevel.DEVELOPER, 1, 1);
		this.bot = bot;
	}

	@Override
	public void execute(Message message, String[] params) {
		try {
			if(params[1].equals("all")) {
				this.bot.forceMaintenance();
				message.getChannel().sendMessage("maintenance started on all servers").queue();
			} else {
				this.bot.forceMaintenance(params[1]);
				message.getChannel().sendMessage("maintenance started on server " + this.bot.getGuildString(params[1])).queue();
			}
		} catch (IllegalArgumentException ex) {
			message.getChannel().sendMessage("invalid server id '" + params[1] + "'").queue();
		}
		
	}

}
