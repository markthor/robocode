package voidious.dmove;

import voidious.utils.StatBufferSet;
import voidious.dmove.buffers.*;

public class SurfNonFiringBufferSet extends StatBufferSet {
	public SurfNonFiringBufferSet(int bins) {
		super();
		
		double weight;
		
		addStatBuffer(new SurfNonFiringBufferA(bins), weight = 32);
		addStatBuffer(new SurfNonFiringBufferB(bins), weight = 8);
		addStatBuffer(new SurfNonFiringBufferC(bins), weight = 3);
	}
}
