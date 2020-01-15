package my.cute.bot.database;

import my.cute.markov2.MarkovDatabase;

class LineGenerator {

	protected final MarkovDatabase db;
	
	LineGenerator(MarkovDatabase db) {
		this.db = db;
	}
	
	String generateLine() {
		return this.db.generateLine();
	}
	
	String generateLine(String startWord) {
		return this.db.generateLine(startWord);
	}
}
