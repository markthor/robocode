// Golden
//
// a bot by Kimstron
//
// Golden is my first wave surfer. It is the first bot in my
// 'Royalty Series' including Golden(Megabot), Royal(MiniBot),
// Monarch(MicroBot), and Servant(NanoBot).
// It's a newbie level bot, mainly for testing.
//
// v.0.10 November 13, 2013
// * Wave surfing- Basicsurfer with modification
// * Guessfactor Targeting from PEZ
// * Testing bot for dive protection
// * Basic distancing

// Good luck in the bot arena.

package kms;
 
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;     // for Point2D's
// not needed, robocode comes with doubles and integers
// import java.lang.*;
import java.util.ArrayList; // for collection of waves
import java.awt.Color;
 
public class Golden extends AdvancedRobot {
	
    public static int BINS = 52;
    public static double _surfStats[] = new double[BINS];
	
    public Point2D.Double _myLocation;     // our bot's location
    public Point2D.Double _enemyLocation;  // enemy bot's location
 
    public ArrayList _enemyWaves;
    public ArrayList _surfDirections;
    public ArrayList _surfAbsBearings;
 
    // to keep track of enemy energy
    public static double _oppEnergy = 100.0;
 
    // 800 x 600 battlefield rectangle
    public static Rectangle2D.Double _fieldRect
        = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
	// the minimum distance from walls
    public static double WALL_STICK = 125;
	
	// to tell wether there are waves exsisting
	// certain actions are performed when waves are non-exsistant
	private boolean waves;
	private boolean scanned;
	
	// for anti-rambot gun
	double enemyLatVel = 0;
	boolean ATR;
	
	static final double BATTLE_FIELD_WIDTH = 800;
    static final double BATTLE_FIELD_HEIGHT = 600;

    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 1.9;
    static final double WALL_MARGIN = 18;
    static final double MAX_TRIES = 125;
    static final double REVERSE_TUNER = 0.421075;
    static final double WALL_BOUNCE_TUNER = 0.699484;

    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;
    static final int LAST_VELOCITY_INDEXES = 5;
    static final int WALL_INDEXES = 2;
    static final int DECCEL_TIME_INDEXES = 6;
    static final int AIM_FACTORS = 25;
    static final int MIDDLE_FACTOR = (AIM_FACTORS - 1) / 2;

    static Point2D enemyLocation;
    static double enemyVelocity;
    static int timeSinceDeccel;
    static double bearingDirection;
    static int[][][][][][] aimFactors = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][LAST_VELOCITY_INDEXES][DECCEL_TIME_INDEXES][WALL_INDEXES][AIM_FACTORS];
    static double direction = 0.4;
    static double enemyFirePower;
    static int GF1Hits;
    static double tries;
	
    public void run() {
        _enemyWaves = new ArrayList();
        _surfDirections = new ArrayList();
        _surfAbsBearings = new ArrayList();
 
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
		
		// do basic actions
		setReadyState();
 
        do {
            // radar turn
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
			setTurnGunRightRadians(1);
        } while (true);
    }
	
	private void setReadyState() {	
		waves = false;
		scanned = false;
		this.setColors(
				Color.yellow,Color.yellow,Color.yellow,Color.yellow,Color.yellow);		
	}
 
    public void onScannedRobot(ScannedRobotEvent e) {
		// scanned enemy
		scanned = true;
		// for wave date from PEZ
		Wave wave = new Wave();
		// track enemy lateral velocity
		enemyLatVel = e.getVelocity()*Math.sin(e.getBearingRadians());
		if (enemyLatVel < 1 && enemyLatVel > -1) {
			ATR = true;
			wave.wBulletPower = 3;
		}

        _myLocation = new Point2D.Double(getX(), getY());
 
        double latVel = getVelocity()*Math.sin(e.getBearingRadians());
        double absBearing = e.getBearingRadians() + getHeadingRadians();
 
        setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2);
 
        _surfDirections.add(0,
            new Integer((latVel >= 0) ? 1 : -1));
        _surfAbsBearings.add(0, new Double(absBearing + Math.PI));
 
 
        double bulletPower = _oppEnergy - e.getEnergy();
        if (bulletPower <= 3 && bulletPower >= 0.1
            && _surfDirections.size() > 2) {
            EnemyWave ew = new EnemyWave();
            ew.fireTime = getTime() - 1;
            ew.bulletVelocity = bulletVelocity(bulletPower);
            ew.distanceTraveled = bulletVelocity(bulletPower);
            ew.direction = ((Integer)_surfDirections.get(2)).intValue();
            ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
            ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick
 
            _enemyWaves.add(ew);
			
			waves = true;
        }
 
        _oppEnergy = e.getEnergy();
 
        // update waves
        _enemyLocation = project(_myLocation, absBearing, e.getDistance());
 
        updateWaves();
        doSurfing();
 
        // gun code would go here...
		// anti rambot
		//while(ATR = true) {
			//double absoluteB = getHeadingRadians() + e.getBearingRadians();
			//setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteB - 
    		//getGunHeadingRadians() + (e.getVelocity() * Math.sin(e.getHeadingRadians() - 
    		//absoluteB) / 13.0)));
			//setFire(3.0);
			//execute();
		//}
		
		// GF gun from PEZ
        double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	double enemyDistance;
        enemyLocation = project(wave.wGunLocation = new Point2D.Double(getX(), getY()), enemyAbsoluteBearing, enemyDistance = e.getDistance());
		Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2);
		int lastVelocityIndex = (int)Math.abs(enemyVelocity) / 2;
	int velocityIndex = (int)Math.abs((enemyVelocity = e.getVelocity()) / 2);
	if (velocityIndex < lastVelocityIndex) {
	    timeSinceDeccel = 0;
	}
	
	if (enemyVelocity != 0) {
	    bearingDirection = enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) > 0 ?
		0.7 / (double)MIDDLE_FACTOR : -0.7 / (double)MIDDLE_FACTOR;
	}
	wave.wBearingDirection = bearingDirection;
	
	int distanceIndex;
	wave.wBulletPower = Math.min(e.getEnergy() / 4,
	    (distanceIndex = (int)(enemyDistance / (MAX_DISTANCE / DISTANCE_INDEXES))) > 1 ? BULLET_POWER : MAX_BULLET_POWER);
	//wave.wBulletPower = MAX_BULLET_POWER; // TargetingChallenge

	wave.wAimFactors = aimFactors[distanceIndex][velocityIndex][lastVelocityIndex][Math.min(5, timeSinceDeccel++ / 13)]
	    [fieldRectangle.contains(project(wave.wGunLocation, enemyAbsoluteBearing + wave.wBearingDirection * 13, enemyDistance)) ? 1 : 0];

	wave.wBearing = enemyAbsoluteBearing;

	int mostVisited = MIDDLE_FACTOR, i = AIM_FACTORS;
	do  {
	    if (wave.wAimFactors[--i] > wave.wAimFactors[mostVisited]) {
		mostVisited = i;
	    }
	} while (i > 0);

	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
	    wave.wBearingDirection * (mostVisited - MIDDLE_FACTOR)));

	setFire(wave.wBulletPower);
	if (getEnergy() >= BULLET_POWER) {
	    addCustomEvent(wave);
	}
    }
 
    public void updateWaves() {
        for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
 
            ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled >
                _myLocation.distance(ew.fireLocation) + 50) {
                _enemyWaves.remove(x);
                x--;
            }
        }
    }
 
    public EnemyWave getClosestSurfableWave() {
        double closestDistance = 50000; // I juse use some very big number here
        EnemyWave surfWave = null;
 
        for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
            double distance = _myLocation.distance(ew.fireLocation)
                - ew.distanceTraveled;
 
            if (distance > ew.bulletVelocity && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }
 
        return surfWave;
    }
 
    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, calculate the index into our stat array for that factor.
    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
            - ew.directAngle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
            / maxEscapeAngle(ew.bulletVelocity) * ew.direction;
 
        return (int)limit(0,
            (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
            BINS - 1);
    }
 
    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, update our stat array to reflect the danger in that area.
    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);
 
        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            _surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }
 
    public void onHitByBullet(HitByBulletEvent e) {
		// for gun
		if (tries < 30) {
	    GF1Hits++;
	}
	enemyFirePower = e.getPower();
    
        // If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!_enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                e.getBullet().getX(), e.getBullet().getY());
            EnemyWave hitWave = null;
 
            // look through the EnemyWaves, and find one that could've hit us.
            for (int x = 0; x < _enemyWaves.size(); x++) {
                EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
 
                if (Math.abs(ew.distanceTraveled -
                    _myLocation.distance(ew.fireLocation)) < 50
                    && Math.abs(bulletVelocity(e.getBullet().getPower()) 
                        - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }
 
            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);
 
                // We can remove this wave now, of course.
                _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
            }
        }
    }
 
    // CREDIT: mini sized predictor from Apollon, by rozu
    // http://robowiki.net?Apollon
    public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
    	Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
    	double predictedVelocity = getVelocity();
    	double predictedHeading = getHeadingRadians();
    	double maxTurning, moveAngle, moveDir;
 
        int counter = 0; // number of ticks in the future
        boolean intercepted = false;
 
    	do {
    		moveAngle =
                wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
                predictedPosition) + (direction * (Math.PI/2)), direction)
                - predictedHeading;
    		moveDir = 1;
 
    		if(Math.cos(moveAngle) < 0) {
    			moveAngle += Math.PI;
    			moveDir = -1;
    		}
 
    		moveAngle = Utils.normalRelativeAngle(moveAngle);
 
    		// maxTurning is built in like this, you can't turn more then this in one tick
    		maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
    		predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + limit(-maxTurning, moveAngle, maxTurning));
 
    		// this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to breack down
    		// otherwise you want to accelerate (look at the factor "2")
    		predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
    		predictedVelocity = limit(-8, predictedVelocity, 8);
 
    		// calculate the new predicted position
    		predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);
 
            counter++;
 
            if (predictedPosition.distance(surfWave.fireLocation) <
                surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                + surfWave.bulletVelocity) {
                intercepted = true;
            }
    	} while(!intercepted && counter < 500);
 
    	return predictedPosition;
    }
 
    public double checkDanger(EnemyWave surfWave, int direction) {
        int index = getFactorIndex(surfWave,
            predictPosition(surfWave, direction));
 
        return _surfStats[index];
    }
 
    public void doSurfing() {
        EnemyWave surfWave = getClosestSurfableWave();
 
        if (surfWave == null) { return; }
 
        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);
 
        double goAngle = absoluteBearing(surfWave.fireLocation, _myLocation);
        if (dangerLeft < dangerRight) {
            goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI/2), -1);
        } else {
            goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI/2), 1);
        }
 
        setBackAsFront(this, goAngle);
    }
 
    // This can be defined as an inner class if you want.
    class EnemyWave {
        Point2D.Double fireLocation;
        long fireTime;
        double bulletVelocity, directAngle, distanceTraveled;
        int direction;
 
        public EnemyWave() { }
    }
 
    // CREDIT: Iterative WallSmoothing by Kawigi
    //   - return absolute angle to move at after account for WallSmoothing
    // robowiki.net?WallSmoothing
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!_fieldRect.contains(project(botLocation, angle, 160))) {
            angle += orientation*0.05;
        }
        return angle;
    }
 
    // CREDIT: from CassiusClay, by PEZ
    //   - returns point length away from sourceLocation, at angle
    // robowiki.net?CassiusClay
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
    }
 
    // got this from RaikoMicro, by Jamougha, but I think it's used by many authors
    //  - returns the absolute angle (in radians) from source to target points
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }
 
    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }
 
    public static double bulletVelocity(double power) {
        return (20D - (3D*power));
    }
 
    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0/velocity);
    }
 
    public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
		
        double angle =
            Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
		
        if (Math.abs(angle) > (Math.PI/2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1*angle);
           } else {
                robot.setTurnRightRadians(angle);
           }
            robot.setAhead(100);
        }
    }
 
    public void onPaint(java.awt.Graphics2D g) {
         g.setColor(java.awt.Color.green);
		 
         for(int i = 0; i < _enemyWaves.size(); i++){
            EnemyWave w = (EnemyWave)(_enemyWaves.get(i));
            Point2D.Double center = w.fireLocation;
 
            int radius = (int)w.distanceTraveled;
 
            // Point2D.Double center = w.fireLocation;
            if(radius - 40 < center.distance(_myLocation))
               g.drawOval((int)(center.x - radius ), (int)(center.y - radius), radius*2, radius*2);
         }
    }
 	static double bulletSpeed(double power) {
	return 20 - 3 * power;
    }

    static Point2D project(Point2D sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    class Wave extends Condition {
	double wBulletPower;
	Point2D wGunLocation;
	double wBearing;
	double wBearingDirection;
	int[] wAimFactors;
	double wDistance;

	public boolean test() {
	    if ((wDistance += bulletSpeed(wBulletPower)) > wGunLocation.distance(enemyLocation) - 18) {
		try {
		    wAimFactors[(int)Math.round(((Utils.normalRelativeAngle(absoluteBearing(wGunLocation, enemyLocation) - wBearing)) /
				wBearingDirection) + MIDDLE_FACTOR)]++;
		}
		catch (Exception e) {
		}
		removeCustomEvent(this);
	    }
	    return false;
	}
    }
	public void onStatus(StatusEvent e) {
		
	}
}