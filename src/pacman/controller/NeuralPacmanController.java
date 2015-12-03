package pacman.controller;

import com.anji.integration.Activator;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public abstract class NeuralPacmanController extends Controller<MOVE> {

	protected Activator currentNetwork;
	//protected Game game;
	private final static int MAX_DISTANCE = 255;
	//private final static int MAX_EDIBLE_TIME = 200;
	private static final double rangeCeiling = 1.0;
	private static final double rangeFloor = -1.0;
	
	@Override
	public abstract MOVE getMove(Game game, long timeDue);
	
	protected double scaleBoolean(boolean b){
		return b == true ? 1d : 0d;
	}
	
	protected double scaleDistance(double d){
		return scale(d, MAX_DISTANCE, 0d);
	}
	
	/*
	protected double scaleEdibleTime(double t){
		return scale(t, MAX_EDIBLE_TIME, 0);
	}
	*/
	
	protected int getMaxDistance(){
		return MAX_DISTANCE;
	}
	
	protected double scalePercentage(double p){
		if (p < 0 || p > 100) {
			System.out.print("");
		}
		return scale(p, 100d, 0d);
	}
	
	private double scale(double value, double max, double min) {
		if(!isWithinRange(value, min, max)) throw new IllegalStateException("Min max values does not match input. Should not happen, revise code.");
		double scaledBetweenOneAndZero = (value/(max - min));
		return ((rangeCeiling - rangeFloor) * scaledBetweenOneAndZero) + rangeFloor;
	}
	
	private boolean isWithinRange(double value, double min, double max) {
		return value <= max && value >= min;
	}
}
