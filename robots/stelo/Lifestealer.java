// Lifestealer (C) 2006 Kim, Tae-gyoon
/*
* features:
* tracker
* ramfire
* linear/circular ramming
*
*
*/


package stelo;



import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;     // for Point2D's
import java.lang.*;         // for Double and Integer objects
import java.util.ArrayList; // for collection of waves

import java.awt.Color;

/**
 * 
 * 
 * This robot moves around in a crazy pattern
 */
public class Lifestealer extends AdvancedRobot {
	boolean movingForward;
	
	private double lastEnemyHeading;
	private double lastW;
	private double lastEnemyVelocity;
	
	private double scantime;
	private double radarturn = 1;
	private double numFire = 0;
	private double numHit = 0;
	
    public static int BINS = 47;
    public static double _surfStats[] = new double[BINS];
    public Point2D.Double _myLocation;     // our bot's location
    public Point2D.Double _enemyLocation;  // enemy bot's location

    public ArrayList _enemyWaves;
    public ArrayList _surfDirections;
    public ArrayList _surfAbsBearings;

    public static double _oppEnergy = 100.0;
	
	
	
	public void run() {
		setColors(Color.blue, Color.red, Color.green);
		
		
		while (true) {
			//setMaxVelocity(6);
			// Tell the game we will want to move ahead 40000 -- some large number
			setAhead(40000);
			movingForward = true;
			
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		}
	}
	
	/**
	 * onHitWall:  Handle collision with wall.
	 */	
	public void onHitWall(HitWallEvent e) {
		// Bounce off!
		// reverseDirection();
	}
	
	/**
	 * reverseDirection: switch from ahead to back & vice versa
	 */	
	public void reverseDirection() {
		if (movingForward) {
			setBack(40000);
			movingForward = false;
		} else {
			setAhead(40000);
			movingForward = true;
		}
	}
	
	/**
	 * onScannedRobot:  Fire!
	 */	
	public void onScannedRobot(ScannedRobotEvent e) {
		out.println(e.getName());
		double bulletPower;
		
		//setMaxVelocity(8);
		if (getDistanceRemaining() == 0)
			setAhead(300);
		setTurnRight(90);
						
        _myLocation = new Point2D.Double(getX(), getY());

        double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());
        double absBearing = e.getBearingRadians() + getHeadingRadians();


        setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2); // 2 original
        // setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * getOthers()); // 2 original

        //setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing - getGunHeadingRadians()) * 2); // stelo



        _oppEnergy = e.getEnergy();

        // gun code would go here...

		// HeadOnTargeting
		/*
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));
		*/
		bulletPower = Math.min(getEnergy() - 2, e.getEnergy() / 4.0);
			
		if (bulletPower <= 0)
			bulletPower = 0.1;
		else if (bulletPower > 3)
			bulletPower = 3;
		// CircularTargeting (quadratic)
		
	
		radarturn=-radarturn;
		double w=e.getHeadingRadians()-lastEnemyHeading;
		double dw = w - lastW;
		// System.out.println(dw);
		
		lastEnemyHeading=e.getHeadingRadians();
		lastW = w;
		
		
		if (w != 0) {
			if (getGunHeat() == 0) {
				System.out.println("CircularTargeting");
			}
			
			double absbearing=e.getBearingRadians()+getHeadingRadians();
			double eX=e.getDistance()*Math.sin(absbearing);
			double eY=e.getDistance()*Math.cos(absbearing);

			double db=0;
			double ww=lastEnemyHeading;  // enemy's starting heading
			do
			{
				// db+=11; //11 is the velocity of a fire(3) bullet.
				db += (20.0 - 3.0 * bulletPower);
				double dx=e.getVelocity()*Math.sin(ww);
				double dy=e.getVelocity()*Math.cos(ww);
				ww+=w;  // turn w radians for next step
				w += dw;
				
				eX+=dx;
				eY+=dy;
			}while (db< Point2D.distance(0,0,eX,eY));	// The bullet travelled far enough to hit our target!
		
			setTurnRightRadians(Math.asin(Math.sin(Math.atan2(eX, eY) - getHeadingRadians())));
			setTurnGunRightRadians(Math.asin(Math.sin(Math.atan2(eX, eY) - getGunHeadingRadians())));
						
			// setTurnRightRadians(e.getBearingRadians() + .5*Math.PI);		
		}
		else
		{
			if (getGunHeat() == 0) {
				System.out.println("LinearTargeting");
			}
			// LinearTargeting (quadratic)
			//This code goes in your onScannedRobot() event handler
			
			
			double myX = getX();
			double myY = getY();
			double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
			double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
			double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
			double enemyHeading = e.getHeadingRadians();
			double enemyVelocity = e.getVelocity();
			
			double dV = enemyVelocity - lastEnemyVelocity;
			// System.out.println(dV);
			
			lastEnemyVelocity = enemyVelocity;
		
			double deltaTime = 0;
			double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
			double predictedX = enemyX, predictedY = enemyY;
			while((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
				predictedX += Math.sin(enemyHeading) * enemyVelocity;	
				predictedY += Math.cos(enemyHeading) * enemyVelocity;
				if(	predictedX < 18.0 
					|| predictedY < 18.0
					|| predictedX > battleFieldWidth - 18.0
					|| predictedY > battleFieldHeight - 18.0){
					predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);	
					predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
					break;
				}
				
				enemyVelocity += dV;
				if (enemyVelocity > 8)
					enemyVelocity = 8;
				else if(enemyVelocity < -8)
					enemyVelocity = -8;
			}
			double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
		
			// setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
			setTurnRightRadians(Utils.normalRelativeAngle(theta - getHeadingRadians()));
			setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
						
		}


		// public void fire(double power) Fires a bullet. The valid range for power is .1 to 3. The bullet will travel in the direction the gun is pointing. The bullet will do (4 * power) damage if it hits another robot. If power is greater than 1, it will do an additional 2 * (power - 1) damage. You will get (3 * power) back if you hit the other robot. An event will be generated when the bullet hits a robot, wall, or other bullet. This call executes immediately. 		
		
		if (getEnergy() > 1 && getGunHeat() == 0) {										
			fire(bulletPower);
		}																																										
		
			
	}

	/**
	 * onHitRobot:  Back up!
	 */
	/*	
	public void onHitRobot(HitRobotEvent e) {
		// If we're moving the other robot, reverse!
		if (e.isMyFault()) {
			reverseDirection();
		}
	}
	*/
	
	
	/**
	 * onHitRobot:  Turn to face robot, fire hard, and ram him again!
	 */
	public void onHitRobot(HitRobotEvent e) {
		turnRight(e.getBearing());
		
		// Determine a shot that won't kill the robot...
		// We want to ram him instead for bonus points
		
		
		if (e.getEnergy() > 16) {
			fire(3);
		} else if (e.getEnergy() > 10) {
			fire(2);
		} else if (e.getEnergy() > 4) {
			fire(1);
		} else if (e.getEnergy() > 2) {
			fire(.5);
		} else if (e.getEnergy() > .4) {
			fire(.1);
		}
		
		
		ahead(40); // Ram him again!
	}
		
	/**
	 * We were hit!  Turn perpendicular to the bullet,
	 * so our seesaw might avoid a future shot.
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		// turnLeft(90 - e.getBearing());
		//ahead(50);
		setMaxVelocity(8);
	}	
	
	
}		

					