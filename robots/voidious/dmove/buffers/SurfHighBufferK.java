package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferK extends SurfHighBufferBase {
	private double _binsLatWallsDletDlftDltft[][][][][][]; 

	public SurfHighBufferK(int bins) {
		super(bins);
		
		_binsLatWallsDletDlftDltft = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[DLFT_SLICES.length + 1]
			[DLTFT_SLICES.length + 1]
			[_bins + 1];
	
		_rollingDepth = 5;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatWallsDletDlftDltft
			[s.latVelIndex][s.wallDistanceIndex]
			[s.dletIndex][s.dlftIndex][s.dltftIndex];
	}

}