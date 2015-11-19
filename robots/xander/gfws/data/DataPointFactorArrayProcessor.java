package xander.gfws.data;

import xander.core.Resources;
import xander.core.math.RCMath;
import xander.core.track.BulletShadow;
import xander.core.track.Wave;
import xander.gfws.BasicFactorArrays;
import xander.gfws.FactorIndexer;
import xander.paint.Paintable;
import xander.paint.Paintables;

/**
 * Factor Array Processor that builds factor arrays from lists of DataPoint objects
 * provided by a LogReader.
 * 
 * @author Scott Arnold
 */
public class DataPointFactorArrayProcessor extends CachingFactorArrayProcessor implements Paintable {
	
	private String painterName;
	private int factors;
	private FactorArrayParms parms;
	private LogReader logReader;
	private int flattenerOn;
	private int flattenerOff;
	private double myHitRatio;
	private double oppHitRatio;
	private FactorIndexer factorIndexer;

	public DataPointFactorArrayProcessor(LogReader logReader, int factors, FactorArrayParms parms) {
		this(null, logReader, factors, parms);
	}
	
	public DataPointFactorArrayProcessor(String painterName, LogReader logReader, int factors, FactorArrayParms parms) {
		super(factors);
		this.painterName = painterName;
		this.logReader = logReader;
		this.factors = factors;
		this.parms = parms;
		this.factorIndexer = parms.isOffensive()? Resources.getOpponentFactorIndexer() : Resources.getMyFactorIndexer();
		Paintables.addPaintable(this);
	}
	
	@Override
	public String getPainterName() {
		return painterName;
	}

	public double getFlattenerUsePercentage() {
		int totalCount = flattenerOn + flattenerOff;
		return (totalCount == 0)? 0 : (double)flattenerOn / (double)totalCount;
	}

	private void addFactorArrayPoint(double[] factorArray, FactorArrayParms parms, double cweight, DataPoint dataPoint, long referenceTime) {
		// adjust weight for decay
		if (parms.varyWeightByAge) {
			//cweight *= Math.max(parms.minAgeWeightPercent, 1 - parms.decayPerTick * (referenceTime-dataPoint.logTime));
			long age = referenceTime - dataPoint.logTime;
			cweight *= Math.max(parms.minAgeWeightPercent, parms.varyWeightByAgeFunction.getMultiplier(age));
		}
		if (parms.varyWeightByDistance) {
			cweight *= dataPoint.distance / 400d;
		}
		if (parms.varyWeightByMEA) {
			cweight *= RCMath.getMEASignificance(dataPoint.mea, dataPoint.distance);
		}
		double preciseFactorIndex = dataPoint.getFactorIndex(factors);
		parms.distributer.addWeight(factorArray, preciseFactorIndex, cweight, 
				dataPoint.distance, dataPoint.bulletVelocity, dataPoint.surfDirection);		
	}
	
	@Override
	public void fillFactorArray(double[] factorArray, Wave surfWave, double currentMinFactorAngle, double currentMaxFactorAngle) {
		long cumulativeTime = Resources.getCumulativeTime();

		//log.info("Getting factor array for segments: " + Logger.format(segmentIndexes));
		//log.info("k value is " + k);
		
		WaveSearchIterable<DataPoint> knnHitData = logReader.getHitData(surfWave);
		//log.info(knnHitData.size() + " data points retreived.");
		int counter = 0;
		this.oppHitRatio = Resources.getGunStats().getOverallOpponentHitRatio();
		this.myHitRatio = Resources.getGunStats().getOverallHitRatio();
		boolean useFlattenerThisWave = parms.useFlattener 
				&& oppHitRatio > parms.useFlattenerOnHitPercentage
				&& myHitRatio < oppHitRatio
				&& Resources.getRobotProxy().getRoundNum() >= parms.useFlattenerEarliestRound;
		if (useFlattenerThisWave) {
			parms.flattenerOnCount++;
			flattenerOn++;
		} else {
			parms.flattenerOffCount++;
			flattenerOff++;
		}
		long referenceTime = 0;
		for (WaveSearchIterator<DataPoint> iter = knnHitData.iterator(); iter.hasNext();) {
			DataPoint dataPoint = iter.next();
			referenceTime = Math.max(referenceTime, dataPoint.logTime);
		}
		double maxDistance = knnHitData.getMaxDistance();
		if (parms.varyWeightBySearchDistanceMinMaxDistance != null) {
			maxDistance = Math.max(maxDistance, parms.varyWeightBySearchDistanceMinMaxDistance.doubleValue());
		}
		double maxDrop = 1d - parms.varyWeightBySearchDistanceMinimum;
		for (WaveSearchIterator<DataPoint> iter = knnHitData.iterator(); iter.hasNext();) {
			DataPoint dataPoint = iter.next();
			double weight = parms.weight;
			if (parms.varyWeightBySearchDistance) {
				weight *= (1d - maxDrop * (iter.getSearchDistance() / maxDistance));
			}
			addFactorArrayPoint(factorArray, parms, weight, dataPoint, referenceTime);
			counter++;
		}
		if (useFlattenerThisWave) {
			double cweight = parms.weight * parms.useFlattenerVisitPercentage;
			WaveSearchIterable<DataPoint> knnVisitData = logReader.getVisitData(surfWave);
			for (WaveSearchIterator<DataPoint> iter = knnVisitData.iterator(); iter.hasNext();) {
				DataPoint visitDataPoint = iter.next();
				addFactorArrayPoint(factorArray, parms, cweight, visitDataPoint, cumulativeTime);
			} 
		}
		
		if (counter <= parms.maxDataPointsForUsingArrayInitializer) {
			parms.arrayInitializer.modify(factorArray, surfWave);
			counter++;			
		}
		
		// process bullet shadows
		if (parms.useBulletShadows) {
			for (BulletShadow shadow : surfWave.getBulletShadows()) {
				double ccwShadowFactorAngle = BasicFactorArrays.getFactorAngle(surfWave, shadow.getCounterClockwiseAngle());
				double cwShadowFactorAngle = BasicFactorArrays.getFactorAngle(surfWave, shadow.getClockwiseAngle());
				double ccwShadowIndex = 0;
				double cwShadowIndex = 0;
				ccwShadowIndex = factorIndexer.getPreciseFactorIndex(
						ccwShadowFactorAngle, factorArray.length, 
						surfWave.getSurfDirection(), surfWave.getInitialMEA());
				cwShadowIndex = factorIndexer.getPreciseFactorIndex(
						cwShadowFactorAngle, factorArray.length, 
						surfWave.getSurfDirection(), surfWave.getInitialMEA());					
				double pBeginIndex = (cwShadowIndex > ccwShadowIndex)? ccwShadowIndex : cwShadowIndex;
				double pEndIndex = (cwShadowIndex > ccwShadowIndex)? cwShadowIndex : ccwShadowIndex;
				int beginIndex = (int)Math.round(Math.ceil(pBeginIndex));
				int endIndex = (int)Math.round(Math.floor(pEndIndex));
				for (int i=beginIndex; i<endIndex; i++) {
					if (i >= 0 && i < factorArray.length) {
						factorArray[i] = 0;
					}
				}
				if (beginIndex-1 >= 0 && beginIndex-1 < factorArray.length) {
					double f = 1 - (beginIndex - pBeginIndex);
					factorArray[beginIndex-1] *= f;
				}
				if (endIndex >= 0 && endIndex < factorArray.length) {
					double f = 1 - (pEndIndex - endIndex);
					factorArray[endIndex] *= f;
				}
			}
		}
		//log.info(counter + " data points used in array.");
		//log.info("Match percents: " + sb.toString());
	}
}
