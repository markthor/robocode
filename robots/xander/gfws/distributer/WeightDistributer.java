package xander.gfws.distributer;

import xander.core.drive.Direction;

public interface WeightDistributer {
	
	/**
	 * Add the given weight to the given factor array.  This method need not add all the weight
	 * requested.  The value returned should be the actual amount of weight added.
	 *   
	 * @param array
	 * @param preciseIndex
	 * @param weight
	 * @param distance
	 * @param bulletVelocity
	 * @param surfDirection
	 * 
	 * @return     the amount of weight actually added
	 */
	public double addWeight(double[] array, double preciseIndex, double weight, double distance, double bulletVelocity, Direction surfDirection);
}
