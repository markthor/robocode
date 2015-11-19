package voidious.dgun;

import robocode.*;
import robocode.util.Utils;
import voidious.utils.*;

import java.util.ArrayList;
import java.awt.geom.Point2D;
import java.awt.Graphics2D;

/**
 * DookiLightning - targeting system for Dookious
 *
 * CREDITS:
 *   Paul Evans - creator of GuessFactorTargeting; brought RollingAverages to
 *         Robocode.
 *   Vic Stewart - creator of WikiTargeting, basis for the gun data saving used
 *         in Dookious.
 *   Mue - original idea of having a separate AntiSurfer gun, as in Ascendant.   
 *   PEZ - deserves some credit for mainstream usage of VirtualGuns; general
 *         source of guidance in OpenSource bot, CassiusClay. 
 *
 * Code is open source, released under the RoboWiki Public Code License:
 * http://robowiki.net/cgi-bin/robowiki?RWPCL
 */

public class DookiLightning {
//	private static int _bigGfs = 0;
    private static final int BINS = 59;
    private static final double DEFAULT_BULLET_POWER = 1.9;
    private static final boolean ENABLE_GUN_DATA_SAVING = true;
    
    private AdvancedRobot _robot;
	private boolean _tcMode;
	private static boolean _gunDataSavingEnabled = ENABLE_GUN_DATA_SAVING;

	private ArrayList _waves;
	private ArrayList _waveRegisters;
	private ArrayList _bulletHitRegisters;

	private double _bulletPower = DEFAULT_BULLET_POWER;		
	private VirtualWaveGun _currentGun;
	private Wave _nextWave;
	private Wave _currentWave;
	private VirtualGunsManager _gunManager;
	private VirtualWaveGun _mainGun;
	private VirtualWaveGun _antiSurferGun;
//	private StatBufferSet _enemySurfBufferSet;
	private String _enemyName;
	private boolean _restoreAttemped = false;
	
	private int _previousOrientation = 1;
	
	private static final double GUN_ORIENTATION_VELOCITY_THRESHOLD = 1.0;
	
	public DookiLightning(AdvancedRobot robot, boolean tc) {
		DUtils.setBattleFieldWidth(robot.getBattleFieldWidth());
		DUtils.setBattleFieldHeight(robot.getBattleFieldHeight());
		_tcMode = tc;
		
		if (_tcMode) { _gunDataSavingEnabled = false; }
		
		_waves = new ArrayList();
		_waveRegisters = new ArrayList();
		_bulletHitRegisters = new ArrayList();
		_gunManager = new VirtualGunsManager();

		initializeGuns();
		reset(robot);
	}

	public void initializeGuns() {
		double weight;
		
		StatBufferSet mainGunBufferSet = new StatBufferSet();
		mainGunBufferSet.addStatBuffer(new MainGunHighBuffer(BINS), 
			weight = 200);
		mainGunBufferSet.addStatBuffer(new MainGunLowBuffer(BINS), 
			weight = 200);
		mainGunBufferSet.addStatBuffer(new MainGunFastHighBuffer(BINS), 
			weight = 50);
		mainGunBufferSet.addStatBuffer(new MainGunFastLowBuffer(BINS), 
			weight = 50);
		mainGunBufferSet.addStatBuffer(new MainGunRawBuffer(BINS), 
			weight = .00002);
		_waveRegisters.add(mainGunBufferSet);
		_mainGun = new VirtualWaveGun("Main Gun", mainGunBufferSet, BINS);
		
		StatBufferSet antiSurfBufferSet = new StatBufferSet();
		antiSurfBufferSet.addStatBuffer(new AntiSurfHighBuffer(BINS), 
			weight = 200);
		antiSurfBufferSet.addStatBuffer(new AntiSurfLowBuffer(BINS), 
			weight = 200);
		antiSurfBufferSet.addStatBuffer(new AntiSurfFastLowBuffer(BINS), 
			weight = .02);
		antiSurfBufferSet.addStatBuffer(new AntiSurfRawBuffer(BINS), 
			weight = .0002);
		_waveRegisters.add(antiSurfBufferSet);
		_bulletHitRegisters.add(antiSurfBufferSet);
		_antiSurferGun = 
			new VirtualWaveGun("AntiSurfer Gun", antiSurfBufferSet, BINS);
	
		_gunManager = new VirtualGunsManager();
		_gunManager.addGun(_mainGun);
		_gunManager.addGun(_antiSurferGun);
		_waveRegisters.add(_gunManager);
		
		_currentGun = _mainGun;
	}
	
	public void reset(AdvancedRobot robot) {		
		System.out.println("Using gun: " + _currentGun.getLabel());
		
		_robot = robot;
		_waves.clear();
		_gunManager.clearBullets();
		_currentWave = null;
		_nextWave = null;
		
		ScanLog.reset();
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		ScanLog.onScannedRobot(_robot, e);
		
		_enemyName = e.getName();

		if (_robot.getRoundNum() == 0 && !_restoreAttemped) {
			_restoreAttemped = true;
			restoreGunData();
		}

		updateBulletPower(e);
		initializeNextWave();
		evaluateVirtualGuns();
		aimGun(e);
		
		boolean realShotFired = false;
		if (Math.abs(_robot.getGunTurnRemainingRadians())
			< DUtils.botWidthAimAngle(e.getDistance())) {
			if (_tcMode) {
				realShotFired = (_robot.setFireBullet(3) != null);
			} else {
				realShotFired =	(_robot.setFireBullet(_bulletPower) != null);
			}
		}

		if (_robot.getEnergy() > 0) {
			if (realShotFired) { fireVirtualBullets(); }
			fireWave(realShotFired ? Wave.FIRING_WAVE : Wave.NON_FIRING_WAVE);
		}
		
		checkActiveWaves();
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
        System.out.println();
        
        _gunManager.printGunRatings();
        System.out.println();

		if (_tcMode) {
            System.out.println("TC score: " +
                (ScanLog.getBulletDamageGiven() / (_robot.getRoundNum() + 1)));
		}
		
		if (_robot.getRoundNum() == _robot.getNumRounds() - 1) {
			saveGunData();
		}
		
//		System.out.println("Big GFs: " + _bigGfs);
	}
	
	public void onBulletHit(BulletHitEvent e) {
		ScanLog.onBulletHit(e);

		if (!_bulletHitRegisters.isEmpty()) {
			int tightMatchDistanceThreshold = 50;
			Wave hitWave = DUtils.findClosestWave(_waves,
				new Point2D.Double(e.getBullet().getX(), e.getBullet().getY()),
				_robot.getTime(), DUtils.ANY_WAVE, DUtils.FIRING_WAVE, 
				tightMatchDistanceThreshold);
			
			if (hitWave == null) { return; }
			
			for (int x = 0; x < _bulletHitRegisters.size(); x++) {
				BulletHitRegister bhr = 
					(BulletHitRegister)_bulletHitRegisters.get(x);
				
				bhr.registerBulletHit(e.getBullet(), hitWave, e.getTime());
			}
		}		
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		ScanLog.onBulletHitBullet(e);
		
	}

	public void onBulletMissed(BulletMissedEvent e) {
		ScanLog.onBulletMissed(e);
		
	}

	public void onHitByBullet(HitByBulletEvent e) {
		ScanLog.onHitByBullet(e);
		
	}

	public void updateBulletPower(ScannedRobotEvent e) {
		if (_tcMode) {
			_bulletPower = 3;
			return;
		}
		
        if (ScanLog.enemyIsRammer() ||
            ScanLog.getLastDistance() < 120) {
            _bulletPower = 3.0;
            return;
        }

        double myEnergy = _robot.getEnergy();
        double enemyEnergy = e.getEnergy();

        if (myEnergy < 5) {
        	_bulletPower = 1;
        } else if (myEnergy < 15 && enemyEnergy > myEnergy) {
        	_bulletPower = 1.4;
        } else if (myEnergy < 25 && enemyEnergy > myEnergy ||
        		myEnergy < enemyEnergy - 20) {
        	_bulletPower = 1.59;
        } else if (_currentGun.getRating() > .28 && _robot.getRoundNum() >= 2) {
        	_bulletPower = 3;
        } else if (_currentGun.getRating() > .22 && _robot.getRoundNum() >= 2 ||
        		ScanLog.getLastDistance() < 250) {
        	_bulletPower = 2.39;
        } else {
        	_bulletPower = DEFAULT_BULLET_POWER;
        }

        _bulletPower = Math.min(_bulletPower, enemyEnergy / 4);
        _bulletPower = Math.min(_bulletPower, myEnergy);
	}
	
	public void initializeNextWave() {
		Point2D.Double myNextLocation = DUtils.nextLocation(_robot);
		Point2D.Double enemyNextLocation = 
			DUtils.nextLocation(ScanLog.getLastScan());
		
		_nextWave = new Wave(ScanLog.getLastScan(),	myNextLocation, 
			enemyNextLocation, _robot.getTime() + 1, Wave.NON_FIRING_WAVE, 
			DUtils.absoluteBearing(myNextLocation, enemyNextLocation), 
			_bulletPower, "Gun Wave", Wave.PRECISE_MAX_ESCAPE_ANGLE, 
			ScanLog.getLastScan().getHeadingRadians(), 
			ScanLog.getLastScan().getVelocity(), _previousOrientation,
			GUN_ORIENTATION_VELOCITY_THRESHOLD);

		_previousOrientation = _nextWave.orientation;
	}
	
	public void aimGun(ScannedRobotEvent e) {		
		if (_robot.getGunHeat() > _robot.getGunCoolingRate() * 3) {
			_robot.setTurnGunRightRadians(Utils.normalRelativeAngle(
				e.getBearingRadians() + _robot.getHeadingRadians() -
				_robot.getGunHeadingRadians()));
		} else {
			BotScan scan = ScanLog.getLastScan();
			BotScan enemyScan = scan.getInverseScan();
			
			_robot.setTurnGunRightRadians(Utils.normalRelativeAngle(
				_currentGun.getAbsoluteAimAngle(_nextWave, scan, enemyScan, 
					_bulletPower)
				- _robot.getGunHeadingRadians()));
		}
	}
	
	public void fireWave(boolean isFiringWave) {
		if (_currentWave != null) {
			_currentWave.firingWave = isFiringWave;
			_currentWave.sourceLocation = ScanLog.myLocation();
			_currentWave.originalTargetLocation = ScanLog.enemyLocation();
			_currentWave.absBearingRadians = 
				ScanLog.getLastScan().getAbsBearingRadians();
			_currentWave.clearEscapeAngleCache();

			_waves.add(_currentWave);
		}
		
		_currentWave = _nextWave;
	}
	
	public void checkActiveWaves() {
		long currentTime = _robot.getTime();
		Point2D.Double enemyLocation = ScanLog.enemyLocation();
		
		for (int x = 0; x < _waves.size(); x++) {
			Wave w = (Wave)_waves.get(x);
			if (w.processedWaveBreak) {
				if (w.wavePassed(enemyLocation, currentTime,
					Wave.INACTIVE_WAVE_OFFSET)) {
					_waves.remove(x--);
				}
			} else {
				if (w.wavePassed(enemyLocation, currentTime)) {
					processWave(w);
				}
			}
		}
	}
	
	public void processWave(Wave w) {
		GuessFactorWindowSet gfWindow = 
			w.guessFactorWindow(ScanLog.enemyLocation());
/*
		if (Math.abs(gfWindow.guessFactor) > 1.1) {
			System.out.println("Big GF (" + _robot.getTime() + "): " + gfWindow.guessFactor);
			System.out.println("MEA: " + w.maxEscapeAngle(gfWindow.guessFactor > 0));
			System.out.println("Smooth: " + (gfWindow.guessFactor > 0 ? w.usedPositiveSmoothingMea : w.usedNegativeSmoothingMea));
			_bigGfs++;
		}
*/
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
	
	public void fireVirtualBullets() {
		_gunManager.fireVirtualBullets(_currentWave, _currentWave.targetScan,
			_currentWave.targetScan.getInverseScan(), _bulletPower);
	}
	
	public void evaluateVirtualGuns() {
		VirtualWaveGun lastGun = _currentGun;

        if (_gunManager.contains(_mainGun) &&
        	((_mainGun.getRating() > .26) ||
        	(_mainGun.getRating() > .22 &&
        		_mainGun.getRating() > (.85 * _antiSurferGun.getRating())))) {
        	_currentGun = _mainGun;
        } else {
        	_currentGun = _gunManager.bestGun();
        }
        
        if (_currentGun != lastGun) {
        	System.out.println("Switching to " + _currentGun.getLabel());
        }
	}

	public void saveGunData() {
		if (_gunDataSavingEnabled && 
			_mainGun.getRating() > _antiSurferGun.getRating() * .96) {
			_mainGun.save(_robot, _enemyName);
		}
	}
	
	public void restoreGunData() {
		if (_gunDataSavingEnabled) {
			_mainGun.restore(_robot, _enemyName);
		}
	}

}
