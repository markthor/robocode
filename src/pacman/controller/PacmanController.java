package pacman.controller;

import java.util.ArrayList;
import java.util.List;

import com.anji.integration.Activator;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class PacmanController extends Controller<MOVE> {

	private Activator neuralNetwork; 
	
	@Override
	public MOVE getMove(Game game, long timeDue) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/***
	 * Evaluates each possible move and returns the move with the highest output from the ANN
	 */
	protected MOVE getBestMove(Game game) {
		int[] neighbours = game.getNeighbouringNodes(game.getPacmanCurrentNodeIndex());
		
		int highestNeighbour = -1;
		double highestNeighbourScore = -1.0;
		for(int i = 0; i < neighbours.length; i++) {
			double evaluation = evaluateNode(neighbours[i], game);
			if(evaluateNode(neighbours[i], game) > highestNeighbourScore) {
				highestNeighbourScore = evaluation;
				highestNeighbour = neighbours[i];
			}
		}
		
		// By now a highest node should have been found
		if(highestNeighbour == -1) {
			throw new IllegalStateException("Should not happen, revise code");
		}
		
		return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), highestNeighbour, DM.MANHATTAN);
	}
	
	/**
	 * Returns the output of the neural network based on the inputs of the provided node.
	 */
	protected double evaluateNode(int node, Game game) {
		return neuralNetwork.next(getInputFromGameStateAndNode(node, game));
	}
	
	/**
	 * Calculates the inputs to the network and scale them.
	 */
	@Override
	protected List<Double> getInputFromGameStateAndNode(int node, Game game) { 
		List<Double> input = new ArrayList<Double>();
		input.add(scaleDistance(getDistanceToGhostFromNode(GHOST.BLINKY, game, node)));
		input.add(scaleDistance(getDistanceToGhostFromNode(GHOST.INKY, game, node)));
		input.add(scaleDistance(getDistanceToGhostFromNode(GHOST.PINKY, game, node)));
		input.add(scaleDistance(getDistanceToGhostFromNode(GHOST.SUE, game, node)));
		input.add(isGhostEdible(GHOST.BLINKY, game));
		input.add(isGhostEdible(GHOST.INKY, game));
		input.add(isGhostEdible(GHOST.PINKY, game));
		input.add(isGhostEdible(GHOST.SUE, game));
		input.add(scaleDistance(getDistanceToNearestPowerPill(node, game)));
		input.add(scaleDistance(getDistanceToNearestPill(node, game)));
		return input;
	}
}
