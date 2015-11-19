/*
FoilistMicro by Sheldor, date: 05/06/2015, codesize:  748 bytes
a microbot with stop and go/random movement and a basic DC gun
v1.2.0--added forward wall dimension, waves based on velocities

Foil is one of the three forms of modern sport fencing,
along with Epee and Sabre.  http://en.wikipedia.org/wiki/Foil_%28fencing%29
 
Credits:
A general thanks to all open source bot authors and contributors to the RoboWiki.  Without them, Robocode would not be nearly as advanced as it currently is.
Bots which I'd like to specifically credit include, but are not limited to:
	targeting: jk.mini.CunobelinDC, Falcon, pez.micro.Aristocles, kc.micro.Thorn, jk.micro.Connavar, nz.jdc.HedgehogGF
	movement : jk.micro.Cotillion, kc.micro.Thorn, nz.jdc.HedgehogGF, wiki.nano.RaikoNano

FoilistMicro is released under the terms of the RoboWiki Public Code License (RWPCL) - Version 1.1.
see license here:  http://robowiki.net/wiki/RWPCL
*/

package sheldor.micro;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.*;

public class FoilistMicro extends AdvancedRobot
{
	//constants
	static final int    GUESS_FACTORS = 49;
	static final int    MIDDLE_FACTOR = (GUESS_FACTORS - 1) / 2;
	static final double MAXIMUM_ESCAPE_ANGLE = 0.72727272727272727272727272727273; //8 / 11
	static final double FACTOR_ANGLE = MAXIMUM_ESCAPE_ANGLE / MIDDLE_FACTOR;
	
	//global variables
	static double direction = 1;
	static double enemyBulletSpeed;
	static double enemyDirection;
	static double enemyEnergy;
	static double previousLateralVelocityDimension;
	static int    movementMode;
	
	static double enemyHeading;
	static double enemyVelocity;
	
	static ArrayList hitScans = new ArrayList();
	
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
		int    antiRam;
		double absoluteBearing;
		double enemyDistance;
		double lateralVelocity;
		double localEnemyDirection;				
		double offset;
		double theta;
		Wave   wave = new Wave();
		
		/*********************************************
		 *---------------MOVEMENT CODE---------------*
		 *********************************************/				
		
		//fire medium power bullets most of the time, use full power at very close range
		//also, cut back power as energy decreases, credit to HedgehogGF for inspiration
		//only fire waves when a bullet is fired
		if (setFireBullet((offset = (2 + (antiRam = (100 / (int)(wave.enemyDistance = enemyDistance = e.getDistance()))))) - (5 / getEnergy())) != null)
		{			
			addCustomEvent(wave);
		}
		
		//wall smoothing based on HedgehogGF's
		while(fieldContains(theta = (wave.absoluteBearing = absoluteBearing = 
			(e.getBearingRadians() + getHeadingRadians())) + direction * (offset -= 0.02), 160) > 0);
		setTurnRightRadians(Math.tan(theta -= getHeadingRadians()));
			
		//variable length stop and go movement
		//move when the enemy fires, or when the robot is moving randomly, or when the enemy is very close
		double energyDrop;
		if ((energyDrop = (enemyEnergy - (enemyEnergy = e.getEnergy()))) > movementMode - antiRam)
		{
			//credit to Cotillion for the movement length calculator
			//credit to HedgehogGF for the copySign trick
			setAhead(Math.copySign(((3 + (int)(energyDrop * 1.999999)) << 3), Math.cos(theta)));
		}
		
		//random movement based on Toorkild's
		//don't reverse randomly if the bot is in Stop and Go mode, or if the enemy is very close
		//reverse direction if the bot starts diving toward the enemy
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
		wave.enemyDirection = localEnemyDirection = (enemyDirection = Math.signum(0.00000000000001 + ((lateralVelocity = ((enemyVelocity = e.getVelocity()) * (Math.sin((enemyHeading = e.getHeadingRadians()) - absoluteBearing))))) + (enemyDirection / 100))) * FACTOR_ANGLE;
		
		
		double[] localScan;
		(localScan = wave.scan = new double[5])[2] = previousLateralVelocityDimension;
		localScan[1] = enemyDistance / 125;
		localScan[3] = previousLateralVelocityDimension = Math.abs(lateralVelocity);
	
		int i = 8;
		do
		{
			localScan[4] += fieldContains(absoluteBearing + (localEnemyDirection * ((MIDDLE_FACTOR / 8) * i)), enemyDistance);
		}
		while(--i > 0);
		
		//DC based on CunobelinDC's movement
		//go through each guess factor and calculate its score
		int highestScoringGF = MIDDLE_FACTOR;
		double highestScore = 0;
		do
		{					
			double score = 0;	
			int k = 0;
			
			try{
			while(true)
			{
				double[] scan = (double[])hitScans.get(k++);
				
				double distance = 1.0 / 4000; //initialise distance as a positive number to prevent division by zero when distance is zero
				int j = 4;
				do
				{
					distance += Math.abs(localScan[j] - scan[j]);
				}
				while(--j > 0);
				
				score += 1 / (distance * ((1.0 / 50) + Math.abs(i - scan[0])));
			}
			}catch (Exception ex){};
				
			if (score > highestScore)
			{
				highestScoringGF = i;
				highestScore = score;
			}
		}
		while (++i <= GUESS_FACTORS - 1);
		
		//aim the gun at the highest scoring guess factor,
		//with a slight offset to defeat simple bullet shielding
		setTurnGunRightRadians(0.0005 + Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()
			+ (localEnemyDirection * (highestScoringGF - MIDDLE_FACTOR))));
		
		//imperfect radar lock
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
	}
	
	public void onBulletHit(BulletHitEvent e)
	{
		//adjust the enemy energy variable when the bot hits the enemy,
		//makes a big difference against LT
		enemyEnergy -= 10;
	}
	
	public void onHitByBullet(HitByBulletEvent e)
	{
		//adjust the enemy energy variable when the bot gets hit
		//store the velocity of the enemy's bullet for the random movement
		enemyEnergy += 20 - (enemyBulletSpeed = e.getVelocity());
    }
	
	public void onDeath(DeathEvent e)
	{
		//if the bot dies in the first five rounds, use random movement
		if (getRoundNum() < 5)
		{
			movementMode = -1;
		}
	}
	
	//This method returns 1 if a point projected from the bot's location by the 
	//"heading" and "distance" parameters is outside of the battlefield, and 0 if it is not.
	//credit to HedgehogGF
	private int fieldContains(double heading, double distance)
	{
		return Integer.signum(new Rectangle2D.Double(18, 18, 764, 564).outcode(getX() + distance * Math.sin(heading), getY() + distance * Math.cos(heading)));
	}
	
	static class Wave extends Condition
	{
		//global variables
		double absoluteBearing;
		double enemyDirection;
		double bearingOffset;
		double enemyDistance;
		double waveDistanceTraveled;
		double[] scan;			
		
		public boolean test()
		{
			double relativeHeading;
			bearingOffset += (enemyVelocity * Math.sin(relativeHeading = (enemyHeading - absoluteBearing))) / (enemyDistance += (enemyVelocity * Math.cos(relativeHeading)));
			
			//check if the wave has passed the enemy's current location
			if (Math.abs((waveDistanceTraveled += 14) - enemyDistance) <= 7)
			{
				//calculate the guess factor that the enemy has visited
				scan[0] = Math.round((bearingOffset / enemyDirection) + MIDDLE_FACTOR);
				
				//add this wave's scan array to the log
				hitScans.add(this.scan);
			}
			return false;
		}
	}
}