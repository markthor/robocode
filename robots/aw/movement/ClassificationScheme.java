package aw.movement;

import aw.Mallorn.tree.KDTreeOld;
import aw.utils.ClassificationWeightingScheme;

public class ClassificationScheme {

	private ClassificationWeightingScheme weightingScheme;
	private int maxTreeSize;
	private int maxBucketSize;
	private int numPointsForDensityCalcDivisor;
	private int maxNumPointsForDensityCalc;
	private double baseDenominatorValue;
	private double timeDecayExponent;
	private double botWidthAngleMultiplier;

	public ClassificationScheme(ClassificationWeightingScheme weightingScheme,
			int maxTreeSize, int maxBucketSize,
			int numPointsForDensityCalcDivisor, int maxNumPointForDensityCalc,
			double baseDenominatiorValue, double timeDecayExponent,
			double botWidthAngleMultiplier) {
		this.weightingScheme = weightingScheme;

		this.maxBucketSize = maxBucketSize;
		this.maxTreeSize = maxTreeSize;

		this.numPointsForDensityCalcDivisor = numPointsForDensityCalcDivisor;
		this.maxNumPointsForDensityCalc = maxNumPointForDensityCalc;
		this.baseDenominatorValue = baseDenominatiorValue;
		this.timeDecayExponent = timeDecayExponent;
		this.botWidthAngleMultiplier = botWidthAngleMultiplier;

	}

	public ClassificationWeightingScheme getWeightingScheme() {
		return weightingScheme;
	}

	public int getMaxTreeSize() {
		return maxTreeSize;
	}

	public int getMaxBucketSize() {
		return maxBucketSize;
	}

	public int getNumPointsForDensityCalcDivisor() {
		return numPointsForDensityCalcDivisor;
	}

	public int getMaxNumPointsForDensityCalc() {
		return maxNumPointsForDensityCalc;
	}

	public double getBaseDenominatorValue() {
		return baseDenominatorValue;
	}

	public double getTimeDecayExponent() {
		return timeDecayExponent;
	}

	public double getBotWidthAngleMultiplier() {
		return botWidthAngleMultiplier;
	}

}
