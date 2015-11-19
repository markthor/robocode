package voidious.dmove.buffers;

import voidious.dmove.SurfLowBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfLowBufferC extends SurfLowBufferBase {
	private double _binsLatAccelWalls[][][][]; 

	public SurfLowBufferC(int bins) {
		super(bins);
		
		_binsLatAccelWalls = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 1;
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatAccelWalls
			[s.latVelIndex][s.accelIndex][s.wallDistanceIndex];
	}

}
