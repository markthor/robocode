package cs.move.extra;

import robocode.Rules;

/**
 * Data gained from movement prediction
 */
public class Move {
	public double angleToTurn = 0;
	public double maxVelocity = Rules.MAX_VELOCITY;
	public int direction = 1;
}