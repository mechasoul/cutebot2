package my.cute.bot.commands;

import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelFilterCommand extends PrivateChannelCommandTargeted {

	PrivateChannelFilterCommand(String name, PermissionLevel permission, int min, int max) {
		super(name, permission, min, max);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void execute(Message message, String[] params, String targetGuild) {
		// TODO Auto-generated method stub

	}

}
