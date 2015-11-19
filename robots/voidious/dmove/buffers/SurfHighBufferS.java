package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferS extends SurfHighBufferBase {
	private double _binsWallsDistDlftVchangeSmst[][][][][][]; 

	public SurfHighBufferS(int bins) {
		super(bins);
		
		_binsWallsDistDlftVchangeSmst = new double
			[WALL_DISTANCE_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[DLFT_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[SINCEMAX_TIME_SLICES.length + 1]
			[_bins + 1];
	
		_rollingDepth = 2;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsWallsDistDlftVchangeSmst
			[s.wallDistanceIndex][s.distanceIndex][s.dlftIndex]
			[s.vChangeIndex][s.sinceMaxIndex];
	}

}