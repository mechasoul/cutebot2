package my.cute.bot.database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import my.cute.markov2.exceptions.ReadObjectException;

/*
 * chatbot database
 */
public interface GuildDatabase extends Maintainable {
	
	/*
	 * TODO add database.isShutdown checks to other methods besides
	 * processLine, generateLine, removeLine?
	 * and also add throws IllegalStateException declaration to methods
	 * that check isShutdown and throw it
	 */
	
	/**
	 * processes the given String into the database. once this method returns, the
	 * given String will be reflected in the database
	 * <p>
	 * note that some operations require a date be attached to processed lines (to
	 * track when they were added, eg in order to enforce maximum age of lines in 
	 * the database) - any lines processed via this method will use the current 
	 * date at time of processing
	 * @param line the line to process into the database
	 * @return true if the given line was processed into the database, false if not
	 * (eg empty line, some lind of error)
	 * @throws IllegalStateException if this method is called when the database has 
	 * been shut down
	 * @throws ReadObjectException if an IOException occurs while trying to read 
	 * part of the database from disk. this commonly occurs if part of the database
	 * has been corrupted (eg if a crash occurs while writing some part of the 
	 * database to disk)
	 * @throws IOException if a problem is encountered with i/o during any other
	 * part of the process
	 */
	public boolean processLine(String line) throws IllegalStateException, ReadObjectException, IOException;
	
	/*
	 * dont like this being in interface
	 */
	/**
	 * functions identically to {@link #processLine(String)}, except uses the explicitly provided
	 * date stamp as the date at which the line has been processed into the database. this is 
	 * useful if, for example, a database needs to be rebuilt - old lines can be processed into
	 * the new database while still preserving their actual age
	 * @param line the line to process
	 * @param dateStamp the date to use for when the line was processed into the database. format
	 * should be an 8-character string, YYYYMMDD, as in the format returned by 
	 * <code>LocalDate.format(DateTimeFormatter.BASIC_ISO_DATE)</code>
	 * @return true if the line was successfully processed into the database, false otherwise (eg 
	 * if the line was empty or an error occurred)
	 * @throws IllegalStateException if this method is called when the database has been shut down
	 * @throws ReadObjectException if an IOException occurs while trying to read 
	 * part of the database from disk. this commonly occurs if part of the database
	 * has been corrupted (eg if a crash occurs while writing some part of the 
	 * database to disk)
	 * @throws IOException if a problem is encountered with i/o during any other
	 * part of the process
	 */
	public boolean processLineWithDate(String line, String dateStamp) throws IllegalStateException, 
		ReadObjectException, IOException;
	
	/**
	 * generates a random line from the contents of the database. uses 
	 * a random weighted word to start the line, from all words used 
	 * to start lines
	 * @return a random line reflected by the contents of the database
	 * @throws IOException if an IOException occurs during database 
	 * loading
	 */
	public String generateLine() throws IOException;
	
	/**
	 * as in {@link #generateLine()}, but seeded with a given starting 
	 * word for the line. note problems may occur if the given word 
	 * has never actually been used as a starting word in the database
	 * @param startWord the word the generated line should start with 
	 * @return a random line reflected by the contents of the database,
	 * starting with the given word
	 * @throws IOException if an exception occurs during database 
	 * loading
	 */
	public String generateLine(String startWord) throws IOException;
	
	/*
	 * returns true if the line was successfully removed, and false if the entire
	 * line wasn't found in the database
	 */
	/**
	 * attempts to remove a line from the database. after this method returns, 
	 * the database will no longer reflect a single use of the given line. ie,
	 * this is basically the inverse of {@link #processLine(String)}. 
	 * <p>
	 * <b>note</b> this method should only be called with a line that has been 
	 * processed into the database in the past, ie 
	 * <code>processLine(line)</code> has been called some time prior to 
	 * <code>removeLine(line)</code>. if not, the database will end up in an
	 * inconsistent state and problems will be encountered with 
	 * {@link #generateLine()}!
	 * <p>
	 * eg, <code>db</code> in the following example will have the exact same 
	 * contents at the start and the end
	 * <pre>
	 * GuildDatabase db = getSomeDatabase();
	 * String line = getSomeLine();
	 * db.processLine(line);
	 * db.removeLine(line);
	 * </pre>
	 * 
	 * @param line the line to remove a single occurrence of from the database.
	 * note problems may (will) occur if the given line has never been processed
	 * into the database (ie, processLine(line) has never been called)
	 * @return true if the given line was successfully removed from the database,
	 * false if not
	 * @throws IOException if an IOException is encountered during the process
	 */
	public boolean removeLine(String line) throws IOException;
	
	/**
	 * saves the database to disk. note that parts of the database are constantly
	 * being updated on disk during general use, but calling this method will
	 * forcibly write all parts of the database to disk, so what's written
	 * on disk reflects the current database state once this method returns 
	 * (although it may very quickly be outdated)
	 * @throws IOException
	 */
	public void save() throws IOException;
	
	/*
	 * consider changing the way this works. i'm not sure it's necessary and 
	 * it very possibly causes more problems than it solves
	 */
	/**
	 * loads necessary parts of the database from disk that are required for it 
	 * to function. <b>note</b> that this should be called before any other
	 * method or the database will not function properly
	 * <p>
	 * this maybe seems weird but since the database can be large and loading 
	 * parts of it is a possibly time-consuming task, separating this from eg
	 * the constructor allows us to call this at a time when we're ready
	 * @throws IOException
	 */
	public void load() throws IOException;
	
	/**
	 * saves all data to disk and shuts the database down. once this method 
	 * returns, the database can no longer be used, and any attempts to modify 
	 * or use the database will result in an exception being thrown
	 * @throws IOException
	 */
	public void shutdown() throws IOException;
	
	/**
	 * checks whether the database has been marked to restore from backup.
	 * this provides a framework for code to detect if a problem has been
	 * encountered with the database and automatically recover from it
	 * @return true if the database has been marked to restore from backup,
	 * false if not
	 */
	public boolean getShouldRestoreFromBackup();
	
	/**
	 * sets whether the database is marked to restore from backup or not
	 * @param shouldRestore if true, the database is considered to need
	 * to be restored from backup, and {@link #getShouldRestoreFromBackup()}
	 * will return true. if false, the database is considered to not need
	 * to be restored from backup
	 */
	public void setShouldRestoreFromBackup(boolean shouldRestore);
	
	/*
	 * automatically attempt to restore database from backups, from newest to oldest,
	 * until either an operational version of the database is found, or all backups
	 * are exhausted
	 * 
	 * return true if a backup was successfully found, false if not
	 */
	/**
	 * attempts to restore the database state from all automatically saved
	 * backups, from newest to oldest. each backup is used to overwrite the
	 * current database and then tested for validity, halting once validity
	 * checks pass after restoring some backup, or all backups are exhausted
	 * @return true if some backup was successfully loaded and passed 
	 * validity checks, false if not (ie, the database is still in a
	 * compromised state)
	 */
	public boolean restoreFromAutomaticBackups();
	
	/**
	 * saves a backup of the database to the local backups folder on disk, 
	 * filename specified by the given String
	 * @param backupName the filename of the backup to be written to disk. 
	 * no directory or file extension is necessary
	 * @return a Path representing the created backup file
	 * @throws IOException
	 */
	public Path saveBackup(String backupName) throws IOException;
	
	/**
	 * loads the specified backup from disk, overwriting the database's 
	 * current contents with those from the backup
	 * @param backupName the filename of the backup to be loaded. no file
	 * extension required, a backup with the given filename will be looked
	 * for in the database's backups directory
	 * @throws FileNotFoundException if no backup with the given name could
	 * be found
	 * @throws IOException
	 */
	public void loadBackup(String backupName) throws FileNotFoundException, IOException;
	
	/**
	 * attempts to delete a backup with the given name from the local 
	 * backups directory
	 * @param backupName the name of the backup to be deleted
	 * @throws IOException
	 */
	public void deleteBackup(String backupName) throws IOException;
	
	/**
	 * deletes all automatically saved backups from local disk. useful if eg
	 * a database rebuild has been performed, and the old backups should no
	 * longer be loaded over the current database
	 * @throws IOException
	 */
	public void clearAutomaticBackups() throws IOException;
	
	/**
	 * clears all contents from the database and deletes all local database
	 * content from disk. once this method returns, the database will be empty. 
	 * note that this does <b>not</b> delete backups!
	 * @throws IOException
	 */
	public void clear() throws IOException;
	
	/**
	 * restructures the database object so that speed is a priority - bulk 
	 * operations can be executed much, much faster this way, but the database
	 * will also require significantly more memory!
	 * @throws IOException
	 */
	public void prioritizeSpeed() throws IOException;
	
	/**
	 * restructures the database object so that memory is a priority - bulk
	 * operations could take some time, but the memory footprint of the 
	 * database will be relatively small. good for long-term use
	 * <p>
	 * this is the default database mode
	 * @throws IOException
	 */
	public void prioritizeMemory() throws IOException;
	
	/**
	 * exports ALL contents of the database to a local text file, in a human-
	 * readable format. may take some time if the database is very large
	 */
	public void exportToText();
}
