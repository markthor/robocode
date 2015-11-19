package suh.nano;

import robocode.*;
import robocode.util.Utils;

public class Worst extends AdvancedRobot {
    @Override
    public void run() {
	setAdjustGunForRobotTurn(true);
	setAdjustRadarForGunTurn(true);
	setAdjustRadarForRobotTurn(true);
	setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	while(true) {
	    execute();
	}
    }
    
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
	double bearing;
	
	// Radar
	setTurnRadarRightRadians(2.0 * Utils.normalRelativeAngle((bearing = getHeadingRadians() + e.getBearingRadians()) - getRadarHeadingRadians()));
	
	// Targeting
	setTurnGunRightRadians(Utils.normalRelativeAngle(bearing + Math.PI - getGunHeadingRadians()));
	setFire(3);
	
	// Movement
	setTurnRightRadians(e.getBearingRadians());
	setAhead(e.getDistance() - 100);
	setMaxVelocity(180.0 / getTurnRemaining());
    }
}
