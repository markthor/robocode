package xander.cat.drive;

import xander.core.Resources;
import xander.core.RobotProxy;
import xander.core.drive.DistancingEquation;
import xander.core.math.RCMath;
import xander.core.math.RCPhysics;
import xander.core.track.GunStats;
import xander.core.track.Wave;

public class SmartDistancingEquation extends DistancingEquation {

	private GunStats gunStats;
	private RobotProxy robotProxy;
	private double beginAdjustHitRatio = 0.12;
	private double zeroAdjustHitRatio = 0.10;
	private double hrRange = beginAdjustHitRatio - zeroAdjustHitRatio;
	
	public SmartDistancingEquation(double maxRetreatDistance,
			double optimalDistance, double maxAdvanceDistance,
			double maxRetreatAngle, double maxAdvanceAngle) {
		super(maxRetreatDistance, optimalDistance, maxAdvanceDistance, maxRetreatAngle,
				maxAdvanceAngle);
		this.robotProxy = Resources.getRobotProxy();
		this.gunStats = Resources.getGunStats();
	}

	public void setMyHitRatioThresholds(double beginAdjustHitRatio, double zeroAdjustHitRatio) {
		this.beginAdjustHitRatio = beginAdjustHitRatio;
		this.zeroAdjustHitRatio = zeroAdjustHitRatio;
		this.hrRange = beginAdjustHitRatio - zeroAdjustHitRatio;
	}
	
	@Override
	public double getAdjustAngle(double distance, Wave wave) {
		double adjustAngle = super.getAdjustAngle(distance, wave);
		if (robotProxy.getRoundNum() > 1) {
			double myHitRatio = gunStats.getOverallHitRatio();
			double oppHitRatio = gunStats.getOverallOpponentHitRatio();
			if (myHitRatio < beginAdjustHitRatio && oppHitRatio > myHitRatio) {
				// If losing against a tough opponent, start maximizing reachable factor
				// range and let the opponent worry about distancing.  Muhahaha.
				// Distancing starts getting reduced at beginAdjust% and drops to none at zeroAdjust%.
				adjustAngle *= (Math.max(0d, myHitRatio - zeroAdjustHitRatio)/hrRange);
			}
			if (distance < getOptimalDistance() && wave != null) {
				// If against wall, retreat is not possible, so make sure no retreat is
				// requested (otherwise, reachable factor range may get reduced)
				double waveApproachAngle = RCMath.getRobocodeAngle(
						wave.getOriginX(), wave.getOriginY(), robotProxy.getX(), robotProxy.getY());
				double distanceToWall = RCMath.getDistanceToIntersect(
						robotProxy.getX(), robotProxy.getY(), waveApproachAngle, robotProxy.getBattleFieldSize());
				if (distanceToWall < RCPhysics.ROBOT_WIDTH) {
					adjustAngle = 0;
				}
			}
		}
		return adjustAngle;
	}

}
