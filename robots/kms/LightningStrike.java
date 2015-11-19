package kms;
import robocode.*;
import robocode.util.*;
/**
 * LightningStorm - a robot by Kimstron
 * 
 * Beginner robot, uses a HOT and LT gun, an Infinity Lock,
 * and random movement
 */
public class LightningStrike extends AdvancedRobot
{
	int direction = 1;
	
	int strat = 1;
	int missed = 0;
	
	public void run() {
		
		while(true) {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		}
	}

	/**
	 * onScannedRobot: Lock radar, move, and fire!
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
		setTurnRight(90 + e.getBearing() - 10 * direction);
		setAhead(100 * direction);
			if (Math.random() < 0.055) {
				direction = -direction;
			}
		// HOT
		if (strat == 1) {
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
setTurnGunRightRadians(
    	robocode.util.Utils.normalRelativeAngle(absoluteBearing - 
        getGunHeadingRadians()));
		}
		// LT
		if (strat == -1) {
    double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
    double linearBearing = absoluteBearing + Math.asin(e.getVelocity() / Rules.getBulletSpeed(getEnergy()/6) * Math.sin(e.getHeadingRadians() - absoluteBearing));
    setTurnGunRightRadians(Utils.normalRelativeAngle(linearBearing - getGunHeadingRadians()));
		}
		setFire(e.getEnergy() / 6);
		if (missed == 5) {
			strat = -strat;
			missed = 0;
		}
	}
	
	/**
	 * onHitWall: Change direction
	 */
	public void onHitWall(HitWallEvent e) {
		direction = -direction;
	}	
	/**
	 * onBulletMissed: Track amount of missed shots
	 */
	public void onBulletMissed() {
		missed = missed + 1;
	}
}
		