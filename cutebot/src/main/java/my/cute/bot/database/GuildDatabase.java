package my.cute.bot.database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/*
 * chatbot database
 */
public interface GuildDatabase extends Maintainable {

	/*
	 * returns true if the given line was processed into database
	 * false if not (eg empty line, some kind of error)
	 * 
	 * throws IllegalStateException if called when database has been shutdown
	 * throws IOException when an IOException occurs as part of line processing:
	 * 		specifically, throws my.cute.markov2.exceptions.ReadObjectException if
	 * 		an IOException occurs while trying to read a part of the database from
	 * 		disk (this likely indicates that the database on disk has been corrupted),
	 * 		and throws an IOException if an IOException occurs during any other part
	 * 		of the process
	 */
	public boolean processLine(String line) throws IllegalStateException, IOException;
	
	/*
	 * dont like this being in interface
	 * dateStamp should be 8-character YYYYMMDD, ie BASIC_ISO_DATE
	 * 
	 * see processLine(String)
	 * 
	 * returns true if the given line was processed into database
	 * false if not (eg empty line, some kind of error)
	 */
	public boolean processLineWithDate(String line, String dateStamp) throws IllegalStateException, IOException;
	
	public String generateLine() throws IOException;
	
	public String generateLine(String startWord) throws IOException;
	
	/*
	 * returns true if the line was successfully removed, and false if the entire
	 * line wasn't found in the database
	 */
	public boolean removeLine(String line) throws IOException;
	
	public void save() throws IOException;
	
	public void load() throws IOException;
	
	public void shutdown() throws IOException;
	
	/*
	 * automatically attempt to restore database from backups, from newest to oldest,
	 * until either an operational version of the database is found, or all backups
	 * are exhausted
	 * 
	 * return true if a backup was successfully found, false if not
	 */
	public boolean restoreFromAutomaticBackups();
	
	public Path saveBackup(String backupName) throws IOException;
	
	public void loadBackup(String backupName) throws FileNotFoundException, IOException;
	
	public void deleteBackup(String backupName) throws IOException;
	
	public void clearAutomaticBackups() throws IOException;
	
	public void clear() throws IOException;
	
	public void prioritizeSpeed() throws IOException;
	
	public void prioritizeMemory() throws IOException;
	
	public void exportToText();
}
