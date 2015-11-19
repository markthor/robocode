package xander.gfws.processor;

import xander.core.track.Wave;
import xander.paint.Paintable;

public interface FactorArrayProcessor extends Paintable {

	public int getFactors();
	
	public double[] getFactorArray(Wave surfWave, double currentMinFactorAngle, double currentMaxFactorAngle);

}
