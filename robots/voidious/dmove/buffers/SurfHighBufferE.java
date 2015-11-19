package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferE extends SurfHighBufferBase {
	private double _binsLatDistAccelWallsRWalls[][][][][][]; 

	public SurfHighBufferE(int bins) {
		super(bins);
		
		_binsLatDistAccelWallsRWalls = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[_bins + 1];
	
		_rollingDepth = 0.6;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistAccelWallsRWalls
			[s.latVelIndex][s.distanceIndex][s.accelIndex]
			[s.wallDistanceIndex][s.wallReverseIndex];
	}

}