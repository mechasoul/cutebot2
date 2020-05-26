package my.cute.bot.database;

import java.io.IOException;

import my.cute.markov2.MarkovDatabase;

class LineGenerator {
	
	String generateLine(MarkovDatabase db) throws IOException {
		return db.generateLine();
	}
	
	String generateLine(MarkovDatabase db, String startWord) throws IOException {
		return db.generateLine(startWord);
	}
}
