package my.cute.bot.commands;

import my.cute.bot.MyListener;
import net.dv8tion.jda.api.entities.Message;

class PrivateChannelExitCommand extends PrivateChannelCommand {

	private MyListener bot;
	
	PrivateChannelExitCommand(MyListener bot) {
		super("exit", PermissionLevel.DEVELOPER, 0, 0);
		this.bot = bot;
	}

	@Override
	public void execute(Message message, String[] params) {
		message.getChannel().sendMessage("ok").queue(msg -> this.bot.shutdown(), ex -> this.bot.shutdown());
	}

}
