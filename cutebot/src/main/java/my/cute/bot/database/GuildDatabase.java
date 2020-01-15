package my.cute.bot.database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;

/*
 * chatbot database
 */
public interface GuildDatabase {
	
	public static final ZoneId timezone = ZoneId.of("America/Vancouver");

	public void processLine(String line);
	
	public String generateLine();
	
	public String generateLine(String startWord);
	
	/*
	 * returns true if the line was successfully removed, and false if the entire
	 * line wasn't found in the database
	 */
	public boolean removeLine(String line);
	
	public void save();
	
	public void load();
	
	public Path saveBackup(String backupName) throws IOException;
	
	public void loadBackup(String backupName) throws FileNotFoundException, IOException;
	
	public void deleteBackup(String backupName) throws IOException;
	
	public void maintenance();
	
	public boolean needsMaintenance();
}
