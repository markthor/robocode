package voidious.dmove;

import voidious.utils.DUtils;
import voidious.utils.StatBuffer;
import voidious.utils.WaveIndexSet;

abstract public class SurfHitsBufferBase extends StatBuffer {
	public SurfHitsBufferBase(int bins) {
		super(bins);
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
				divisor = DUtils.square(x - gfIndex) + 1;
			}
			
			thisBuffer[x+1] = rollingAverage(thisBuffer[x+1],
				Math.min(thisBuffer[0], _rollingDepth), score / divisor, 
				weight);
		}
		
		thisBuffer[0] += weight;
	}
}
