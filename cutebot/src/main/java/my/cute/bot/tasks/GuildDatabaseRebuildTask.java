package my.cute.bot.tasks;

import my.cute.bot.database.GuildDatabase;

public final class GuildDatabaseRebuildTask implements Runnable {
	
	private final GuildDatabase db;
	
	public GuildDatabaseRebuildTask(GuildDatabase db) {
		this.db = db;
	}

	@Override
	public void run() {
		synchronized(this.db) {
			
		}
	}

}
