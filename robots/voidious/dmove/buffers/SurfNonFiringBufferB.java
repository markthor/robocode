package voidious.dmove.buffers;

import voidious.dmove.SurfNonFiringBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfNonFiringBufferB extends SurfNonFiringBufferBase {
	private double _binsLatDistAccelWallsRWallsVchange[][][][][][][]; 

	public SurfNonFiringBufferB(int bins) {
		super(bins);
		
		_binsLatDistAccelWallsRWallsVchange = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[DISTANCE_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
		
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatDistAccelWallsRWallsVchange
			[s.latVelIndex][s.distanceIndex][s.accelIndex]
			[s.wallDistanceIndex][s.wallReverseIndex][s.vChangeIndex];
	}

}