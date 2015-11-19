package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferO extends SurfHighBufferBase {
	private double _binsLatWallsRWallsDletSzt[][][][][][]; 

	public SurfHighBufferO(int bins) {
		super(bins);
		
		_binsLatWallsRWallsDletSzt = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[SINCEZERO_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 2;
		
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatWallsRWallsDletSzt
			[s.latVelIndex][s.wallDistanceIndex][s.wallReverseIndex]
			[s.dletIndex][s.sinceZeroIndex];
	}

}