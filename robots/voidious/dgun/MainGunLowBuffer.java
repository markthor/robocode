package voidious.dgun;

import voidious.utils.WaveIndexSet;

public class MainGunLowBuffer extends MainBufferBase {
	private double[][][][][][][] _binsLatBulAccelWallVchangeDlet;
	
	public MainGunLowBuffer(int bins) {
		super(bins);
		
		LATERAL_VELOCITY_SLICES = new double[] {.5, 2.25, 6.75};
		BULLET_TIME_SLICES = new double[] {24, 48};
		WALL_DISTANCE_SLICES = new double[] {.25, .45, .65};
		VCHANGE_TIME_SLICES = new double[] {.05, .35};
		DLET_SLICES = new double[] {55};
		
		_binsLatBulAccelWallVchangeDlet = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[BULLET_TIME_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[_bins + 1];
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatBulAccelWallVchangeDlet
		    [s.latVelIndex][s.bulletTimeIndex][s.accelIndex]
		    [s.wallDistanceIndex][s.vChangeIndex][s.dletIndex];
	}
}
