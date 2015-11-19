package voidious.utils;

import robocode.util.Utils;
import java.awt.geom.Point2D;

public class Wave {
	public static final int BINS = 59;
	public static final double MAX_GUESSFACTOR = 1.1;
	public boolean usedNegativeSmoothingMea = false;
	public boolean usedPositiveSmoothingMea = false;
	
	public BotScan targetScan;
	public Point2D.Double sourceLocation;
	public Point2D.Double originalTargetLocation;
	public long fireTime;
	public boolean firingWave;
	public double absBearingRadians;
	public double bulletPower;
	public String targetName;
	public boolean preciseEscapeAngle;
	public double targetHeadingRadians;
	public double targetVelocity;
	public int orientation;
	public boolean processedWaveBreak = false;
	public boolean processedBulletHit = false;
	private double _cachedPositiveEscapeAngle = NO_CACHED_ESCAPE_ANGLE;
	private double _cachedNegativeEscapeAngle = NO_CACHED_ESCAPE_ANGLE;
	
	public static final boolean FIRING_WAVE = true;
	public static final boolean NON_FIRING_WAVE = false;
	public static final boolean PRECISE_MAX_ESCAPE_ANGLE = true;
	public static final boolean TRADITIONAL_MAX_ESCAPE_ANGLE = false;
	public static final boolean POSITIVE_GUESSFACTOR = true;
	public static final boolean NEGATIVE_GUESSFACTOR = false;
	public static final double INACTIVE_WAVE_OFFSET = -150;
	public static final double NO_CACHED_ESCAPE_ANGLE = -1;
	
	public Wave(BotScan scan, Point2D.Double source, Point2D.Double target, 
		long time, boolean firing, double absBearing, double power, 
		String name, boolean precise, double heading, double velocity, 
		int lastOrientation, double lastOrientationVelocityThreshold) {

		targetScan = scan;
		sourceLocation = source;
		originalTargetLocation = target;
		fireTime = time;
		firingWave = firing;
		absBearingRadians = absBearing;
		bulletPower = power;
		targetName = name;
		preciseEscapeAngle = precise;
		targetHeadingRadians = heading;
		targetVelocity = velocity;
		
		setOrientation(lastOrientation, lastOrientationVelocityThreshold);
	}

	public double bulletVelocity() {
		return (20 - (3 * bulletPower));
	}
	
	public boolean wavePassed(Point2D.Double enemyLocation, long currentTime) {
		double defaultWaveInterceptOffset = 1.5 * bulletVelocity();

		return wavePassed(enemyLocation, currentTime, 
			defaultWaveInterceptOffset);
	}

	public boolean wavePassed(Point2D.Double enemyLocation, long currentTime,
		double interceptOffset) {
		
		if (enemyLocation.distanceSq(sourceLocation) < DUtils.square(
				bulletVelocity() * (currentTime - fireTime) 
				+ interceptOffset)) {
			return true;
		} else {
			return false;
		}
	}

	public GuessFactorWindowSet guessFactorWindow(Point2D.Double targetLocation) {
		double newBearingToTarget = 
			DUtils.absoluteBearing(sourceLocation, targetLocation);
		double distanceToTarget =
			sourceLocation.distance(targetLocation);
		double windowOffsetRadians = DUtils.botWidthAimAngle(distanceToTarget);
		
		double guessAngle = orientation * Utils.normalRelativeAngle( 
			newBearingToTarget - absBearingRadians);
		double guessAngleLow = orientation * Utils.normalRelativeAngle(
			newBearingToTarget - absBearingRadians) - windowOffsetRadians;
		double guessAngleHigh = orientation * Utils.normalRelativeAngle(
			newBearingToTarget - absBearingRadians) + windowOffsetRadians;
		double maxEscapeAngle = maxEscapeAngle(guessAngle > 0 ? 
			POSITIVE_GUESSFACTOR : NEGATIVE_GUESSFACTOR);
		
		return new GuessFactorWindowSet(
			guessAngle / maxEscapeAngle(guessAngle > 0 ? 
				POSITIVE_GUESSFACTOR : NEGATIVE_GUESSFACTOR),
			guessAngleLow / maxEscapeAngle(guessAngleLow > 0 ? 
				POSITIVE_GUESSFACTOR : NEGATIVE_GUESSFACTOR), 
			guessAngleHigh / maxEscapeAngle(guessAngleHigh > 0 ? 
				POSITIVE_GUESSFACTOR : NEGATIVE_GUESSFACTOR));
	}

	public double guessFactor(Point2D.Double targetLocation) {
		double guessAngle = orientation * Utils.normalRelativeAngle( 
			(DUtils.absoluteBearing(sourceLocation, targetLocation) -
			absBearingRadians));
		
		return guessAngle / maxEscapeAngle(guessAngle > 0 ? 
			POSITIVE_GUESSFACTOR : NEGATIVE_GUESSFACTOR);		
	}
	
	public int guessFactorIndex(Point2D.Double targetLocation) {
		double guessFactor = guessFactor(targetLocation);
		
        return guessFactorIndex(guessFactor);
	}

	public int guessFactorIndex(double guessFactor) {
        return (int)DUtils.limit(0, Math.round(guessFactor * ((BINS-1)/2))
                + ((BINS-1)/2), BINS - 1);
	}

	public double guessAngleFromIndex(int gfIndex) {
        return orientation * (((double)gfIndex - ((BINS-1)/2)) / ((BINS-1)/2))
        	* maxEscapeAngle(gfIndex > ((BINS-1)/2) ?
        		POSITIVE_GUESSFACTOR : NEGATIVE_GUESSFACTOR);
	}

	public double maxEscapeAngle(boolean positiveGuessFactor) {
		if (preciseEscapeAngle) {
			return preciseEscapeAngle(positiveGuessFactor);
		} else {
			return Math.asin(8 / bulletVelocity()) * MAX_GUESSFACTOR;
		}
	}
	
	public double escapeAngleRange() {
		return (preciseEscapeAngle(POSITIVE_GUESSFACTOR) 
				+ preciseEscapeAngle(NEGATIVE_GUESSFACTOR));
	}
	
	public double preciseEscapeAngle(boolean positiveGuessFactor) {
		if (positiveGuessFactor) {
			if (_cachedPositiveEscapeAngle == NO_CACHED_ESCAPE_ANGLE) {
				_cachedPositiveEscapeAngle =
					uncachedPreciseEscapeAngle(positiveGuessFactor) 
						* MAX_GUESSFACTOR;
			} 
			return _cachedPositiveEscapeAngle;
		} else {
			if (_cachedNegativeEscapeAngle == NO_CACHED_ESCAPE_ANGLE) {
				_cachedNegativeEscapeAngle =
					uncachedPreciseEscapeAngle(positiveGuessFactor) 
						* MAX_GUESSFACTOR;
			} 
			return _cachedNegativeEscapeAngle;
		}
	}
	
	public double uncachedPreciseEscapeAngle(boolean positiveGuessFactor) {
		boolean hitWall = false;
		boolean wavePassed = false;
		
		RobotState predictedState = new RobotState( 
			(Point2D.Double)originalTargetLocation.clone(),
			targetHeadingRadians, targetVelocity);
		long predictedTime = fireTime;

		boolean clockwisePrediction =
			(orientation == 1 && positiveGuessFactor) ||
			(orientation == -1 && !positiveGuessFactor);
		
		double noSmoothingEscapeAngle = 0;
		double bulletVelocity = bulletVelocity();
		
		do {	
			predictedState = DUtils.nextPerpendicularLocation(
				predictedState.location, absBearingRadians, 
				predictedState.velocity, predictedState.heading, 
				clockwisePrediction, predictedTime, DUtils.IGNORE_WALLS);
			predictedTime = predictedState.time;
			
			if (!DUtils.battleField.contains(predictedState.location)) {
				hitWall = true;
			} else if (
				wavePassed(predictedState.location, 
					predictedTime, bulletVelocity)) {
				wavePassed = true;
			}
		} while (!hitWall && !wavePassed);

		noSmoothingEscapeAngle = 
			Math.abs(Utils.normalRelativeAngle(DUtils.absoluteBearing(
			sourceLocation, predictedState.location) - absBearingRadians));

		double withSmoothingEscapeAngle = 0;
		
		if (hitWall) {
			double wallSmoothingStick = 80;
			double purelyPerpendicularAttackAngle = 0;
			double fullVelocity = 8.0;
			double orbitAbsBearing = absBearingRadians;
			double bestSmoothingEscapeAngle = 0;
			
			for (int x = 0; x < 3; x++) {
				wavePassed = false;
				predictedState = new RobotState( 
					(Point2D.Double)originalTargetLocation.clone(),
					targetHeadingRadians, targetVelocity);
				predictedTime = fireTime;
				
				do {	
					predictedState = 
						DUtils.nextPerpendicularWallSmoothedLocation(
							predictedState.location, orbitAbsBearing, 
							predictedState.velocity, fullVelocity, 
							predictedState.heading,	
							purelyPerpendicularAttackAngle,	clockwisePrediction,
							predictedTime, DUtils.battleField, 
							DUtils.battleFieldWidth, DUtils.battleFieldHeight, 
							wallSmoothingStick, DUtils.OBSERVE_WALL_HITS);
					predictedTime = predictedState.time;
	//				orbitAbsBearing = DUtils.absoluteBearing(
	//					sourceLocation, predictedState.location);
					
					if (
						wavePassed(predictedState.location, 
							predictedTime, bulletVelocity)) {
						wavePassed = true;
					}					
				} while (!wavePassed);
				
				orbitAbsBearing = DUtils.absoluteBearing(originalTargetLocation, 
					predictedState.location) -
					((clockwisePrediction ? 1 : -1) * (Math.PI / 2));
				
				bestSmoothingEscapeAngle = Math.max(bestSmoothingEscapeAngle,
					Math.abs(Utils.normalRelativeAngle(DUtils.absoluteBearing(
					sourceLocation, predictedState.location) - absBearingRadians)));
			}
			withSmoothingEscapeAngle = bestSmoothingEscapeAngle;
		}

		if (withSmoothingEscapeAngle > noSmoothingEscapeAngle) {
			if (positiveGuessFactor) {
				usedPositiveSmoothingMea = true;
			} else {
				usedNegativeSmoothingMea = true;				
			}
		}

		return Math.max(noSmoothingEscapeAngle, withSmoothingEscapeAngle);        
	}
	
	public void setOrientation(int lastOrientation, 
		double lastOrientationVelocityThreshold) {
		
		if (Math.abs(targetVelocity) <= lastOrientationVelocityThreshold) {
			orientation = lastOrientation;
		} else {
			orientation = DUtils.getOrientation(targetHeadingRadians, 
				targetVelocity, absBearingRadians);
		}	
	}
	
	public double distanceTraveled(long currentTime) {
		return (currentTime - fireTime) * bulletVelocity();
	}
	
	public void clearEscapeAngleCache() {
		_cachedPositiveEscapeAngle = NO_CACHED_ESCAPE_ANGLE;
		_cachedNegativeEscapeAngle = NO_CACHED_ESCAPE_ANGLE;		
	}
}
