package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferC extends SurfHighBufferBase {
	private double _binsLatDistAccelVchange[][][][][]; 

	public SurfHighBufferC(int bins) {
		super(bins);
		
		_binsLatDistAccelVchange = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];

		_rollingDepth = 0.2;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistAccelVchange
			[s.latVelIndex][s.distanceIndex][s.accelIndex][s.vChangeIndex];
	}

}