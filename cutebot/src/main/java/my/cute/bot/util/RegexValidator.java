package my.cute.bot.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LongSummaryStatistics;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.google.common.base.Stopwatch;

public class RegexValidator {

	//in ms
	private static final long TIMEOUT_THRESHOLD = 300;
	
	//return true if no line in the test lines takes at least TIMEOUT_THRESHOLD time
	//return false if at least one line does
	public static boolean regexTimeoutTest(Pattern pattern) throws IOException {
		Stopwatch stopwatch = Stopwatch.createUnstarted();
		try (Stream<String> lines = Files.lines(Paths.get("./testinput.txt"), StandardCharsets.UTF_8)) {
			LongStream times = lines.mapToLong(line -> {
				stopwatch.reset();
				stopwatch.start();
				pattern.matcher(line).find();
				stopwatch.stop();
				return stopwatch.elapsed(TimeUnit.MILLISECONDS);
			});
			LongSummaryStatistics stats = times.summaryStatistics();
			if(stats.getMax() >= TIMEOUT_THRESHOLD) {
				System.out.println("RegexTimeoutTest failure - processed " + stats.getCount() + " messages. max " 
						+ stats.getMax() + "ms, average " + stats.getAverage() + "ms");
				return false;
			}
			
			System.out.println("processed " + stats.getCount() + " messages, max " + stats.getMax() + "ms, avg " + stats.getAverage() + "ms");
		}
		
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./testlongmessage.txt"), StandardCharsets.UTF_8)) {
			String line = reader.readLine();
			stopwatch.reset();
			stopwatch.start();
			pattern.matcher(line).find();
			stopwatch.stop();
		}
		
		long longMessageTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		System.out.println(longMessageTime);
		if(longMessageTime >= TIMEOUT_THRESHOLD) {
			System.out.println("RegexTimeoutTest failure - long message took " + longMessageTime + "ms");
			return false;
		}
		return true;
	}
	
}
