package xander.gfws.data;

import java.util.Arrays;

import xander.core.log.Log;
import xander.core.log.Logger;
import xander.core.track.Wave;
import xander.gfws.segment.Segmenter;

/**
 * Wave logger that logs waves into a KD Tree.
 * 
 * @author Scott Arnold
 */
public abstract class KDTreeWaveLogger extends AbstractWaveLogger<KNNParms> {
	
	private static final Log log = Logger.getLog(KDTreeWaveLogger.class);
	
	private KDTreeAdapter<DataPoint> hitTree;
	private KDTreeAdapter<DataPoint> visitTree;
	private double[] lowRange;
	private double[] highRange;
	private double[] segmentWeights;
	
	public KDTreeWaveLogger(boolean offensive, Segmenter... segmenters) {
		this(offensive, Integer.MAX_VALUE, Integer.MAX_VALUE, segmenters);
	}
	
	public KDTreeWaveLogger(boolean offensive, int maxHits, int maxVisits, Segmenter... segmenters) {
		super(offensive, segmenters);
		this.hitTree = createHitTreeAdapter(segmenters.length, maxHits);
		this.visitTree = createVisitTreeAdapter(segmenters.length, maxVisits);
		this.lowRange = new double[segmenters.length];
		this.highRange = new double[segmenters.length];
		this.segmentWeights = new double[segmenters.length];
		Arrays.fill(segmentWeights, 1d);
	}

	protected abstract KDTreeAdapter<DataPoint> createHitTreeAdapter(int dimensions, int maxSize);
	
	protected abstract KDTreeAdapter<DataPoint> createVisitTreeAdapter(int dimensions, int maxSize);
	
	public void setSegmentWeight(int segmentIndex, double weight) {
		this.segmentWeights[segmentIndex] = weight;
	}
	
	private double[] createKey(Wave wave, Segmenter[] segmenters) {
		double[] key = new double[segmenters.length];
		for (int i=0; i<segmenters.length; i++) {
			key[i] = segmenters[i].getSegmentValuePercent(wave) * segmentWeights[i];
		}	
		return key;
	}
	
	private void setRanges(double[] key, double[] rangeSpan) {
		for (int i=0; i<rangeSpan.length; i++) {
			double halfSpan = rangeSpan[i] / 2d;
			lowRange[i] = key[i]-halfSpan;
			highRange[i] = key[i]+halfSpan;
		}
	}
	
	@Override
	public WaveSearchIterable<DataPoint> getHitData(Wave wave, Segmenter[] segmenters, KNNParms parms) {
		double[] key = createKey(wave, segmenters);
		WaveSearchIterable<DataPoint> searchResult = null;
		try {
			if (parms.isSelectByKNN()) {
				int n = Math.min(hitTree.size(), parms.getK(totalHits));
				if (n > 0) {
					searchResult = hitTree.searchByKNN(key, n);
				}
			} else if (parms.isSelectByHitsWithinKNNVisits()) {
				if (hitTree.size() > 0) {
					searchResult = hitTree.searchByDistance(key, parms.visitDistance);
					log.debug(searchResult.size() + " hits pulled.");
				}
			} else {
				setRanges(key, parms.getRangeSpan());
				searchResult = hitTree.searchByRange(lowRange, highRange);
				log.debug(searchResult.size() + " hits pulled.");
			}
			if ((searchResult == null || searchResult.size() == 0)
					&& hitTree.size() > 0 && parms.getKnnWhenSelectionEmpty() > 0) {
				searchResult = hitTree.searchByKNN(key, parms.getKnnWhenSelectionEmpty());
			} 
		} catch (Exception e) {
			log.error("Error on getData: " + e.getMessage());
		}
		return (searchResult == null)? new EmptyWaveSearchIterable<DataPoint>() : searchResult;
	}

	@Override
	protected WaveSearchIterable<DataPoint> getVisitData(Wave wave, Segmenter[] segmenters, KNNParms parms) {
		double[] key = createKey(wave, segmenters);
		WaveSearchIterable<DataPoint> searchResult = null;
		try {
			if (parms.isSelectByKNN()) {
				int n = Math.min(visitTree.size(), parms.getVisitKAsPercentOfHitK(totalHits));
				if (n > 0) {
					searchResult = visitTree.searchByKNN(key, n);
				}
			} else if (parms.isSelectByHitsWithinKNNVisits()) {
				int n = Math.min(visitTree.size(), parms.getK(totalVisits));
				if (n > 0) {
					searchResult = visitTree.searchByKNN(key, n);
					log.debug(searchResult.size() + " visits pulled.");
				}
			} else {
				setRanges(key, parms.getRangeSpan());
				searchResult = hitTree.searchByRange(lowRange, highRange);
			}
			if ((searchResult == null || searchResult.size() == 0)
					&& visitTree.size() > 0 && parms.getKnnWhenSelectionEmpty() > 0) {
				searchResult = visitTree.searchByKNN(key, parms.getKnnWhenSelectionEmpty());
			}
		} catch (Exception e) {
			log.error("Error on getData: " + e.getMessage());
		} 
		return (searchResult == null)? new EmptyWaveSearchIterable<DataPoint>() : searchResult;
	}

	protected void addHit(Wave bulletWave, Segmenter[] segmenters, DataPoint dataPoint) {
		double[] key = createKey(bulletWave, segmenters);
		hitTree.addPoint(key, dataPoint);
	}
	
	protected void addVisit(Wave bulletWave, Segmenter[] segmenters, DataPoint dataPoint) {
		double[] key = createKey(bulletWave, segmenters);
		visitTree.addPoint(key, dataPoint);
	}
}
