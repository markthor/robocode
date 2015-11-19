package voidious.dgun;

import voidious.utils.WaveIndexSet;

public class AntiSurfRawBuffer extends AntiSurfBufferBase {
	private double[] _binsRaw;
	
	public AntiSurfRawBuffer(int bins) {
		super(bins);
		
		_binsRaw = new double[_bins + 1];
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsRaw;
	}
}
