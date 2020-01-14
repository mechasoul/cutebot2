package my.cute.bot.database;

import java.util.concurrent.TimeUnit;

public class BackupRecord {
	
	

	/*
	 * durations will be checked against duration to determine if it's time for a new
	 * backup record to be created
	 */
	private final long duration;
	private final String name;
	private long prevTime;
	
	BackupRecord(String name, TimeUnit unit, int duration) {
		this.name = name;
		this.duration = unit.toMillis(duration);
		this.prevTime = System.currentTimeMillis();
	}
	
	boolean shouldSaveBackup(long time) {
		return (time - this.prevTime) >= this.duration;
	}
	
	void setPrevTime(long time) {
		this.prevTime = time;
	}
	
	String getName() {
		return this.name;
	}
}
