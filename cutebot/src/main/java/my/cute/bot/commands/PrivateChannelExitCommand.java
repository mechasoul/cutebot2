package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

class PrivateChannelExitCommand extends PrivateChannelCommand {

	final static String NAME = "exit";
	private final static String DESCRIPTION = "shut down cutebot";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("shut down cutebot")
			.addField("type `!exit` and i shut down", "wow", false));
	
	private final MyListener bot;
	
	PrivateChannelExitCommand(MyListener bot) {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.DEVELOPER, 0, 0);
		this.bot = bot;
	}

	@Override
	public void execute(Message message, String[] params) {
		message.getChannel().sendMessage("ok").queue(msg -> this.bot.shutdown(), ex -> this.bot.shutdown());
	}

}
