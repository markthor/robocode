package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferM extends SurfHighBufferBase {
	private double _binsWallsRWallsDletSztVChange[][][][][][]; 

	public SurfHighBufferM(int bins) {
		super(bins);
		
		_binsWallsRWallsDletSztVChange = new double
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[SINCEZERO_TIME_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
	
		_rollingDepth = 1;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsWallsRWallsDletSztVChange
			[s.wallDistanceIndex][s.wallReverseIndex][s.dletIndex]
			[s.sinceZeroIndex][s.vChangeIndex];
	}

}