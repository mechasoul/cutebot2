package my.cute.bot.audio;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;

public class AudioPlayerReceiveHandler implements AudioReceiveHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(AudioPlayerReceiveHandler.class);
	private final Queue<byte[]> data = new ConcurrentLinkedQueue<>();
	
	@Override
	public boolean canReceiveCombined() {
		return true;
	}
	
	@Override
	public void handleCombinedAudio(CombinedAudio audio) {
		//TODO
		byte[] data = audio.getAudioData(1.0);
		byte[] zeroes = new byte[data.length];
//		logger.info(String.format("empty: %b, size: %d", Arrays.equals(data, zeroes), data.length));
	}
}
