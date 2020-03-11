package my.cute.bot.database;

import my.cute.markov2.MarkovDatabase;

class LineGenerator {
	
	String generateLine(MarkovDatabase db) {
		return db.generateLine();
	}
	
	String generateLine(MarkovDatabase db, String startWord) {
		return db.generateLine(startWord);
	}
}
