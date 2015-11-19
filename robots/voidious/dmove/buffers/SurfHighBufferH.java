package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferH extends SurfHighBufferBase {
	private double _binsLatDistWallsRWallsDlet[][][][][][]; 

	public SurfHighBufferH(int bins) {
		super(bins);
		
		_binsLatDistWallsRWallsDlet = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[_bins + 1];

		_rollingDepth = 1.5;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistWallsRWallsDlet
			[s.latVelIndex][s.distanceIndex]
			[s.wallDistanceIndex][s.wallReverseIndex][s.dletIndex];
	}

}