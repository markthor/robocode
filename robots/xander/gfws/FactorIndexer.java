package xander.gfws;

import xander.core.drive.Direction;

public interface FactorIndexer {

	public double getPreciseFactorIndex(double factorAngle, int factors, Direction surfDirection, RelativeAngleRange mea);
	
	public int getFactorIndex(double factorAngle, int factors, Direction surfDirection, RelativeAngleRange mea);
	
	public double getFactorAngle(double preciseFactorIndex, int factors, Direction surfDirection, RelativeAngleRange mea);
	
	public IndexRange getRobotIndexWidth(int factors, double distance, double currentRobotFactorAngle, Direction surfDirection, RelativeAngleRange mea);
}
