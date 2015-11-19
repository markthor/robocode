package xander.cat.processor;

import xander.core.Resources;
import xander.core.gun.detect.TargetingDetector;
import xander.core.track.Wave;
import xander.gfws.BasicFactorArrays;
import xander.gfws.FactorIndexer;
import xander.gfws.distributer.WaveDistributer;
import xander.gfws.distributer.WeightDistributer;
import xander.gfws.processor.FactorArrayProcessor;
import xander.paint.Paintables;

public class TargetingDetectorFactorArrayProcessor implements FactorArrayProcessor {

	private String painterName;
	private int factors;
	private TargetingDetector[] targetingDetectors;
	private WeightDistributer distributer = new WaveDistributer();
	private int testShotsThreshold = 2;   // per detector
	private FactorIndexer factorIndexer;
	
	public TargetingDetectorFactorArrayProcessor(String painterName, int factors, TargetingDetector... targetingDetectors) {
		this.painterName = painterName;
		this.factors = factors;
		this.targetingDetectors = targetingDetectors;
		this.factorIndexer = Resources.getOpponentFactorIndexer(); // this will only ever be offensive
		Paintables.addPaintable(this);
	}
	
	@Override
	public String getPainterName() {
		return painterName;
	}

	@Override
	public int getFactors() {
		return factors;
	}
	
	@Override
	public double[] getFactorArray(Wave surfWave, double currentMinFactorAngle, double currentMaxFactorAngle) {
		double[] factorArray = new double[factors];
		for (int i=0; i<targetingDetectors.length; i++) {
			double weight = targetingDetectors[i].getShotsTested() > testShotsThreshold? 
					Math.pow(targetingDetectors[i].getDetectionPercentage(), 2) :
					0.05;
			//System.out.println(targetingDetectors[i].getName() + ":" + Logger.format(100d*targetingDetectors[i].getDetectionPercentage()));
			if (weight > 0) {
				double angle = targetingDetectors[i].getDetectionAngle(surfWave);
				double factorAngle = BasicFactorArrays.getFactorAngle(surfWave, angle);
				double preciseIndex = factorIndexer.getPreciseFactorIndex(factorAngle, factors, surfWave.getSurfDirection(), surfWave.getInitialMEA());
				//TODO: What should we do if preciseIndex is out of factor array index bounds?  Should distributers be able to handle this?
				distributer.addWeight(factorArray, preciseIndex, weight, surfWave.getOriginDistance(), surfWave.getBulletVelocity(), surfWave.getSurfDirection());
			}
		}
		return factorArray;
	}

}
