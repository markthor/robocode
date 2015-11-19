package voidious.dmove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.lang.Comparable;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;
import java.awt.Color;


import robocode.*;
import robocode.util.Utils;
import voidious.utils.*;

/**
 * DookiCape - movement system for Dookious
 *
 * CREDITS:
 *   ABC - inventor of WaveSurfing.
 *   PEZ - inspiration for my WaveSurfing algorithm; general source of
 *         guidance in OpenSource bot, CassiusClay.
 *   Albert - FuturePosition class used in PrecisePrediction.
 *   Paul Evans - brought RollingAverages to Robocode.
 *   David Alves - DrawingBot code used for graphical debugging; proper 
 *          handling of onDeath.
 *
 * Code is open source, released under the RoboWiki Public Code License:
 * http://robowiki.net/cgi-bin/robowiki?RWPCL
 */

public class DookiCape {
    public static final int BINS = 59;
    public static final int WAVES_TO_SURF = 2;
    public static final double DEFAULT_WALL_STICK = 160;
    public static final double RAMMER_WALL_STICK = 130;
    public static final double FEARFUL_DISTANCING_EXPONENT = 4;
    public static final double NORMAL_DISTANCING_EXPONENT = 1.5;
    public static final double BOT_HALF_WIDTH = 18;
    public static final double SURF_ORIENTATION_VELOCITY_THRESHOLD = 0.1;
    public static final double TYPICAL_DISTANCE = 465;
    public static final double TYPICAL_ESCAPE_RANGE = 0.98;
    
    private AdvancedRobot _robot;
	private boolean _mcMode;
 
	private ArrayList _waves;
	private ArrayList _waveRegisters;
	private ArrayList _bulletHitRegisters;
	private ArrayList<MovementChoice> _movementOptions;

	private MovementChoice _optionCounterClockwise;
	private MovementChoice _optionStop;
	private MovementChoice _optionClockwise;
	
    private double _wallStick = DEFAULT_WALL_STICK;
    private double _desiredDistance;
    private double _fearDistance;
    private double _smoothAwayDistance;
    private boolean _flattenerEnabled;
    private double _weightedEnemyShotsFired = 0;
    private double _weightedEnemyShotsHit = 0;
    private double _weightedEnemyShotsFiredThisRound = 0;
    private double _weightedEnemyShotsHitThisRound = 0;
    
    private int _lastEnemyWaveOrientation = 1;
    private int _lastMovementChoice = 1;
    private double _lastRoundNormalizedHitPercentage;
    private int _flattenerToggleTimer = 0;
    private DistanceController _subtleDistancer;
    private DistanceController _aggressiveDistancer;
    private DistanceController _currentDistancer;
    
    private StatBufferSet _lowStatBuffers;
    private StatBufferSet _highStatBuffers;
    private StatBufferSet _extraFlatStatBuffers;

    public static final int CLOCKWISE_OPTION = 1;
    public static final int STOP_OPTION = 0;
    public static final int COUNTERCLOCKWISE_OPTION = -1;
    public static final int NO_SURFABLE_WAVES = 0;
    public static final int FIRST_WAVE = 0;
    public static final Wave NO_WAVE_FOUND = null;
    
    private static Vector _renderables;
    
	public DookiCape(AdvancedRobot robot, boolean mc) {
		DUtils.setBattleFieldWidth(robot.getBattleFieldWidth());
		DUtils.setBattleFieldHeight(robot.getBattleFieldHeight());
		_mcMode = mc;
		
		_waves = new ArrayList();
		_waveRegisters = new ArrayList();
		_bulletHitRegisters = new ArrayList();

		_flattenerEnabled = false;
		_currentDistancer = _subtleDistancer = new SubtleDistancing();
		_aggressiveDistancer = new AggressiveDistancing();
		_renderables = new Vector();
		
		initializeSurfBuffers();
		initializeMovementOptions();
		reset(robot);
	}
	
	public void initializeSurfBuffers() {
		_lowStatBuffers = new SurfLowBufferSet(BINS);
		_highStatBuffers = new SurfHighBufferSet(BINS);
		_extraFlatStatBuffers = new SurfNonFiringBufferSet(BINS);

		_bulletHitRegisters.add(_lowStatBuffers);
		_bulletHitRegisters.add(_highStatBuffers);
		_waveRegisters.add(_extraFlatStatBuffers);
	}
	
	public void initializeMovementOptions() {
		_movementOptions = new ArrayList<MovementChoice>();
		
		_movementOptions.add(_optionCounterClockwise = 
			new MovementCounterClockwise());
		_movementOptions.add(_optionStop = new MovementStop());
		_movementOptions.add(_optionClockwise = new MovementClockwise());
		
	}

	public void reset(AdvancedRobot robot) {		
		_robot = robot;
		_waves.clear();
		_renderables.clear();
		_weightedEnemyShotsFiredThisRound = 0;
		_weightedEnemyShotsHitThisRound = 0;

		ScanLog.reset();
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		ScanLog.onScannedRobot(_robot, e);
		
		evaluateDistancingControl();
		evaluateWallStick();
		evaluateFlattener();
		
		boolean realShotFired;
        if (ScanLog.enemyFired(e.getTime())) {
            realShotFired = true;
            _weightedEnemyShotsFired++;
            _weightedEnemyShotsFiredThisRound++;
//            	(e.getDistance()
//            		/ DUtils.bulletVelocity(ScanLog.getLastEnemyBulletPower()))
//            	/ (TYPICAL_DISTANCE / POWER_TWO_BULLET_VELOCITY);
        } else {
        	realShotFired = false;
        }
        
        fireWave(realShotFired ? Wave.FIRING_WAVE : Wave.NON_FIRING_WAVE);
		checkActiveWaves();
		
		surf();
	}
	
	public void onWin(WinEvent e) {
		ScanLog.onWin(e);
		
		roundOver();
	}

	public void onDeath(DeathEvent e) {
		ScanLog.onDeath(e);

		roundOver();
	}

	public void roundOver() {
        _lastRoundNormalizedHitPercentage = 
        	normalizedEnemyHitPercentageThisRound();

        System.out.println();
        System.out.println("Enemy's hit % this round: "
            + ((int)ScanLog.getEnemyBulletHitsThisRound()) + " / "
            + ((int)ScanLog.getEnemyBulletsFiredThisRound())
            + ", " + DUtils.round(ScanLog.getEnemyHitPercentageThisRound(), 2));
        System.out.println("Enemy's normalized hit % this round: "
            + DUtils.round(_weightedEnemyShotsHitThisRound, 2) + " / "
            + DUtils.round(_weightedEnemyShotsFiredThisRound, 0)
            + ", " + DUtils.round(_lastRoundNormalizedHitPercentage, 2));
        
        System.out.println("Enemy's cumulative hit %: "
            + ((int)ScanLog.getEnemyBulletHits()) + " / "
            + ((int)ScanLog.getEnemyBulletsFired())
            + ", " + DUtils.round(ScanLog.getEnemyHitPercentage(), 2));
        System.out.println("Enemy's normalized cum hit %: " + 
        	DUtils.round(_weightedEnemyShotsHit, 2)
            + " / " + DUtils.round(_weightedEnemyShotsFired, 0) + ", " +
            DUtils.round(normalizedEnemyHitPercentage(), 2));
        System.out.println("Curve Flattener: " + 
        	((_flattenerEnabled)?"Enabled":"Disabled"));
        System.out.println("Total damage taken: " + 
        	DUtils.round(ScanLog.getBulletDamageTaken(), 2));

        if (_mcMode) {
            System.out.println();
        	System.out.println("MC score: " +
                DUtils.round((100.0 - (ScanLog.getBulletDamageTaken() 
                	/ (_robot.getRoundNum() + 1.0))), 3));
        }
	}
	
	public void onHitByBullet(HitByBulletEvent e) {
		ScanLog.onHitByBullet(e);

		Wave hitWave =  
			processBulletReturnFiringWave(e.getBullet(), e.getTime());

		if (hitWave != NO_WAVE_FOUND) {
			double thisHit = (hitWave.targetScan.getDistance()
					/ TYPICAL_DISTANCE)
				* (hitWave.escapeAngleRange() / TYPICAL_ESCAPE_RANGE);
			_weightedEnemyShotsHit += thisHit;
			_weightedEnemyShotsHitThisRound += thisHit;
		}
		
	}

	public void onBulletHit(BulletHitEvent e) {
		ScanLog.onBulletHit(e);

	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		ScanLog.onBulletHitBullet(e);

		processBulletReturnFiringWave(e.getHitBullet(), e.getTime());
		
        _weightedEnemyShotsFired--; 
        _weightedEnemyShotsFiredThisRound--; 
//        	(firingDistance 
//        		/ DUtils.bulletVelocity(e.getBullet().getPower())) 
//        	/ (TYPICAL_DISTANCE / POWER_TWO_BULLET_VELOCITY);
	}

	public void onBulletMissed(BulletMissedEvent e) {
		ScanLog.onBulletMissed(e);
		
	}
/*	
	public void onPaint(Graphics2D g) {
		Iterator i = _renderables.iterator();
		while(i.hasNext()){
			Renderable r = (Renderable) i.next();
			r.render(g);
		}
		_renderables.clear();
	}
*/
	public Wave processBulletReturnFiringWave(Bullet bullet, 
		long currentTime) {
		
		int tightMatchDistanceThreshold = 50;
		Wave hitWave = DUtils.findClosestWave(_waves,
			new Point2D.Double(bullet.getX(), bullet.getY()),
			_robot.getTime(), DUtils.ANY_WAVE, DUtils.FIRING_WAVE,
			tightMatchDistanceThreshold);
		
		if (hitWave == null) { return NO_WAVE_FOUND; }
		
		for (int x = 0; x < _bulletHitRegisters.size(); x++) {
			BulletHitRegister bhr = 
				(BulletHitRegister)_bulletHitRegisters.get(x);
			
			bhr.registerBulletHit(bullet, hitWave, currentTime);
		}
		
		hitWave.processedBulletHit = true;
		
		return hitWave;		
	}
	
	public void fireWave(boolean isFiringWave) {
		int timeSinceEnemyFired = (int)ScanLog.getLastFireOffset();
		double bulletPower = ScanLog.getLastEnemyBulletPower();

		try {
			Point2D.Double myTargetLocation = 
				ScanLog.getLocation(timeSinceEnemyFired);
			Point2D.Double enemyFireLocation = 
				ScanLog.getEnemyLocation(timeSinceEnemyFired);
			BotScan enemySegmentationScan =
				ScanLog.getEnemyScan(timeSinceEnemyFired + 1);
			
			Wave enemyWave = new Wave(enemySegmentationScan,
				enemyFireLocation, myTargetLocation, 
				_robot.getTime() - timeSinceEnemyFired,
				isFiringWave ? Wave.FIRING_WAVE : Wave.NON_FIRING_WAVE,
				enemySegmentationScan.getAbsBearingRadians(),
				bulletPower, "Surf Wave", Wave.TRADITIONAL_MAX_ESCAPE_ANGLE,
				enemySegmentationScan.getHeadingRadians(),
				enemySegmentationScan.getVelocity(),
				_lastEnemyWaveOrientation, SURF_ORIENTATION_VELOCITY_THRESHOLD);
			
			_waves.add(enemyWave);

			_lastEnemyWaveOrientation = enemyWave.orientation;
		} catch (IndexOutOfBoundsException noScanDataYet) { }
	}

	public void checkActiveWaves() {
		long currentTime = _robot.getTime();
		Point2D.Double myLocation = ScanLog.myLocation();
		
		for (int x = 0; x < _waves.size(); x++) {
			Wave w = (Wave)_waves.get(x);
			if (w.processedWaveBreak) {
				if (w.wavePassed(myLocation, currentTime,
					Wave.INACTIVE_WAVE_OFFSET)) {
					_waves.remove(x--);
				}
			} else {
				if (w.wavePassed(myLocation, currentTime)) {
					processWave(w);
				}
			}
		}		
	}
	
	public void processWave(Wave w) {
		GuessFactorWindowSet gfWindow = 
			w.guessFactorWindow(ScanLog.myLocation());
		int gfBin = w.guessFactorIndex(gfWindow.guessFactor);
		int gfBinLow = w.guessFactorIndex(gfWindow.guessFactorLow);
		int gfBinHigh = w.guessFactorIndex(gfWindow.guessFactorHigh);
		BotScan enemyScan = w.targetScan.getInverseScan();
		
		for (int x = 0; x < _waveRegisters.size(); x++) {
			WaveRegister wr = (WaveRegister)_waveRegisters.get(x);
			
			wr.registerWaveHit(w.targetScan, enemyScan,	w.bulletPower, 
				gfWindow.guessFactor, gfBin, gfBinLow, gfBinHigh, w.firingWave, 
				w.fireTime, w.orientation, w.escapeAngleRange());
		}
		
		w.processedWaveBreak = true;
	}
	
	public Wave findSurfableWave(int waveIndex) {
		int searchWaveIndex = 0;
		long currentTime = _robot.getTime();
		
		for (int x = 0; x < _waves.size(); x++) {
			Wave w = (Wave)_waves.get(x);
			double distanceToWaveSource = 
				ScanLog.myLocation().distance(w.sourceLocation);
			double distanceToWave = distanceToWaveSource -
				w.distanceTraveled(currentTime);
			
			if (w.firingWave && !w.processedBulletHit &&
				distanceToWave > w.bulletVelocity()) {

				if (searchWaveIndex == waveIndex) {
//					drawCircle(w.sourceLocation, 
//						w.distanceTraveled(currentTime + 1), Color.green);
//					drawLine(w.sourceLocation,
//						DUtils.project(w.sourceLocation, w.absBearingRadians, 
//								w.distanceTraveled(currentTime + 1)),
//							Color.blue);
					return w;
				} else {
					searchWaveIndex++;
				}
			}
		}
		
		return null;
	}
	
	public Wave findNonSurfableWave(double minDistanceToWave) {
		long currentTime = _robot.getTime();
		
		for (int x = 0; x < _waves.size(); x++) {
			Wave w = (Wave)_waves.get(x);
			double distanceToWaveSource = 
				ScanLog.myLocation().distance(w.sourceLocation);
			double distanceToWave = distanceToWaveSource -
				w.distanceTraveled(currentTime);
			
			if (!w.firingWave && distanceToWave > minDistanceToWave) {
				return w;
			}
		}
		
		return null;		
	}
	
	public double checkDanger(RobotState startState, int movementOption, 
		boolean previouslyMovingClockwise, int surfableWaveIndex, 
		int recursionLevels, double cutoffDanger) {
		
		if (surfableWaveIndex >= recursionLevels) {
			return 0;
		}

		boolean predictClockwiseOrNot;
		if (movementOption == CLOCKWISE_OPTION) {
			predictClockwiseOrNot = true;
		} else if (movementOption == COUNTERCLOCKWISE_OPTION) {
			predictClockwiseOrNot = false;
		} else {
			predictClockwiseOrNot = previouslyMovingClockwise;
		}

		Wave surfWave = findSurfableWave(surfableWaveIndex);
		
		if (surfWave == null) {
			if (surfableWaveIndex == FIRST_WAVE) {
				double nonSurfableWaveDistance = 150;
				surfWave = findNonSurfableWave(nonSurfableWaveDistance);
			}
			
			if (surfWave == null) { return NO_SURFABLE_WAVES; }
		}
/*		
		Color drawColor = Color.white;
		if (surfableWaveIndex != 0 || movementOption == STOP_OPTION) {
			drawColor = Color.blue;			
		}
*/		
		double waveHitInterceptOffset = 
			surfWave.bulletVelocity() + BOT_HALF_WIDTH;
		double wavePassedInterceptOffset = surfWave.bulletVelocity();
		RobotState predictedState = startState;
		RobotState dangerState = startState;
		
		boolean wavePassed = false;
		boolean waveHit = false;
		
		double maxVelocity = (movementOption == STOP_OPTION) ? 0 : 8;
		
		do {	
			double orbitAbsBearing = DUtils.absoluteBearing(
				surfWave.sourceLocation, predictedState.location);
			double orbitDistance = 
				surfWave.sourceLocation.distance(predictedState.location);
			double attackAngle = _currentDistancer.attackAngle(orbitDistance, 
				_desiredDistance);
			boolean clockwiseSmoothing = predictClockwiseOrNot;

			if (orbitDistance < _smoothAwayDistance) {
				clockwiseSmoothing = !clockwiseSmoothing;
			}
			
			predictedState = 
				DUtils.nextPerpendicularWallSmoothedLocation(
					predictedState.location, orbitAbsBearing, 
					predictedState.velocity, maxVelocity, 
					predictedState.heading,	attackAngle, clockwiseSmoothing, 
					predictedState.time, DUtils.battleField, DUtils.battleFieldWidth, 
					DUtils.battleFieldHeight, _wallStick, 
					DUtils.OBSERVE_WALL_HITS);
			
			if (!waveHit &&
				surfWave.wavePassed(predictedState.location, 
					predictedState.time, waveHitInterceptOffset)) {
				
				dangerState = predictedState;
				waveHit = true;
			}
			
			if (!wavePassed &&
				surfWave.wavePassed(predictedState.location, 
					predictedState.time, wavePassedInterceptOffset)) {

				wavePassed = true;				
			}
		} while (!wavePassed);
		
//		drawPoint(predictedState.location, drawColor);

		double danger = getBinScore(surfWave, dangerState.location);
		danger *= DUtils.bulletDamage(surfWave.bulletPower); 

		double currentDistanceToWaveSource = 
			ScanLog.myLocation().distance(surfWave.sourceLocation);
		double currentDistanceToWave = 
			currentDistanceToWaveSource - 
				surfWave.distanceTraveled(_robot.getTime());
		double timeToImpact = currentDistanceToWave 
			/ DUtils.bulletVelocity(surfWave.bulletPower);
		
		if (_flattenerEnabled) {
			danger /= DUtils.square(timeToImpact);			
		} else {
			danger /= timeToImpact;
		}

		double firstWaveMultiplier = 1;
		if (surfableWaveIndex == FIRST_WAVE) {
			double predictedDistanceToWaveSource = 
				surfWave.sourceLocation.distance(predictedState.location);
			double predictedDistanceToEnemy =
				ScanLog.enemyLocation().distance(predictedState.location);
			
			double shorterDistance = Math.min(predictedDistanceToWaveSource,
				predictedDistanceToEnemy);
			double distancingDangerBase = 
				Math.max(currentDistanceToWaveSource / shorterDistance, .99);
			double distancingDangerExponent = shorterDistance > _fearDistance ?
				NORMAL_DISTANCING_EXPONENT : FEARFUL_DISTANCING_EXPONENT;

			firstWaveMultiplier = Math.pow(distancingDangerBase, 
				distancingDangerExponent);			
		}

		if (danger * firstWaveMultiplier > cutoffDanger) {
			return danger * firstWaveMultiplier;
		}
		
		double nextCounterClockwiseDanger = checkDanger(predictedState,
			COUNTERCLOCKWISE_OPTION, predictClockwiseOrNot, 
			surfableWaveIndex + 1, recursionLevels, cutoffDanger);
		double nextStopDanger = checkDanger(predictedState, 
			STOP_OPTION, predictClockwiseOrNot, surfableWaveIndex + 1,
			recursionLevels, cutoffDanger);
		double nextClockwiseDanger = checkDanger(predictedState,
			CLOCKWISE_OPTION, predictClockwiseOrNot, 
			surfableWaveIndex + 1, recursionLevels, cutoffDanger);
		
		danger += Math.min(nextCounterClockwiseDanger,
			Math.min(nextStopDanger, nextClockwiseDanger));

		danger *= firstWaveMultiplier;		
		
		return danger;
	}

	public void surf() {
		RobotState currentState = new RobotState(ScanLog.myLocation(),
			_robot.getHeadingRadians(), _robot.getVelocity(), _robot.getTime());
		boolean goingClockwise = (_lastMovementChoice == CLOCKWISE_OPTION);
				Collections.sort(_movementOptions);
		
		Collections.sort(_movementOptions);

		double bestSurfDanger = Double.POSITIVE_INFINITY;
		for (int x = 0; x < _movementOptions.size(); x++) {
			MovementChoice mc = _movementOptions.get(x);
			double thisDanger = checkDanger(currentState, 
				mc.getMovementOption(), goingClockwise, FIRST_WAVE, 
				WAVES_TO_SURF, bestSurfDanger);
			mc.lastDanger = thisDanger;
			
			bestSurfDanger = Math.min(bestSurfDanger, thisDanger);
		}
		
		double orbitCounterClockwiseDanger = _optionCounterClockwise.lastDanger;
		double stopDanger = _optionStop.lastDanger;
		double orbitClockwiseDanger = _optionClockwise.lastDanger;
		
		int goOrientation = _lastMovementChoice;

		Wave orbitWave = findSurfableWave(FIRST_WAVE);
		double orbitAbsBearing, distanceToClosestWaveSource;

		try {
			distanceToClosestWaveSource = ScanLog.myLocation()
				.distance(orbitWave.sourceLocation);
			orbitAbsBearing = DUtils.absoluteBearing(orbitWave.sourceLocation,
				ScanLog.myLocation());
		} catch (NullPointerException noSurfableWaves) {
			distanceToClosestWaveSource = ScanLog.getLastDistance();
			orbitAbsBearing = ScanLog.getLastEnemyScan().getAbsBearingRadians();
		}
		
		double goAngle, attackAngle;
		if (stopDanger == NO_SURFABLE_WAVES) {
			attackAngle = -1.047;
			_robot.setMaxVelocity(8);
			
			double goAngleCcw = orbitAbsBearing +
				(COUNTERCLOCKWISE_OPTION * ((Math.PI / 2) + attackAngle));
			goAngleCcw = wallSmoothing(ScanLog.myLocation(), goAngleCcw, 
				COUNTERCLOCKWISE_OPTION, distanceToClosestWaveSource);

			double goAngleCw = orbitAbsBearing +
				(CLOCKWISE_OPTION * ((Math.PI / 2) + attackAngle));
			goAngleCw = wallSmoothing(ScanLog.myLocation(), goAngleCw, 
				CLOCKWISE_OPTION, distanceToClosestWaveSource);
			
			if (Math.abs(Utils.normalRelativeAngle(goAngleCw - orbitAbsBearing))
				< Math.abs(Utils.normalRelativeAngle(goAngleCcw - orbitAbsBearing))) {
				goOrientation = CLOCKWISE_OPTION;
				goAngle = goAngleCw;
			} else {
				goOrientation = COUNTERCLOCKWISE_OPTION;
				goAngle = goAngleCcw;
			}
		} else {
			_robot.setMaxVelocity(8);
			attackAngle = 
				_currentDistancer.attackAngle(distanceToClosestWaveSource, 
					_desiredDistance);

			if (ScanLog.enemyIsRammer() && ScanLog.getLastDistance() < 300 &&
				orbitWave != NO_WAVE_FOUND) {
				if (Utils.normalRelativeAngle(DUtils.absoluteBearing(
						ScanLog.myLocation(), orbitWave.sourceLocation)
						- ScanLog.getLastScan().getAbsBearingRadians()) > 0) {
					goOrientation = -1;
				} else {
					goOrientation = 1;
				}
			} else if (stopDanger <= orbitCounterClockwiseDanger &&
				stopDanger <= orbitClockwiseDanger &&
				!ScanLog.enemyIsRammer()) {
				
				_robot.setMaxVelocity(0);
			} else {			
				if (orbitClockwiseDanger < orbitCounterClockwiseDanger) {
					goOrientation = CLOCKWISE_OPTION;
				} else {
					goOrientation = COUNTERCLOCKWISE_OPTION;
				}
			}
			
			goAngle = orbitAbsBearing +
				(goOrientation * ((Math.PI / 2) + attackAngle));
			goAngle = wallSmoothing(ScanLog.myLocation(), goAngle, goOrientation,
				distanceToClosestWaveSource);
		}
		
		
		DUtils.setBackAsFront(_robot, goAngle);
		
		_lastMovementChoice = goOrientation;
	}
	
	public void evaluateDistancingControl() {
        double powerTwoHitPercentageThreshold;

        if (_robot.getRoundNum() < 2) {
            _desiredDistance = 420;
            _fearDistance = 175;
            _smoothAwayDistance = 75;
        } else if (normalizedEnemyHitPercentage() 
        		> (powerTwoHitPercentageThreshold = 7.5)) {
            _desiredDistance = 
            	525 + (Math.max((ScanLog.avgEnemyPower() - 2), 0) * 165);
            _fearDistance = 250;
            _smoothAwayDistance = 75;
        } else if (normalizedEnemyHitPercentage() 
        		> (powerTwoHitPercentageThreshold = 3)) {
            _desiredDistance = 
            	450 + (Math.max((ScanLog.avgEnemyPower() - 2), 0) * 150);
            _fearDistance = 175;
            _smoothAwayDistance = 75;
        } else {
            _desiredDistance = 420;
            _fearDistance = 0;
            _smoothAwayDistance = 0;
        }
        
    	if (_robot.getRoundNum() < 2 || 
    		normalizedEnemyHitPercentage() > 7.5) {
    		_currentDistancer = _aggressiveDistancer;
    	} else {
    		_currentDistancer = _subtleDistancer;
    	}
	}

	public void evaluateWallStick() {
        _wallStick = (ScanLog.enemyIsRammer()) ? 
            	RAMMER_WALL_STICK : DEFAULT_WALL_STICK;
	}
	
	public void evaluateFlattener() {
        double powerTwoHitPercentageThreshold;
        
		if (_robot.getRoundNum() >= 20) {
            setFlattener(
            		normalizedEnemyHitPercentage() > ( 
            			(powerTwoHitPercentageThreshold = 9.5)) &&
            		_weightedEnemyShotsFired > 100);
		} else if (_robot.getRoundNum() >= 10) {
            setFlattener(
            		normalizedEnemyHitPercentage() > ( 
            			(powerTwoHitPercentageThreshold = 10.2)) &&
            		_weightedEnemyShotsFired > 50);
        } else if (_robot.getRoundNum() >= 5) {
            setFlattener(
            		normalizedEnemyHitPercentage() > ( 
            			(powerTwoHitPercentageThreshold = 13)) &&
            		normalizedEnemyHitPercentageLastRound() >
            			(powerTwoHitPercentageThreshold = 13) &&
            		_weightedEnemyShotsFired > 20);
        } else {
            setFlattener(false);
        }
	}
	
    public double normalizedEnemyHitPercentage() { 
    	return ((_weightedEnemyShotsFired == 0) ? 0 : 
    		(((double)_weightedEnemyShotsHit / 
    			_weightedEnemyShotsFired) * 100.0)); 
    }

    public double normalizedEnemyHitPercentageThisRound() { 
    	return ((_weightedEnemyShotsFiredThisRound == 0) ? 0 : 
    		(((double)_weightedEnemyShotsHitThisRound / 
    			_weightedEnemyShotsFiredThisRound) * 100.0)); 
    }

    public double normalizedEnemyHitPercentageLastRound() {
    	return _lastRoundNormalizedHitPercentage;
    }
    
    public void setFlattener(boolean enableFlattener) {
    	boolean oldFlattenerSetting = _flattenerEnabled;
    	
    	int preventFlattenerThrashing = 100;
    	if (_flattenerToggleTimer++ > preventFlattenerThrashing && 
    		oldFlattenerSetting != enableFlattener) {
    		
    		_flattenerToggleTimer = 0;
    		
    		if (enableFlattener) { enableFlattener(); } 
    		else { disableFlattener(); }
    	}
    }
    
    public void enableFlattener() {
        _flattenerEnabled = true;
        
        _waveRegisters.add(_highStatBuffers);
        _waveRegisters.add(_lowStatBuffers);        

        System.out.println("Curve Flattening enabled.");
    }

    public void disableFlattener() {
        _flattenerEnabled = false;
        
        _waveRegisters.remove(_highStatBuffers);
        _waveRegisters.remove(_lowStatBuffers);
        
        if (_robot.getRoundNum() >= 3 
        		&& normalizedEnemyHitPercentage() < 4) {
            _highStatBuffers.setBulletHitWeight(0.2);
            _lowStatBuffers.setBulletHitWeight(0.2);
        } else {
            _highStatBuffers.setBulletHitWeight(1.0);
            _lowStatBuffers.setBulletHitWeight(1.0);
        }

        System.out.println("Curve Flattening disabled.");
    }
    
    public double getBinScore(Wave w, Point2D.Double targetLocation) {
    	double binScore = 0;
    	
    	GuessFactorWindowSet gfWindow = w.guessFactorWindow(targetLocation);
    	int lowIndex = w.guessFactorIndex(gfWindow.guessFactorLow);
    	int highIndex = w.guessFactorIndex(gfWindow.guessFactorHigh);

    	binScore += _highStatBuffers.getWindowScore(w.targetScan, 
    		w.targetScan.getInverseScan(), w.bulletPower, BINS,
    		lowIndex, highIndex);
    	binScore += _lowStatBuffers.getWindowScore(w.targetScan, 
       		w.targetScan.getInverseScan(), w.bulletPower, BINS,
       		lowIndex, highIndex); 

    	if (_flattenerEnabled) {
    		binScore += _extraFlatStatBuffers.getWindowScore(w.targetScan, 
    			w.targetScan.getInverseScan(), w.bulletPower, BINS,
    			lowIndex, highIndex); 
    	}
    	
    	return binScore;
    }
    
    public double wallSmoothing(Point2D.Double startLocation, 
    	double goAngleRadians, int orientation, double currentDistance) {
    	
    	if (currentDistance < _smoothAwayDistance) {
    		orientation *= -1;
    	}
    	
    	double smoothedAngle = DUtils.wallSmoothing(
    		DUtils.battleField, DUtils.battleFieldWidth, 
    		DUtils.battleFieldHeight, startLocation, goAngleRadians,
    		orientation, _wallStick);

    	return smoothedAngle;
    }

    public void victoryDance() {

    }
/*
	private static abstract class Renderable{
		public abstract void render(Graphics2D g);
		
		private static class Circle extends Renderable{
			Point2D.Double center;
			double radius;
			Color color;
			public Circle(Point2D.Double center, double radius, Color color){
				this.center = center;
				this.radius = radius;
				this.color = color;
			}
			public void render(Graphics2D g) {
				g.setColor(color);
				g.drawOval(	(int)Math.round(center.x - radius),
							(int)Math.round(center.y - radius),
							(int)Math.round(2 * radius),
							(int)Math.round(2 * radius));
			}
		}
		
		private static class Dot extends Renderable{
			Point2D.Double point;
			double radius;
			Color color;
			public Dot(Point2D.Double point, Color color){
				this.point = point;
				this.radius = 2;
				this.color = color;
			}
			public void render(Graphics2D g) {
				g.setColor(color);
				g.fillOval(	(int)Math.round(point.x - radius),
							(int)Math.round(point.y - radius),
							(int)Math.round(2 * radius),
							(int)Math.round(2 * radius));
			}
		}
		
		private static class Line extends Renderable{
			Point2D.Double p1, p2;
			Color color;
			
			double radius;
			public Line(Point2D.Double p1, Point2D.Double p2, Color color){
				this.p1 = p1;
				this.p2 = p2;
				this.color = color;
			}
			public void render(Graphics2D g) {
				g.setColor(color);
				g.drawLine(	(int)Math.round(p1.x),
							(int)Math.round(p1.y),
							(int)Math.round(p2.x),
							(int)Math.round(p2.y));
			}
		}
		
		private static class Text extends Renderable{
			String text;
			double x, y;
			Color color;
			
			double radius;
			public Text(String text, double x, double y, Color color){
				this.text = text;
				this.x = x;
				this.y = y;
				this.color = color;
			}
			public void render(Graphics2D g) {
				g.setColor(color);
				g.drawString(text, (float)x, (float)y);
			}
		}
	}
	
	public static void drawLine(Point2D.Double p1, Point2D.Double p2, Color color){
		_renderables.add(new Renderable.Line(p1, p2, color));
	}
	
	public static void drawCircle(Point2D.Double center, double radius, Color color){
		_renderables.add(new Renderable.Circle(center, radius, color));
	}
	
	public static void drawPoint(Point2D.Double p1, Color color){
		_renderables.add(new Renderable.Dot(p1, color));
	}
	
	public static void drawText(String text, double x, double y, Color color){
		_renderables.add(new Renderable.Text(text, x, y, color));
	}
*/	
}

interface DistanceController {
	public double attackAngle(double currentDistance, double desiredDistance);
}

class SubtleDistancing implements DistanceController {
	public double attackAngle(double currentDistance, double desiredDistance) {
        
		double distanceOffset = currentDistance - desiredDistance;
		double attackAngle = 
			DUtils.square(distanceOffset / desiredDistance) 
				* DUtils.sign(distanceOffset);
		
		return attackAngle;
   }
}
 
class AggressiveDistancing implements DistanceController {
    public double attackAngle(double currentDistance, double desiredDistance) {
        
		double distanceOffset = currentDistance - desiredDistance;
		double attackAngle = 
			DUtils.square(distanceOffset / desiredDistance) 
				* DUtils.sign(distanceOffset) * 3;
		
		return attackAngle;
   }
}

abstract class MovementChoice implements java.lang.Comparable<MovementChoice> {
	public double lastDanger = Double.POSITIVE_INFINITY;
	
	abstract public int getMovementOption();
	
	public int compareTo(MovementChoice m2) {
		if (lastDanger < m2.lastDanger) { return -1; }
		else if (lastDanger > m2.lastDanger) { return 1; }
		else { return 0; }
	}
}

class MovementCounterClockwise extends MovementChoice {
	public int getMovementOption() {
		return DookiCape.COUNTERCLOCKWISE_OPTION;
	}
}

class MovementStop extends MovementChoice {
	public int getMovementOption() {
		return DookiCape.STOP_OPTION;
	}
}

class MovementClockwise extends MovementChoice {
	public int getMovementOption() {
		return DookiCape.CLOCKWISE_OPTION;
	}
}


/*
		double orbitCounterClockwiseDanger = checkDanger(currentState,
			COUNTERCLOCKWISE_OPTION, goingClockwise, FIRST_WAVE, WAVES_TO_SURF);
		double stopDanger = checkDanger(currentState,
			STOP_OPTION, goingClockwise, FIRST_WAVE, WAVES_TO_SURF);
		double orbitClockwiseDanger = checkDanger(currentState,
			CLOCKWISE_OPTION, goingClockwise, FIRST_WAVE, WAVES_TO_SURF);
*/			
