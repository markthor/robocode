package voidious.dmove;

import voidious.utils.StatBuffer;
import voidious.utils.WaveIndexSet;

abstract public class SurfNonFiringBufferBase extends StatBuffer {
	public SurfNonFiringBufferBase(int bins) {
		super(bins);
		
		ADVANCING_VELOCITY_SLICES = new double[] {-3, 3};
		LATERAL_VELOCITY_SLICES = new double[] {1, 3, 5, 7};
		DISTANCE_SLICES = new double[] {150, 350, 550};
		WALL_DISTANCE_SLICES = new double[] {.3, .65, 1.1};
		WALL_REVERSE_SLICES = new double[] {.35, .7};
		VCHANGE_TIME_SLICES = new double[] {.05, .15, .35, .45};		

		_rollingDepth = 30;
		_firingWaveWeight = 5;
		_nonFiringWaveWeight = 1;
		_bulletHitWeight = 0;
	}
	
	public void logHit(WaveIndexSet s, int gfIndex, int gfLowIndex, 
		int gfHighIndex, double score, double weight) {
		
		if (weight == 0) { return; }
		
		double[] thisBuffer = getStatArray(s);
		
		for (int x = 0; x < _bins; x++) {
			double divisor;
			if (x == gfIndex) {
				divisor = 1;
			} else {
				divisor = Math.pow(Math.abs(x - gfIndex), 1.4) + 1;
			}
			
			thisBuffer[x+1] = rollingAverage(thisBuffer[x+1],
				Math.min(thisBuffer[0], _rollingDepth), score / divisor, 
				weight);
		}
		
		thisBuffer[0] += weight;
	}
}
