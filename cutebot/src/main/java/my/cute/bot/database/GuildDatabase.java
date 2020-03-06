package my.cute.bot.database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/*
 * chatbot database
 */
public interface GuildDatabase extends Maintainable {

	public boolean processLine(String line);
	
	public String generateLine();
	
	public String generateLine(String startWord);
	
	/*
	 * returns true if the line was successfully removed, and false if the entire
	 * line wasn't found in the database
	 */
	public boolean removeLine(String line);
	
	public void save();
	
	public void load();
	
	public void shutdown();
	
	public Path saveBackup(String backupName) throws IOException;
	
	public void loadBackup(String backupName) throws FileNotFoundException, IOException;
	
	public void deleteBackup(String backupName) throws IOException;
	
	public void clear() throws IOException;
	
	public void exportToText();
}
