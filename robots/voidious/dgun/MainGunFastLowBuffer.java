package voidious.dgun;

import voidious.utils.WaveIndexSet;

public class MainGunFastLowBuffer extends MainBufferBase {
	private double[][][][] _binsLatBulAccel;

	public MainGunFastLowBuffer(int bins) {
		super(bins);
		
		LATERAL_VELOCITY_SLICES = new double[] {2, 4, 6};
		BULLET_TIME_SLICES = new double[] {24, 48};

		_rollingDepth = 5;

		_binsLatBulAccel = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[BULLET_TIME_SLICES.length + 1]
			[ACCEL_SLICES]
			[_bins + 1];
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatBulAccel[s.latVelIndex][s.bulletTimeIndex][s.accelIndex];
	}
}
