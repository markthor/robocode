package voidious.dgun;

import voidious.utils.*;

abstract public class AntiSurfBufferBase extends StatBuffer {
	
	public AntiSurfBufferBase(int bins) {
		super(bins);
		
		_bulletHitWeight = -2.0;
		_rollingDepth = 1;
		_firingWaveWeight = 1;
		_nonFiringWaveWeight = 0.1;
	}
}
