package voidious.dmove;

import voidious.utils.StatBufferSet;
import voidious.dmove.buffers.*;

public class SurfLowBufferSet extends StatBufferSet {
	public SurfLowBufferSet(int bins) {
		super();
		
		double weight;
		
		addStatBuffer(new SurfLowBufferA(bins), weight = 3);
		addStatBuffer(new SurfLowBufferB(bins), weight = 3);
		addStatBuffer(new SurfLowBufferC(bins), weight = 3);
		addStatBuffer(new SurfLowBufferD(bins), weight = 3);
		addStatBuffer(new SurfLowBufferE(bins), weight = 3);
		addStatBuffer(new SurfLowBufferF(bins), weight = 3);
		addStatBuffer(new SurfLowBufferG(bins), weight = 3);
		addStatBuffer(new SurfLowBufferH(bins), weight = 3);
		addStatBuffer(new SurfLowBufferI(bins), weight = 3);
		addStatBuffer(new SurfLowBufferJ(bins), weight = 3);
		addStatBuffer(new SurfLowBufferK(bins), weight = 3);
		addStatBuffer(new SurfLowBufferL(bins), weight = 3);
	}
}
