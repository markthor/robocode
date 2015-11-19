package voidious.dmove.buffers;

import voidious.dmove.SurfLowBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfLowBufferH extends SurfLowBufferBase {
	private double _binsLatVchange[][][]; 

	public SurfLowBufferH(int bins) {
		super(bins);
		
		_binsLatVchange = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 0.7;
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatVchange
			[s.latVelIndex][s.vChangeIndex];
	}

}
