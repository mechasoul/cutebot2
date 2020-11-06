package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelMaintCommand extends PrivateChannelCommand {
	
	private MyListener bot;

	PrivateChannelMaintCommand(MyListener bot) {
		super("maint", PermissionLevel.DEVELOPER, 1, 1, true);
		this.bot = bot;
	}

	@Override
	public void execute(Message message) {
		String[] words = MiscUtils.getWords(message);
		
		try {
			if(words[1].equals("all")) {
				this.bot.forceMaintenance();
				message.getChannel().sendMessage("maintenance started on all servers").queue();
			} else {
				this.bot.forceMaintenance(words[1]);
				message.getChannel().sendMessage("maintenance started on server " + this.bot.getGuildString(words[1])).queue();
			}
		} catch (IllegalArgumentException ex) {
			message.getChannel().sendMessage("invalid server id '" + words[1] + "'").queue();
		}
		
	}

}
