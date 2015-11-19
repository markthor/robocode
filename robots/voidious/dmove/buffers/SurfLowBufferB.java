package voidious.dmove.buffers;

import voidious.dmove.SurfLowBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfLowBufferB extends SurfLowBufferBase {
	private double _binsLatDistWallsVchange[][][][][]; 

	public SurfLowBufferB(int bins) {
		super(bins);
		
		_binsLatDistWallsVchange = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[WALL_DISTANCE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];

		_rollingDepth = 1;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistWallsVchange
			[s.latVelIndex][s.distanceIndex]
			[s.wallDistanceIndex][s.vChangeIndex];
	}

}
