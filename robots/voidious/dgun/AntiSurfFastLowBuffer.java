package voidious.dgun;

import voidious.utils.WaveIndexSet;

public class AntiSurfFastLowBuffer extends AntiSurfBufferBase {
	private double[][][][] _binsLatDistAccel;

	public AntiSurfFastLowBuffer(int bins) {
		super(bins);
		
		LATERAL_VELOCITY_SLICES = new double[] {2, 4, 6};
		DISTANCE_SLICES = new double[] {250, 500};

		_binsLatDistAccel = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[_bins + 1];
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistAccel[s.latVelIndex][s.distanceIndex][s.accelIndex];
	}
}
