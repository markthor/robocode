package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferG extends SurfHighBufferBase {
	private double _binsAdvLatDistWallsRWalls[][][][][][]; 

	public SurfHighBufferG(int bins) {
		super(bins);
		
		_binsAdvLatDistWallsRWalls = new double
			[ADVANCING_VELOCITY_SLICES.length + 1]
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[_bins + 1];

		_rollingDepth = 1;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsAdvLatDistWallsRWalls
			[s.advVelIndex][s.latVelIndex][s.distanceIndex]
			[s.wallDistanceIndex][s.wallReverseIndex];
	}

}