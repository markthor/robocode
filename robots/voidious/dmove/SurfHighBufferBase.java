package voidious.dmove;

abstract public class SurfHighBufferBase extends SurfHitsBufferBase {
	public SurfHighBufferBase(int bins) {
		super(bins);
		
		ADVANCING_VELOCITY_SLICES = new double[] {-3, 3};
		LATERAL_VELOCITY_SLICES = new double[] {1, 3, 5, 7};
		DISTANCE_SLICES = new double[] {150, 300, 450, 600};
		WALL_DISTANCE_SLICES = new double[] {.2, .4, .6, .8, 1.1};
		WALL_REVERSE_SLICES = new double[] {.35, .7};
		VCHANGE_TIME_SLICES = new double[] {.05, .15, .35, .55};		
		SINCEZERO_TIME_SLICES = new double[] {0.35, 0.55, 0.75};		
		SINCEMAX_TIME_SLICES = new double[] {0.15, 0.4, 0.5};		
		DLET_SLICES = new double[] {26, 44, 60};
		DLFT_SLICES = new double[] {36, 64, 92};
		DLTFT_SLICES = new double[] {60, 100, 155};

		_rollingDepth = 0.7;
		_bulletHitWeight = 1;
		_firingWaveWeight = 0.1;
		_nonFiringWaveWeight = 0;
	}
}
