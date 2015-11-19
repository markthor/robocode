package voidious.dmove.buffers;

import voidious.dmove.SurfLowBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfLowBufferA extends SurfLowBufferBase {
	private double _binsAdvLatDistAccelWallsVchange[][][][][][][]; 

	public SurfLowBufferA(int bins) {
		super(bins);
		
		_binsAdvLatDistAccelWallsVchange = new double
			[ADVANCING_VELOCITY_SLICES.length + 1]
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 1;
		
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsAdvLatDistAccelWallsVchange
			[s.advVelIndex][s.latVelIndex][s.distanceIndex]
			[s.accelIndex][s.wallDistanceIndex][s.vChangeIndex];
	}

}
