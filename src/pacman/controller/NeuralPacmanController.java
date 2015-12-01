package pacman.controller;

import com.anji.integration.Activator;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public abstract class NeuralPacmanController extends Controller<MOVE> {

	protected Activator currentNetwork;
	//protected Game game;
	private final static int MAX_DISTANCE = 255;
	private final static int MAX_EDIBLE_TIME = 200;
	
	@Override
	public abstract MOVE getMove(Game game, long timeDue);
	
	protected double scaleBoolean(boolean b){
		return b == true ? 1d : 0d;
	}
	
	protected double scaleDistance(double d){
		return d / MAX_DISTANCE;
	}
	
	protected double scaleEdibleTime(double t){
		return t / MAX_EDIBLE_TIME;
	}
	
	protected int getMaxDistance(){
		return MAX_DISTANCE;
	}
}
