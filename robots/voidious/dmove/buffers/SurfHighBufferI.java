package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferI extends SurfHighBufferBase {
	private double _binsDistWallsRWallsDletVchange[][][][][][]; 

	public SurfHighBufferI(int bins) {
		super(bins);
		
		_binsDistWallsRWallsDletVchange = new double
			[DISTANCE_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 2;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsDistWallsRWallsDletVchange
			[s.distanceIndex][s.wallDistanceIndex][s.wallReverseIndex]
			[s.dletIndex][s.vChangeIndex];
	}

}