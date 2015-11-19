package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferR extends SurfHighBufferBase {
	private double _binsAdvLatDletDltftVchange[][][][][][]; 

	public SurfHighBufferR(int bins) {
		super(bins);
		
		_binsAdvLatDletDltftVchange = new double
			[ADVANCING_VELOCITY_SLICES.length + 1]
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[DLTFT_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 3;
		
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsAdvLatDletDltftVchange
			[s.advVelIndex][s.latVelIndex][s.dletIndex]
			[s.dltftIndex][s.vChangeIndex];
	}

}