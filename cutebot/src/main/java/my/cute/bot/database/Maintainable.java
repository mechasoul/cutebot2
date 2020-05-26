package my.cute.bot.database;

import java.io.IOException;

public interface Maintainable {

	public boolean needsMaintenance();
	
	//need a better name?
	public void markForMaintenance();
	
	public void maintenance() throws IOException;
	
}
