package gh.ghgun;
import robocode.*;
import robocode.util.Utils;
import robocode.Rules;
import java.awt.Color;
import java.awt.geom.*;
import java.util.*;

// check the !??! : these are things to do / to check

/**
 * GresVretter - a gun by Gert Heijenk
 *
 * GuessFactorTargeting for the bot GresSuffurd
 *
 * Revision information:
 * v0.0.1  20060514 no gunnery at all, just make it compile
 * v0.0.2  20060531 implemented radar
 *                  first gun-attempt with my own waves and Kawigi's tutorial
 * v0.0.3  20060612 segmenting on distance only
 * v0.0.4  20060618 segmenting on velocity only
 * v0.0.5  20060621 segmenting on acceleration only
 * v0.0.6  20060624 segmenting on wallproximity only
 * v0.0.7  20060627 segmenting on acceleration, distance and wallproximity
 * v0.0.8  20060628 segmenting on acceleration, distance
 * v0.0.9  20060628 segmenting on acceleration, wallproximity
 * v0.1.0  20060629 segmenting on acc, dist, wall
 *                  use nr of samples to choose simpler gun if not enough samples available
 * v0.1.1  20060713 some updates due to WeefSuffurd
 *                  added rudimentary onPaint() routine to see the GF's
 * v0.1.2  20061001 segmenting only on lateral and advancing velocity (improved v0.0.4)
 * v0.1.3  20061003 0.1.1 plus: set all bins that would hit instead of only one
 *                  take the highest concentration of bins(1-2-1) instead of just highest bin
 * v0.1.4  20061007 bugfixing hittedbin detection, because I only win 7-3 against SittingDuck
 *                  if enemy is disabled, forget ALL waves and shoot HOT
 * v0.1.5  20061028 handle all necessary events, no functional change
 * v0.2.0  20061115 also fire non-bullet waves with 0.1 weighting
 * v0.2.1  20061127 updated energymanagement and anti-ram behaviour
 * v0.2.2  20061209 back to v0.2.0
 * v0.2.3  20070125 do some decay on binvalues
 *                  correct weighting for non-bullet waves in unsegmented buffer
 * v0.2.4  20070207 do not fire virtual waves if opponent has not moved yet
 * v0.2.5  20070222 change wallsegmentation from 3 to 5 segments
 * v0.2.6  20070522 no decay on binvalues
 *                  no GF1 protection multiplyfactor
 * v0.2.7  20080213 be more conservative on energy in endgame
 *                  small tweak to prevent disabling myself
 *                  added 'distance last 14 ticks' as segmentation (4 segments)
 * v0.2.8  20090905 add some randomness to bullets fired at bulletcatchers
 * v0.2.9  20090914 decrease randomness of fire
 *                  changed wallsegment calculation from linear to orbital
 * v0.2.10 20090926 keep firing full-power against rammers
 * v0.2.11 20091003 non-bullet waves are weighted less ( 0.1 -> 0.05)
 * v0.2.12 20091026 0.2.10 + increase nr bins in gun, record best bin to fire relative to botsize
 * v0.2.13 20100413 fire 1.95 power bullets iso 1.9 power (inspired by voidious.Diamond 0.5.5)
 * v0.2.14 20110115 fire x.x5 power bullets whenever possible, minor energymanagement thingies
 * v0.2.15 20110123 second gun with decay on stats
 * v0.2.16 20110125	updated painting and processing the 'killing bullet'
 * v0.2.17 20110128 improved 'precise intersection'
 *					remember enemies previous direction, handles stop-and-go better
 * v0.2.18 20110228 re-enable 'BulletCatcher tweak'
 *					prevent pumping bullets in wall by simulating precise MEA in gun
 * v0.2.19 20110313 Use estimated bearing iso bearing previous tick when aiming gun. Solves bullet-angle vs virtual angle issue.
 * v0.2.20 20110411 use lateral velocity as attribute instead of distance
 * v0.3.0  20110427 improve precise MEA especially against rambots
 *					mark bullet-hit-bullet waves as non-bullet waves
 *					some energymanagement / bulletpower issues
 *					general cleanup
 * v0.3.1  20110503 revert 'mark bullet-hit-bullet waves as non-bullet waves', what was I thinking . . .
 * v0.3.2  20110922 normal gun: make bulletwaves less important: 7 virtualwaves = 1 bulletwave
 *                  decaying gun: make bulletwaves more important: 14 virtualwaves = 1 bulletwave
 *                  in both guns it was till now: 10 virtualwaves = 1 bulletwave
 * v0.3.3  20121006 better bulletpower calculation: x.x5 powerlist and NO adaption to enemyenergy
 */

public class GresVretter {
	// some constants
	static final String VERSION			= "0.3.3";
	static final double WALLMARGIN		= 17.9;
	static final double BOTWIDTH		= 36;
	static final double HALFBOTWIDTH	= BOTWIDTH / 2;
	static final double BLINDMANSTICK	= 110;	// max blindmanstick for enemies predicted movement
	static final double RAMBOTDISTANCE	= 120;
	static final double RAMFIREPOWER	= 3.00;	// rambot firepower
	static final double MAXFIREPOWER	= 2.65;	// close range firepower
	static final double STDFIREPOWER	= 1.95;	// standard firepower
	static final double MINFIREPOWER	= 0.15;	// shooting 'peas' for energy management reasons
	static final double DUCKFIREPOWER	= 0.1;	// real minimum firepower, only for sitting ducks
	static final double BINFACTOR		= 1.0;	// the calculation factor used in maxEscapeAngle
	static final double GUNDECAY		= 0.9;	// multiply factor on each real bullet to decay the stats
	static final double VWAVEWEIGHT1	= 0.14;	// virtual wave weight of normal gun
	static final double VWAVEWEIGHT2	= 0.07;	// virtual wave weight of decaying gun
	static final int	BINS			= 73;	// Botwidth would cover 4-6 bins on default distance
	static final int	MIDBIN			= BINS / 2;
	static final int	ACCSEG			= 6;	// accell, decell, time <6, <16, <36, >=36
//	static final int	DISTSEG			= 4;	// 0-300-450-600-9999
	static final int	VLATSEG			= 3;	// 0 - 2.5 - 5.5 - 8
	static final int	WALLSEG			= 6;	// none, heading for wall close, heading for wall far, retreating from wall close, retreating from wall far, corner
	static final int	DL14SEG			= 4;	// 14-tick distance 0-28 28-56 56-84 84-112 (max in 14 ticks)
    public static boolean TC_flag		= false;	// TargetingChallenge

	// real statistics counters
	static int	bullhit;	// the total number of bullets that hit
	static int	bullmis;	// the total number of bullets that missed
	static int	scanmis;	// the total number of radarkicks, indicating lock lost
	static int  enemyCollision;	// keep track of number of collisions with enemy, for anti-ram bulletpower
	static double [] powerlist = { MAXFIREPOWER, 2.15, 1.95, 1.65, 1.45, 1.15, 0.95, 0.65, 0.45, MINFIREPOWER, MINFIREPOWER };

	// (virtual) gun statistics
	static int 	gunfired;	// bullets fired that would have reached target
	static int	gun1hit;	// gun1 bullets fired that would have hit target
	static int	gun2hit;	// gun2 bullets fired that would have hit target

	static Rectangle2D.Double fireField;	// the shootable battlefield

//	ArrayList<Wave> waveList;				// the list with waves
	ArrayList waveList;						// the list with waves
	
	// gun1: acc, vlat, wall, d14ticks, without decay
	static double[]			att0Buf = new double[BINS];
	static double[][]		att1Buf = new double[ACCSEG][BINS+1];								// acceleration only
//	static double[][][]		att2Buf = new double[ACCSEG][DISTSEG][BINS+1];						// + distance
	static double[][][]		att2Buf = new double[ACCSEG][VLATSEG][BINS+1];						// + lateral velocity
	static double[][][][]	att3Buf = new double[ACCSEG][VLATSEG][WALLSEG][BINS+1];				// + wall
	static double[][][][][] att4Buf = new double[ACCSEG][VLATSEG][WALLSEG][DL14SEG][BINS+1];	// + distance last 14 ticks

	// gun2: same, but with decay
	static double[]			att0Bufd = new double[BINS];
	static double[][]		att1Bufd = new double[ACCSEG][BINS+1];  							// acceleration only
//	static double[][][]		att2Bufd = new double[ACCSEG][DISTSEG][BINS+1];						// + distance
	static double[][][]		att2Bufd = new double[ACCSEG][VLATSEG][BINS+1];						// + lateral velocity
	static double[][][][]	att3Bufd = new double[ACCSEG][VLATSEG][WALLSEG][BINS+1];			// + wall
	static double[][][][][]	att4Bufd = new double[ACCSEG][VLATSEG][WALLSEG][DL14SEG][BINS+1];	// + distance last 14 ticks

	int direction = 1;

	Point2D.Double myPos		= new Point2D.Double();	// my current position
	Point2D.Double myNextPos	= new Point2D.Double(); // used to determine HOTAngle in wave !!
	Point2D.Double enemyPos		= new Point2D.Double();	// enemy's position
	Point2D.Double enemyNextPos	= new Point2D.Double();	// enemy's next position
//	ArrayList<Point2D.Double> ePosT;				// history of enemypositions
	ArrayList ePosT;				// history of enemypositions
	Wave	newWave;
	
	double enemyDistance;			// the distance between me and the enemy
	double enemyVelocity;			// the velocity of the enemy
	double enemySignVelocity = 1;	// the sign of the velocity (needed in rare cases)
//	double enemyEnergy;				// the remaining energy of the enemy
	double enemyDirection;			// the direction the enemy is currently moving
	double enemyLastDirection = 1;	// the previous direction
	double enemyHeading;			// the heading of the enemy
	int currTime;					// the current scantime
	long timeSinceVChange;			// the time since the last velocitychange
	int enemyAccBin;				// the acceleration bin that is used
//	int enemyDistBin;				// the distance bin that is used
	int enemyVLatBin;				// the lateral velocity bin that is used
	int enemyWallBin;				// the wall-proximity bin that is used
	int enemyDL14Bin;				// the 'distance last 14 ticks' bin that is used
	double firePower;				// the power I will shoot with
	double radarMove = 1;			// the size and direction the radar has to move
	boolean sittingDuck;			// if opponent is disabled, fire HOT and do not fire waves
	boolean fireWaves;				// do not fire virtual waves until opponent starts moving
	int duckShot;					// Timer for shot taken at sitting duck, do not fire more bullets
	
    AdvancedRobot robot;

	/**
	 * GresVretter
	 */
    public GresVretter( AdvancedRobot robot) {
		this.robot = robot;

		fireField = new Rectangle2D.Double( WALLMARGIN, WALLMARGIN,
						robot.getBattleFieldWidth() - 2 * WALLMARGIN,
						robot.getBattleFieldHeight() - 2 * WALLMARGIN);

//		waveList = new ArrayList<Wave>();
//		ePosT = new ArrayList<Point2D.Double>();
		waveList = new ArrayList();
		ePosT = new ArrayList();
		newWave = new Wave();
		
		if (robot.getRoundNum() == 0) {
		    System.out.println("GresVretter gun version: " + VERSION);
			att0Buf[MIDBIN] = att0Bufd[MIDBIN] = 0.1;		// prepopulate head-on
		}
	}

	/**
	 * onScannedRobot: Event from scanning another robot
	 */
    public void onScannedRobot( ScannedRobotEvent e) {
		// gather all kind of information
		currTime = newWave.fireTime = (int)robot.getTime();

		// information about me !??! next pos: take into account my accel/decel
		myPos.setLocation( robot.getX(), robot.getY());
		myNextPos.setLocation( doProjectPos( myPos, robot.getHeadingRadians(), robot.getVelocity()));

		// information about the enemy
		double eEnergy = e.getEnergy();
		double enemyAbsBearing = robot.getHeadingRadians() + e.getBearingRadians();
		double enemyLastVelocity = enemyVelocity;
		enemyDistance = e.getDistance();
		enemyHeading  = e.getHeadingRadians();
		enemyVelocity = e.getVelocity();
		if (enemyVelocity != 0.0) {
			fireWaves = true;		// do not count 'sur place' at beginning of round
			enemySignVelocity = Math.signum(enemyVelocity);		// remember sign of enemyvelocity
		}
		double latVelocity = enemyVelocity * Math.sin( e.getHeadingRadians() - enemyAbsBearing);
		enemyDirection = Math.signum(latVelocity);
		if (enemyDirection == 0)
			enemyDirection = enemyLastDirection;
		enemyLastDirection = newWave.direction = enemyDirection;
		
		enemyPos.setLocation( doProjectPos( myPos, enemyAbsBearing, enemyDistance));
		// !??! Calculating next enemy position should take acc/decel into account
		enemyNextPos.setLocation(doProjectPos( enemyPos, e.getHeadingRadians(), enemyVelocity));
		ePosT.add(0, enemyPos);
		
		// calculate the active segments of the attributes for this tick
//		enemyDistBin = newWave.distanceBin = Math.max( 0, Math.min( DISTSEG - 1, (int)enemyDistance / 150 - 1));
		enemyVLatBin = newWave.vlateralBin = (int)((Math.abs( latVelocity) + 0.5) / 3.0);
		// accelerationsegment, ignore small variances in speed
		if (Math.abs( enemyVelocity - enemyLastVelocity) > 0.9) {
			enemyAccBin = (Math.abs( enemyVelocity) - Math.abs( enemyLastVelocity) > 0 ? 0 : 1);
			timeSinceVChange = 0;
		}
		else {
			timeSinceVChange++;
			if (timeSinceVChange < 6 ) enemyAccBin = 2;
			else if (timeSinceVChange < 16) enemyAccBin = 3;
			else if (timeSinceVChange < 36) enemyAccBin = 4;
			else enemyAccBin = 5;
		}
		newWave.accelerationBin = enemyAccBin;
		
		double MEA = Math.asin( Rules.MAX_VELOCITY / Rules.getBulletSpeed( firePower));

        // Check when enemy reaches the wall when circling me.
		int ewallbinbck = enemyWallBin = 0;
		for (int i = 0; i < BINS; i++) {
			if (!fireField.contains( doProjectPos( myPos, enemyAbsBearing + (enemyDirection * (i/(double)BINS) * MEA), enemyDistance))) {
				enemyWallBin = (i < MIDBIN ? 1 : 2);
				break;
			}
		}
//		if (enemyWallBin == 0) {
			for (int i = 0; i < BINS; i++) {
				if (!fireField.contains( doProjectPos( myPos, enemyAbsBearing - (enemyDirection * (i/(double)BINS) * MEA), enemyDistance))) {
//					enemyWallBin = (i < MIDBIN ? 3 : 4);
					ewallbinbck = (i < MIDBIN ? 3 : 4);
					break;
				}
			}
//		}
		if (enemyWallBin == 0)
			enemyWallBin = ewallbinbck;
		else if ((enemyWallBin == 1) && (ewallbinbck == 3))
			enemyWallBin = 5;
		newWave.wallProxBin = enemyWallBin;
						
 		// distance last 14 ticks segment
		enemyDL14Bin = (int)(enemyPos.distance((Point2D.Double)ePosT.get(Math.min(14, ePosT.size() - 1))) / 28);
		newWave.dist14Bin = enemyDL14Bin = Math.min( DL14SEG - 1, enemyDL14Bin);

		if (eEnergy == 0.0 && enemyVelocity == 0.0 && robot.getOthers() > 0) 
			sittingDuck = true;
		else {
			sittingDuck = false;
			duckShot = 0;
		}
		// information complete, do the 'normal' actions
		doCheckWaves();
		// if my or enemies energy significantly changed, do not fire this tick (rare)
		if (Math.abs( doFirePower(eEnergy) - firePower) < 0.26)
			doFireGun();

		// start with the preparation for next tick
		doCheckRadar();
		firePower = doFirePower( eEnergy);
		
		newWave = new Wave();
		newWave.fireLocation.setLocation( myNextPos);
		newWave.HOTAngle = doGetAngle( myNextPos, enemyNextPos);
		doMoveGun();
	}

	/**
	 * simulatedScannedRobot: Simulated event so the 'killer-bullet' is really counted
	 */
    public boolean simulatedScannedRobot( ) {
		currTime = (int)robot.getTime();
		doCheckWaves();
		return (waveList.isEmpty());
	}

	/**
	 * onBulletHit: I hit the other
	 */
    public void onBulletHit( BulletHitEvent e) {
		bullhit++;
//		System.out.println("Regular bullethit");
    }

	/**
	 * onBulletMissed: Damn, I missed (again)
	 */
	public void onBulletMissed(BulletMissedEvent e)
	{
		bullmis++;
	}

	/**
	 * onBulletHitBullet: Do nothing here, virtual bullet still counts
	 */
    public void onBulletHitBullet( BulletHitBulletEvent e) {
//		System.out.println("BulletHitBullet !");
    }

	/**
	 * onHitRobot: Count how many times we collided, too much: you're a rammer
	 */
	public void onHitRobot( HitRobotEvent e) {
		// just count the collisions
		enemyCollision++;
	}

	/**
	 * onWin: Start with the victory dance
	 */
	public void onWin(WinEvent e)
	{
		//Victory dance	
		robot.setTurnGunRight( Double.POSITIVE_INFINITY);
		robot.setTurnRadarLeft( Double.POSITIVE_INFINITY);
	}

	/**
	 * doCheckRadar: Let the radar produce a nice sweep
	 */
	void doCheckRadar( ) {
		// make a nice 'botwide' scanarc
		radarMove = Utils.normalRelativeAngle( doGetAngle( myNextPos, enemyNextPos) - robot.getRadarHeadingRadians());
		radarMove += Math.atan( (Math.signum( radarMove) * 25) / enemyDistance);
		robot.setTurnRadarRightRadians( radarMove);
	}

	/**
	 * doKickRadar: Kick the radar, because it lost its lock
	 */
	public void doKickRadar() {
		// I am in need of an(other) opponent, find one !
		robot.setTurnRadarRight( radarMove * Double.NEGATIVE_INFINITY);
//		System.out.println(robot.getTime()+" radarlock lost! Kick radar");
		scanmis++;
	}

	/**
	 * doFirePower: Select the optimal firepower to use
	 */
	double doFirePower( double eLife) {

		if (TC_flag == true)
			return Math.min( RAMFIREPOWER, robot.getEnergy());

		double power;
		// do energymanagement, but not at very close range
		if (enemyDistance > RAMBOTDISTANCE) {
			double basepower = (enemyDistance > (RAMBOTDISTANCE * 1.5) ? STDFIREPOWER : MAXFIREPOWER);
			double killpower = (eLife <= 4.0 ? eLife/4.0 : (eLife + 2.0)/6.0);
//			double padjust = Math.max( -10, Math.min( robot.getEnergy() - eLife, 30)); // code from DrussGT
			double padjust = 0;
			double balancedpower = ( robot.getEnergy() + 0.5 * padjust) / 15;
			power = Math.min( killpower, Math.min( balancedpower, basepower));
			if (power > DUCKFIREPOWER) {					// enough energy left, exploit firepower x.x5 bug
	            int i=0;
				while ( i < powerlist.length - 1) {
					if ( power >= powerlist[i])
						break;
					i++;
				}
				power = powerlist[i];
			} else
				power = Math.max( DUCKFIREPOWER, power);	// for survival reasons, shoot absolute minimum
		}
		else {
			power = (enemyCollision > (robot.getRoundNum() + 1)) ? RAMFIREPOWER : MAXFIREPOWER;
		}
		return power;
	}

	/**
	 * doMoveGun: Move the gun to the optimal angle to hit the target
	 */
	void doMoveGun() {
		int bestindex1, bestindex2;
		int minbin, maxbin;

		// fire HOT if enemy is disabled or gun is hot
		if ((sittingDuck == true) || (robot.getGunHeat() > 0.29))  {
			bestindex1 = MIDBIN;
			bestindex2 = MIDBIN;
		}
		else {
			// check if minbin and maxbin can reach target without ending up in wall
//			minbin = 0;	// reverse direction
			minbin = doCheckMEA ( -1 );	// reverse direction
//			maxbin = BINS-1;
			maxbin = doCheckMEA (  1 );	// continue direction

//			System.out.print(" min: " + minbin + " max: " + maxbin);
			// !??! // swap if necessary (only with rambots?)
			if (minbin > maxbin) {
				int tmpbin = minbin;
				minbin = maxbin;
				maxbin = tmpbin;
//				System.out.print(" Swap minbin <=> maxbin !!");
			}
			// head-on should always be possible ! !??! especially with rambots this is needed
//			if (minbin > MIDBIN) { minbin = MIDBIN - 4; System.out.print(" minbin wrong!"); }
//			if (maxbin < MIDBIN) { maxbin = MIDBIN + 4; System.out.print(" maxbin wrong!"); }
			// !??!
//			System.out.println(" min: " + minbin + " max: " + maxbin);
			
			// get best bin for gun1
			if (att4Buf[enemyAccBin][enemyVLatBin][enemyWallBin][enemyDL14Bin][BINS] > 4) {
				bestindex1 = doGetBestIndex( minbin, maxbin, att4Buf[enemyAccBin][enemyVLatBin][enemyWallBin][enemyDL14Bin]);
			}
			else if (att3Buf[enemyAccBin][enemyVLatBin][enemyWallBin][BINS] > 4) {
				bestindex1 = doGetBestIndex( minbin, maxbin, att3Buf[enemyAccBin][enemyVLatBin][enemyWallBin]);
			}
			else if (att2Buf[enemyAccBin][enemyVLatBin][BINS] > 4) {
				bestindex1 = doGetBestIndex( minbin, maxbin, att2Buf[enemyAccBin][enemyVLatBin]);
			}
			else if (att1Buf[enemyAccBin][BINS] > 4) {
				bestindex1 = doGetBestIndex( minbin, maxbin, att1Buf[enemyAccBin]);
			}
			else {
				bestindex1 = doGetBestIndex( minbin, maxbin, att0Buf);
			}
			// get best bin for gun2
			if (att4Bufd[enemyAccBin][enemyVLatBin][enemyWallBin][enemyDL14Bin][BINS] > 4) {
				bestindex2 = doGetBestIndex( minbin, maxbin, att4Bufd[enemyAccBin][enemyVLatBin][enemyWallBin][enemyDL14Bin]);
			}
			else if (att3Bufd[enemyAccBin][enemyVLatBin][enemyWallBin][BINS] > 4) {
				bestindex2 = doGetBestIndex( minbin, maxbin, att3Bufd[enemyAccBin][enemyVLatBin][enemyWallBin]);
			}
			else if (att2Bufd[enemyAccBin][enemyVLatBin][BINS] > 4) {
				bestindex2 = doGetBestIndex( minbin, maxbin, att2Bufd[enemyAccBin][enemyVLatBin]);
			}
			else if (att1Bufd[enemyAccBin][BINS] > 4) {
				bestindex2 = doGetBestIndex( minbin, maxbin, att1Bufd[enemyAccBin]);
			}
			else {
				bestindex2 = doGetBestIndex( minbin, maxbin, att0Bufd);
			}
		//			System.out.println(currTime + " min " + minbin + " max " + maxbin + " index1 " + bestindex1 + " index2 " + bestindex2);
		}
//		System.out.println( currTime + " ==> " + bestindex1 + "  " + bestindex2);
		// to keep 'real' statistics on guns
		newWave.gun1Bin = bestindex1;
		newWave.gun2Bin = bestindex2;

		// this should do the opposite of the math in the WaveBullet:
		double guessfactor = (double)(bestindex1 - MIDBIN) / MIDBIN;
		double angleOffset = enemyDirection * guessfactor * Math.asin( Rules.MAX_VELOCITY / Rules.getBulletSpeed( firePower)) * BINFACTOR;
		double anglerandom = (Math.random() < 0.5 ? 0.6 : -0.6) / enemyDistance;		// imperfection to lure bulletcatchers
		newWave.gun1Angle = angleOffset + anglerandom;
		guessfactor = (double)(bestindex2 - MIDBIN) / MIDBIN;
		angleOffset = enemyDirection * guessfactor * Math.asin( Rules.MAX_VELOCITY / Rules.getBulletSpeed( firePower)) * BINFACTOR;
		newWave.gun2Angle = angleOffset + anglerandom;
		double bestangle = (gun1hit >= gun2hit ? newWave.gun1Angle : newWave.gun2Angle);
		robot.setTurnGunRightRadians( Utils.normalRelativeAngle( newWave.HOTAngle - robot.getGunHeadingRadians() + bestangle));
				
	}

	/**
	 * doFireGun: Fire the gun.
	 */
	void doFireGun() {

		// firepower is determined previous tick, so do extra checks to prevent disableing ourselves
		if ((TC_flag == false) && (enemyDistance > RAMBOTDISTANCE)) {
			if ((sittingDuck == false) && (robot.getEnergy() <= firePower))
				firePower = 0.0;
			if ((sittingDuck == true) && (firePower > 0.01))
				if (--duckShot <= 0) {
					firePower = DUCKFIREPOWER;
					duckShot = (int)(enemyDistance / Rules.getBulletSpeed( DUCKFIREPOWER));		// fire once until hit or missed
//					System.out.println("Fired at sitting Duck");
				}
				else
					firePower = 0.0;
		}
		newWave.bulletVelocity = Rules.getBulletSpeed( firePower);
		newWave.maxEscAngle = Math.asin( Rules.MAX_VELOCITY / newWave.bulletVelocity) * BINFACTOR;
		if ((robot.getGunHeat() == 0.0) && (robot.getGunTurnRemaining() == 0.0) && (firePower >= DUCKFIREPOWER)) {
			robot.setFire( firePower);
//System.out.println("Firing with bulletpower: " + firePower);
			newWave.bulletWave = true;
			if (sittingDuck == false)
				waveList.add( newWave);
		}
		else if (firePower >= MINFIREPOWER) { 
			if ((sittingDuck == false) && (fireWaves == true)) {
				newWave.bulletWave = false;
				waveList.add( newWave);
			}
		}
	}

	/**
	 * doProjectPos: Calculate position out of origin position, heading and distance
	 */
	public Point2D.Double doProjectPos( Point2D.Double originpos, double head, double dist){	
		return new Point2D.Double( originpos.x + (dist * Math.sin( head)), originpos.y + (dist * Math.cos( head)));			
	}

	/**
	 * doGetAngle: Calculate angle out of two positions
	 */
	double doGetAngle( Point2D.Double startPos, Point2D.Double endPos) {
		return ( Math.atan2( endPos.x - startPos.x, endPos.y - startPos.y));
	}

	/**
	 * doCheckMEA: Check if the full MEA can be used, or just a subset
	 * When an opponent closes in on the wall, he can not reach the ultimate MEA.
     * Calculate the precise reacheable MEA, so no bullets end up in the wall.
     * The same for when reversing direction, calculate max reacheable bin.
	 * Instead of always calculating Precise MEA, just do it just before ready to fire.
	 * Note: direction = 1: continue, direction = -1: reverse
	 * Note: use Rozu's PredictPosition on enemies movement !??!
	 */
	int doCheckMEA( int direction) {
		Point2D.Double predictedPosition = (Point2D.Double)enemyNextPos.clone();
		double predictedVelocity = enemyVelocity;
		double predictedHeading = enemyHeading;
		double moveDir = Math.signum( direction * enemySignVelocity);
		double clockwise = Math.signum( direction * enemyDirection);
		double bulletSpeed = Rules.getBulletSpeed( firePower);
		double maxTurning, moveAngle, angle, angledif;
		boolean intercepted = false;
		int ticks = 0;

//		if (moveDir == 0.0) { System.out.print("! mD !" + enemySignVelocity); moveDir = Math.signum(direction); }
//		System.out.println("mD: " + moveDir + " eV: " + enemyVelocity + " eD: " + enemyDirection + " cw: " + clockwise);
		do {
			angle = doGetAngle( myNextPos, predictedPosition);
			angledif = angle + (clockwise * Math.PI/2) - predictedHeading;
//			moveAngle = doWallSmoothing( predictedPosition, angle + (enemyDirection * direction * Math.PI/2)) - predictedHeading;
			moveAngle = doWallSmoothing( predictedPosition, angle + (clockwise * Math.PI/2), clockwise) - predictedHeading;

//			if ((ticks/2)*2 == ticks)
//				System.out.print("T:" + (ticks+1) + " ang: " + (int)(angle* 100)/100.0 + " mAng: " + (int)(moveAngle*100)/100.0 + " cos: "+ (int)(Math.cos(moveAngle)*100)/100.0);
			// this probably is done once if applicable
			if (Math.cos( angledif) < -0.4) {
				moveAngle += Math.PI;
//				System.out.print(" swap ");
			} else if ((Math.abs(Math.cos( angledif)) < 0.4) && (Math.abs(enemyVelocity) > 4.01)) {
				moveDir = Math.signum( predictedVelocity);
//			    System.out.print(" cont ");
			} else if (Math.cos( angledif) < 0.0) {
				moveAngle += Math.PI;
//				System.out.print(" swp2 ");
			}
//			} else System.out.print(" none ");

			moveAngle = Utils.normalRelativeAngle( moveAngle);
			
    		// maxTurning is built in like this, you can't turn more than this in one tick
    		maxTurning = Rules.getTurnRateRadians( Math.abs(predictedVelocity));  // bug if velocity is negative !
    		predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + Math.max(-maxTurning, Math.min(moveAngle, maxTurning)));

    		// Nice ;). if predictedVelocity and moveDir have different signs you want to brake down
    		// otherwise you want to accelerate (look at the factor "2")
 			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
    		predictedVelocity = Math.max(-Rules.MAX_VELOCITY, Math.min(predictedVelocity, Rules.MAX_VELOCITY));
			
    		// calculate the new predicted position
    		predictedPosition = doProjectPos( predictedPosition, predictedHeading, predictedVelocity);
//			if ((ticks/2)*2 == ticks)
//				System.out.println("mAng: " + (int)(moveAngle*100)/100.0 + " predH: " + (int)(predictedHeading*100)/100.0 + " predV:" + predictedVelocity);

            // check if wave reached enemies predicted position
			if (predictedPosition.distance( myNextPos) < (++ticks * bulletSpeed))
                intercepted = true;
				
			if (ticks > 200) {
				intercepted = true;
				System.out.println( "AAAAArgh, enemies predicted position is non-existing anymore");
			}
			if (direction == -1) {
				newWave.pntccw.add( predictedPosition.clone());
			}
			else {
				newWave.pntcw.add( predictedPosition.clone());
			}
								
    	} while (!intercepted);
		
		// just to paint max reacheable positions
		if (direction == -1) {
			newWave.pposccw.setLocation( predictedPosition);
//			System.out.println("edir " + enemyDirection + " CCWpos: " + predictedPosition);
		}
		else {
			newWave.pposcw.setLocation( predictedPosition);
//			System.out.println("edir " + enemyDirection + "  CWpos: " + predictedPosition);
		}

   		// take size of bot in account (rudimentary)
   		predictedPosition = doProjectPos( predictedPosition, predictedHeading, Rules.MAX_VELOCITY);

		// Now calculate max reacheable bin
		angle = Utils.normalRelativeAngle( doGetAngle( myNextPos, predictedPosition) - 
										   doGetAngle( myNextPos, enemyNextPos));
		double mea = Math.asin( Rules.MAX_VELOCITY / bulletSpeed) * BINFACTOR;
		double guessFactor = Math.max(-1, Math.min(1, angle / mea)) * enemyDirection;
		return ( (int)Math.round( MIDBIN * ( guessFactor + 1.0)));
	}

	/**
	 * doGetBestIndex: Get the best scoring index to aim
	 * minbin and maxbin represent the maximum reacheable escape angle (MEA)
	 * !??! I know this should be checked with the nr of bins of BOTWIDTH,
	 * but that would require two nested loops.
	 * I am just lazy for now and use 5 bins instead of the real nr of bins
	 */
	int doGetBestIndex( int minbin, int maxbin, double[] testbuf) {
		int mini = Math.min( Math.max( 0, minbin - 2), BINS - 5);	// set startbin
		int maxi = Math.max( Math.min( maxbin, BINS - 3), 3);		// set endbin
		int bindex = mini + 2;
		double tvalue = testbuf[bindex-2] + testbuf[bindex-1] + testbuf[bindex] + testbuf[bindex] + testbuf[bindex+1] + testbuf[bindex+2];
		double bvalue = tvalue;
		for (int i = mini+3; i <= maxi; i++) {
			tvalue += (testbuf[i] + testbuf[i+2] - testbuf[i-1] - testbuf[i-3]);
			if (tvalue > bvalue) {
				bindex = i;
				bvalue = tvalue;
			}
		}
		return bindex;
	}

	/**
	 * doWallSmoothing: Suppose the opponent uses some kind of wallsmoothing
	 * opponent always smooths towards me
	 */
	public double doWallSmoothing( Point2D.Double startPoint, double angle, double dirction) {
        // always turn towards enemy
		double di;
		double stick = Math.min( BLINDMANSTICK, enemyDistance/2);
		for (di = Math.PI/2; di >= 0; di -= Math.PI/72) {
			if (!fireField.contains( doProjectPos( startPoint, angle + (di * dirction), stick))) {
				break;
			}
		}
		return ( angle + (di * dirction));
	}

	/**
	 * doCheckWaves: Check the status of the waves
	 */
	void doCheckWaves() {
		int i, j;
		double waveweight;

		// for every still available wave
		for (j = waveList.size() - 1; j >= 0; j--) {
		    Wave currWave = (Wave)waveList.get( j);

			// update statistics if wave has passed enemy
			if (currWave.wavepasscheck() == true) {
//				System.out.println("MinB "+currWave.getminbin()+" MaxB "+currWave.getmaxbin());

				// Decay in decayed gun
				if (currWave.bulletWave == true) {
					for (i = 0; i < BINS; i++) {
						att0Bufd[i] *= GUNDECAY;
						for (int i1 = 0; i1 < ACCSEG; i1++) {
							att1Bufd[i1][i] *= GUNDECAY;
							for (int i2 = 0; i2 < VLATSEG; i2++) {
								att2Bufd[i1][i2][i] *= GUNDECAY;
								for (int i3 = 0; i3 < WALLSEG; i3++) {
									att3Bufd[i1][i2][i3][i] *= GUNDECAY;
									for (int i4 = 0; i4 < DL14SEG; i4++) {
										att4Bufd[i1][i2][i3][i4][i] *= GUNDECAY;
									}
								}
							}
						}
					}
				}

				// Gun 1: update every bin that would have hit
				waveweight = (currWave.bulletWave == true ? 1.0 : VWAVEWEIGHT1);
				for (i = currWave.getminbin(); i <= currWave.getmaxbin(); i++) {
					att0Buf[i] += waveweight;
					att1Buf[currWave.accelerationBin][i] += waveweight;
					att2Buf[currWave.accelerationBin][currWave.vlateralBin][i] += waveweight;
					att3Buf[currWave.accelerationBin][currWave.vlateralBin][currWave.wallProxBin][i] += waveweight;
					att4Buf[currWave.accelerationBin][currWave.vlateralBin][currWave.wallProxBin][currWave.dist14Bin][i] += waveweight;
				}
				// count the samples
				att1Buf[currWave.accelerationBin][BINS] += waveweight;
				att2Buf[currWave.accelerationBin][currWave.vlateralBin][BINS] += waveweight;
				att3Buf[currWave.accelerationBin][currWave.vlateralBin][currWave.wallProxBin][BINS] += waveweight;
				att4Buf[currWave.accelerationBin][currWave.vlateralBin][currWave.wallProxBin][currWave.dist14Bin][BINS] += waveweight;

				// Gun 2: update every bin that would have hit
				waveweight = (currWave.bulletWave == true ? 1.0 : VWAVEWEIGHT2);
				for (i = currWave.getminbin(); i <= currWave.getmaxbin(); i++) {
					att0Bufd[i] += waveweight;
					att1Bufd[currWave.accelerationBin][i] += waveweight;
					att2Bufd[currWave.accelerationBin][currWave.vlateralBin][i] += waveweight;
					att3Bufd[currWave.accelerationBin][currWave.vlateralBin][currWave.wallProxBin][i] += waveweight;
					att4Bufd[currWave.accelerationBin][currWave.vlateralBin][currWave.wallProxBin][currWave.dist14Bin][i] += waveweight;
				}
				att1Bufd[currWave.accelerationBin][BINS] += waveweight;
				att2Bufd[currWave.accelerationBin][currWave.vlateralBin][BINS] += waveweight;
				att3Bufd[currWave.accelerationBin][currWave.vlateralBin][currWave.wallProxBin][BINS] += waveweight;
				att4Bufd[currWave.accelerationBin][currWave.vlateralBin][currWave.wallProxBin][currWave.dist14Bin][BINS] += waveweight;

//				if (currWave.bulletWave == true) {
//					System.out.println("Hits on bins:"+currWave.getminbin()+" till "+currWave.getmaxbin());
//				}
				// update gun statistics
				if (currWave.bulletWave == true) {
					gunfired++;
					if ((currWave.gun1Bin >= currWave.getminbin()) && (currWave.gun1Bin <= currWave.getmaxbin()))
						gun1hit++;
					if ((currWave.gun2Bin >= currWave.getminbin()) && (currWave.gun2Bin <= currWave.getmaxbin()))
						gun2hit++;
				}	

				waveList.remove(j);			// remove when wave passed enemy
			}

			// remove waves not touching the target if enemy is disabled or killed
			else if ((sittingDuck == true) || (robot.getOthers() == 0)) {
				if (currWave.getmaxbin() == 0) 
					waveList.remove(j);
			}
		}
	}

	/**
	 * onPaint: Show some gunnery stuff, maybe I am calculating something wrong
	 * updated 20110125: show angles of both guns and show angles that would have hit
     * updated 20110227: show max reacheable predicted positions of enemy
	 */
	public void onPaint(java.awt.Graphics2D g) {
		int j;
		double dist;
		Point2D botpos = new Point2D.Double();
		Point2D vbpos = new Point2D.Double();

//		if (robot.getOthers() == 0) return;
		// Make a rectangle around the enemy
		if (ePosT.size() > 0) {
			g.setColor( Color.yellow);
			g.drawRect( (int)(enemyPos.getX() - HALFBOTWIDTH), (int)(enemyPos.getY() - HALFBOTWIDTH), (int)BOTWIDTH, (int)BOTWIDTH);
		}

		// for every still available wave
		for (j = waveList.size() - 1; j >= 0; j--) {
		    Wave currWave = (Wave)waveList.get( j);
			if (currWave.bulletWave == true) {
				dist   = currWave.getWaveDistance();
				botpos = currWave.fireLocation;
				g.setColor( Color.cyan);
				vbpos  = doProjectPos( (Point2D.Double)botpos, currWave.gun1Angle + currWave.HOTAngle, dist);
				g.drawLine( (int)botpos.getX(), (int)(botpos.getY()), (int)vbpos.getX(), (int)(vbpos.getY()));
				g.setColor( Color.orange);
				vbpos = doProjectPos( (Point2D.Double)botpos, currWave.gun2Angle + currWave.HOTAngle, dist);
				g.drawLine( (int)botpos.getX(), (int)(botpos.getY()), (int)vbpos.getX(), (int)(vbpos.getY()));

				g.setColor( Color.gray);
				g.drawArc((int)(botpos.getX() - dist +1),(int)(botpos.getY() - dist+1),(int)(dist*2-2),(int)(dist*2-2), 
				          (int)Math.toDegrees(currWave.HOTAngle - currWave.maxEscAngle), (int)Math.toDegrees(2 * currWave.maxEscAngle));

				// show which angles are recorded as hit
				if (currWave.maximumBin > 0) {
					double angle = currWave.direction * ((double)(currWave.minimumBin - MIDBIN)/MIDBIN) * currWave.maxEscAngle + currWave.HOTAngle;
					Point2D.Double pnt = doProjectPos(  (Point2D.Double)botpos, angle, dist);
					g.drawLine( (int)botpos.getX(), (int)botpos.getY(), (int)pnt.x, (int)pnt.y);
					angle = currWave.direction * ((double)(currWave.maximumBin - MIDBIN)/MIDBIN) * currWave.maxEscAngle + currWave.HOTAngle;
					pnt.setLocation( doProjectPos(  (Point2D.Double)botpos, angle, dist));
					g.drawLine( (int)botpos.getX(), (int)botpos.getY(), (int)pnt.x, (int)pnt.y);
				}
				// predicted positions (only for debugging?)
				g.setColor( Color.green);
				for (int i = 0; i < currWave.pntcw.size(); i++) {
					vbpos.setLocation( (Point2D.Double)currWave.pntcw.get( i));
					g.drawOval((int)(vbpos.getX() - 2), (int)(vbpos.getY() - 2), 4, 4);
				}
				g.drawRect( (int)(currWave.pposcw.getX() - HALFBOTWIDTH), (int)(currWave.pposcw.getY() - HALFBOTWIDTH), (int)BOTWIDTH, (int)BOTWIDTH);
				g.setColor( Color.blue);
				for (int i = 0; i < currWave.pntccw.size(); i++) {
					vbpos.setLocation( (Point2D.Double)currWave.pntccw.get( i));
					g.drawOval((int)(vbpos.getX() - 2), (int)(vbpos.getY() - 2), 4, 4);
				}
				g.drawRect( (int)(currWave.pposccw.getX() - HALFBOTWIDTH), (int)(currWave.pposccw.getY() - HALFBOTWIDTH), (int)BOTWIDTH, (int)BOTWIDTH);
			}
		}
    }

	/**
	 * printStats: Print gunnery statistics on end of round
	 */
	public void printStats( int numberRound, boolean lastRound)
	{
		if (bullmis + bullhit > 0) {
			if (scanmis > numberRound)
				System.out.println("Lock lost  :" + (scanmis - numberRound));
			System.out.print("BulletStats: " + bullhit);
			System.out.print("/" + (bullhit+bullmis));
			System.out.println("   [" + ((float)bullhit*100)/(float)(bullhit + bullmis) + "%]");
			System.out.println("Robot collisions: " + enemyCollision );
			if (gunfired > 0) {
				System.out.println("Gun 1 " + Math.round(gun1hit * 10000 / gunfired) / 100.0 );
				System.out.println("Gun 2 " + Math.round(gun2hit * 10000 / gunfired) / 100.0 ); 
			}
		}
	}

	/**
	 * Wave: Info about the gunwaves
	 *
	 */
	class Wave {
		private int fireTime;
		private boolean bulletWave;
		private double bulletVelocity;
		private double HOTAngle;
		private double gun1Angle;
		private double gun2Angle;
		private double maxEscAngle;
		private double direction;
		private Point2D.Double fireLocation = new Point2D.Double();
		private Point2D.Double pposcw		= new Point2D.Double();
		private Point2D.Double pposccw		= new Point2D.Double();
		private int accelerationBin;
		private int distanceBin;
		private int vlateralBin;
		private int wallProxBin;
		private int dist14Bin;
		private int minimumBin = BINS - 1;
		private int maximumBin = 0;
		private int	gun1Bin;
		private int	gun2Bin;
		private ArrayList pntcw = new ArrayList();
		private ArrayList pntccw = new ArrayList();

		public Wave( ) {
//			System.out.println(this.fireTime+"  "+this.bulletWave);
//			if (bwave == true) {
//				System.out.println("HOT : " +Utils.normalRelativeAngle(HOTAngle));
//				System.out.println("Wav1: " +Utils.normalRelativeAngle(HOTAngle+g1angle));
//				System.out.println("Wav2: " +Utils.normalRelativeAngle(HOTAngle+g2angle));
//			}
		}

		/**
		 * wavepasscheck: Check if wave passed the current position of the enemy
		 */
		public boolean wavepasscheck() {

			int hitbin;
			Rectangle2D.Double enemySpace;	// the space the enemy occupies
			Line2D.Double  bullet  = new Line2D.Double();
		    double waveDistance = bulletVelocity * ( currTime - fireTime);
	    	if (waveDistance > fireLocation.distance( enemyPos) + 40) {
				return true;		// wave has passed, process it in stats
			}
			if (waveDistance > fireLocation.distance( enemyPos) - 3 * bulletVelocity) {
				enemySpace = new Rectangle2D.Double( enemyPos.getX() - HALFBOTWIDTH, enemyPos.getY() - HALFBOTWIDTH, BOTWIDTH, BOTWIDTH);
				if (maximumBin == 0) {
					double hitAngle = doGetAngle( fireLocation, enemyPos);
					double angleDiff = Utils.normalRelativeAngle( hitAngle - HOTAngle);
					double guessFactor = Math.max(-1, Math.min(1, angleDiff / maxEscAngle)) * direction;
					hitbin = (int)Math.round( MIDBIN * ( guessFactor + 1.0));
				} else {
					hitbin = minimumBin;
				}

				for (int tbin = hitbin; tbin >= 0; tbin--) {
					double tguessfactor = (double)(tbin - MIDBIN) / MIDBIN;
					double tangleOffset = direction * tguessfactor * maxEscAngle;
					double thead = Utils.normalRelativeAngle( HOTAngle + tangleOffset);
					bullet.setLine( doProjectPos( fireLocation, thead, waveDistance),
									doProjectPos( fireLocation, thead, waveDistance + bulletVelocity));
					if (bullet.intersects( enemySpace))
						minimumBin = tbin;
					else
						break;
				} 

				if (maximumBin != 0)
					hitbin = maximumBin;
				for (int tbin = hitbin; tbin < BINS; tbin++) {
					double tguessfactor = (double)(tbin - MIDBIN) / MIDBIN;
					double tangleOffset = direction * tguessfactor * maxEscAngle;
					double thead = Utils.normalRelativeAngle( HOTAngle + tangleOffset);
					bullet.setLine( doProjectPos( fireLocation, thead, waveDistance),
									doProjectPos( fireLocation, thead, waveDistance + bulletVelocity));
					if (bullet.intersects( enemySpace))
						maximumBin = tbin;
					else
						break;
				} 
//				System.out.println( currTime + "  Minbin: " + minimumBin + "  Maxbin: " + maximumBin);
			}
			return false;
		}

		/**
		 * getminbin: Get the lowest bin that was hit by the wave
		 */
		public int getminbin( ) {
			return Math.max(0, minimumBin);
		}

		/**
		 * getmaxbin: Get the highest bin that was hit by the wave
		 */
		public int getmaxbin( ) {
			return Math.min(maximumBin, BINS - 1);
		}

		/**
		 * isBulletWave: Check if it was a bulletwave or a non-bulletwave
		 */
	//	public boolean isBulletWave() {
	//	    return bulletWave;
	//	}

		/**
		 * getWaveDistance: return the distance the wave has travelled since firing
		 *                    for onPaint()
		 */
		public double getWaveDistance() {
		    return (bulletVelocity * ( currTime - fireTime));
		}

		/**
		 * getWaveOrigin: return the point the wave was fired from
		 *                  for onPaint()
		 */
	//	public Point2D getWaveOrigin() {
	//	    return( fireLocation);
	//	}

	}
}																																																							
