package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferU extends SurfHighBufferBase {
	private double _binsLatDistAccelDletDltft[][][][][][]; 

	public SurfHighBufferU(int bins) {
		super(bins);
		
		_binsLatDistAccelDletDltft = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[DLET_SLICES.length + 1]
			[DLTFT_SLICES.length + 1]
			[_bins + 1];
	
		_rollingDepth = 2.5;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistAccelDletDltft
			[s.latVelIndex][s.distanceIndex][s.accelIndex]
			[s.dletIndex][s.dltftIndex];
	}

}