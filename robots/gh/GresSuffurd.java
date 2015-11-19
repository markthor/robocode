package gh;
import gh.ghgun.*;
import gh.ghmove.*;
import robocode.*;
import java.awt.Color;
//import java.util.Vector;
import java.io.*;

/**
 * GresSuffurd - a robot by Gert Heijenk
 *
 * GuessFactorTargeting and WaveSurfer movement
 *     GresVretter              WeefSuffurd
 *
 * Revision information:
 * v0.0.1  20060514 GV_0.0.1 + WS_0.0.1 and make things to compile
 * v0.0.2  20060531 GV_0.0.2 + WS_0.0.1 it does fire!!
 * v0.0.3  20060918 GV_0.1.1 + WS_0.0.3 and cleanup
 * v0.1.0  20060926 GV_0.1.1 + WS_0.0.4 Basic GuessFactorTargeting and Basic WaveSurfing
 * v0.1.1  20061001 GV_0.1.4 + WS_0.0.4 Record all hitted bins and fire on highest concentration
 * v0.1.2  20061014 GV_0.1.4 + WS_0.0.5 Keep preferred distance, also move without a wave
 * v0.1.3  20061023 GV_0.1.4 + WS_0.0.7 continue current direction if direction does not matter
 * v0.1.4  20061024 GV_0.1.4 + WS_0.0.8 add stopposition evaluation (bugfixed)
 * v0.1.5  20061028 GV_0.1.5 + WS_0.1.0 DavidAlves' onDeath and similar onWin behaviour, slowdown when wallhit is eminent
 * v0.1.6  20061117 GV_0.2.0 + WS_0.1.0 also fire non-bullet waves with 0.1 weight
 * v0.1.7  20061127 GV_0.2.1 + WS_0.1.1 energymanagement and anti-ram behaviour
 * v0.1.8  20061212 GV_0.2.2 + WS_0.1.2 revert energymanagement, introduce first segmenting in wavesurfing
 * v0.1.9  20070102 GV_0.2.2 + WS_0.1.3 segment surfing also on nearwall
 * v0.2.0  20070114 GV_0.2.2 + WS_0.1.4 remove nearwall segment, take into account second enemywave
 * v0.2.1  20070116 GV_0.2.2 + WS_0.1.5 bugfix weighting of waves
 * v0.2.2  20070125 GV_0.2.3 + WS_0.1.6 decay in gunstats, bugfix in close range surfing
 * v0.2.3  20070207 GV_0.2.4 + WS_0.1.7 also use onBulletHitBullet
 * v0.2.4  20070222 GV_0.2.5 + WS_0.1.7 increase gun-wallsegment from 3 to 5 segments
 * v0.2.5  20070311 GV_0.2.5 + WS_0.1.8 rolling surfingstats (0.7*old + 0.3*new)
 * v0.2.6  20070313 GV_0.2.5 + WS_0.1.9 no rolling surfingstats, no GF1 protection multiply factor (move)
 * v0.2.7  20070516 GV_0.2.6 + WS_0.2.0 no decay in gunstats, no GF1 protection (gun), segment also on distance (move)
 * v0.2.12 20090905 GV_      + WS_0.2.4 no idea, check the Gun and Surfing yourself
 * v0.2.22 20100413 GV_0.2.13 + WS_0.2.9 top-20 bot, APS 82.25
 * v0.2.23 20110114 GV_0.2.14 + WS_0.2.9  
 * v0.2.24 20110123 GV_0.2.15 + WS_0.2.9 added second (decayed) gun
 * v0.2.25 20110126 GV_0.2.16 + WS_0.2.9 bugfixing gun (process killing bullet), updated gunpainting
 * v0.2.28 20110315 GV_0.2.19 + WS_0.2.9
 * v0.2.29 20110411 GV_0.2.20 + WS_0.2.9 precise intersection, precise MEA: top-10 !!
 * v0.3.0  20110427 GV_0.3.0  + WS_0.2.9  guncode cleanup
 * v0.3.1  20110503 GV_0.3.1  + WS_0.2.10 update movement graphics
 * v0.3.12 20120930 GV_0.3.3  + WS_0.3.8  a lot
  
 */
public class GresSuffurd extends AdvancedRobot
{
	WeefSuffurd movement;
	GresVretter gunnery;

	// statistics counters
	static int	skipturn;	// the total number of skipped turns

	// some flags for the several challanges, intially set in readIniFile() !!
	static boolean TC_flag;	// Targeting/PM challenge (no movement, always fire 3.0, no energy-management)
	static boolean MC_flag;	// Movement challenge	(do not fire or ram)
	static boolean TG_flag;	// Targeting Graphics
	static boolean MG_flag;	// Movement Graphics
	static boolean RR_flag;	// RR Release if set, development otherwise

	boolean WIN_flag;		// continue flow until safe

	/**
	 * run: GresSuffurd's default behavior
	 */
	public void run() {

		// first of all, read the initialisation file
		readIniFile();
//		MC_flag = true;
		GresVretter.TC_flag = TC_flag;
		WeefSuffurd.MC_flag = MC_flag;
		movement = new WeefSuffurd( this);
		gunnery = new GresVretter( this);
		// Give the robot an appealing look
		setColors( Color.red, Color.blue.brighter().brighter().brighter().brighter(), Color.red.brighter());

		// Let gun and radar move independently
		setAdjustGunForRobotTurn( true);
		setAdjustRadarForGunTurn( true);

		// Start the mainloop
		while(true) {
			if (getRadarTurnRemaining() == 0.0)
				gunnery.doKickRadar();
			if (WIN_flag == true) {
				boolean noWaves = movement.simulatedScannedRobot();
				if ((gunnery.simulatedScannedRobot() == true) && (noWaves == true))
					postponedWinningParty();
			}
			execute();				// execute all commands
		}
	}


	/**
	 * onScannedRobot: What to do when you see another robot (just save everything and do nothing here)
	 *
	 */
	public void onScannedRobot( ScannedRobotEvent e) {

		if (TC_flag == false) {
			movement.onScannedRobot( e);
		}
		if (MC_flag == false) {
			gunnery.onScannedRobot( e);
		}
	}

	/**
	 * onRobotDeath: What to do when someone else dies
	 */
//	do nothing if someone else dies, you have won!
//	public void onRobotDeath( RobotDeathEvent e)
//	{
//		out.println("RobotDeath");
//	}

	/**
	 * onHitRobot: Bounce off !
	 */	
	public void onHitRobot( HitRobotEvent e)
	{
		movement.onHitRobot( e);
		gunnery.onHitRobot( e);
	}

	/**
	 * onHitWall:  Handle collision with wall.
	 */
	public void onHitWall( HitWallEvent e)
	{
		movement.onHitWall( e);
	}

	/**
	 * onBulletHit: Yes, I hit somebody
	 */
	public void onBulletHit(BulletHitEvent e)
	{
		movement.onBulletHit( e);
		gunnery.onBulletHit( e);
	}

	/**
	 * onBulletMissed: Damn, I missed (again)
	 */
	public void onBulletMissed(BulletMissedEvent e)
	{
		if (WIN_flag == false) {
			gunnery.onBulletMissed( e);
		}
	}

	/**
	 * onHitByBullet:  I am hit, handle some (movement) statistics.
	 */
	public void onHitByBullet( HitByBulletEvent e)
	{
		movement.onHitByBullet( e);
	}

	/**
	 * onBulletHitBullet:  The bullets hit eachother, see if I can do something with it
	 */
	public void onBulletHitBullet( BulletHitBulletEvent e)
	{
		movement.onBulletHitBullet( e);
		gunnery.onBulletHitBullet( e);
	}

	/**
	 * onSkippedTurn:  Handle a skipped turn.
	 */
	public void onSkippedTurn(SkippedTurnEvent e)
	{
		skipturn++;
//		out.println(getTime()+" AAaaaarghhhhh");
	}

	/**
	 * onWin: Show my private victory dance
	 */
	public void onWin(WinEvent e)
	{
		// remember some variables
		WIN_flag = true;
		gunnery.onWin( e);
//		out.println(getTime() + " I've won !");
	}

	/**
	 * postponedWinningParty: Show my private victory dance
	 */
	public void postponedWinningParty( )
	{
		printStats();
		//Victory dance, well at least the movement	
		setTurnRight(1);
		ahead(0);
		waitFor(new RadarTurnCompleteCondition(this));
	}

	/**
	 * onDeath: Show some statistics
	 */
	public void onDeath(DeathEvent e)
	{
//		movement.onDeath( e);
		printStats();
	}

	/**
	 * printStats: Print statistics on end of round
	 */
	public void printStats( )
	{
		boolean lastRound = ((getRoundNum() + 1) == getNumRounds());
		if (lastRound == true) {
			if (skipturn > 0) 
				out.println("SkipTurns  :" + skipturn);
		}
		movement.printStats( lastRound);
		gunnery.printStats( getRoundNum() + 1, lastRound);
	}

	/**
	 * onPaint: Show some stuff, maybe I am calculating something wrong
	 */
	public void onPaint(java.awt.Graphics2D g) {
		if (getTime() < 5) return;	// nothing to draw yet
		if (TG_flag == true) {
			gunnery.onPaint( g);
		}
		if (MG_flag == true) {
			movement.onPaint( g);
		}
    }

	/**
	 * This routine is copied from Loki's bot Freya !!
	 *
	 * <p>This function reads the GresSuffurd.ini file for behaviour and graphics.</p>
	 * <p>The parameter "behaviour" may have following values:
	 * <ul>
	 * <li>normal   : moves and schoots (default-value).</li>
	 * <li>reference: sits still, shoots with power 3.0.</li>
	 * <li>challenge: moves normally, does not shoot.</li>
	 * </ul></p>
	 * <p>The parameters "gungraph" and "surfgraph" may have following values:
	 * <ul>
	 * <li>true : enable Robocode 1.1.1 graphics.</li>
	 * <li>false: disable Robocode 1.1.1 graphics (default value).</li>
	 * </ul></p>
	 * <p>The parameter "logging" may have following values:
	 * <ul>
	 * <li>true : extended type of debug output.</li>
	 * <li>false: release type of debug output (default value).</li>
	 * </ul></p>
	 */
	private void readIniFile() {
		if (getRoundNum() == 0) {

			// set the flags initially
			TC_flag = MC_flag = false;		// No TargetingChallenge or MovementChallenge
			TG_flag = MG_flag = false;		// No graphics
			RR_flag = true;					// Release output to debug window

			try {
				BufferedReader	bufferedreader = new BufferedReader(new FileReader(getDataFile("GresSuffurd.ini")));
				String			s;
				String			gedrag = null;

				while ((s = bufferedreader.readLine()) != null) {
					s = s.trim();

					if (!s.substring(0, 1).equals("#")) {
						if (s.length() > 11 && s.substring(0, 10).equalsIgnoreCase("behaviour=")) {
							gedrag = s.substring(10).trim();
						}

						if (s.length() > 10 && s.substring(0, 9).equalsIgnoreCase("gungraph=")) {
							TG_flag = s.substring(9).trim().equalsIgnoreCase("false") ^ true;
						}
						if (s.length() > 10 && s.substring(0, 10).equalsIgnoreCase("surfgraph=")) {
							MG_flag = s.substring(10).trim().equalsIgnoreCase("false") ^ true;
						}
						
						if (s.length() > 9 && s.substring(0, 8).equalsIgnoreCase("logging=")) {
							RR_flag = s.substring(8).trim().equalsIgnoreCase("true") ^ true;
						}
					}
				}

				if (gedrag != null) {
					TC_flag = gedrag.equalsIgnoreCase("reference") ? true : false;
					MC_flag = gedrag.equalsIgnoreCase("challenge") ? true : false;
				}
			} catch (IOException ioexception) {
				out.println("readIniFile(): IOException reading data GresSuffurd.ini: " + ioexception);
			}

			if (TC_flag == true) out.println("Targeting Challenge");
			if (MC_flag == true) out.println("Movement Challenge");
			if (TG_flag == true) out.println("Targeting Graphics ON");
			if (MG_flag == true) out.println("Movement Graphics ON");
		}
	}
}
