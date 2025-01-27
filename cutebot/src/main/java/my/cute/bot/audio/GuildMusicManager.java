package my.cute.bot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class GuildMusicManager {

	private final AudioPlayer player;
	private final TrackScheduler scheduler;
	private final long guildId;
	
	public GuildMusicManager(long guildId, AudioPlayerManager manager) {
		player = manager.createPlayer();
		this.guildId = guildId;
		scheduler = new TrackScheduler(guildId, player);
		player.addListener(scheduler);
	}
	
	public AudioPlayerSendHandler getSendHandler() {
		return new AudioPlayerSendHandler(player);
	}
	
	public TrackScheduler getScheduler() {
		return scheduler;
	}
}
