package xander.gfws.segment;

import xander.core.track.Wave;

public interface Segmenter {

	/**
	 * Returns a short name for this segmenter.
	 * 
	 * @return     name for segmenter
	 */
	public String getName();
	
	/**
	 * Returns the value used to determine where the wave falls in the segment.
	 * This value can be used in place of the segment index for systems that
	 * handle segment slices in a dynamic manner.
	 * 
	 * @param wave       wave
	 * 
	 * @return        segment value
	 */
	public double getSegmentValue(Wave wave);
	
	/**
	 * Returns a normalized percent-in-range for the segment value.  For
	 * example, if the segment range is 0 to 10, and the segment value is
	 * 4, this method should return a value of 0.4)
	 * 
	 * @param wave      wave
	 * 
	 * @return          normalized percent-in-range for the segment value
	 */
	public double getSegmentValuePercent(Wave wave);
	
	/**
	 * Returns a description of a segment value.
	 * 
	 * @param segmentValue       value for segment
	 * 
	 * @return        description for segment value
	 */
	public String getSegmentValueDescription(double segmentValue);
}
