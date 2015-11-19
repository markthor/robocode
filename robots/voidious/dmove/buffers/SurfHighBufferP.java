package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferP extends SurfHighBufferBase {
	private double _binsAdvWallsRWallsDlftSzt[][][][][][]; 

	public SurfHighBufferP(int bins) {
		super(bins);
		
		_binsAdvWallsRWallsDlftSzt = new double
			[ADVANCING_VELOCITY_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[DLFT_SLICES.length + 1]
			[SINCEZERO_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 0.7;
		
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsAdvWallsRWallsDlftSzt
			[s.advVelIndex][s.wallDistanceIndex][s.wallReverseIndex]
			[s.dlftIndex][s.sinceZeroIndex];
	}

}