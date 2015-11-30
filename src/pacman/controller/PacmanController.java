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
	protected static final double MAX_DISTANCE = 255.0;
	
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
			double evaluation = evaluateNode(neighbours[i], game)[0];
			if(evaluation > highestNeighbourScore) {
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
	protected double[] evaluateNode(int node, Game game) {
		return neuralNetwork.next(getInputFromGameStateAndNode(node, game));
	}
	
	/**
	 * Calculates the inputs to the network and scale them.
	 */
	protected double[] getInputFromGameStateAndNode(int node, Game game) { 
		double[] result = new double[10];
		result[0] = scaleDistance(getDistanceToGhostFromNode(GHOST.BLINKY, game, node));
		result[1] = scaleDistance(getDistanceToGhostFromNode(GHOST.INKY, game, node));
		result[2] = scaleDistance(getDistanceToGhostFromNode(GHOST.PINKY, game, node));
		result[3] = scaleDistance(getDistanceToGhostFromNode(GHOST.SUE, game, node));
		result[4] = isGhostEdible(GHOST.BLINKY, game);
		result[5] = isGhostEdible(GHOST.INKY, game);
		result[6] = isGhostEdible(GHOST.PINKY, game);
		result[7] = isGhostEdible(GHOST.SUE, game);
		result[8] = scaleDistance(getDistanceToNearestPowerPill(node, game));
		result[9] = scaleDistance(getDistanceToNearestPill(node, game));
		return result;
	}
	
	protected double isGhostEdible(GHOST ghost, Game game) {
		if(game.isGhostEdible(ghost)) {
			return 1.0;
		} else {
			return 0.0;
		}
	}
	
	/**
	 * Scales the distance between 0 and 1.
	 */
	protected double scaleDistance(double distance) {
		return distance/MAX_DISTANCE;
	}
	
	protected double getDistanceToNearestPowerPill(int node, Game game) {
		int[] powerPills = game.getActivePowerPillsIndices();
		if(powerPills.length == 0) {
			return 1.0;
		}
		return game.getShortestPathDistance(node, game.getClosestNodeIndexFromNodeIndex(node, powerPills, DM.PATH));
	}
	
	protected double getDistanceToNearestPill(int node, Game game) {
		int[] pills = game.getActivePillsIndices();
		if(pills.length == 0) {
			return 1.0;
		}
		return game.getShortestPathDistance(node, game.getClosestNodeIndexFromNodeIndex(node, pills, DM.PATH));
	}
	
	protected double getDistanceToGhostFromNode(GHOST ghost, Game game, int node) {
		return game.getShortestPathDistance(node, game.getGhostCurrentNodeIndex(ghost));
	}
}
