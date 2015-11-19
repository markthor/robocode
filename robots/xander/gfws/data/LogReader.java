package xander.gfws.data;


import xander.core.track.Wave;

public interface LogReader {

	public WaveSearchIterable<DataPoint> getHitData(Wave wave);
	
	public WaveSearchIterable<DataPoint> getVisitData(Wave wave);
	
}
