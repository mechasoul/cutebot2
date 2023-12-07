package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelMaintCommand extends PrivateChannelCommand {
	
	final static String NAME = "maint";
	private final static String DESCRIPTION = "force start maintenance on a specific server or all servers";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("immediately forces maintenance to start on a specific server or all servers")
			.addField("use:", "`!maint <options>`", false)
			.addField("options", "`<options>` should be a single server ID to perform maintenance on a single "
					+ "server, or the word `all` to perform maintenance on all servers", false)
			.addField("examples", "`!maint 11111111111`"
					+ System.lineSeparator()
					+ "start maintenance on server ID `11111111111`", false));
	
	private MyListener bot;

	PrivateChannelMaintCommand(MyListener bot) {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.DEVELOPER, 1, 1);
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
				message.getChannel().sendMessage("maintenance started on server `" + this.bot.getGuildString(params[1]) + "`").queue();
			}
		} catch (IllegalArgumentException ex) {
			message.getChannel().sendMessage("invalid server id `" + params[1] + "`").queue();
		}
		
	}

}
