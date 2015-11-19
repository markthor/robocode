package voidious.dmove;

abstract public class SurfLowBufferBase extends SurfHitsBufferBase {
	public SurfLowBufferBase(int bins) {
		super(bins);
		
		ADVANCING_VELOCITY_SLICES = new double[] {2};
		LATERAL_VELOCITY_SLICES = new double[] {2, 4, 6};
		DISTANCE_SLICES = new double[] {150, 350, 550};
		WALL_DISTANCE_SLICES = new double[] {.3, .65};
		VCHANGE_TIME_SLICES = new double[] {.05, .15, .35};
		
		_rollingDepth = 0.7;
		_bulletHitWeight = 1;
		_firingWaveWeight = 0.1;
		_nonFiringWaveWeight = 0;
	}
}
