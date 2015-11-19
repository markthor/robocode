package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferA extends SurfHighBufferBase {
	private double _binsLatDistAccelWallsRWallsVchangeDlet[][][][][][][][]; 

	public SurfHighBufferA(int bins) {
		super(bins);
		
		_binsLatDistAccelWallsRWallsVchangeDlet = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 0.7;
		
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistAccelWallsRWallsVchangeDlet
			[s.latVelIndex][s.distanceIndex][s.accelIndex]
			[s.wallDistanceIndex][s.wallReverseIndex][s.vChangeIndex]
			[s.dletIndex];
	}

}