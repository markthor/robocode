package voidious.dgun;

import voidious.utils.WaveIndexSet;

public class AntiSurfLowBuffer extends AntiSurfBufferBase {
	private double[][][][][][][] _binsLatDistAccelWallVchangeDlet;
	
	public AntiSurfLowBuffer(int bins) {
		super(bins);
		
		LATERAL_VELOCITY_SLICES = new double[] {2.25, 6.25};
		DISTANCE_SLICES = new double[] {250, 500};
		WALL_DISTANCE_SLICES = new double[] {.25, .5, .75};
		VCHANGE_TIME_SLICES = new double[] {.05, .35};
//		DLET_SLICES = new double[] {10};
		
		_binsLatDistAccelWallVchangeDlet = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[_bins + 1];
	}

	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistAccelWallVchangeDlet
		    [s.latVelIndex][s.distanceIndex][s.accelIndex]
		    [s.wallDistanceIndex][s.vChangeIndex][s.dletIndex];
	}
}
