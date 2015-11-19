package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferQ extends SurfHighBufferBase {
	private double _binsLatWallsRWallsDltftSmst[][][][][][]; 

	public SurfHighBufferQ(int bins) {
		super(bins);
		
		_binsLatWallsRWallsDltftSmst = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[DLTFT_SLICES.length + 1]
			[SINCEMAX_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 0.7;
		
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatWallsRWallsDltftSmst
			[s.latVelIndex][s.wallDistanceIndex][s.wallReverseIndex]
			[s.dltftIndex][s.sinceMaxIndex];
	}

}