package cutebot;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

public class SimpleTest {

	public static void main(String[] args) {
		ZoneId id = ZoneId.of("America/Vancouver");
		LocalDateTime now = LocalDateTime.now();
		String time = now.format(DateTimeFormatter.BASIC_ISO_DATE);
		System.out.println(time);
		long nowM = System.currentTimeMillis();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		
//		now = ZonedDateTime.now(ZoneId.of("America/Vancouver"));
//		Duration duration = Duration.between(now2, now);
//		System.out.println(duration.toMillis());
//		System.out.println("greater than 0.5s? " + (duration.toMillis() > 500));
//		System.out.println("greater than 1hr? " + (duration.toHours() >= 1));
//		TimeUnit unit = TimeUnit.DAYS;
//		int dur = 3;
//		System.out.println(unit.toMillis(dur));
		
		Path p = Paths.get("hello" + File.separator + "you.txt");
		System.out.println(p);
		System.out.println(p.getParent());
		
		
	}

}
