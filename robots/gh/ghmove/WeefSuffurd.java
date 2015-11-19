package gh.ghmove;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.*;
import java.util.*;

/**
 * WeefSuffurd - a movement by Gert Heijenk
 *
 * WaveSurfing movement for the bot GresSuffurd
 *
 * Revision information:
 * v0.0.1  20060514 no movement at all, just make things compile
 * v0.0.2  20060530 some stupid movement (oscillate on enemyfire)
 * v0.0.3  20060918 basic WaveSurfing, mostly taken from BasicSurfer (WaveSurfing Tutorial)
 * v0.0.4  20060925 stop surfing wave when wavefront hits me, start surfing next wave
 *                  no safe margin from walls anymore (28 ==> 18)
 * v0.0.5  20061009 adding quite strict distancing, which automatically provides some simple dive-in protection
 *                  also move when there are no waves, just circle enemy
 * v0.0.6  20061019 use a 5-bin wide hitmark iso 3 when hit
 *                  continue current move when directions have same risk
 * v0.0.7  20061023 add stopposition evaluation
 * v0.0.8  20061024 bugfix: really stop when choosing stop-position (plus some rudimentary eye-candy)
 * v0.1.0  20061028 continue surfing until no more enemywaves (means proper onWin() handling)
 *                  slowdown when wallhit is eminent (precise prediction becomes less precise in that case)
 *                  proper onDeath() handling from David Alves
 * v0.1.1  20061118 do not evaluate stop-position when opponent closer than half defaultdistance
 * v0.1.2  20061212 introduce first segmenting (currentvelocity) in wavesurfing
 * v0.1.3  20070101 do not stay too far away
 *                  segment also on nearwall
 * v0.1.4  20070114 revert nearwall segmentation
 *					also take into account second wave (if real one present)
 *					simulate wave when no airborne one available
 * v0.1.5  20070116 danger of bin independent of popularity of segment, only on popularity of bin
 * v0.1.6  20070123 always have a surfable wave (real or simulated) available
 * v0.1.7  20070207 consider also onBulletHitBullet
 * v0.1.8  20070311 introduce rolling averages in movementstats (old * 0.7 + new * 0.3)
 * v0.1.9  20070316 rollback rolling surfingstats
 *                  get rid of that 'GF1' protection multiply factor
 * v0.2.0  20070522 handle first virtual wave with correct parameters
 *                  remove virtual waves when reaching enemy
 *                  segment also on distance
 *                  small preference for current direction
 * v0.2.1  20070524 remove distance segmentation
 *                  segment on nearwall (new implementation)
 * v0.2.2  20070611 remove nearwall segmentation (abs current velocity is only one)
 * v0.2.3  20080830 use 'fire-detection window' and improved wall-collision determination to further eliminate ghost-bullets
 *                  when facing a rammer, assume it is chasing you. This way GF -1/1 will be a lot closer together (distancing)
 * v0.2.4  20090905 bugfix wall-collision, now detects bullets from accelerating bots that suddenly brake (e.g. BulletCatcher)
 * v0.2.5  20090915 segment on nearwall (3rd implementation), seen from opponent
 * v0.2.6  20090916 Tweak: only segment on nearwall when enough hits per round are taken.
 * v0.2.7  20090918 Tweak: better threshold to start using nearwall segment
 * v0.2.8  20090926 instead of multidimensional, add multiple unidimensional buffers (more info with less data)
 * v0.2.9  20091017 Increase workdistance from 480 to 540
 * v0.2.10 20110427	Improved debuggraphics, inspired by voidious.Diamond
 *					some code cleanup
 *					set a minimum angle (11.6 degrees) to move at very close ranges
 * v0.3.0  20110520	remove virtual wave after one tick, it will be recreated next tick
 *					if enemy was killed while able to fire, assume it fired
 * v0.3.1  20110610	when hit, mark every position (bin) that would have been hit
 *					evaluate risks for whole botwidth instead of just 1 bin
 *					increase bins from 37 to 73
 * v0.3.2  20110612	re-introduce binfactor, to counter possible problems at extreme MEA
 * v0.3.3  20110709 back to 0.3.0, evaluate risk for 0.5*(bin-1), 1.0*bin, 0.5*(bin+1) instead of just 1 bin
 * v0.3.4/5 20110813 use 'classic' binsmoothing instead of 0.2, 0.6, 1.0, 0.6, 0.2
 * v0.3.6  20111015 back to 0.3.3, normalise surfingdangers relative to wave-average, surfingdangers dependent on bulletpower
 * v0.3.7  20111018 go to 'safespot' created by BulletHitBullet, first step to implementing Bullet Shadows
 * v0.3.8  20111025 back to 0.3.3, evaluate risk for 0.2*(bin-2), 0.6*(bin-1), 1.0*bin, 0.6*(bin+1), 0.2*(bin+2)
 *                  including surfingdanger normalisation
 *                  including surfingdanger bulletpower dependent
 * v0.3.9  20121205 added acceleration segment, adapted weighting of segmentations
 * v0.3.10 20121223 acceleration segment tweaks
 */
public class WeefSuffurd {
	// some constants
	static final String VERSION			= "0.3.10";
	static final double WALLMARGIN		= 18;	// keep skindeep away from the wall
	static final double BOTWIDTH		= 36;
	static final double HALFBOTWIDTH	= BOTWIDTH / 2;
	static final double BLINDMANSTICK	= 130;	// wallsmoothing distance
	static final double BINFACTOR		= 1.0;	// the calculation factor used in maxEscapeAngle
	static final double DEFDISTANCE		= 540;  // preferred distance
	static final int	BINS			= 37;
	static final int	MIDBIN			= BINS / 2;
	static final int	VELSEG			= 3;	// 0..2 - 2..6 - 6..8
	static final int	WALLSEG			= 5;	// none, heading for wall close, heading for wall far, retreating from wall close, retreating from wall far
	static final int	ACCSEG			= 5;	// accell, decell, constant < 6, constant < 16, constant > 16
	public static boolean MC_flag	= false;	// MovementChallenge

	static int		wallhit;	// the total number of times I hit the wall
	static double	wallhitdam;	// the total amount of damage due to wallhits
	static int		b_hit_b;	// the total number of times my bullet hit his bullet
	static double[] unsegStat = new double[BINS+1];		// the stats
	static double[][] velStat = new double[VELSEG][BINS+1];
	static double[][] wallStat = new double[WALLSEG][BINS+1];
	static double[][] accStat  = new double[ACCSEG][BINS+1];

	static RoundRectangle2D.Double moveField;	// the moveable battlefield

	List eWaveList;					// the list with enemywaves

	Point2D.Double myPos	= new Point2D.Double();	// my current position
	int myDirection_t;
	int myDirection_t1;
	int myDirection_t2;
	double absBearing_t;
	double absBearing_t1;
	double absBearing_t2;
	double mySpeed_t;
	double mySpeed_t1;
	double mySpeed_t2;
	int myWallHit_t;
	int myWallHit_t1;
	int myWallHit_t2;
    int timeSinceVChange;

	int myVelBin;
	int myWallBin;
	int myAccBin;

	Point2D.Double enemyPos	= new Point2D.Double();		// enemy's position PREVIOUS tick (firemoment)
	Point2D.Double enemyPosNew = new Point2D.Double();	// enemy's position THIS tick
	double enemyDistance;			// the distance between me and the enemy
	double enemyVelocity_t;			// the velocity of the enemy
	double enemyVelocity_t1;		// the previous velocity
	double enemyVelocity_t2;		// for 'perfect' enemybullet detection
//	double enemyLatVelocity;		// the (absolute) lateral velocity
//	double enemyAdvVelocity;		// advancing velocity (pos = rammer, neg = flee-er)
	double enemyEnergy;				// the remaining energy of the enemy

	double enemyLastEnergy;			// the previous energy
	double energyDropCorrection;	// energydrop correction (wallhits, bullethits, collisions etc)
//	double enemyFirePower;			// the power the enemy fired
	boolean enemyHitMe;				// A collision with the enemy
	static int  enemyCollision;		// keep track of number of collisions with enemy, for anti-ram behaviour

	int dirToGoBck;
	long currTime;					// the current scantime
	long fireWindowOpen;			// openingtime of next firing window

	// for the Paint routine
	Point2D.Double posCW  = new Point2D.Double();
	Point2D.Double posCCW = new Point2D.Double();
	Point2D.Double posHLT = new Point2D.Double();

	// for (not so) precise prediction
	boolean wallhitCW, wallhitCCW, wallhitHLT, wallHitEminent;

    AdvancedRobot robot;

	/**
	 * WeefSuffurd
	 */
	public WeefSuffurd( AdvancedRobot robot) {
		this.robot = robot;

        moveField = new RoundRectangle2D.Double( WALLMARGIN, WALLMARGIN,
						robot.getBattleFieldWidth() - 2 * WALLMARGIN,
						robot.getBattleFieldHeight() - 2 * WALLMARGIN, 50, 50);

		eWaveList = new ArrayList();

		if (robot.getRoundNum() == 0) {
		    System.out.println("WeefSuffurd movement version: " + VERSION);
			recordHit( MIDBIN, MIDBIN, 0, 0, 4);	// only to avoid HOT !
		}

		myDirection_t = (Math.random() < 0.5 ? -1 : 1);
		fireWindowOpen = (long)(3.0 / robot.getGunCoolingRate());
	}

	/**
	 * onScannedRobot: Event from scanning another robot
	 */
    public void onScannedRobot( ScannedRobotEvent e) {
		Point2D.Double myOldPos = new Point2D.Double();
		currTime = robot.getTime();
		
		// information about me
		myOldPos.setLocation( myPos);
		myPos.setLocation( robot.getX(), robot.getY());
		myDirection_t2 = myDirection_t1;
		myDirection_t1 = myDirection_t;
		myDirection_t  = ( robot.getVelocity() * Math.sin( e.getBearingRadians()) >= 0 ? 1 : -1);
		if (robot.getVelocity() == 0.0) myDirection_t = myDirection_t1;		// if stopped, keep direction
		// do the acceleration BEFORE, to be more precise
        if (Math.abs( mySpeed_t1 - mySpeed_t2) > 0.5) {
           myAccBin = (Math.abs( mySpeed_t1) - Math.abs( mySpeed_t2) > 0 ? 0 : 1);
           timeSinceVChange = 0;
        } else {
           timeSinceVChange++;
           if (timeSinceVChange < 6) myAccBin = 2;
           else if (timeSinceVChange < 16) myAccBin = 3;
           else myAccBin = 4;
        }
//        double speedDif = Math.abs(mySpeed_t1) - Math.abs(mySpeed_t2);
//        myAccBin   = (speedDif < -0.5 ? 1 : (speedDif > 0.5 ? 0 : 2));
		mySpeed_t2 = mySpeed_t1;
		mySpeed_t1 = mySpeed_t;
		mySpeed_t  = Math.abs(robot.getVelocity());
//		mySpeed_t  = Math.abs(robot.getVelocity() * Math.sin(e.getBearingRadians()));
		myVelBin   = (int)((mySpeed_t2 + 1.99) / 4);

		// information about the enemy
		enemyDistance = e.getDistance();
		enemyVelocity_t2 = enemyVelocity_t1;
		enemyVelocity_t1 = enemyVelocity_t;
		enemyVelocity_t = Math.abs( e.getVelocity());
		enemyLastEnergy = enemyEnergy;
		enemyEnergy = e.getEnergy();
		absBearing_t2 = absBearing_t1;
		absBearing_t1 = absBearing_t;
		absBearing_t  = robot.getHeadingRadians() + e.getBearingRadians();
//		enemyLatVelocity = Math.abs( enemyVelocity_t * Math.sin( e.getHeadingRadians() - absBearing_t));
//		enemyAdvVelocity = e.getVelocity() * Math.cos( Math.PI + e.getHeadingRadians() - absBearing_t);
//		System.out.println(currTime+ " adv velo: "+enemyAdvVelocity + " latvelo " +enemyLatVelocity);
		enemyPosNew.setLocation( doProjectPos( myPos, absBearing_t, enemyDistance));

		if (MC_flag == true) {
			doCheckRadar();			// Handle radar, because gunnery is offline
		}
		// Check if enemy fired, and handle accordingly
		double enemyFirePower = doCheckEnemyFired();		// Check if enemy fired
		if (enemyFirePower != 0.0) {
			double defAngle = doGetAngle( enemyPos, myOldPos);
//			System.out.print(currTime+" OldWall:"+myWallHit_t);
			double MEA = Math.asin( Rules.MAX_VELOCITY / Rules.getBulletSpeed( enemyFirePower));

	        // Check when I reach the wall when circling the enemy.
			myWallHit_t1 = 0;
			for (int i = 0; i < (BINS*2); i++) {
				if (!moveField.contains( doProjectPos( enemyPos, defAngle + (myDirection_t1 * (i/(BINS*2.0)) * MEA), enemyDistance))) {
					myWallHit_t1 = (i < BINS ? 1 : 2);
					break;
				}
			}
			if (myWallHit_t1 == 0) {
				for (int i = 0; i < (BINS*2); i++) {
					if (!moveField.contains( doProjectPos( enemyPos, defAngle - (myDirection_t1 * (i/(BINS*2.0)) * MEA), enemyDistance))) {
						myWallHit_t1 = (i < BINS ? 3 : 4);
						break;
					}
				}
			}
//			System.out.println("  NewWall:"+myWallHit_t1);
		}
		myWallBin  = myWallHit_t1;

		if (enemyFirePower != 0.0) {
			enemyWave eWave = new enemyWave();
            eWave.bulletVelocity = Rules.getBulletSpeed( enemyFirePower);
			eWave.trueWave = true;
			addNewWave( eWave);
		}
		else if ((getVirtualWaves() <= 1) && (getRealWaveToSurf() == null)) {
			enemyWave eWave = new enemyWave();
            eWave.bulletVelocity = 14.0;
			eWave.trueWave = false;
			addNewWave( eWave);
		}

		enemyPos.setLocation( doProjectPos( myPos, absBearing_t, enemyDistance));
		doCheckEnemyWaves();		// Check enemywaves for actions to do (delete etc)
		doCheckMovement();			// Check if adaptions have to be made for movement
    }

	/**
	 * simulatedScannedRobot: Simulated event to keep surfing airborne enemybullets
	 *                        when enemy is already killed
	 */
    public boolean simulatedScannedRobot( ) {
		double simBearingRadians;

//		System.out.println("Simulating . . .");
		currTime = robot.getTime();
		myPos.setLocation( robot.getX(), robot.getY());
		enemyDistance = myPos.distance( enemyPos);
		simBearingRadians = doGetAngle( myPos, enemyPos) - robot.getHeadingRadians();
		myDirection_t1 = myDirection_t;
		myDirection_t  = ( robot.getVelocity() * Math.sin( simBearingRadians) >= 0 ? 1 : -1);
		if (robot.getVelocity() == 0.0) myDirection_t = myDirection_t1;		// if stopped, keep direction

//		enemyAdvVelocity = enemyLatVelocity = 0;

		doCheckEnemyWaves();		// Check enemywaves for actions to do (delete etc)
		doCheckMovement();			// Check if adaptions have to be made for movement
		return (eWaveList.isEmpty());
    }

	/**
	 * addNewWave: Enemy fired (virtual?)
	 */
	public void addNewWave( enemyWave ew) {

		ew.fireTime = currTime - 1;
		ew.maxEscAngle = Math.asin( Rules.MAX_VELOCITY / ew.bulletVelocity) * BINFACTOR;
		ew.direction = myDirection_t2;
		ew.HOTAngle = absBearing_t2;
		ew.fireLocation.setLocation( enemyPos);	// from last tick
		ew.myVelocityBin = myVelBin;
		ew.myNearWallBin = myWallBin;
		ew.myAccelerationBin = myAccBin;
		// calculate (one time) risks for each bin
		for (int i = 0; i < BINS; i++) {
			ew.avgrisk += ew.binrisks[i] = binDanger( ew, i);
		}
		ew.avgrisk /= BINS;
		eWaveList.add( ew);
//		System.out.println("Vbin: "+velStat[myVelBin][BINS]+"  Wbin: "+vwStat[myVelBin][myWallBin][BINS]);
	}

	/**
	 * onHitByBullet: Update surfing statistics
	 */
	public void onHitByBullet( HitByBulletEvent e) {
		energyDropCorrection += (-3.0 * e.getPower());
		if (eWaveList.isEmpty()) {
			System.out.println( robot.getTime()+" Hit by ghost bullet(" + e.getPower() + ") I did not detect an EnemyFire");
			return;
		}

		Point2D.Double hitBulletLocation = new Point2D.Double( e.getBullet().getX(), e.getBullet().getY());
//		System.out.println(robot.getX()+" "+robot.getY());
//		System.out.println(myPos);
//		System.out.println(hitBulletLocation);
//		System.out.println( robot.getTime()+" Hit by bullet");
		if (e.getPower() != e.getBullet().getPower()) System.out.println(" !!?? E" + e.getPower() + " B " + e.getBullet().getPower());
		logEnemyHit( true, hitBulletLocation, e.getBullet().getPower());
		recalcRisks();
	}

	/**
	 * onBulletHit: I hit the other
	 */
    public void onBulletHit( BulletHitEvent e) {
		double damage = 4 * e.getBullet().getPower() + 2 * Math.max( e.getBullet().getPower() - 1, 0);
		energyDropCorrection += damage;
		if (robot.getOthers() == 0) {
//			System.out.println( robot.getTime() + " enemy killed");
			if ((robot.getTime() >= fireWindowOpen) && (enemyLastEnergy > 0.09)) {
				damage = (enemyDistance < (BLINDMANSTICK * 1.5) ? 3.0 : 1.9);
				damage = Math.max( 0.1, Math.min( enemyLastEnergy/2, damage));
				System.out.println( robot.getTime() + " Enemy able to fire while killed, assume bulletpower = " + damage);
				enemyWave eWave = new enemyWave();
            	eWave.bulletVelocity = Rules.getBulletSpeed( damage);
				eWave.trueWave = true;
				addNewWave( eWave);
			}
		}
//		out.println(hittime+" Bullet hit with damage "+damage);
    }

	/**
	 * onHitRobot: Surfers do no hit other robots, except rammers
	 */
	public void onHitRobot( HitRobotEvent e) {
		// no energyDropCorrection here, more than one event possible
		enemyHitMe = true;
		enemyCollision++;
	}

	/**
	 * onBulletHitBullet: No rare occasion, against other good bots it happens ~3 times/round
	 */
    public void onBulletHitBullet( BulletHitBulletEvent e) {
		b_hit_b++;
		if (eWaveList.isEmpty()) {
			System.out.println( robot.getTime()+" Bullet hit by ghost bullet, I did not detect an EnemyFire");
			return;
		}

		Point2D.Double hitBulletLocation = new Point2D.Double( e.getHitBullet().getX(), e.getHitBullet().getY());

		logEnemyHit( false, hitBulletLocation, e.getHitBullet().getPower());
		recalcRisks();
    }

	/**
	 * onHitWall:  Handle collision with wall.
	 */
	public void onHitWall( HitWallEvent e)
	{
		wallhit++;
		double estSpeed = mySpeed_t * 2 - mySpeed_t1;
		if (estSpeed > 2.0)
			wallhitdam += Rules.getWallHitDamage( estSpeed); 
	}

	/**
	 * onDeath: Special case handling by David Alves
	 * obsolete since Robocode 1.5
	 */
//	public void onDeath(DeathEvent e)
//	{
//		System.out.println(robot.getTime() + " I died !! Aaaaargh");
//		Vector v = robot.getAllEvents();
//		Iterator i = v.iterator();
//		while (i.hasNext()) {
//			Object obj = i.next();
//			if (obj instanceof HitByBulletEvent) {
//				System.out.println("Extra HitByBulletEvent => !");
//				onHitByBullet( (HitByBulletEvent) obj);
//			}
//		}
//	}

	/**
	 * doCheckRadar: Let the radar produce a nice sweep
	 */
	void doCheckRadar( ) {
		// make a nice 'botwide' scanarc
		double radarMove = Utils.normalRelativeAngle( absBearing_t - robot.getRadarHeadingRadians());
		radarMove += Math.atan( (Math.signum( radarMove) * 25) / enemyDistance);
		robot.setTurnRadarRightRadians( radarMove);
	}

	/**
	 * doCheckEnemyFired: Check if the enemy has fired a bullet. If so, remember it
	 */
	double doCheckEnemyFired() {

		double enemyFirePower = 0.0;
		if ((currTime >= fireWindowOpen) && (enemyEnergy != enemyLastEnergy)) {
			if (enemyHitMe == true) {
				energyDropCorrection += 0.6;
			}
			else {
				double estVelocity = enemyVelocity_t1 * 2 - enemyVelocity_t2;
				if ((enemyVelocity_t == 0.0) && ( estVelocity > 2.0)) {
					if ((enemyPosNew.x < 19) || (enemyPosNew.x > (robot.getBattleFieldWidth() - 19)) ||
					    (enemyPosNew.y < 19) || (enemyPosNew.y > (robot.getBattleFieldHeight() - 19))) {
							energyDropCorrection += Rules.getWallHitDamage( estVelocity);
					}
				}
			}
			enemyFirePower = enemyLastEnergy - (enemyEnergy + energyDropCorrection);
//			if (Math.abs(enemyFirePower) > 0.01)
//				System.out.println(currTime+" Energydrop:"+enemyFirePower);
			if (enemyFirePower < 0.09) enemyFirePower = 0.0;
			if (enemyFirePower > 3.0) enemyFirePower = 3.0;
		}
//		if (enemyFirePower != 0.0) System.out.println(currTime+" Bullet Fired !!! Energydrop: "+enemyFirePower);
		energyDropCorrection = 0.0;
		enemyHitMe = false;
		if (enemyFirePower != 0.0) {
			fireWindowOpen = currTime + (long)( Rules.getGunHeat( enemyFirePower) / robot.getGunCoolingRate());
//			System.out.println("fireWindowOpen="+fireWindowOpen);
		}
		return enemyFirePower;
	}

	/**
	 * doCheckEnemyWaves: Check the status of the enemy waves
	 */
	void doCheckEnemyWaves() {
		int i;
		boolean scndwave = false;
		
		// for every still available enemywave
		for (i = eWaveList.size() - 1; i >= 0; i--) {
			enemyWave currWave = (enemyWave)eWaveList.get( i);

			double waveDistance = currWave.bulletVelocity * ( currTime - currWave.fireTime);
			// remove real waves when passed enemy
			if (currWave.trueWave == true) {
				if ((waveDistance - BOTWIDTH) > currWave.fireLocation.distance( myPos))
					eWaveList.remove(i);			// remove when wave passed enemy
				else
					scndwave = true;
			}
			else { // remove virtual wave if more than 2, or when real wave exists
				if ((scndwave == true) || (robot.getOthers() == 0))
					eWaveList.remove(i);			// remove second and next simulated waves
				scndwave = true;
			}
		}
	}

	/**
	 * doCheckMovement: Check if something has to be done about the movement
	 *
	 */
	void doCheckMovement() {

        int dirToGo, currDir;
		double goAngle, adjustAngle;
		double dangerLeft, dangerRight, dangerStop;
		enemyWave surfWave = getWaveToSurf();
		enemyWave secondWave;

		wallhitCW = wallhitCCW = wallhitHLT = false;
		goAngle = doGetAngle( enemyPos, myPos);
		adjustAngle = (enemyDistance / DEFDISTANCE) * Math.PI/2;
		adjustAngle = Math.max( 0.0, Math.min( adjustAngle, Math.PI));
//		adjustAngle = Math.max( 0.2, Math.min( adjustAngle, Math.PI));
//		System.out.print(currTime+" "+enemyDistance+" "+mySpeed_t);
        currDir = dirToGoBck;
        if (surfWave == null) {
			dirToGo = dirToGoBck;
//			System.out.println(" no wave !! ");
// 			dangerLeft = dangerRight = dangerStop = 1.0;
		}
		else if (surfWave.trueWave == false) {
			if ((enemyDistance < DEFDISTANCE/1.5) || (enemyDistance > DEFDISTANCE * 1.25)) {
				dangerLeft = checkDanger( surfWave, -1);
				dangerRight = checkDanger( surfWave, 1);
				if (enemyDistance > DEFDISTANCE/2.0)
					dangerStop = checkDanger( surfWave, 0);
				else {
					dangerStop = Double.POSITIVE_INFINITY;
//					System.out.println("Do not stop!");
				}
				if (dirToGoBck == 1) dangerRight *= 0.95;
				if (dirToGoBck == -1) dangerLeft *= 0.95;
				if (dirToGoBck == 0) dangerStop *= 0.95;
				if ((dangerLeft == dangerRight) && (dangerRight == 0.0) && (dirToGoBck != 0)) {
					dirToGo = dirToGoBck;
				}
				else if (dangerLeft < dangerRight) {
					if (dangerLeft <= dangerStop) dirToGo = -1;
					else dirToGo = 0;
				}
				else if (dangerStop < dangerRight) dirToGo = 0;
				else dirToGo = 1;
			}
			else {
				dirToGo = 0;
			}
//			System.out.println("V="+robot.getVelocity()+" d="+dirToGo+" dB="+dirToGoBck);
//			System.out.println("F dR: "+dangerRight+" dL: "+dangerLeft+" dS: "+dangerStop);
//			if ((robot.getVelocity() != 0.0) && (dirToGo != 0) && (dirToGoBck != 0) && (dangerLeft == dangerRight)) dirToGo = dirToGoBck;		// continue if it does not matter !
		}
		else {
			// calculate secondWave first! some globals are set that should reflect firstWave
			secondWave = getSecondWaveToSurf( surfWave);
			if (secondWave != null) {
//				System.out.print(" two ");
				dangerLeft = checkDanger( secondWave, -1);
				dangerRight = checkDanger( secondWave, 1);
				dangerStop = checkDanger( secondWave, 0);
//				System.out.println(currTime+" "+dangerLeft+" "+dangerStop+" "+dangerRight);
				wallhitCW = wallhitCCW = wallhitHLT = false;
			}
			else {
//				System.out.print(" one ");
				dangerLeft = dangerRight = dangerStop = 0.0;
			}
			dangerLeft += checkDanger( surfWave, -1);
			dangerRight += checkDanger( surfWave, 1);
			if (enemyDistance > DEFDISTANCE/2.0)
				dangerStop += checkDanger( surfWave, 0);
			else {
				dangerStop = Double.POSITIVE_INFINITY;
//				System.out.println("Do not stop!");
			}
//			System.out.println(currTime+" "+dangerLeft+" "+dangerStop+" "+dangerRight);
			if (dirToGoBck == 1) dangerRight *= 0.95;
			if (dirToGoBck == -1) dangerLeft *= 0.95;
			if (dirToGoBck == 0) dangerStop *= 0.95;

			if ((dangerLeft == dangerRight) && (dangerRight == 0.0) && (dirToGoBck != 0)) {
				dirToGo = dirToGoBck;
			}
			else if (dangerLeft < dangerRight) {
				if (dangerLeft <= dangerStop) dirToGo = -1;
				else dirToGo = 0;
			}
			else if (dangerStop < dangerRight) dirToGo = 0;
			else dirToGo = 1;
//			System.out.println("V="+robot.getVelocity()+" d="+dirToGo+" dB="+dirToGoBck);
//			System.out.println("R dR: "+dangerRight+" dL: "+dangerLeft+" dS: "+dangerStop);
//			if ((robot.getVelocity() != 0.0) && (dirToGo != 0) && (dirToGoBck != 0) && (dangerLeft == dangerRight)) dirToGo = dirToGoBck;		// continue if it does not matter !
//			System.out.println(currTime+"  "+dirToGo+" dL="+dangerLeft+" dS="+dangerStop+" dR="+dangerRight);
		}
//		System.out.println(myPos+"  "+posHLT);
        if (dirToGo == 0) {
			goAngle += adjustAngle;
			wallHitEminent = wallhitHLT;
		}
		else wallHitEminent = (dirToGo == 1 ? wallhitCW : wallhitCCW);
		goAngle = wallSmoothing( myPos, goAngle + (adjustAngle * dirToGo));
//		System.out.println(currTime+" "+dirToGo+"  "+goAngle);
        setBackAsFront( goAngle, dirToGo);
        dirToGoBck = dirToGo;
	}

	/**
	 * doProjectPos: Calculate position out of heading and distance
	 */
	Point2D.Double doProjectPos( Point2D.Double enemypos, double head, double dist){	
		return new Point2D.Double( enemypos.x + (dist * Math.sin( head)), enemypos.y + (dist * Math.cos( head)));			
	}

	/**
	 * doGetAngle: Calculate angle out of two positions
	 */
	double doGetAngle( Point2D.Double startPos, Point2D.Double endPos) {
		return ( Math.atan2( endPos.x - startPos.x, endPos.y - startPos.y));
	}

	/**
	 * getWaveToSurf: Get the first (virtual) wave I have to surf (closest, first intercepting, wettest . . .)
	 */
    enemyWave getWaveToSurf() {
        double interceptTime = Double.POSITIVE_INFINITY;
        enemyWave waveToSurf = null;

		for (int i = eWaveList.size() - 1; i >= 0; i--) {
			enemyWave currWave = (enemyWave)eWaveList.get( i);
//			if (currWave.trueWave == true) {
				double waveHitTime = currWave.fireTime + (myPos.distance( currWave.fireLocation) - HALFBOTWIDTH) / currWave.bulletVelocity;
				if (currWave.trueWave == false) waveHitTime += 1000;
				if ((waveHitTime < interceptTime) && (waveHitTime > currTime)) {
					waveToSurf = currWave;
					interceptTime = waveHitTime;
				}
//            }
        }
        return waveToSurf;
    }

	/**
	 * getRealWaveToSurf: Get the first real wave I have to surf (closest, first intercepting, wettest . . .)
	 */
    enemyWave getRealWaveToSurf() {
        double interceptTime = Double.POSITIVE_INFINITY;
        enemyWave waveToSurf = null;

		for (int i = eWaveList.size() - 1; i >= 0; i--) {
			enemyWave currWave = (enemyWave)eWaveList.get( i);
			if (currWave.trueWave == true) {
				double waveHitTime = currWave.fireTime + (myPos.distance( currWave.fireLocation) - HALFBOTWIDTH) / currWave.bulletVelocity;
				if (currWave.trueWave == false) waveHitTime += 1000;
				if ((waveHitTime < interceptTime) && (waveHitTime > currTime)) {
					waveToSurf = currWave;
					interceptTime = waveHitTime;
				}
            }
        }
        return waveToSurf;
    }

	/**
	 * checkRealWave: Check if a real wave present
	 */
    boolean checkRealWave() {

		for (int i = 0; i < eWaveList.size(); i++) {
			enemyWave currWave = (enemyWave)eWaveList.get( i);
			if (currWave.trueWave == true) {
				return (true);
            }
        }
        return (false);
    }

	/**
	 * getVirtualWaves: Get number of virtual waves
	 */
    int getVirtualWaves() {
		int nrwaves = 0;
		for (int i = 0; i < eWaveList.size(); i++) {
			enemyWave currWave = (enemyWave)eWaveList.get( i);
			if (currWave.trueWave == false) {
				nrwaves++;
            }
        }
        return (nrwaves);
    }

	/**
	 * getSecondWaveToSurf: Get the second real wave I have to surf (second intercepting)
	 */
    enemyWave getSecondWaveToSurf( enemyWave skipwave) {
        double interceptTime = Double.POSITIVE_INFINITY;
        enemyWave waveToSurf = null;

		for (int i = eWaveList.size() - 1; i >= 0; i--) {
			enemyWave currWave = (enemyWave)eWaveList.get( i);
			if ((currWave != skipwave) && (currWave.trueWave == true)) {
				double waveHitTime = currWave.fireTime + (myPos.distance( currWave.fireLocation) - HALFBOTWIDTH) / currWave.bulletVelocity;
	            if ((waveHitTime < interceptTime) && (waveHitTime > currTime)) {
    	            waveToSurf = currWave;
        	        interceptTime = waveHitTime;
            	}
			}
        }
        return waveToSurf;
    }

	/**
	 * getWaveHitTime: Get the number of ticks till enemywavewave will hit me
	 */
    double getWaveHitTime( enemyWave calcWave) {

		return (calcWave.fireTime + (myPos.distance( calcWave.fireLocation) - HALFBOTWIDTH) / calcWave.bulletVelocity) - (double)currTime;
    }

	/**
	 * logEnemyHit: find hitting wave, calculate bin and update stats
	 */
	public void logEnemyHit( boolean trueHit, Point2D.Double hitLocation, double enemyBPower) {
		int i, hitbin, minbin, maxbin;
		enemyWave hitWave = null;
//		Line2D.Double bullet = new Line2D.Double();
//		Rectangle2D.Double botspace = new Rectangle2D.Double();

		// look through the EnemyWaves, and find one that could've hit us.
		for (i = eWaveList.size() - 1; i >= 0; i--) {
			enemyWave currWave = (enemyWave)eWaveList.get( i);
			double interceptTime = currWave.fireTime + hitLocation.distance( currWave.fireLocation) / currWave.bulletVelocity;

			if ((currWave.trueWave == true) && (Math.abs( currTime - interceptTime) < 3 && Math.abs( Rules.getBulletSpeed(enemyBPower) - currWave.bulletVelocity) < 0.2)) {
				hitWave = currWave;
				break;
			}
		}

		if (hitWave != null) {
			hitbin = minbin = maxbin = getHittedBin( hitWave, hitLocation);

//			// If bullet-hit-bullet, clear the dangers of the wave, and mark bin of intercepted bullet as very attractive
//			if (trueHit == false) {
//				hitWave.attracBin = hitbin;
//				for (int j = 0; j < BINS; j++) {
//					hitWave.binrisks[j] = hitWave.avgrisk / 10;
//				}
//				hitWave.binrisks[hitbin] = -hitWave.avgrisk;
//				if (hitbin > 0) hitWave.binrisks[hitbin - 1] = -hitWave.avgrisk;
//				if (hitbin < BINS-1) hitWave.binrisks[hitbin + 1] = -hitWave.avgrisk;
//				hitWave.avgrisk /= 20;
//			}

//			// if I want to calculate every position where I would have been hit
//			if (true == false) {
//			double hitangle = doGetAngle( hitWave.fireLocation, hitLocation);
////			System.out.println( robot.getTime()+" hitloc: "+hitLocation);
//			double chkdist  = myPos.distance( hitWave.fireLocation);
//			bullet.setLine( hitWave.fireLocation, doProjectPos( hitWave.fireLocation, hitangle, chkdist+100));
////			System.out.println( robot.getTime()+" Line: "+bullet);
//			for (int tbin = hitbin; tbin >= 0; tbin--) {
//				double tguessfactor = (double)(tbin-MIDBIN) / MIDBIN;
//				double tangleOffset = hitWave.direction * tguessfactor * hitWave.maxEscAngle;
//				double thead = Utils.normalRelativeAngle( hitWave.HOTAngle + tangleOffset);
//				Point2D.Double chkpoint = doProjectPos( hitWave.fireLocation, thead, -chkdist);
////				System.out.println( "bin: "+tbin+" chkpoint: "+chkpoint);
//				botspace.setRect( chkpoint.x - HALFBOTWIDTH, chkpoint.y - HALFBOTWIDTH, BOTWIDTH, BOTWIDTH);
//				if (bullet.intersects( botspace))
//					minbin = tbin;
//				else
//					break;
//			}
//			for (int tbin = hitbin; tbin < BINS; tbin++) {
//				double tguessfactor = (double)(tbin-MIDBIN) / MIDBIN;
//				double tangleOffset = hitWave.direction * tguessfactor * hitWave.maxEscAngle;
//				double thead = Utils.normalRelativeAngle( hitWave.HOTAngle + tangleOffset);
//				Point2D.Double chkpoint = doProjectPos( hitWave.fireLocation, thead, -chkdist);
////				System.out.println( "bin: "+tbin+" chkpoint: "+chkpoint);
//				botspace.setRect( chkpoint.x - HALFBOTWIDTH, chkpoint.y - HALFBOTWIDTH, BOTWIDTH, BOTWIDTH);
//				if (bullet.intersects( botspace))
//					maxbin = tbin;
//				else
//					break;
//			}
			recordHit( minbin, maxbin, hitWave.myVelocityBin, hitWave.myNearWallBin, hitWave.myAccelerationBin);
//			if (trueHit) eWaveList.remove( i);	// We can remove this wave now, of course.
			eWaveList.remove( i);	// We can remove this wave now, of course.
		}
		else {
			System.out.println( robot.getTime()+" Hit by unexpected "+enemyBPower+" bullet, my FiredWave selection is faulty");
		}
	}

	/**
	 * getHittedBin: Return the bin that was hit
	 */
    public int getHittedBin( enemyWave ew, Point2D.Double hitLocation) {
		double hitAngle = doGetAngle( hitLocation, ew.fireLocation);
		double angleDiff = Utils.normalRelativeAngle( hitAngle - ew.HOTAngle);
		double guessFactor = Math.max(-1, Math.min(1, angleDiff / ew.maxEscAngle)) * ew.direction;
		return (int)Math.round( MIDBIN * ( guessFactor + 1.0));
    }

	/**
	 * recordHit: routine to record that I was hit
	 */
	public void recordHit( int minbin, int maxbin, int vseg, int wseg, int aseg) {
		int bin;
//		double bindanger;

		unsegStat[BINS]++;
		velStat[vseg][BINS]++;	// count number of data in segment
		wallStat[wseg][BINS]++;
		accStat[aseg][BINS]++;

//		// decay
//		for (int i = 0; i < BINS; i++) {
//			unsegStat[i] *= 0.7;
//			velStat[vseg][i] *= 0.7;
//		}
		// mark every place where I would have been hit as dangerous
//		for (bin = 0; bin < (BINS-1); bin++) {
//			bindanger = 1.0 / ((bin - minbin) * (bin - minbin) + 1);
//			unsegStat[bin] += bindanger;
//			velStat[vseg][bin] += bindanger;
//			wallStat[wseg][bin] += bindanger;
//		}

		for (bin = minbin; bin <= maxbin; bin++) {
			unsegStat[bin] += 1.0;
			velStat[vseg][bin] += 1.0;
			wallStat[wseg][bin] += 1.0;
			accStat[aseg][bin] += 1.0;
		}

		if (minbin > 0) {
			unsegStat[minbin-1] += 0.6;
			velStat[vseg][minbin-1] += 0.6;
			wallStat[wseg][minbin-1] += 0.6;
			accStat[aseg][minbin-1] += 0.6;
		}
		if (minbin > 1) {
			unsegStat[minbin-2] += 0.2;
			velStat[vseg][minbin-2] += 0.2;
			wallStat[wseg][minbin-2] += 0.2;
			accStat[aseg][minbin-2] += 0.2;
		}
		if (maxbin < (BINS-1)) {
			unsegStat[maxbin+1] += 0.6;
			velStat[vseg][maxbin+1] += 0.6;
			wallStat[wseg][maxbin+1] += 0.6;
			accStat[aseg][maxbin+1] += 0.6;
		}
		if (maxbin < (BINS-2)) {
			unsegStat[maxbin+2] += 0.2;
			velStat[vseg][maxbin+2] += 0.2;
			wallStat[wseg][maxbin+2] += 0.2;
			accStat[aseg][maxbin+2] += 0.2;
		}
//		System.out.println( robot.getTime()+" Hit on bin:"+bin+" at vseg:"+vseg);
	}

	/**
	 * recalcRisks: recalculate risks for every wave
	 */
	public void recalcRisks( ) {

		for (int i = eWaveList.size() - 1; i >= 0; i--) {
			enemyWave ew = (enemyWave)eWaveList.get( i);
//			if (ew.attracBin < 0) {
				ew.avgrisk = 0;
				for (int j = 0; j < BINS; j++) {
					ew.avgrisk += ew.binrisks[j] = binDanger( ew, j);
				}
				ew.avgrisk /= BINS;
//			}
		}
	}

																								
/*======================================*/
/* Some stuff directly from BasicSurfer */
/*======================================*/
    // CREDIT: mini sized predictor from Apollon, by rozu
    // http://robowiki.net?Apollon
	// adapted with simple distancing and evaluation of 'stop'
	// also adapted with 'constant distance' when facing a rammer
    public Point2D.Double predictPosition( enemyWave surfWave, int direction) {
    	Point2D.Double predictedPosition = (Point2D.Double)myPos.clone();
    	double predictedVelocity = robot.getVelocity();
    	double predictedHeading = robot.getHeadingRadians();
    	double maxTurning, moveAngle, moveDir;
		double angle, adjustAngle;

        int counter = 0; // number of ticks in the future
        boolean intercepted = false;

    	do {
 			angle = doGetAngle( surfWave.fireLocation, predictedPosition);
//			if (enemyAdvVelocity > 5.6) {
			if (enemyCollision > (robot.getRoundNum() + 1)) {
				adjustAngle = ( enemyDistance / DEFDISTANCE) * Math.PI/2;
			} else {
				adjustAngle = ( predictedPosition.distance( surfWave.fireLocation) / DEFDISTANCE) * Math.PI/2;
			}
    		adjustAngle = Math.max( 0.2, Math.min( adjustAngle, Math.PI));
        	if (direction == 0) angle += adjustAngle;
			moveAngle =  wallSmoothing( predictedPosition, angle + (direction * adjustAngle)) - predictedHeading;
    		moveDir = 1;

    		if(Math.cos(moveAngle) < 0) {
    			moveAngle += Math.PI;
    			moveDir = -1;
    		}

    		moveAngle = Utils.normalRelativeAngle(moveAngle);

    		// maxTurning is built in like this, you can't turn more then this in one tick
			maxTurning = Rules.getTurnRateRadians( Math.abs(predictedVelocity));
//    		maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
    		predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + Math.max(-maxTurning, Math.min(moveAngle, maxTurning)));

    		// this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to brake down
    		// otherwise you want to accelerate (look at the factor "2")
    		if (direction == 0) {
				predictedVelocity += (predictedVelocity < 0 ? 2 : -2);
				if (Math.abs(predictedVelocity) < 2.0) {
					predictedVelocity = 0.0;
					intercepted = true;
				}
			}
			else
				predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
    		predictedVelocity = Math.max(-Rules.MAX_VELOCITY, Math.min(predictedVelocity, Rules.MAX_VELOCITY));

    		// calculate the new predicted position
    		predictedPosition = doProjectPos( predictedPosition, predictedHeading, predictedVelocity);

			if (!moveField.contains( predictedPosition)) {
				if (direction == -1) wallhitCCW = true;
				else if (direction == 1) wallhitCW = true;
				else wallhitHLT = true;
			}

            counter++;

            if (predictedPosition.distance( surfWave.fireLocation) <
               (currTime + counter - surfWave.fireTime) * surfWave.bulletVelocity) {
                intercepted = true;
            }
    	} while(!intercepted && counter < 200);

		if (direction == -1) posCCW.setLocation( predictedPosition);
		else if (direction == 1) posCW.setLocation( predictedPosition);
		else posHLT.setLocation( predictedPosition);

    	return predictedPosition;
    }

/*======================================*/
/* End stuff directly from BasicSurfer */
/*======================================*/

    public double checkDanger(enemyWave surfWave, int direction) {
		double danger;
		int index = getHittedBin( surfWave,
           predictPosition( surfWave, direction));
//		System.out.println(direction+"  "+index+" "+unsegStat[index]+" "+unsegStat[BINS]);
		danger = surfWave.binrisks[index];
		if (index > 1) {
			danger += (surfWave.binrisks[index-1] * 0.6);
			danger += (surfWave.binrisks[index-2] * 0.2);
		}
		else if (index > 0)
			danger += (surfWave.binrisks[index-1] * 0.8);
		else
			danger += (surfWave.binrisks[index  ] * 0.8);
		if (index < BINS-2) {
			danger += (surfWave.binrisks[index+1] * 0.6);
			danger += (surfWave.binrisks[index+2] * 0.2);
		}
		else if (index < BINS-1)
			danger += (surfWave.binrisks[index+1] * 0.8);
		else
			danger += (surfWave.binrisks[index  ] * 0.8);
		
		danger /= surfWave.avgrisk;									// normalise against average for this wave
		danger *= (10.0 / (4.0 + getWaveHitTime( surfWave)));		// bullet flighttime dependent
		danger *= (1.0 + (20.0 - surfWave.bulletVelocity)/3.0);		// bulletpower dependent
		return danger;
    }

	public double binDanger(enemyWave ew, int index) {
		double danger;
		danger = unsegStat[index] / unsegStat[BINS];
		if (velStat[ew.myVelocityBin][BINS] > 2)
			danger = (velStat[ew.myVelocityBin][index] * 3) / velStat[ew.myVelocityBin][BINS];
		if (wallStat[ew.myNearWallBin][BINS] > 6)
			danger += wallStat[ew.myNearWallBin][index] / wallStat[ew.myNearWallBin][BINS];
		if (accStat[ew.myAccelerationBin][BINS] > 4)
			danger += (accStat[ew.myAccelerationBin][index] * 2) / accStat[ew.myAccelerationBin][BINS];
		return danger;
    }
			
	/**
	 * setBackAsFront: Take the shortest turn to the desired angle
	 */
	public void setBackAsFront( double goAngle, int direction) {
		double angle = Utils.normalRelativeAngle( goAngle - robot.getHeadingRadians());
		double myAngle = Math.atan( Math.tan( angle));
		robot.setTurnRightRadians( myAngle);
		if (direction == 0)
			robot.setAhead( 0);		// stop-position
		else
			robot.setAhead( angle == myAngle ? BLINDMANSTICK : -BLINDMANSTICK);
		robot.setMaxVelocity( wallHitEminent == true ? 0 : Rules.MAX_VELOCITY);
	}

	/**
	 * printStats: Print statistics on end of round
	 */
	public void printStats( boolean lastRound)
	{

		if (lastRound == true) {
			if (wallhitdam > 0) {
				System.out.println("Wall hits  :" + wallhit);
				System.out.println("Wallhitdam :" + wallhitdam);
			}
			System.out.println("Bullet hit bullet:"+ b_hit_b);
			System.out.println("Hitted bin statistics :");
			System.out.println("Unsegment: "+unsegStat[BINS]);
			for (int i = 0; i < VELSEG; i++) {
				System.out.println("Velseg "+i+" :   "+velStat[i][BINS]);
			}
			for (int j = 0; j < WALLSEG; j++) {
				System.out.println("  Wallseg "+j+": "+wallStat[j][BINS]);
			}
			for (int i = 0; i < ACCSEG; i++) {
				System.out.println("Accseg "+i+" :   "+accStat[i][BINS]);
			}
//			}
//			for (int i = 0; i <= BINS; i++) {
//				System.out.println( i+" "+unsegStat[i]+" "+velStat[0][i]+" "+velStat[1][i]+" "+velStat[2][i]);
//			}
		}
	}

	/**
	 * onPaint: Show some stuff, maybe I am calculating something wrong
	 */
	public void onPaint(java.awt.Graphics2D g) {
		int i, j;
		double dist, tothits, binangle, bindanger;
		Point2D.Double botpos = new Point2D.Double();
		Point2D.Double HOTpos = new Point2D.Double();
		Point2D.Double binpos = new Point2D.Double();

//		g.setColor( Color.yellow);
//		g.drawRect( (int)(myPos.getX() - HALFBOTWIDTH), (int)(myPos.getY() - HALFBOTWIDTH), (int)BOTWIDTH, (int)BOTWIDTH);
		// print enemy waves to check if I am aligned
		for (i = eWaveList.size() - 1; i >= 0; i--) {
			enemyWave currWave = (enemyWave)eWaveList.get( i);
			dist = (robot.getTime() - currWave.fireTime) * currWave.bulletVelocity;
			botpos.setLocation( currWave.fireLocation);
			HOTpos.setLocation( doProjectPos( botpos, currWave.HOTAngle, -dist));
			g.setColor( Color.gray);
			if (currWave.trueWave == true) {
//				g.drawOval( (int)(botpos.getX() - dist), (int)(botpos.getY() - dist), (int)(dist*2),(int)(dist*2));
				g.drawLine( (int)botpos.getX(), (int)(botpos.getY()), (int)HOTpos.getX(), (int)(HOTpos.getY()));
//				tothits = velStat[currWave.myVelocityBin][BINS] + wallStat[currWave.myNearWallBin][BINS];
				for (j = 0; j < BINS; j++) {
					binangle = (((double)j - MIDBIN) / MIDBIN) * currWave.maxEscAngle * currWave.direction;
					binpos.setLocation( doProjectPos( botpos, currWave.HOTAngle + Math.PI + binangle, dist - 4));
//					bindanger = (int)(((velStat[currWave.myVelocityBin][j] * 2 + wallStat[currWave.myNearWallBin][j]) * 255) / tothits);
					bindanger = currWave.binrisks[j] * 127 / currWave.avgrisk;
//					System.out.println("bin " + j + " dang " + (int)bindanger);
					g.setColor( new Color((int)Math.max( 0, Math.min( bindanger, 255)), 0, (int)Math.max(0, Math.min(255 - bindanger, 255))));
					g.drawOval( (int)(binpos.x - 1), (int)(binpos.y - 1), 2, 2);
				}
			} else {	// virtual wave(s)
				g.drawOval( (int)(botpos.getX() - dist), (int)(botpos.getY() - dist), (int)(dist*2),(int)(dist*2));
			}
        }
		if (checkRealWave() == true) {
			g.setColor( Color.white);
			g.drawRect( (int)(posHLT.getX() - 4), (int)(posHLT.getY() - 4), 8, 8);
			g.setColor( Color.red.brighter());
			g.drawRect( (int)(posCCW.getX() - 4), (int)(posCCW.getY() - 4), 8, 8);
			g.setColor( Color.red.darker());
			g.drawRect( (int)(posCW.getX() - 4), (int)(posCW.getY() - 4), 8, 8);
		}
    }

	/**
	 * wallSmoothing: A WaveSurfer does not avoid the walls, but moves smoothly along them
	 */
	public double wallSmoothing( Point2D.Double startPoint, double angle) {
		double di;

		for (di = 0; di < Math.PI; di += Math.PI/72) {
			if (moveField.contains( doProjectPos( startPoint, angle+di, BLINDMANSTICK))) {
				break;
			}
			if (moveField.contains( doProjectPos( startPoint, angle-di, BLINDMANSTICK))) {
				di = -di;
				break;
			}
		}
		return ( angle + di);
	}

	/**
	 * enemyWave: Info about the waves generated by the enemy.
	 *
	 */
	class enemyWave {
		long fireTime;
		double bulletVelocity;
		double HOTAngle;
		double maxEscAngle;
		double direction;
		int myVelocityBin;
//		int myDistanceBin;
		int myNearWallBin;
		int myAccelerationBin;
		int attracBin = -1;		// very attractive bin, an intercepted bullet would end up here.
		boolean trueWave;

		Point2D.Double fireLocation = new Point2D.Double();
		double[] binrisks = new double[BINS];
		double avgrisk;

		public enemyWave() { }

	}
}
