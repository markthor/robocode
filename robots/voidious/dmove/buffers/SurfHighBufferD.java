package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferD extends SurfHighBufferBase {
	private double _binsLatDistWallsRWallsVchange[][][][][][]; 

	public SurfHighBufferD(int bins) {
		super(bins);
		
		_binsLatDistWallsRWallsVchange = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
	
		_rollingDepth = 0.4;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistWallsRWallsVchange
			[s.latVelIndex][s.distanceIndex]
			[s.wallDistanceIndex][s.wallReverseIndex][s.vChangeIndex];
	}

}