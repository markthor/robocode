package xander.gfws.data;

import xander.core.Resources;
import xander.core.drive.Direction;
import xander.gfws.FactorIndexer;
import xander.gfws.RelativeAngleRange;

/**
 * Represents a single data point for a WaveLogger indicating the angle of the defender
 * at the time a wave hits the defender.
 * 
 * @author Scott Arnold
 */
public class DataPoint implements Comparable<DataPoint> {
	
	double factorAngle;
	double distance;
	double bulletVelocity;
	Direction surfDirection;
	RelativeAngleRange mea;
	long roundTime;
	long logTime;
	boolean self;
	
	public DataPoint(double factorAngle, double distance, double bulletVelocity, Direction surfDirection, RelativeAngleRange mea, long logTime, long roundTime, boolean forSelf) {
		this.factorAngle = factorAngle;
		this.distance = distance;
		this.bulletVelocity = bulletVelocity;
		this.surfDirection = surfDirection;
		this.mea = mea;
		this.logTime = logTime;
		this.roundTime = roundTime;
		this.self = forSelf;
	}

	@Override
	public int compareTo(DataPoint other) {
		return (int)(other.logTime-logTime); 
	}
	
	public double getFactorIndex(int factors) {
		FactorIndexer factorIndexer = self? Resources.getMyFactorIndexer() : Resources.getOpponentFactorIndexer();
		return factorIndexer.getPreciseFactorIndex(factorAngle, factors, surfDirection, mea);
	}
}
