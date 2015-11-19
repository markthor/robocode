package xander.gfws.data;

import xander.gfws.distributer.TriangleDistributer;
import xander.gfws.distributer.WaveDistributer;
import xander.gfws.distributer.WeightDistributer;
import xander.gfws.modifier.FactorArrayModifier;
import xander.gfws.modifier.HeadOnFactorArrayModifier;
import xander.gfws.modifier.LinearFactorArrayModifier;

/**
 * KNNFactorArrayprocessor parameters used to determine how to build a factor array
 * from a given set of data provided by a LogReader.
 * 
 * @author Scott Arnold
 */
public class FactorArrayParms {
	
	WeightDistributer distributer;
	double weight = 3d;
	FactorArrayModifier arrayInitializer;
	int maxDataPointsForUsingArrayInitializer = 0; // by default, use initializer when less than 2 data points
	boolean useFlattener = false;
	double useFlattenerOnHitPercentage;
	double useFlattenerVisitPercentage;
	int useFlattenerEarliestRound;
	int flattenerOnCount;
	int flattenerOffCount;
	boolean useBulletShadows = true;
	boolean varyWeightByAge = false;
	boolean varyWeightByDistance = false;
	boolean varyWeightBySearchDistance = false;
	double varyWeightBySearchDistanceMinimum = 0.1;  // minimum weight (e.g. 0.1 would mean weight could be reduced to 1/10)
	Double varyWeightBySearchDistanceMinMaxDistance = null;  // sets a minimum for max search distance used when computing weight
	AgeRollFunction varyWeightByAgeFunction;
	double minAgeWeightPercent = 0.01;
	boolean varyWeightByMEA = false;
	boolean offensive;
	
	public FactorArrayParms(boolean offensive) {
		this.offensive = offensive;
		if (offensive) {
			this.distributer = new TriangleDistributer();
			this.arrayInitializer = new LinearFactorArrayModifier(distributer, weight);
		} else {
			this.distributer = new WaveDistributer();
			this.arrayInitializer = new HeadOnFactorArrayModifier(distributer, weight);
		}
	}
	
	public boolean isOffensive() {
		return offensive;
	}
	
	public WeightDistributer getDistributer() {
		return distributer;
	}

	public void setDistributer(WeightDistributer distributer) {
		this.distributer = distributer;
	}

	public void setMinAgeWeightPercent(double minAgeWeightPercent) {
		this.minAgeWeightPercent = minAgeWeightPercent;
	}

	public void setUseFlattener(boolean useFlattener, double opponentHitPercentage, 
			double visitPercentage, int earliestRound) {
		this.useFlattener = useFlattener;
		this.useFlattenerOnHitPercentage = opponentHitPercentage;
		this.useFlattenerEarliestRound = earliestRound;
		this.useFlattenerVisitPercentage = visitPercentage;
	}
	
	public void setUseBulletShadows(boolean useBulletShadows) {
		this.useBulletShadows = useBulletShadows;
	}
	
	public void setVaryWeightByAgeFunction(AgeRollFunction varyWeightByAgeFunction) {
		this.varyWeightByAgeFunction = varyWeightByAgeFunction;
		this.varyWeightByAge = this.varyWeightByAgeFunction != null;
	}

	public AgeRollFunction getVaryWeightByAgeFunction() {
		return varyWeightByAgeFunction;
	}
	
	public boolean isVaryWeightByDistance() {
		return varyWeightByDistance;
	}

	public void setVaryWeightByDistance(boolean varyWeightByDistance) {
		this.varyWeightByDistance = varyWeightByDistance;
	}
	
	public void setArrayInitializer(FactorArrayModifier arrayInitializer) {
		this.arrayInitializer = arrayInitializer;
	}

	public void setMaxDataPointsForUsingArrayInitializer(
			int maxDataPointsForUsingArrayInitializer) {
		this.maxDataPointsForUsingArrayInitializer = maxDataPointsForUsingArrayInitializer;
	}

	public boolean isVaryWeightByMEA() {
		return varyWeightByMEA;
	}

	public void setVaryWeightByMEA(boolean varyWeightByMEA) {
		this.varyWeightByMEA = varyWeightByMEA;
	}

	public boolean isVaryWeightBySearchDistance() {
		return varyWeightBySearchDistance;
	}

	public void setVaryWeightBySearchDistance(boolean varyWeightBySearchDistance) {
		this.varyWeightBySearchDistance = varyWeightBySearchDistance;
	}
	
	public void setVaryWeightBySearchDistance(boolean varyWeightBySearchDistance, double minimumValueMultiplier) {
		this.varyWeightBySearchDistance = varyWeightBySearchDistance;
		this.varyWeightBySearchDistanceMinimum = minimumValueMultiplier;
	}
	
	public double getVaryWeightBySearchDistanceMinimum() {
		return varyWeightBySearchDistanceMinimum;
	}
	
	public void setVaryWeightBySearchDistanceMinMaxDistance(Double minMaxDistance) {
		this.varyWeightBySearchDistanceMinMaxDistance = minMaxDistance;
	}
}
