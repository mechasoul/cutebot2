package my.cute.bot.commands;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.audio.AudioHandler;
import my.cute.bot.util.MiscUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class PrivateChannelPlayCommand extends PrivateChannelCommand {
	
	private static final Logger logger = LoggerFactory.getLogger(PrivateChannelPlayCommand.class);
	static final String NAME = "play";
	private static final String DESCRIPTION = "play a song in a voice channel";
	private static final EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("begin playing a song in the voice channel you're currently in. requires you to be in a "
					+ "voice channel that cutebot can join")
			.addField("use:", "`!play <options>`", false)
			.addField("options", "`<options>` should be a youtube song id, or a link to a youtube song", false)
			.addField("examples", "`!play Va31JGXUmkg`"
					+ System.lineSeparator()
					+ "start playing the youtube video with id `Va31JGXUmkg` in your current voice channel", false));
	
	private final AudioHandler audioHandler;

	PrivateChannelPlayCommand(AudioHandler audioHandler) {
		super(NAME, DESCRIPTION, HELP, PermissionLevel.DEVELOPER, 1, 1);
		this.audioHandler = audioHandler;
	}

	@Override
	public void execute(Message message, String[] params) {
		User user = message.getAuthor();
		List<Guild> mutualGuilds = user.getMutualGuilds();
		VoiceChannel targetChannel = null;
		for(int i=0; i < mutualGuilds.size() && targetChannel == null; i++) {
			Guild guild = mutualGuilds.get(i);
			for(VoiceChannel channel : guild.getVoiceChannels()) {
				if(channel.getMembers().contains(guild.getMember(user))) {
					targetChannel = channel;
					break;
				}
			}
		}
		if(targetChannel == null) {
			logger.info("unable to find voice channel for play request from user " + user.toString());
			message.getChannel().sendMessage("error: unable to find voice channel to join. "
					+ "you must be in a visible voice channel to use this command").queue();
			return;
		}
		audioHandler.loadAndPlay(targetChannel, params[1]);
	}
}
