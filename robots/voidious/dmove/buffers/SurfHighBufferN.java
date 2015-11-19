package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferN extends SurfHighBufferBase {
	private double _binsDletDlftDltftSmstSztVchange[][][][][][][]; 

	public SurfHighBufferN(int bins) {
		super(bins);
		
		_binsDletDlftDltftSmstSztVchange = new double
		[DLET_SLICES.length + 1]
			[DLFT_SLICES.length + 1]
			[DLTFT_SLICES.length + 1]
			[SINCEMAX_TIME_SLICES.length + 1]
			[SINCEZERO_TIME_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[_bins + 1];
		
		_rollingDepth = 1.5;
		
	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsDletDlftDltftSmstSztVchange
			[s.dletIndex][s.dlftIndex][s.dltftIndex]
			[s.sinceMaxIndex][s.sinceZeroIndex][s.vChangeIndex];
	}

}