package xander.gfws.data.red;

import xander.gfws.data.DataPoint;
import xander.gfws.data.KDTreeAdapter;
import xander.gfws.data.KDTreeWaveLogger;
import xander.gfws.segment.Segmenter;

public class RedKDTreeWaveLogger extends KDTreeWaveLogger {

	private final int bucketSize = 64;
	
	public RedKDTreeWaveLogger(boolean offensive, Segmenter... segmenters) {
		super(offensive, segmenters);
	}

	public RedKDTreeWaveLogger(boolean offensive, int maxHits, int maxVisits,
			Segmenter... segmenters) {
		super(offensive, maxHits, maxVisits, segmenters);
	}

	@Override
	protected KDTreeAdapter<DataPoint> createHitTreeAdapter(int dimensions,
			int maxSize) {
		return new RedKDTreeAdapter(dimensions, bucketSize);
	}

	@Override
	protected KDTreeAdapter<DataPoint> createVisitTreeAdapter(int dimensions,
			int maxSize) {
		return new RedKDTreeAdapter(dimensions, bucketSize);
	}

}
