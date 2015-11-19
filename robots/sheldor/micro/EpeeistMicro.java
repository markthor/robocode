/*
EpeeistMicro v2.1.0 by Sheldor.  05/06/2015  Codesize:  745 bytes
a microbot with stop and go/random movement and a guess factor gun
v2.1.0 -- major segmentation changes, revert SAG, stronger radar

Epee (pronounced ey-PEY) is one of the three forms of modern sport fencing,
along with Foil and Sabre.  http://en.wikipedia.org/wiki/Epee

Credits:
A general thanks to all open source bot authors and contributors to the RoboWiki.  Without them, Robocode would not be nearly as advanced as it currently is.
Bots which I'd like to specifically credit include, but are not limited to:
	Targeting: Falcon, pez.micro.Aristocles, voidious.mini.Komarious, nz.jdc.HedgehogGF, pez.mini.Pugilist, kc.micro.Thorn, jam.micro.RaikoMicro, jk.mini.CunobelinDC, jk.micro.Connavar, mld.LittleBlackBook
	Movement : jk.micro.Toorkild, kc.micro.Thorn, wiki.nano.RaikoNano, nz.jdc.HedgehogGF
Special thanks go to Jdev and Voidious for helping me with a mathematical error.

EpeeistMicro is open source and released under the terms of the RoboWiki Public Code License (RWPCL) - Version 1.1.
see license here:  http://robowiki.net/wiki/RWPCL
*/

package sheldor.micro;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

public class EpeeistMicro extends AdvancedRobot
{
	//constants
	static final int    GUESS_FACTORS = 25;
	static final int    MIDDLE_FACTOR = (GUESS_FACTORS - 1) / 2;
	static final double MAXIMUM_ESCAPE_ANGLE = 0.72727272727272727272727272727273; //8 / 11
	static final double FACTOR_ANGLE = MAXIMUM_ESCAPE_ANGLE / MIDDLE_FACTOR;
	
	//global variables
	static double direction = 1;
	static double enemyBulletSpeed;	
	static double enemyDirection;
	static double enemyEnergy;
	static double enemyX;
	static double enemyY;
	static double hits;
	static int    absoluteEnemyLateralVelocity;
	static int    movementMode;
	static int    timeSinceVelocityChange;
	
	static int enemyVelocity;
	static int ticksSinceDecel;
	static int ticksSinceVelocityChange;
	
	//array to store the number of times the enemy has visited each guess factor
	//segmented on deceleration, ticks since velocity change, velocity, forward wall, distance, and reverse wall
	//deceleration and ticks since velocity change should be considered one segment, as when decel is 1 or 2, vel change should always be at 0
	static int[][][][][][][] guessFactors = new int[3][5][3][3][4][2][GUESS_FACTORS];
	
	public void onStatus(StatusEvent e)
	{		
		//turn the radar every tick
		//Putting the code here instead of in a while(true) loop in the run() method saves one byte.
		//I believe Wompi discovered this.
    	setTurnRadarRightRadians(1);
		
		//set the radar and gun to turn independently
		//Putting these here with the radar code lets me get rid of the run() method, saving one byte.  credit to Cotillion
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
	}
	
	public void onScannedRobot(ScannedRobotEvent e)
	{
		//local variables	
		//declare the most used integer before the most used double to save several bytes
		int    antiRam;
		double enemyDistance;
		double absoluteBearing;
		double localEnemyDirection;
		double offset;
		double theta;
		
		//fire a wave
		Wave wave;
		addCustomEvent(wave = new Wave());
		
		//update the enemy location variables
		enemyX = (wave.sourceX = getX()) + Math.sin(wave.absoluteBearing = absoluteBearing = 
			(e.getBearingRadians() + getHeadingRadians())) * (enemyDistance = e.getDistance());
		enemyY = (wave.sourceY = getY()) + Math.cos(absoluteBearing) * enemyDistance;
		
		/*********************************************
		 *---------------MOVEMENT CODE---------------*
		 *********************************************/				
		
		//fire medium power bullets most of the time, but use full power at very close range
		//if the enemy is weak, fire the minimum power needed to destroy them
		//retreat very heavily when the enemy is ramming
		setFire(Math.min((offset = (2 + (antiRam = (100 / (int)enemyDistance)))), e.getEnergy() / 4));
		
		//wall smoothing based on HedgehogGF's
		while(fieldContains(theta = absoluteBearing + direction * (offset -= 0.02), 160) > 0);
		setTurnRightRadians(Math.tan(theta -= getHeadingRadians()));
			
		//stop and go movement originally based on Thorn's
		//move when the enemy fires, or when the robot is moving randomly, or when the enemy is ramming
		double energyDrop;
		if ((energyDrop = (enemyEnergy - (enemyEnergy = e.getEnergy()))) > movementMode - antiRam)
		{			
			//credit to Cotilion for stop and go length calculator
			//credit to HedgehogGF for the copySign trick						
			setAhead(Math.copySign(((3 + (int)(energyDrop * 1.999999)) << 3), Math.cos(theta)));
		}
		
		//random movement from Toorkild
		//don't move randomly if the enemy is ramming, or if the bot is in Stop and Go mode
		//reverse direction if the bot gets too close to a wall
		if (Math.random() + antiRam < (-0.6 * Math.sqrt(enemyBulletSpeed / enemyDistance) + 0.04) * movementMode || offset < Math.PI/3.5)
		{
			direction = -direction;
		}
		
		/********************************************
		 *--------------TARGETING CODE--------------*
		 ********************************************/
		
		//determine the enemy's lateral movement direction
		//use a simple rolling average to store the previous lateral direction if enemy lateral velocity == 0
		//credit to HedgehogGF
		wave.enemyDirection = localEnemyDirection = (enemyDirection = Math.signum(0.00000000000001 + (e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing)) + (enemyDirection / 100))) * FACTOR_ANGLE;
		
		//calculate deceleration and whether or not to reset ticks since velocity change
		int deceleration;
		if ((deceleration = Integer.signum(enemyVelocity - (enemyVelocity = (int)Math.abs(e.getVelocity())))) != 0)
		{
			ticksSinceVelocityChange = 0;
		}

		//calculate how close the enemy is to a wall
		//inspired by Pugilist and HedgehogGF		
		int i = 2;	
		int wallSegment = 0;
		do		
		{
			wallSegment += fieldContains(absoluteBearing + (localEnemyDirection * ((MIDDLE_FACTOR / 4) << i)), enemyDistance);
		}
		while (--i > 0);
					
		//determine the current situation
		//Declaring a local array saves two bytes.
		int[] guessFactorsLocal = wave.guessFactors = guessFactors
			[1 + deceleration] //deceleration signum
			[VELOCITY_HISTORY_TABLE.charAt(Math.min(60, ticksSinceVelocityChange++))] //ticks since velocity change
			[enemyVelocity / 3] //velocity
			[wallSegment] //forward wall
			[(int)enemyDistance >> 8] //distance
			[fieldContains(absoluteBearing + (localEnemyDirection * (-(MIDDLE_FACTOR / 2))), enemyDistance)] //reverse wall
			;

		//find the most visited guess factor for the current situation
		//Looping like this is ugly, but it saves two bytes over the proper way.
		int mostVisited = MIDDLE_FACTOR;
		try
		{
			while (true)
			{
				if (guessFactorsLocal[i] > guessFactorsLocal[mostVisited])
				{
					mostVisited = i;
				}
				i++;
			}
		}
		catch(Exception ex)
		{
		}		
		
		//turn the gun to the most visited guess factor
		//The slight offset helps to defeat simple bullet shielding.
		setTurnGunRightRadians(0.0005 + Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()
			+ (localEnemyDirection * (mostVisited - MIDDLE_FACTOR))));
		
		//radar lock
		setTurnRadarRightRadians(2 * Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
	}
	
	public void onBulletHit(BulletHitEvent e)
	{
		//adjust the enemy energy variable when the bot hits the enemy
		//This makes a big difference against linear targeting.
		enemyEnergy -= 10;
	}
	
	public void onHitByBullet(HitByBulletEvent e)
	{
		//adjust the enemy energy variable when the bot gets hit
		//store the velocity of the enemy's bullet for the random movement
		enemyEnergy += 20 - (enemyBulletSpeed = e.getVelocity());
		
		//if the bot takes an unacceptable amount of damage relative to the number of rounds
		//that have passed while in Stop and Go mode, switch to random movement
		//credit to Toorkild for the improved mode selection
		if ((hits += (4.25 / enemyBulletSpeed)) > getRoundNum() + 2)
		{
			movementMode = -1;
		}
    }
	
	static class Wave extends Condition
	{
		//global variables
		double absoluteBearing;
		double enemyDirection;
		double distanceTraveled;
		double sourceX;
		double sourceY;
		int[]  guessFactors;
		
		public boolean test()
		{
			//check if the wave has passed the enemy's current location,
			//but only count waves when they have just passed so waves aren't counted more than once
			if (Math.abs((distanceTraveled += 14) - Point2D.distance(sourceX, sourceY, enemyX, enemyY)) <= 7)
			{
				//calculate the guess factor that the enemy has visited
				//increment the bin that represents that guess factor
				guessFactors[(int)Math.round(((Utils.normalRelativeAngle(Math.atan2(enemyX - sourceX,
					enemyY - sourceY) - absoluteBearing)) / enemyDirection) + MIDDLE_FACTOR)]++;
			}
			return false;
		}
	}
	
	//This method returns 1 if a point projected from the bot's location by the 
	//"heading" and "distance" parameters is outside of the battlefield, and 0 if it is not.
	//credit to HedgehogGF
	private int fieldContains(double heading, double distance)
	{
		return Integer.signum(new Rectangle2D.Double(18, 18, 764, 564).outcode(getX() + distance * Math.sin(heading), getY() + distance * Math.cos(heading)));
	}
	
	//table to look up ticks since velocity change
	static final String VELOCITY_HISTORY_TABLE = ""
		+ (char)0 + (char)0 + (char)0 + (char)0 + (char)0 + (char)0
		+ (char)1 + (char)1 + (char)1 + (char)1 + (char)1 + (char)1
		+ (char)1 + (char)1 + (char)1 + (char)1 + (char)1 + (char)1	
		+ (char)2 + (char)2 + (char)2 + (char)2 + (char)2 + (char)2
		+ (char)2 + (char)2 + (char)2 + (char)2 + (char)2 + (char)2
		+ (char)2 + (char)2 + (char)2 + (char)2 + (char)2 + (char)2
		+ (char)3 + (char)3 + (char)3 + (char)3 + (char)3 + (char)3
		+ (char)3 + (char)3 + (char)3 + (char)3 + (char)3 + (char)3
		+ (char)3 + (char)3 + (char)3 + (char)3 + (char)3 + (char)3
		+ (char)3 + (char)3 + (char)3 + (char)3 + (char)3 + (char)3		
		+ (char)4
		;
}																																						