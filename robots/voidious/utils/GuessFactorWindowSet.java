package voidious.utils;

public class GuessFactorWindowSet {
	public double guessFactor;
	public double guessFactorLow;
	public double guessFactorHigh;
	
	public GuessFactorWindowSet(double gfMain, double gfLow, double gfHigh) {
		guessFactor = gfMain;
		guessFactorLow = gfLow;
		guessFactorHigh = gfHigh;
	}
	
	public double guessFactorRange() {
		return Math.abs(guessFactorHigh - guessFactorLow);		
	}
}
