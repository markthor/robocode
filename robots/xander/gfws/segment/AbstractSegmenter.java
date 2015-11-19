package xander.gfws.segment;

import xander.core.track.Wave;

/**
 * Abstract segmenter class that takes care of implementing the getSegmentValuePercent method 
 * of the Segmenter interface.  This requires the implementing clas to specify the minimum
 * and maximum expected segment values.  
 * 
 * The segment value percent is ensured to remain in the proper range of 0 to 1, even if 
 * the segment value falls outside of the provided minimum and maximum segment values.
 * 
 * @author Scott Arnold
 */
public abstract class AbstractSegmenter implements Segmenter {

	private double minOfRange;
	private double maxOfRange;
	private double range;
	
	public AbstractSegmenter(double minOfRange, double maxOfRange) {
		this.minOfRange = minOfRange;
		this.maxOfRange = maxOfRange;
		this.range = maxOfRange - minOfRange;
	}

	@Override
	public double getSegmentValuePercent(Wave wave) {
		double segmentValue = Math.max(minOfRange, Math.min(maxOfRange, getSegmentValue(wave)));		
		return (segmentValue - minOfRange)/range;
	}
}
