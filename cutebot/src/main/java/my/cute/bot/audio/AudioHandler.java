package my.cute.bot.audio;

import java.util.HashMap;
import java.util.Map;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.lavalink.youtube.clients.TvHtml5Embedded;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.skeleton.Client;
import my.cute.bot.CutebotTask;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

public class AudioHandler {

	private final AudioPlayerManager playerManager;
	private final Map<Long, GuildMusicManager> musicManagers;
	
	@SuppressWarnings("deprecation")
	public AudioHandler() {
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager, YoutubeAudioSourceManager.class);
		AudioSourceManagers.registerLocalSource(playerManager);
		dev.lavalink.youtube.YoutubeAudioSourceManager ytSourceManager = new dev.lavalink.youtube.YoutubeAudioSourceManager(false, true, false, 
				new Client[] {new TvHtml5Embedded(), new Web()});
		ytSourceManager.useOauth2(CutebotTask.YOUTUBE_OAUTH_TOKEN, true);
		playerManager.registerSourceManager(ytSourceManager);
		musicManagers = new HashMap<>();
	}
	
	private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
		long id = guild.getIdLong();
		GuildMusicManager manager = musicManagers.computeIfAbsent(id, guildId -> new GuildMusicManager(guildId, playerManager));
		guild.getAudioManager().setSendingHandler(manager.getSendHandler());
		return manager;
	}
	
	public void loadAndPlay(VoiceChannel channel, String trackUrl) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				play(channel, musicManager, track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				AudioTrack firstTrack = playlist.getSelectedTrack();
				if(firstTrack == null) firstTrack = playlist.getTracks().getFirst();
				play(channel, musicManager, firstTrack);
			}

			@Override
			public void noMatches() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	private void play(VoiceChannel channel, GuildMusicManager musicManager, AudioTrack track) {
		AudioManager audioManager = channel.getGuild().getAudioManager();
		if(!audioManager.isConnected()) {
			audioManager.openAudioConnection(channel);
		}
		musicManager.getScheduler().queue(track);
	}
	
	public void shutdown() {
		playerManager.shutdown();
	}
}
