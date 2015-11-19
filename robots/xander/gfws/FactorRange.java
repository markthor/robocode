package xander.gfws;

import java.awt.geom.Point2D;
import java.util.List;

public class FactorRange {
	
	int beginIndex;                    // will always be <= end index (regardless of array flipping)
	int endIndex;                      // will always be >= begin index (regardless of array flipping)
	int currentDefenderIndex;
	int maxClockwiseIndex;             // can be less than max counter-clockwise index due to array flipping
	int maxCounterClockwiseIndex;      // can be greater than clockwise index due to array flipping
	RelativeAngleRange mea;
	List<Point2D.Double> clockwisePath;
	double[] clockwiseDistances;
	double[] clockwiseFactorIndexes;
	List<Point2D.Double> counterClockwisePath;
	double[] counterClockwiseDistances;
	double[] counterClockwiseFactorIndexes;
	boolean diveProtected;

	public int getBeginIndex() {
		return beginIndex;
	}
	public int getEndIndex() {
		return endIndex;
	}
	public int getCurrentDefenderIndex() {
		return currentDefenderIndex;
	}
	public int getMaxClockwiseIndex() {
		return maxClockwiseIndex;
	}
	public int getMaxCounterClockwiseIndex() {
		return maxCounterClockwiseIndex;
	}
	public List<Point2D.Double> getClockwisePath() {
		return clockwisePath;
	}
	public double[] getClockwiseDistances() {
		return clockwiseDistances;
	}
	public double[] getClockwiseFactorIndexes() {
		return clockwiseFactorIndexes;
	}
	public List<Point2D.Double> getCounterClockwisePath() {
		return counterClockwisePath;
	}
	public double[] getCounterClockwiseDistances() {
		return counterClockwiseDistances;
	}
	public double[] getCounterClockwiseFactorIndexes() {
		return counterClockwiseFactorIndexes;
	}
	public RelativeAngleRange getMaxFactorAngles() {
		return mea;
	}
	public double getMaxClockwiseFactorAngle() {
		return (mea == null)? 0 : mea.getClockwiseOffset();
	}
	public double getMaxCounterClockwiseFactorAngle() {
		return (mea == null)? 0 : mea.getCounterClockwiseOffset();
	}
	public boolean isDiveProtected() {
		return diveProtected;
	}
	public double getDistanceForPreciseFactorIndex(double preciseIndex) {
		double closestIndexOffset = Double.POSITIVE_INFINITY;
		double distance = -1;
		for (int i=0; i<counterClockwiseFactorIndexes.length; i++) {
			double offset = Math.abs(preciseIndex-counterClockwiseFactorIndexes[i]);
			if (offset < closestIndexOffset) {
				closestIndexOffset = offset;
				distance = counterClockwiseDistances[i];
			}
		}
		for (int i=0; i<clockwiseFactorIndexes.length; i++) {
			double offset = Math.abs(preciseIndex-clockwiseFactorIndexes[i]);
			if (offset < closestIndexOffset) {
				closestIndexOffset = offset;
				distance = clockwiseDistances[i];
			}
		}		
		return distance;
	}
}
