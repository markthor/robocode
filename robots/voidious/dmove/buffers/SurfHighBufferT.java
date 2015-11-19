package voidious.dmove.buffers;

import voidious.dmove.SurfHighBufferBase;
import voidious.utils.WaveIndexSet;

public class SurfHighBufferT extends SurfHighBufferBase {
	private double _binsVchangeSmstDletDlftDltft[][][][][][]; 

	public SurfHighBufferT(int bins) {
		super(bins);
		
		_binsVchangeSmstDletDlftDltft = new double
			[VCHANGE_TIME_SLICES.length + 1]
			[SINCEMAX_TIME_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[DLFT_SLICES.length + 1]
			[DLTFT_SLICES.length + 1]
			[_bins + 1];
	
		_rollingDepth = 1;

	}
	
	public double[] getStatArray(WaveIndexSet s) {
		return _binsVchangeSmstDletDlftDltft
			[s.vChangeIndex][s.sinceMaxIndex]
			[s.dletIndex][s.dlftIndex][s.dltftIndex];
	}

}