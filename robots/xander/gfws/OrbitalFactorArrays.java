package xander.gfws;

import java.awt.geom.Point2D;

import xander.core.Resources;
import xander.core.drive.Direction;
import xander.core.drive.DistancingEquation;
import xander.core.drive.DriveController;
import xander.core.drive.DrivePrediction;
import xander.core.drive.OrbitalDrivePredictor;
import xander.core.math.RCMath;
import xander.core.math.RCPhysics;
import xander.core.math.VelocityVector;
import xander.core.track.Snapshot;
import xander.core.track.Wave;

public class OrbitalFactorArrays {
	
	public static FactorRange getReachableFactorRange(double bulletVelocity, 
			Snapshot initialAttackerSnapshot, Snapshot initialDefenderSnapshot, 
			long time, int numFactors, boolean diveProtect, 
			DistancingEquation distancingEquation, OrbitalDrivePredictor orbitalDrivePaths,
			double[] velocityConstraints) {
		double bulletPower = RCPhysics.getBulletPower(bulletVelocity);
		Wave wave = Resources.getWaveHistory().createWave(initialDefenderSnapshot, initialAttackerSnapshot, bulletPower, initialAttackerSnapshot.getTime(), false, false);
		return getReachableFactorRange(wave, 
				initialAttackerSnapshot, initialDefenderSnapshot, time, 
				numFactors, diveProtect, distancingEquation, orbitalDrivePaths,
				velocityConstraints);
	}
	
	/**
	 * Returns information on the reachable factor range for the given defender
	 * under the given circumstances.
	 * 
	 * @param wave                  bullet wave advancing on defender
	 * @param attackerSnapshot      attacker snapshot at current time
	 * @param defenderSnapshot      defender snapshot at current time
	 * @param smoothing             wall smoothing strategy
	 * @param time                  current time
	 * @param numFactors            number of factors in the factor array
	 * @param diveProtect           whether or not to limit range to protect against dive
	 * @param distancingEquation    distancing equation
	 * @param orbitalDrivePaths     orbital drive predictor object
	 * 
	 * @return                      information on defender's reachable factor range
	 */
	public static FactorRange getReachableFactorRange(Wave wave, 
			Snapshot attackerSnapshot, Snapshot defenderSnapshot, 
			long time, int numFactors, boolean diveProtect,
			DistancingEquation distancingEquation, OrbitalDrivePredictor orbitalDrivePaths,
			double[] velocityConstraints) {
		FactorIndexer factorIndexer = wave.getFactorIndexer();
		FactorRange rfr = new FactorRange();
		double currentAngle = RCMath.getRobocodeAngle(
				wave.getOriginX(), wave.getOriginY(), 
				defenderSnapshot.getX(), defenderSnapshot.getY());
		double divisionAngle = RCMath.getTurnAngle(wave.getInitialDefenderBearing(), currentAngle);
		double currentDistance = Double.POSITIVE_INFINITY;
		if (attackerSnapshot != null) {
			currentDistance = attackerSnapshot.getDistance();
		}

		// determine how far defender can get clockwise
		DrivePrediction cwDrivePrediction = orbitalDrivePaths.predictMaxPathBeforeBulletHit(
				defenderSnapshot, Direction.CLOCKWISE, wave.getOriginX(), wave.getOriginY(), 
				time-wave.getOriginTime(), wave.getBulletVelocity(), 
				currentDistance, distancingEquation, velocityConstraints);
		Point2D.Double cwPosition = cwDrivePrediction.getFinalDriveState().getPosition();
		double cwAngleFromOrigin = RCMath.getRobocodeAngle(
				wave.getOriginX(), wave.getOriginY(), cwPosition.x, cwPosition.y);
		double cwAngleOffset = RCMath.getTurnAngle(wave.getInitialDefenderBearing(),cwAngleFromOrigin);
		double maxClockwiseFactorAngle = cwAngleOffset;
		rfr.clockwisePath = cwDrivePrediction.getDrivePath();
		rfr.clockwiseDistances = new double[rfr.clockwisePath.size()];
		rfr.clockwiseFactorIndexes = new double[rfr.clockwisePath.size()];
		double[] attackerDelta = attackerSnapshot.getXYShift();
		
		// determine how far we can get counter-clockwise
		DrivePrediction ccwDrivePrediction = orbitalDrivePaths.predictMaxPathBeforeBulletHit(
				defenderSnapshot, Direction.COUNTER_CLOCKWISE, wave.getOriginX(), wave.getOriginY(), 
				time-wave.getOriginTime(), wave.getBulletVelocity(), 
				currentDistance, distancingEquation, velocityConstraints);
		Point2D.Double ccwPosition = ccwDrivePrediction.getFinalDriveState().getPosition();
		double ccwAngleFromOrigin = RCMath.getRobocodeAngle(
				wave.getOriginX(), wave.getOriginY(), ccwPosition.x, ccwPosition.y);
		double ccwAngleOffset = RCMath.getTurnAngle(wave.getInitialDefenderBearing(),ccwAngleFromOrigin);
		double maxCounterClockwiseFactorAngle = ccwAngleOffset;
		rfr.counterClockwisePath = ccwDrivePrediction.getDrivePath();
		rfr.counterClockwiseDistances = new double[rfr.counterClockwisePath.size()];
		rfr.counterClockwiseFactorIndexes = new double[rfr.counterClockwisePath.size()];
		
		rfr.mea = new RelativeAngleRange(maxCounterClockwiseFactorAngle, maxClockwiseFactorAngle, "OrbitalFactorArrays");
		int cwDivisionIndex = 0;
		int ccwDivisionIndex = 0;
		cwDivisionIndex = factorIndexer.getFactorIndex(cwAngleOffset, numFactors, wave.getSurfDirection(), wave.getInitialMEA());
		ccwDivisionIndex = factorIndexer.getFactorIndex(ccwAngleOffset, numFactors, wave.getSurfDirection(), wave.getInitialMEA());
		rfr.currentDefenderIndex = factorIndexer.getFactorIndex(divisionAngle, numFactors, wave.getSurfDirection(), wave.getInitialMEA());
		rfr.maxClockwiseIndex = cwDivisionIndex;
		rfr.maxCounterClockwiseIndex = ccwDivisionIndex;

		Point2D.Double attackerPos = new Point2D.Double(attackerSnapshot.getX(), attackerSnapshot.getY());
		for (int i=0; i<rfr.clockwiseDistances.length; i++) {
			Point2D.Double point = rfr.clockwisePath.get(i);
			rfr.clockwiseDistances[i] = RCMath.getDistanceBetweenPoints(point.x, point.y, attackerPos.x, attackerPos.y);
			double pointAngle = RCMath.getRobocodeAngle(
					wave.getOriginX(), wave.getOriginY(), 
					point.x, point.y);
			double pointFactorAngle = RCMath.getTurnAngle(wave.getInitialDefenderBearing(), pointAngle);
			rfr.clockwiseFactorIndexes[i] = factorIndexer.getPreciseFactorIndex(pointFactorAngle, numFactors, wave.getSurfDirection(), wave.getInitialMEA());
			// need to roughly predict attackers movement to get better distance values
			attackerPos.x += attackerDelta[0];
			attackerPos.y += attackerDelta[1];
		}
		attackerPos = new Point2D.Double(attackerSnapshot.getX(), attackerSnapshot.getY());
		for (int i=0; i<rfr.counterClockwiseDistances.length; i++) {
			Point2D.Double point = rfr.counterClockwisePath.get(i);
			rfr.counterClockwiseDistances[i] = RCMath.getDistanceBetweenPoints(point.x, point.y, attackerPos.x, attackerPos.y);
			double pointAngle = RCMath.getRobocodeAngle(
					wave.getOriginX(), wave.getOriginY(), 
					point.x, point.y);
			double pointFactorAngle = RCMath.getTurnAngle(wave.getInitialDefenderBearing(), pointAngle);
			rfr.counterClockwiseFactorIndexes[i] = factorIndexer.getPreciseFactorIndex(pointFactorAngle, numFactors, wave.getSurfDirection(), wave.getInitialMEA());				
			// need to roughly predict attackers movement to get better distance values
			attackerPos.x += attackerDelta[0];
			attackerPos.y += attackerDelta[1];
		}
		
//		log.debug("Predicted clockwise position is " + Logger.format(cwPosition) + " at division " + cwDivisionIndex);
//		log.debug("Predicted counter-clockwise position is " + Logger.format(ccwPosition) + " at division " + ccwDivisionIndex);
		
		// dive protection
		double cwDistance = rfr.clockwiseDistances[rfr.clockwiseDistances.length-1];
		double ccwDistance = rfr.counterClockwiseDistances[rfr.counterClockwiseDistances.length-1];
		if (diveProtect) {
			//System.out.println("Dive protection code check");
			//System.out.println("currentDistance=" + Logger.format(currentDistance));
			//System.out.println("ccwDistance=" + Logger.format(ccwDistance));
			//System.out.println("cwDistance=" + Logger.format(cwDistance));
			if (currentDistance < (distancingEquation.getOptimalDistance()+distancingEquation.getMaxRetreatDistance())/2d) {
				if ((ccwDistance < currentDistance - 35 || ccwDistance < 70) 
						&& cwDistance >= currentDistance) {
					ccwDivisionIndex = Math.min(rfr.currentDefenderIndex, cwDivisionIndex);
					rfr.diveProtected = true;
				} else if ((cwDistance < currentDistance - 35 || cwDistance < 70) 
						&& ccwDistance >= currentDistance) {
					cwDivisionIndex = Math.max(rfr.currentDefenderIndex, ccwDivisionIndex);
					rfr.diveProtected = true;
				}
			}
		} 	
		//TODO: indexes can be slightly out of range since the precise MEA changes; might be worth looking into
		// to combat the problem -- added using Math.min and Math.max
		if (ccwDivisionIndex > cwDivisionIndex) {
			rfr.beginIndex = Math.max(0, cwDivisionIndex);
			rfr.endIndex = Math.min(numFactors-1, ccwDivisionIndex);
		} else {
			rfr.beginIndex = Math.max(0, ccwDivisionIndex);
			rfr.endIndex = Math.min(numFactors-1, cwDivisionIndex);
		}
		return rfr;
	}
	
	/**
	 * Drive to the given factor angle for the given bullet wave
	 * 
	 * @param wave                 bullet wave being surfed
	 * @param factorAngle          factor angle to drive to
	 * @param smoothing            wall smoothing strategy
	 * @param x                    current x-coordinate for our position
	 * @param y                    current y-coordinate for our position
	 * @param opponentDistance     opponent distance (for distancing)
	 * @param distancingEquation   distancing equation
	 * @param orbitalDrivePaths    orbital drive paths object
	 * @param driveController      drive controller
	 */
	public static void driveToFactor(Wave wave, double factorAngle, double x, double y, double opponentDistance, DistancingEquation distancingEquation, OrbitalDrivePredictor orbitalDrivePaths, DriveController driveController) {
		double facing = RCMath.getRobocodeAngle(wave.getOriginX()-x,wave.getOriginY()-y);
		double opposingAngle = RCMath.normalizeDegrees(facing + 180);
		double turnAngle = RCMath.getTurnAngle(opposingAngle, wave.getInitialDefenderBearing() + factorAngle);
		Direction direction = (turnAngle < 0)? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE;
		double targetHeadingFromOrigin = RCMath.normalizeDegrees(wave.getInitialDefenderBearing() + factorAngle);
		boolean isOvershoot = orbitalDrivePaths.isOvershoot(
				wave.getOrigin(), opponentDistance, direction, 
				targetHeadingFromOrigin, wave, distancingEquation);
		double speed = (isOvershoot)? 0 : RCPhysics.MAX_SPEED;
		VelocityVector driveVector = orbitalDrivePaths.getSmoothedOrbitAngle( 
				wave.getOrigin(), opponentDistance, 
				direction, distancingEquation, speed);
		driveController.drive(driveVector.getRoboAngle(), driveVector.getMagnitude());
	}
}
