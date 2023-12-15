package my.cute.bot.commands;

import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class PrivateChannelChannelCommand extends PrivateChannelCommand {

	final static String NAME = "channel";
	private final static String DESCRIPTION = "view the name of a given channel ID";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("view the name of a given channel ID. in order for this command to work, the given channel"
					+ " must be in a server that cutebot is a member of")
			.addField("use:", "`!channel <options>`", false)
			.addField("options", "`<options>` should be a single channel ID", false)
			.addField("examples", "`!channel 1111111111`"
					+ System.lineSeparator()
					+ "view the name of the discord channel with ID `1111111111`", false));
	
	public PrivateChannelChannelCommand() {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.DEVELOPER, 1, 1);
	}
	
	@Override
	public void execute(Message message, String[] params) {
		try {
			TextChannel channel = message.getJDA().getTextChannelById(params[1]);
			message.getChannel().sendMessage(channel != null ? channel.toString() : "no channel found with id `" 
					+ params[1] + "`").queue();
		} catch (NumberFormatException e) {
			message.getChannel().sendMessage("error parsing channel id `" + params[1] + "`").queue();
		}
	}

}
