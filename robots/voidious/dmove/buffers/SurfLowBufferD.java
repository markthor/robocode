package voidious.dmove.buffers;

import voidious.dmove.SurfLowBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfLowBufferD extends SurfLowBufferBase {
	private double _binsLatAccelVchange[][][][]; 

	public SurfLowBufferD(int bins) {
		super(bins);
		
		_binsLatAccelVchange = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[ACCEL_SLICES]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 0.5;
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatAccelVchange
			[s.latVelIndex][s.accelIndex][s.vChangeIndex];
	}

}
