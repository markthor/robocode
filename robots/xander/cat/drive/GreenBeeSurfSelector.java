package xander.cat.drive;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;

import xander.core.Resources;
import xander.core.drive.DirectDrivePredictor;
import xander.core.drive.DistancingEquation;
import xander.core.drive.DriveState;
import xander.core.event.PaintListener;
import xander.core.log.Log;
import xander.core.log.Logger;
import xander.core.math.RCMath;
import xander.core.math.RCPhysics;
import xander.core.track.Snapshot;
import xander.core.track.Wave;
import xander.gfws.BasicFactorArrays;
import xander.gfws.FactorIndexer;
import xander.gfws.IndexRange;
import xander.gfws.RelativeAngleRange;
import xander.gfws.drive.DirectSurfSelection;
import xander.gfws.drive.DirectSurfSelector;
import xander.gfws.processor.FactorArrayProcessor;

/**
 * The "Green Bee" surf selector.
 * 
 * Rough edges:
 * 1)  Distance calculations for robot width consideration are fixed at a single distance
 * 2)  Opponent drive prediction is absurdly simple
 *  
 * @author Scott Arnold
 */
public class GreenBeeSurfSelector implements DirectSurfSelector, PaintListener {

	private static final Log log = Logger.getLog(GreenBeeSurfSelector.class);
	private static final int DTA = 30;  // directional test angles
	private static final double TI = 180d / DTA;  // test angle increment
	
	private static class WeightedIndex implements Comparable<WeightedIndex> {
		double weight;
		int index;
		@Override
		public int compareTo(WeightedIndex other) {
			double diff = weight - other.weight;
			if (diff < 0) {
				return -1;
			} else if (diff > 0) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	private FactorArrayProcessor factorArrayProcessor;
	private double[][] cwTestAngles;
	private double[][] ccwTestAngles;
	private DistancingEquation de;   // optimal distance from opponent
	private WeightedIndex[] weightedIndicies;
	private int[] factorIndicies;
	private FactorIndexer factorIndexer;
	private double allowedPercentMEALossForDistancing = 0.20;
	
	// surf paint variables
	private double[] surfArray;
	private double minFactorAngle;
	private double maxFactorAngle;
	
//	private RunTimeLogger s1RTL;
//	private RunTimeLogger s2RTL;
//	private RunTimeLogger s3RTL;
//	private RunTimeLogger s4RTL;
//	private RunTimeLogger s5RTL;
//	private RunTimeLogger faRTL;
//	private RunTimeLogger fRTL;
	
	public GreenBeeSurfSelector(FactorArrayProcessor factorArrayProcessor, DistancingEquation de) {
//		s1RTL = RunTimeLogger.getLoggerFor("Setup Test Angles");
//		s2RTL = RunTimeLogger.getLoggerFor("Maximum MEA");
//		s3RTL = RunTimeLogger.getLoggerFor("Apply Distancing");
//		s4RTL = RunTimeLogger.getLoggerFor("Distancing MEA");
//		s5RTL = RunTimeLogger.getLoggerFor("Relax Distancing");
//		faRTL = RunTimeLogger.getLoggerFor("Get Factor Array");
//		fRTL = RunTimeLogger.getLoggerFor("Pick Factor Index");
		this.factorArrayProcessor = factorArrayProcessor;
		this.de = de;
		// first dimension is test point
		// second dimension is test angle, reachable factor angle, and end position x and y
		this.cwTestAngles = new double[DTA][4];
		this.ccwTestAngles = new double[DTA][4];
		this.factorIndexer = Resources.getMyFactorIndexer();
	}
	
	public double getAllowedPercentMEALossForDistancing() {
		return allowedPercentMEALossForDistancing;
	}

	/**
	 * Set the allowed percent escape angle loss for the sake of preferred distancing.  Number should be 
	 * in range 0.0 to 1.0.
	 * 
	 * @param allowedPercentMEALossForDistancing
	 */
	public void setAllowedPercentMEALossForDistancing(double allowedPercentMEALossForDistancing) {
		this.allowedPercentMEALossForDistancing = allowedPercentMEALossForDistancing;
	}

	@Override
	public void onPaint(Graphics2D g) {
		g.setColor(Color.BLUE);
		Ellipse2D.Double point = new Ellipse2D.Double();
		for (int i=0; i<cwTestAngles.length; i++) {
			point.x = cwTestAngles[i][2]-1;
			point.y = cwTestAngles[i][3]-1;
			point.width = 2;
			point.height = 2;
			g.draw(point);
		}
		for (int i=0; i<ccwTestAngles.length; i++) {
			point.x = ccwTestAngles[i][2]-1;
			point.y = ccwTestAngles[i][3]-1;
			point.width = 2;
			point.height = 2;
			g.draw(point);
		}
	}

	@Override
	public void updateSurfSelection(DirectSurfSelection[] surfSelections,
			Wave surfWave, DirectDrivePredictor predictor, 
			DriveState myDriveState, Snapshot currentOpponentSnapshot, long fromTime) {
		long currentTime = fromTime;
		if (factorIndicies == null || factorIndicies.length != surfSelections.length) {
			factorIndicies = new int[surfSelections.length];
		}
		
		// setup which test angles are clockwise and which are counter-clockwise
//		s1RTL.start();
		double inHeading = RCMath.getRobocodeAngle(myDriveState.getPosition(), surfWave.getOrigin());
		for (int i=0; i<DTA; i++) {
			ccwTestAngles[i][0] = RCMath.normalizeDegrees(inHeading+(i+0.5d)*TI);
			cwTestAngles[i][0] = RCMath.normalizeDegrees(inHeading+180+(i+0.5d)*TI);
		}
//		s1RTL.stop();
		
		// determine greatest reachable factors and distances for all test angles, note greatest
//		s2RTL.start();
		int greatestCWIndex = -1;
		int greatestCCWIndex = -1;
		for (int i=0; i<cwTestAngles.length; i++) {
			DriveState testDriveState = predictor.predictDriveStateUntilWaveHits(surfWave, myDriveState, cwTestAngles[i][0], RCPhysics.MAX_SPEED, currentTime);
			cwTestAngles[i][1] = BasicFactorArrays.getFactorAngle(surfWave, testDriveState.getPosition());
			cwTestAngles[i][2] = testDriveState.getX();
			cwTestAngles[i][3] = testDriveState.getY();
			if (greatestCWIndex == -1 || cwTestAngles[i][1] > cwTestAngles[greatestCWIndex][1]) {
				greatestCWIndex = i;
			}
			testDriveState = predictor.predictDriveStateUntilWaveHits(surfWave, myDriveState, ccwTestAngles[i][0], RCPhysics.MAX_SPEED, currentTime);
			ccwTestAngles[i][1] = BasicFactorArrays.getFactorAngle(surfWave, testDriveState.getPosition());
			ccwTestAngles[i][2] = testDriveState.getX();
			ccwTestAngles[i][3] = testDriveState.getY();
			if (greatestCCWIndex == -1 || ccwTestAngles[i][1] < ccwTestAngles[greatestCCWIndex][1]) {
				greatestCCWIndex = i;
			}
		}
		RelativeAngleRange currentMEA = new RelativeAngleRange(ccwTestAngles[greatestCCWIndex][1], cwTestAngles[greatestCWIndex][1], "GreenBee");
//		s2RTL.stop();
		
		// apply initial distancing, setting up restricted test ranges
//		s3RTL.start();
		double[] oppXYShift = currentOpponentSnapshot.getXYShift();
		long cwTimeToHit = surfWave.getTimeUntilHit(cwTestAngles[greatestCWIndex][2], cwTestAngles[greatestCWIndex][3], currentTime);
		double oppX = currentOpponentSnapshot.getX() + oppXYShift[0]/2*cwTimeToHit;
		double oppY = currentOpponentSnapshot.getY() + oppXYShift[1]/2*cwTimeToHit;
		double oppDistance = RCMath.getDistanceBetweenPoints(oppX, oppY, cwTestAngles[greatestCWIndex][2], cwTestAngles[greatestCWIndex][3]);
		double cwAdjustAngle = de.getAdjustAngle(oppDistance, surfWave);
		int cwIndexShift = (int)Math.round(-cwAdjustAngle / TI);
		int cwBeginIndex = greatestCWIndex;
		int cwEndIndex = greatestCWIndex;
		if (cwAdjustAngle < 0) {
			int maxCWBeginIndex = (int)Math.round(DTA*0.5d + de.getMaxAdvanceAngle()/TI);
			cwBeginIndex = Math.max(0, Math.min(maxCWBeginIndex, greatestCWIndex + cwIndexShift));
			cwEndIndex = DTA-1;
		} else if (cwAdjustAngle > 0) {
			int minCWEndIndex = (int)Math.round(DTA*0.5d - de.getMaxRetreatAngle()/TI);
			cwBeginIndex = 0;
			cwEndIndex = Math.min(DTA-1, Math.max(minCWEndIndex, greatestCWIndex + cwIndexShift));
		}
		long ccwTimeToHit = surfWave.getTimeUntilHit(ccwTestAngles[greatestCCWIndex][2], ccwTestAngles[greatestCCWIndex][3], currentTime);
		oppX = currentOpponentSnapshot.getX() + oppXYShift[0]/2*ccwTimeToHit;
		oppY = currentOpponentSnapshot.getY() + oppXYShift[1]/2*ccwTimeToHit;
		oppDistance = RCMath.getDistanceBetweenPoints(oppX, oppY, ccwTestAngles[greatestCCWIndex][2], ccwTestAngles[greatestCCWIndex][3]);
		double ccwAdjustAngle = de.getAdjustAngle(oppDistance, surfWave);
		int ccwIndexShift = (int)Math.round(cwAdjustAngle / TI);
		int ccwBeginIndex = greatestCCWIndex;
		int ccwEndIndex = greatestCCWIndex;
		if (ccwAdjustAngle < 0) {
			int minCCWEndIndex = (int)Math.round(DTA*0.5d - de.getMaxAdvanceAngle()/TI);
			ccwBeginIndex = 0;
			ccwEndIndex = Math.min(DTA-1, Math.max(minCCWEndIndex, greatestCCWIndex + ccwIndexShift));
		} else if (ccwAdjustAngle > 0) {
			int maxCCWBeginIndex = (int)Math.round(DTA*0.5d + de.getMaxRetreatAngle()/TI);
			ccwBeginIndex = Math.max(0, Math.min(maxCCWBeginIndex, greatestCCWIndex + ccwIndexShift));
			ccwEndIndex = DTA-1;
		}
//		s3RTL.stop();
		
		// determine factor range after initial distancing
//		s4RTL.start();
		minFactorAngle = 360;
		maxFactorAngle = -360;
		for (int i=ccwBeginIndex; i<=ccwEndIndex; i++) {
			minFactorAngle = Math.min(minFactorAngle, ccwTestAngles[i][1]);
		}
		for (int i=cwBeginIndex; i<=cwEndIndex; i++) {
			maxFactorAngle = Math.max(maxFactorAngle, cwTestAngles[i][1]);
		}
		if (minFactorAngle > maxFactorAngle) {
			log.warn("Possible error in surf selection- minFactorAngle > maxFactorAngle; reversing to prevent bad things from happening.");
			double temp = minFactorAngle;
			minFactorAngle = maxFactorAngle;
			maxFactorAngle = temp;
		}
//		s4RTL.stop();
		
		// relax distancing if it reduces factor range excessively
//		s5RTL.start();
		double maxRange = cwTestAngles[greatestCWIndex][1] - ccwTestAngles[greatestCCWIndex][1];
		double distancedRange = maxFactorAngle - minFactorAngle;
		boolean rangeExtendable = true;
		while (rangeExtendable && !RCMath.differenceLessThanPercent(maxRange, distancedRange, allowedPercentMEALossForDistancing)) {
			rangeExtendable = false;
			if (ccwBeginIndex == 0 && ccwEndIndex < (DTA-1)) {
				rangeExtendable = true;
				ccwEndIndex++;
				minFactorAngle = Math.min(minFactorAngle, ccwTestAngles[ccwEndIndex][1]);
			} else if (ccwBeginIndex > 0){
				rangeExtendable = true;
				ccwBeginIndex--;
				minFactorAngle = Math.min(minFactorAngle, ccwTestAngles[ccwBeginIndex][1]);
			}
			if (cwBeginIndex == 0 && cwEndIndex < (DTA-1)) {
				rangeExtendable = true;
				cwEndIndex++;
				maxFactorAngle = Math.max(maxFactorAngle, cwTestAngles[cwEndIndex][1]);
			} else if (cwBeginIndex > 0){
				rangeExtendable = true;
				cwBeginIndex--;
				maxFactorAngle = Math.max(maxFactorAngle, cwTestAngles[cwBeginIndex][1]);
			}
			if (minFactorAngle > maxFactorAngle) {
				log.warn("Possible error in surf selection- minFactorAngle > maxFactorAngle; reversing to prevent bad things from happening.");
				double temp = minFactorAngle;
				minFactorAngle = maxFactorAngle;
				maxFactorAngle = temp;
			}
			distancedRange = maxFactorAngle - minFactorAngle;
		}
//		s5RTL.stop();
		
		// get factor array to surf
//		faRTL.start();
		this.surfArray = factorArrayProcessor.getFactorArray(surfWave, currentMEA.getCounterClockwiseOffset(), currentMEA.getClockwiseOffset());
//		faRTL.stop();
		
		// prep the weighted indicies
		if (this.weightedIndicies == null || this.weightedIndicies.length != surfArray.length) {
			this.weightedIndicies = new WeightedIndex[surfArray.length];
			for (int i=0; i<weightedIndicies.length; i++) {
				this.weightedIndicies[i] = new WeightedIndex();
			}
		}
		for (int i=0; i<surfArray.length; i++) {
			weightedIndicies[i].index = i;
			weightedIndicies[i].weight = Double.MAX_VALUE;  
		}
		
		// decide on which factor to go to
//		fRTL.start();
		double minFactorAngleIndex = factorIndexer.getPreciseFactorIndex(minFactorAngle, surfArray.length, surfWave.getSurfDirection(), surfWave.getInitialMEA());
		double maxFactorAngleIndex = factorIndexer.getPreciseFactorIndex(maxFactorAngle, surfArray.length, surfWave.getSurfDirection(), surfWave.getInitialMEA());
		int minIndex = (int)Math.round(Math.floor(Math.min(minFactorAngleIndex, maxFactorAngleIndex)));
		int maxIndex = (int)Math.round(Math.ceil(Math.max(minFactorAngleIndex, maxFactorAngleIndex)));
		minIndex = Math.min(surfArray.length-1, Math.max(0, minIndex));
		maxIndex = Math.min(surfArray.length-1, Math.max(0, maxIndex));
		double robotDistance = RCMath.getDistanceBetweenPoints(surfWave.getOrigin(), myDriveState.getPosition());
		
		double avgRobotIndexWidth = 0;
		for (int i=minIndex; i<=maxIndex; i++) {
			double robotFactorAngle = factorIndexer.getFactorAngle(i, surfArray.length, surfWave.getSurfDirection(), surfWave.getInitialMEA());
			IndexRange robotWidthIndexRange = factorIndexer.getRobotIndexWidth(surfArray.length, robotDistance, robotFactorAngle, surfWave.getSurfDirection(), surfWave.getInitialMEA());
			avgRobotIndexWidth += (robotWidthIndexRange.getPreciseMaxIndex() - robotWidthIndexRange.getPreciseMinIndex());
			weightedIndicies[i].weight = getDangerForIndexRange(robotWidthIndexRange);
		}
		avgRobotIndexWidth /= (maxIndex-minIndex+1);
		Arrays.sort(weightedIndicies);  // sorts dangers from lowest to highest
		int surfSelectionIndex = 0;
		int weightedIndiciesIndex = 0;
		while (surfSelectionIndex < surfSelections.length 
				&& weightedIndiciesIndex < weightedIndicies.length
				&& weightedIndicies[weightedIndiciesIndex].weight < Double.MAX_VALUE) {
			int factorIndex = weightedIndicies[weightedIndiciesIndex].index;
			if (factorIndex >= minIndex && factorIndex <= maxIndex) {
				boolean sufficientSeparation = true;
				for (int i=0; i<surfSelectionIndex; i++) {
					if (Math.abs(factorIndicies[i] - factorIndex) < avgRobotIndexWidth) {
						sufficientSeparation = false;
					}
				}
				if (sufficientSeparation) {
					updateSurfSelection(surfSelections[surfSelectionIndex], 
							surfSelectionIndex,
							factorIndex,
							weightedIndicies[weightedIndiciesIndex].weight,
							surfWave, surfArray, 
							minFactorAngle, maxFactorAngle, 
							cwBeginIndex, cwEndIndex, ccwBeginIndex, ccwEndIndex, 
							fromTime, myDriveState, predictor, 
							currentOpponentSnapshot);
					surfSelectionIndex++;
				}
			}
			weightedIndiciesIndex++;
		}
//		fRTL.stop();
	}

	private double getDangerForIndexRange(IndexRange indexRange) {
		double b = Math.max(indexRange.getPreciseMinIndex(), 0);
		double e = Math.min(indexRange.getPreciseMaxIndex(), surfArray.length-1);
		double fullRange = indexRange.getPreciseMaxIndex() - indexRange.getPreciseMinIndex();
		double limRange = e - b;
		int cb = (int) Math.round(Math.ceil(b));
		int fb = (int) Math.round(Math.floor(b));
		int fe = (int) Math.round(Math.floor(e));
		double danger = 0;
		for (int i=cb; i<fe; i++) {
			danger += surfArray[i];
		}
		danger += (cb-b)*surfArray[fb];
		danger += (e-fe)*surfArray[fe];
		danger *= (fullRange / limRange);
		return danger;
	}
	
	private void updateSurfSelection(DirectSurfSelection surfSelection, int surfSelectionIndex,
			int factorIndex, double danger, Wave surfWave,
			double[] surfArray, double minFactorAngle, double maxFactorAngle,
			int cwBeginIndex, int cwEndIndex, int ccwBeginIndex, int ccwEndIndex,
			long fromTime, DriveState myDriveState,	DirectDrivePredictor predictor, 
			Snapshot currentOpponentSnapshot) {
		double factorAngle = factorIndexer.getFactorAngle(factorIndex, surfArray.length, surfWave.getSurfDirection(), surfWave.getInitialMEA());
		
		// factor angle can be slightly beyond range, so limit it to be within range
		factorAngle = RCMath.limit(factorAngle, minFactorAngle, maxFactorAngle);
		
		DriveState finalDriveState = new DriveState();
		double targetHeading = getTargetHeading(surfWave, cwBeginIndex, cwEndIndex, 
				ccwBeginIndex, ccwEndIndex, factorAngle, 
				fromTime, myDriveState, finalDriveState, 
				predictor, currentOpponentSnapshot);
		surfSelection.setSelection(finalDriveState, targetHeading, factorAngle, danger);
		factorIndicies[surfSelectionIndex] = factorIndex;
	}
	
	private double getTargetHeading(Wave surfWave, int cwBeginIndex, int cwEndIndex, 
			int ccwBeginIndex, int ccwEndIndex, double targetFactorAngle, long fromTime,
			DriveState myDriveState, DriveState finalDriveState, DirectDrivePredictor predictor, 
			Snapshot currentOpponentSnapshot) {
		DriveState dDriveState = new DriveState();
		double targetHeading = myDriveState.getHeading();
		double closestDistance = Double.MAX_VALUE;
		double[] oppXYShift = currentOpponentSnapshot.getXYShift();
		double currentFactorAngle = BasicFactorArrays.getFactorAngle(surfWave, myDriveState.getPosition());
		for (int i=cwBeginIndex; i<=cwEndIndex; i++) {
			if (RCMath.between(targetFactorAngle, currentFactorAngle, cwTestAngles[i][1])) {
				long time = fromTime;
				dDriveState.setState(myDriveState);
				long tuh = surfWave.getTimeUntilHit(dDriveState.getX(), dDriveState.getY(), time);
				while (tuh > 0 && !predictor.shouldStop(surfWave, targetFactorAngle, dDriveState, cwTestAngles[i][0], RCPhysics.MAX_SPEED, time)) {
					predictor.advanceDriveState(dDriveState, cwTestAngles[i][0], RCPhysics.MAX_SPEED);
					time++;
					tuh = surfWave.getTimeUntilHit(dDriveState.getX(), dDriveState.getY(), time);
				}
				while (tuh > 0 && Math.abs(dDriveState.getVelocity()) > 0) {
					predictor.advanceDriveState(dDriveState, cwTestAngles[i][0], 0);
					time++;
					tuh = surfWave.getTimeUntilHit(dDriveState.getX(), dDriveState.getY(), time);
				}
				tuh = surfWave.getTimeUntilHit(dDriveState.getX(), dDriveState.getY(), fromTime);
				double oppX = currentOpponentSnapshot.getX() + tuh * 0.5 * oppXYShift[0];
				double oppY = currentOpponentSnapshot.getY() + tuh * 0.5 * oppXYShift[1];
				double distance = Math.abs(de.getOptimalDistance() - RCMath.getDistanceBetweenPoints(dDriveState.getX(), dDriveState.getY(), oppX, oppY));
				if (distance < closestDistance) {
					closestDistance = distance;
					targetHeading = cwTestAngles[i][0];
					finalDriveState.setState(dDriveState);
				}				
			}
		}
		for (int i=ccwBeginIndex; i<=ccwEndIndex; i++) {
			if (RCMath.between(targetFactorAngle, currentFactorAngle, ccwTestAngles[i][1])) {
				long time = fromTime;
				dDriveState.setState(myDriveState);
				long tuh = surfWave.getTimeUntilHit(dDriveState.getX(), dDriveState.getY(), time);
				while (tuh > 0 && !predictor.shouldStop(surfWave, targetFactorAngle, dDriveState, ccwTestAngles[i][0], RCPhysics.MAX_SPEED, time)) {
					predictor.advanceDriveState(dDriveState, ccwTestAngles[i][0], RCPhysics.MAX_SPEED);
					time++;
					tuh = surfWave.getTimeUntilHit(dDriveState.getX(), dDriveState.getY(), time);
				}
				while (tuh > 0 && Math.abs(dDriveState.getVelocity()) > 0) {
					predictor.advanceDriveState(dDriveState, ccwTestAngles[i][0], 0);
					time++;
					tuh = surfWave.getTimeUntilHit(dDriveState.getX(), dDriveState.getY(), time);
				}
				tuh = surfWave.getTimeUntilHit(dDriveState.getX(), dDriveState.getY(), fromTime);
				double oppX = currentOpponentSnapshot.getX() + tuh * 0.5 * oppXYShift[0];
				double oppY = currentOpponentSnapshot.getY() + tuh * 0.5 * oppXYShift[1];
				double distance = Math.abs(de.getOptimalDistance() - RCMath.getDistanceBetweenPoints(dDriveState.getX(), dDriveState.getY(), oppX, oppY));
				if (distance < closestDistance) {
					closestDistance = distance;
					targetHeading = ccwTestAngles[i][0];
					finalDriveState.setState(dDriveState);
				}				
			}
		}	
		return targetHeading;
	}
}
