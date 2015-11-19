package xander.gfws;

import xander.core.drive.Direction;
import xander.core.math.RCPhysics;

public class BalancedFactorIndexer implements FactorIndexer {

	@Override
	public double getPreciseFactorIndex(double factorAngle, int factors,
			Direction surfDirection, RelativeAngleRange mea) {
		if (surfDirection.isClockwise()) {
			double m = factors/(mea.getClockwiseOffset() - mea.getCounterClockwiseOffset());
			double b = -m * mea.getCounterClockwiseOffset();
			return m * factorAngle + b;
		} else {
			double m = factors/(mea.getCounterClockwiseOffset() - mea.getClockwiseOffset());
			double b = -m * mea.getClockwiseOffset();
			return m * factorAngle + b;
		}
	}

	@Override
	public int getFactorIndex(double factorAngle, int factors,
			Direction surfDirection, RelativeAngleRange mea) {
		return (int) Math.round(Math.floor(getPreciseFactorIndex(factorAngle, factors, surfDirection, mea)));
	}

	@Override
	public double getFactorAngle(double preciseFactorIndex, int factors,
			Direction surfDirection, RelativeAngleRange mea) {
		if (surfDirection.isClockwise()) {
			double m = (mea.getClockwiseOffset() - mea.getCounterClockwiseOffset())/factors;
			return m * preciseFactorIndex + mea.getCounterClockwiseOffset();
		} else {
			double m = (mea.getCounterClockwiseOffset() - mea.getClockwiseOffset())/factors;
			return m * preciseFactorIndex + mea.getClockwiseOffset();
		}
	}

	@Override
	public IndexRange getRobotIndexWidth(int factors, double distance,
			double currentRobotFactorAngle, Direction surfDirection, RelativeAngleRange mea) {
		double circ = 2*Math.PI*distance;
		double halfBodyWidthDegrees = 180d*RCPhysics.ROBOT_WIDTH/circ;
		double ccwIdx = getPreciseFactorIndex(currentRobotFactorAngle-halfBodyWidthDegrees, factors, surfDirection, mea);
		double cwIdx = getPreciseFactorIndex(currentRobotFactorAngle+halfBodyWidthDegrees, factors, surfDirection, mea);
		if (ccwIdx < cwIdx) {
			return new IndexRange(ccwIdx, cwIdx);
		} else {
			return new IndexRange(cwIdx, ccwIdx);
		}
	}

}
