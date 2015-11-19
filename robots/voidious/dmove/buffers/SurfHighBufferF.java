package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferF extends SurfHighBufferBase {
	private double _binsAdvLatDistAccel[][][][][]; 

	public SurfHighBufferF(int bins) {
		super(bins);
		
		_binsAdvLatDistAccel = new double
			[ADVANCING_VELOCITY_SLICES.length + 1]
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[_bins + 1];

		_rollingDepth = 0.8;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsAdvLatDistAccel
			[s.advVelIndex][s.latVelIndex][s.distanceIndex][s.accelIndex];
	}

}