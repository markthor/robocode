package voidious.dmove;

import voidious.utils.StatBufferSet;
import voidious.dmove.buffers.*;

public class SurfHighBufferSet extends StatBufferSet {
	public SurfHighBufferSet(int bins) {
		super();
		
		double weight;
		
		addStatBuffer(new SurfHighBufferA(bins), weight = 3);
		addStatBuffer(new SurfHighBufferB(bins), weight = 3);
		addStatBuffer(new SurfHighBufferC(bins), weight = 3);
		addStatBuffer(new SurfHighBufferD(bins), weight = 3);
		addStatBuffer(new SurfHighBufferE(bins), weight = 3);
		addStatBuffer(new SurfHighBufferF(bins), weight = 3);
		addStatBuffer(new SurfHighBufferG(bins), weight = 3);
		addStatBuffer(new SurfHighBufferH(bins), weight = 3);
		addStatBuffer(new SurfHighBufferI(bins), weight = 3);
		addStatBuffer(new SurfHighBufferJ(bins), weight = 3);
		addStatBuffer(new SurfHighBufferK(bins), weight = 3);
		addStatBuffer(new SurfHighBufferL(bins), weight = 3);
		addStatBuffer(new SurfHighBufferM(bins), weight = 3);
		addStatBuffer(new SurfHighBufferN(bins), weight = 3);
		addStatBuffer(new SurfHighBufferO(bins), weight = 3);
		addStatBuffer(new SurfHighBufferP(bins), weight = 3);
		addStatBuffer(new SurfHighBufferQ(bins), weight = 3);
		addStatBuffer(new SurfHighBufferR(bins), weight = 3);
		addStatBuffer(new SurfHighBufferS(bins), weight = 3);
		addStatBuffer(new SurfHighBufferT(bins), weight = 3);
		addStatBuffer(new SurfHighBufferU(bins), weight = 3);
		addStatBuffer(new SurfHighBufferV(bins), weight = 3);
	}
}
