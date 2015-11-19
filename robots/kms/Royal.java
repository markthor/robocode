// Royal
// By Kimstron
//
// My first minibot:
// Adaptive random movement + Mushai Trick
// sorry about my spelling.
// Based off RandomMovementBot.
// Gun from WeekendObsession.
// Released under RWPCL. See robowiki.net/wiki/RWPCL for more info.
// This is the microbot version. The mini will have more. Currently,
// it only adapts to win/deaths. The mini will adapt to being hit by
// bullets. The mini will also include distancing and colors.

package kms;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.awt.Color;

public class Royal extends AdvancedRobot {
	// variables and constants
    static final double MAX_VELOCITY = 8;
    static final double WALL_MARGIN = 25;
    Point2D robotLocation;
    Point2D enemyLocation;
    double enemyDistance;
    double enemyAbsoluteBearing;
    double movementLateralAngle = 0.2;
	double randomizerFactor = 0.000;
	// death/win count for movement
	static double deathCount = 0;
	static double winCount = 0;
	// for weekendObsession's gun
	static final int maxMatchLen = 30;
	static final int targetDistance = 200;
	static final int firePower = 2;
	static final int fireSpeed = 20 - firePower * 3;
	static int distance;

    public void run() {
        setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		
		setColors(Color.red, Color.white, Color.white);

        do {
            turnRadarRightRadians(distance = 1000000); 
        } while (true);
    }
	
	// minor events
	public void onDeath() {
		deathCount ++;
	}
	public void onWin() {
		winCount ++;
	}
	//public void onHitByBullet() {
		//randomizerFactor = randomizerFactor + 0.00075;
	//}
	//public void onBulletHit() {
		//randomizerFactor = randomizerFactor - 0.0005;
	//}
	
    public void onScannedRobot(ScannedRobotEvent e) {
        robotLocation = new Point2D.Double(getX(), getY());
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        enemyLocation = vectorToLocation(enemyAbsoluteBearing, enemyDistance, robotLocation);
		// added to standard movement
		if (deathCount > 2) {
			// adapt to deaths - switch to mushai trick
			randomizerFactor = randomizerFactor + 0.01;
		}
		if (winCount > (deathCount + 1)) {
			// adapt to wins
			if (randomizerFactor > 0.09) {
				randomizerFactor = randomizerFactor - 0.01;
			}
		}
		// Change direction at random
		if (Math.random() < randomizerFactor) {
			movementLateralAngle *= -1;
		}
		move();
		
		// gun from WeekendObsession
		int matchLen = maxMatchLen;
		double a;
		int dist;
		int matchPos;
		int i;
		
		if ((dist = (int) e.getDistance()) < distance + 32) {
            
			a = e.getBearingRadians() + getHeadingRadians();
				
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY 
				* Utils.normalRelativeAngle(a - getRadarHeadingRadians()));
			
			enemyHistory = 
				String.valueOf((char) Math.rint(
					e.getVelocity() * Math.sin(e.getHeadingRadians() - a)))
				.concat(enemyHistory);
			while(
				(matchPos = 
					enemyHistory.indexOf(
						enemyHistory.substring(0, matchLen--), 
						i = dist / fireSpeed)) 
				< 0);
			
			// aim.
			do {
				a += (double) (short) enemyHistory.charAt(--matchPos) / dist;
			} while (--i > 0);
			setTurnGunRightRadians(
				Utils.normalRelativeAngle(a - getGunHeadingRadians()));
			
			// fire.
			fire(firePower);
		}
		
		// radar
        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    // Always try to move a bit further away from the enemy.
    // Only when the walls forces us we will close in on the enemy. We never bounce of walls.
    void move() {
	Point2D robotDestination = null;
	double tries = 0;
	do {
	    robotDestination = vectorToLocation(absoluteBearing(enemyLocation, robotLocation) + movementLateralAngle,
		    enemyDistance * (1.1 - tries / 100.0), enemyLocation);
	    tries++;
	} while (tries < 100 && !fieldRectangle(WALL_MARGIN).contains(robotDestination));
	goTo(robotDestination);
    }

    RoundRectangle2D fieldRectangle(double margin) {
        return new RoundRectangle2D.Double(margin, margin,
	    getBattleFieldWidth() - margin * 2, getBattleFieldHeight() - margin * 2, 75, 75);
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
	double turnAngle = Math.atan(Math.tan(angle));
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * (angle == turnAngle ? 1 : -1));
	// Hit the brake pedal hard if we need to turn sharply
	setMaxVelocity(Math.abs(getTurnRemaining()) > 33 ? 0 : MAX_VELOCITY);
    }

    static Point2D vectorToLocation(double angle, double length, Point2D sourceLocation) {
	return vectorToLocation(angle, length, sourceLocation, new Point2D.Double());
    }

    static Point2D vectorToLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
	return targetLocation;
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }
	static String enemyHistory = ""
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 1
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 2
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char)-1
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char) 0 + (char) 0 + (char) 0
		+ (char) 0 + (char)-2 + (char)-4 + (char)-6
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-8 + (char)-8 + (char)-8 + (char)-8
		+ (char)-7 + (char)-6 + (char)-5 + (char)-4
		+ (char)-3 + (char)-2 + (char)-1 + (char)0
		+ (char) 2 + (char) 4 + (char) 6 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 8 + (char) 8 + (char) 8 + (char) 8
		+ (char) 7 + (char) 6 + (char) 5 + (char) 4
		+ (char) 3 + (char) 2 + (char) 1 + (char) 0;
}
