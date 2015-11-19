package maribo;
import robocode.*;
import robocode.util.*;
import java.awt.Color;

/**
 * FollowFire - a robot by Alexander MacKenzie
 * Version 1: Code size: 202 bytes.
 * 				NanoRumble: 167
 * 					   APS: 48.83
 * 					  PWIN: 33.74
 * 		- Linear Targeting.
 * 		- Don't turn gun, just turn robot to aim.
 * 
 * Version 1.1 Code size: 136 bytes.
 * 						: 155 bytes with colours.
 * 				NanoRumble: 138
 * 					   APS: 53.76
 * 					  PWIN: 42.5
 * 		- Linear Targeting.
 * 		- Don't turn gun, just turn robot to aim.
 * 		- Removed colour.
 * 		- Fire power 3 bullets more often.
 * 		- Removed if radarturnleft == 0 check.
 * 
 * Version 1.11 Code size: 159 bytes with colours.
 * 				NanoRumble: 
 * 					   APS: 
 * 					  PWIN: 
 * 		- Now we actually continue to ram the enemy once we hit them.
*/
public class FollowFire extends AdvancedRobot {	

	public void run() {
		setColors(Color.red,Color.black,Color.black,Color.black,Color.red);
		turnRadarRightRadians(Double.POSITIVE_INFINITY);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		int bulletPower = (e.getDistance() > 127) ? 2 : 3;

    	double headOnBearing;
		double turnAmount = Utils.normalRelativeAngle(((headOnBearing = getHeadingRadians() + e.getBearingRadians())
				 + Math.asin(e.getVelocity() / Rules.getBulletSpeed(bulletPower)
				 * Math.sin(e.getHeadingRadians() - headOnBearing)))
				 - getHeadingRadians());
    	setTurnRightRadians(turnAmount);
		setTurnRadarRightRadians(2.2*Utils.normalRelativeAngle(headOnBearing - getRadarHeadingRadians()));
		setAhead(e.getDistance()+20);
		setMaxVelocity(Math.abs(turnAmount) > 0.4 ? 1 : 8d);
		setFire(bulletPower);
	}
}