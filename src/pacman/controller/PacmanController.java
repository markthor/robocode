package pacman.controller;

import java.util.ArrayList;

import com.anji.integration.Activator;

import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class PacmanController extends NeuralPacmanController {

	private boolean activePowerPill;
	private int powerPillTime;
	private int powerPillTimeLeft;
	private int currentLevel;
	private boolean firstUpdate = true;
	private int powerPillsInLevel;
	private int powerPillsEaten;
	private int totalScore;

	public PacmanController(Activator nn) {
		currentNetwork = nn;
	}
	
	@Override
	public MOVE getMove(Game game, long timeDue) {
		return getBestMove(game);
	}
	
	/**
	 * Evaluates each possible move and returns the move with the highest output from the ANN
	 */
	private MOVE getBestMove(Game game) {
		update(game);
		
		int[] neighbours = game.getNeighbouringNodes(game.getPacmanCurrentNodeIndex());
		
		int highestNeighbour = -1;
		double highestNeighbourScore = -1.0;
		int pacmanNode = game.getPacmanCurrentNodeIndex();
		for(int i = 0; i < neighbours.length; i++) {
			double evaluation = evaluateNode(neighbours[i], game, game.getMoveToMakeToReachDirectNeighbour(pacmanNode, neighbours[i]))[0];
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
	
	private void update(Game game) {
		totalScore = game.getScore();
		
		if (game.getCurrentLevel() > currentLevel || firstUpdate) {
			firstUpdate = false;
			currentLevel = game.getCurrentLevel();
			powerPillsInLevel = game.getActivePowerPillsIndices().length;
			powerPillsEaten = 0;
		} else if(game.wasPillEaten()) {
			powerPillsEaten++;
		}
		
		if (game.wasPowerPillEaten()) {
			int time = -1;
			for (GHOST g : GHOST.values()) {
				if (time < game.getGhostEdibleTime(g)) {
					time = game.getGhostEdibleTime(g);
				}
			}
			
			activePowerPill = true;
			powerPillTime = time;
			powerPillTimeLeft = time;
		} else {
			powerPillTimeLeft--;
			if (powerPillTimeLeft <= 0) {
				activePowerPill = false;
			}
		}
	}

	/**
	 * Returns the output of the neural network based on the inputs of the provided node.
	 */
	protected double[] evaluateNode(int node, Game game, MOVE m) {
		return currentNetwork.next(getInputFromGameStateAndNode(node, game, m));
	}
	
	/**
	 * Calculates the inputs to the network and scale them.
	 */
	protected double[] getInputFromGameStateAndNode(int node, Game game, MOVE m) {
		int highestPillCount = 240;
		int otherInputs = 41;
		
		double[] result = new double[highestPillCount + otherInputs];
		//All ghosts
		//All ghost edible
		//Nearest ghost, next, next, next
		//Nearest ghost edible, next, next, next
		//Is ghost edible or distance to ghost
		//Ghost lair time?
		//Nearest pill
		//All pills
		//Nearest power pill, next, next, next
		//Power pill time remaining
		//Percentage pills left
		//Percentage pills eaten
		//current direction
		//Time since last reversal
		//Nearest junction
		//Is in junction?
		//game.getPacmanNumberOfLivesRemaining()
		double tempArray[];
		//GHOST INPUTS
		result[0] = scaleDistance(getDistanceToGhostFromNode(GHOST.BLINKY, game, node));
		result[1] = scaleDistance(getDistanceToGhostFromNode(GHOST.INKY, game, node));
		result[2] = scaleDistance(getDistanceToGhostFromNode(GHOST.PINKY, game, node));
		result[3] = scaleDistance(getDistanceToGhostFromNode(GHOST.SUE, game, node));
		result[4] = scaleBoolean(isGhostEdible(GHOST.BLINKY, game));
		result[5] = scaleBoolean(isGhostEdible(GHOST.INKY, game));
		result[6] = scaleBoolean(isGhostEdible(GHOST.PINKY, game));
		result[7] = scaleBoolean(isGhostEdible(GHOST.SUE, game));
		tempArray = getDistanceToNearestGhosts(game, node);
		result[8] = scaleDistance(tempArray[0]);
		result[9] = scaleDistance(tempArray[1]);
		result[10] = scaleDistance(tempArray[2]);
		result[11] = scaleDistance(tempArray[3]);
		tempArray = getDistanceToNearestEdibleGhosts(game, node);
		result[12] = scaleDistance(tempArray[0]);
		result[13] = scaleDistance(tempArray[1]);
		result[14] = scaleDistance(tempArray[2]);
		result[15] = scaleDistance(tempArray[3]);
		result[16] = scaleDistance(getGhostEdibleOrDistance(GHOST.BLINKY, game, node));
		result[17] = scaleDistance(getGhostEdibleOrDistance(GHOST.INKY, game, node));
		result[18] = scaleDistance(getGhostEdibleOrDistance(GHOST.PINKY, game, node));
		result[19] = scaleDistance(getGhostEdibleOrDistance(GHOST.SUE, game, node));
		tempArray = getNearestGhostEdibleOrDistance(game, node);
		result[20] = scaleDistance(tempArray[0]);
		result[21] = scaleDistance(tempArray[1]);
		result[22] = scaleDistance(tempArray[2]);
		result[23] = scaleDistance(tempArray[3]);
		//POWER PILL INPUTS
		tempArray = getDistanceToPowerPills(game, node);
		result[24] = scaleDistance(tempArray[0]);
		result[25] = scaleDistance(tempArray[1]);
		result[26] = scaleDistance(tempArray[2]);
		result[27] = scaleDistance(tempArray[3]);
		tempArray = getDistanceToNearestPowerPills(game, node);
		result[28] = scaleDistance(tempArray[0]);
		result[29] = scaleDistance(tempArray[1]);
		result[30] = scaleDistance(tempArray[2]);
		result[31] = scaleDistance(tempArray[3]);
		result[32] = scaleBoolean(isPowerPillActive(game));
		result[33] = getPowerPillTimeLeftPercent(game);
		result[34] = getPowerPillTimeGonePercent(game);
		//MISCELLANEUOS INPUTS
		result[35] = scaleBoolean(isCurrentDirection(game, node, m));
		result[36] = scaleDistance(getDistanceToNearestJunction(game, node));
		result[37] = scaleBoolean(isJunction(game, node));
		//PILL INPUTS
		result[38] = scaleDistance(getDistanceToNearestPill(game, node));
		result[39] = getPercentagePillsLeft(game);
		result[40] = getPercentagePillsEaten(game);
		tempArray = getDistanceToAllPills(game, node);
		int k = 41;
		for (int i = 0; i < tempArray.length; i++) {
			result[k+i] = scaleDistance(tempArray[i]);
		}
		
		if (tempArray.length != highestPillCount) {
			for (int i = tempArray.length + otherInputs; i < otherInputs + highestPillCount; i++) {
				result[i] = Integer.MAX_VALUE;
			}
		}
		
		return result;
	}

	private double[] getDistanceToAllPills(Game game, int currentNode) {
		int[] allPills = game.getPillIndices();
		double[] result = new double[allPills.length];
		for (int i = 0 ; i < allPills.length ; i++) {
			result[i] = game.getShortestPathDistance(currentNode, allPills[i]);
		}
		return result;
	}

	private double getPercentagePillsEaten(Game game) {
		return (powerPillsEaten / powerPillsInLevel) * 100;
	}

	private double getPercentagePillsLeft(Game game) {
		return ((powerPillsInLevel - powerPillsEaten) / powerPillsInLevel) * 100;
	}

	private boolean isJunction(Game game, int currentNode) {
		return game.isJunction(currentNode);
	}

	private double getDistanceToNearestJunction(Game game, int currentNode) {
		int[] junctions = game.getJunctionIndices();
		return game.getShortestPathDistance(currentNode, game.getClosestNodeIndexFromNodeIndex(currentNode, junctions, DM.PATH));
	}

	private boolean isCurrentDirection(Game game, int currentNode, MOVE m) {
		if (game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), currentNode, DM.PATH) == m) {
			return true;
		} else {
			return false;
			}
		}

	private double getPowerPillTimeGonePercent(Game game) {
		return ((powerPillTime-powerPillTimeLeft) / powerPillTime) * 100;
	}

	private double getPowerPillTimeLeftPercent(Game game) {
		return (powerPillTimeLeft / powerPillTime) * 100;
	}

	private boolean isPowerPillActive(Game game) {
		return activePowerPill ? true : false;
	}

	private double[] getDistanceToNearestPowerPills(Game game, int currentNode) {
		double[] result = new double[4];
		int[] active = game.getActivePillsIndices();
		
		ArrayList<Integer> done = new ArrayList<Integer>();
		for (int i = 0; i < active.length; i++) {
			int node = -1;
			int shortestDistance = Integer.MAX_VALUE;
			for (int j = 0; j < active.length; j++) {
				if (!done.contains(active[j])) {
					int d = game.getShortestPathDistance(currentNode, active[j]);
					if (d < shortestDistance) {
						node = active[j];
						shortestDistance = d;
					}
				}
			}
			result[i] = shortestDistance;
			done.add(node);
		}
		
		for (int i = 0; i < 4 - active.length; i++) {
			result[4-i] = Integer.MAX_VALUE;
		}
		
		return result;
	}

	private double[] getDistanceToPowerPills(Game game, int currentNode) {
		double[] result = new double[4];
		int[] powerPills = game.getPowerPillIndices();
		for(int i = 0; i < powerPills.length; i++){
			if (game.isPowerPillStillAvailable(powerPills[i])) {
				result[i] = game.getShortestPathDistance(currentNode, powerPills[i]);
				continue;
			}
			result[i] = Integer.MAX_VALUE;
		}
		
		return result;
	}

	private double[] getNearestGhostEdibleOrDistance(Game game, int currentNode) {
		double[] result = new double[4];
		
		int distanceToNearestGhost = Integer.MAX_VALUE;
		GHOST closest = null;
		ArrayList<GHOST> done = new ArrayList<GHOST>();
		while (done.size() != 4) {
			for (GHOST g : GHOST.values()) {
				if (!done.contains(g)) {
					int d = game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(g));
					if (d < distanceToNearestGhost) {
						distanceToNearestGhost = d;
						closest = g;
					}
				}
			}
			if (isGhostEdible(closest, game)) {
				result[done.size()] = game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(closest));
			} else {
				result[done.size()] = - game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(closest));
			}
			
			done.add(closest);
			closest = null;
			distanceToNearestGhost = Integer.MAX_VALUE;
		}
		
		return result;
	}

	private double getGhostEdibleOrDistance(GHOST g, Game game, int currentNode) {
		if (isGhostEdible(g, game)) {
			return game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(g));
		} else {
			return - game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(g));
		}
	}

	private double[] getDistanceToNearestEdibleGhosts(Game game, int currentNode) {
		double[] result = new double[4];
		
		int distanceToNearestGhost = Integer.MAX_VALUE;
		GHOST closest = null;
		int not = 0;
		ArrayList<GHOST> done = new ArrayList<GHOST>();
		while (done.size() + not != 4) {
			for (GHOST g : GHOST.values()) {
				if (!done.contains(g)) {
					if (isGhostEdible(g, game)) {
						int d = game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(g));
						if (d < distanceToNearestGhost) {
							distanceToNearestGhost = d;
							closest = g;
						}
					} else{
						result[3-not] = -1;
					}
				}
			}
			result[done.size()] = game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(closest));
			done.add(closest);
			closest = null;
			distanceToNearestGhost = Integer.MAX_VALUE;
		}
		return result;
	}

	private double[] getDistanceToNearestGhosts(Game game, int currentNode) {
		double[] result = new double[4];
		
		int distanceToNearestGhost = Integer.MAX_VALUE;
		GHOST closest = null;
		ArrayList<GHOST> done = new ArrayList<GHOST>();
		while (done.size() != 4) {
			for (GHOST g : GHOST.values()) {
				if (game.getGhostEdibleTime(g) <= 0 && !done.contains(g)) {
					int d = game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(g));
					if (d < distanceToNearestGhost) {
						distanceToNearestGhost = d;
						closest = g;
					}
				}
			}
			result[done.size()] = game.getShortestPathDistance(currentNode, game.getGhostCurrentNodeIndex(closest));
			done.add(closest);
			closest = null;
			distanceToNearestGhost = Integer.MAX_VALUE;
		}
		
		return result;
	}

	protected boolean isGhostEdible(GHOST ghost, Game game) {
		return game.isGhostEdible(ghost);
		/*if(game.isGhostEdible(ghost)) {
			return 1.0;
		} else {
			return 0.0;
		}*/
	}
	
	protected double getDistanceToNearestPowerPill(int node, Game game) {
		int[] powerPills = game.getActivePowerPillsIndices();
		if(powerPills.length == 0) {
			return 1.0;
		}
		return game.getShortestPathDistance(node, game.getClosestNodeIndexFromNodeIndex(node, powerPills, DM.PATH));
	}
	
	protected double getDistanceToNearestPill(Game game, int node) {
		int[] pills = game.getActivePillsIndices();
//		if(pills.length == 0) {
//			return 1.0;
//		}
		return game.getShortestPathDistance(node, game.getClosestNodeIndexFromNodeIndex(node, pills, DM.PATH));
	}
	
	protected double getDistanceToGhostFromNode(GHOST ghost, Game game, int node) {
		return game.getShortestPathDistance(node, game.getGhostCurrentNodeIndex(ghost));
	}

	public int getScore() {
		return totalScore;
	}
}