package voidious.dgun;

import voidious.utils.WaveIndexSet;

public class MainGunFastHighBuffer extends MainBufferBase {
	private double[][][][] _binsLatBulAccel;

	public MainGunFastHighBuffer(int bins) {
		super(bins);
		
		LATERAL_VELOCITY_SLICES = new double[] {.5, 2.5, 5.0, 7.5};
		BULLET_TIME_SLICES = new double[] {14, 28, 42, 56};

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
