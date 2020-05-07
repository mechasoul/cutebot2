package my.cute.bot.commands;

import my.cute.bot.MyListener;
import net.dv8tion.jda.api.entities.Message;

class PrivateChannelExitCommand extends PrivateChannelCommand {

	private MyListener bot;
	
	PrivateChannelExitCommand(String name, MyListener bot) {
		super(name, false, PermissionLevel.DEVELOPER);
		this.bot = bot;
	}

	@Override
	public void execute(Message message) {
		message.getChannel().sendMessage("ok").queue(msg -> this.bot.shutdown(), ex -> this.bot.shutdown());
	}

}
