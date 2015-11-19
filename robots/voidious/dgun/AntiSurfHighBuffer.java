package voidious.dgun;

import voidious.utils.WaveIndexSet;

public class AntiSurfHighBuffer extends AntiSurfBufferBase {
	private double[][][][][][][][] _binsLatDistAccelWallRWallVchangeDlet;
	
	public AntiSurfHighBuffer(int bins) {
		super(bins);
		
		LATERAL_VELOCITY_SLICES = new double[] {.25, 1.25, 2.25, 6.75};
		DISTANCE_SLICES = new double[] {150, 300, 450, 600};
		WALL_DISTANCE_SLICES = new double[] {.2, .35, .65, .95};
		WALL_REVERSE_SLICES = new double[] {.5};
		VCHANGE_TIME_SLICES = new double[] {.05, .15, .35, .45};
//		DLET_SLICES = new double[] {10, 25, 45};
		
		_binsLatDistAccelWallRWallVchangeDlet = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[_bins + 1];
	}

	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistAccelWallRWallVchangeDlet
		    [s.latVelIndex][s.distanceIndex][s.accelIndex]
		    [s.wallDistanceIndex][s.wallReverseIndex][s.vChangeIndex]
		    [s.dletIndex];
	}
}
