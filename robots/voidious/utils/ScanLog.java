package voidious.utils;

import robocode.*;
import robocode.util.Utils;
import java.util.ArrayList;
import java.awt.geom.Point2D;

public class ScanLog {
    public static ArrayList scanLog;
    public static ArrayList moveLog;

    private static Point2D.Double _myLocation;    
    private static Point2D.Double _enemyLocation;    
    private static long _lastScanTime = -1;
    private static long _lastBulletHitTime = -1;
    private static long _lastBulletMissedTime = -1;
    private static long _lastBulletHitBulletTime = -1;
    private static long _lastHitByBulletTime = -1;
    private static long _lastWinTime = -1;
    private static long _lastDeathTime = -1;

    private static int _scanLimit = 25;
    private static long _travelTime = 0; // ticks since Dookious was at near 0 velocity
    private static long _enemyTravelTime = 0; // since enemy at near 0 velocity
    private static long _vchangeTime = 0; // ticks since Dookious velocity changed
    private static long _enemyVchangeTime = 0; // ticks since enemy velocity changed
    private static long _sinceHitTime = 0; // ticks since Dookious was hit by a bullet
    private static long _enemySinceHitTime = 0; // ticks since enemy was hit by a bullet
    private static long _sinceMaxSpeedTime = 0; // ticks since Dookious was near 8.0 speed
    private static long _enemySinceMaxSpeedTime = 0; // ticks since Dookious was near 8.0 speed
    private static long _bulletHits = 0;
    private static long _bulletMisses = 0;
    private static long _bulletHitsThisRound = 0;
    private static long _bulletMissesThisRound = 0;
    private static double _bulletDamage = 0;
    private static long _enemyBulletHits = 0;
    private static long _enemyBulletsFired = 0;
    private static double _enemyBulletDamage = 0;
    private static long _enemyBulletHitsThisRound = 0;
    private static long _enemyBulletsFiredThisRound = 0;

    private static double _lastBearing = 0;
    private static double _lastHeading = 0;
    private static double _lastDistance = 0;
    private static double _lastVelocity = 0;
    private static double _lastEnergy = 100;
    private static double _lastMyEnergy = 100;
    private static double _enemyPower = 1; // use enemyPower(), last power from enemy
    private static double _enemyPowerTotal = 0; // sum of all enemy powers
    private static long _lastFireOffset = 0; // offset between detection of bullet
                                      // and the time it was actually fired
    private static double _lastEnergyDrop = -1; // make sure to not double detect fire
    private static long _lastEnemyFireCheck = -1; // time of last check for enemy fire
    private static double _lastBulletDamage = -1; // bullet damage of our last shot landed
    private static double _lastEnemyBulletLifeGained = -1; // bullet damage of our last shot landed

    private static int _lastScanLogOrientation = 1;
    private static int _lastMoveLogOrientation = 1;
    private static double _scanLogVThreshold = 1.0;
    private static double _moveLogVThreshold = 0.1;
    private static long _enemyRammingCounter = 0;
    private static boolean _enemyIsRammer = false;

    /**
     * onScannedRobot: If this is a new scan, log it. If not, ignore it.
     */
    public static void onScannedRobot(AdvancedRobot robot, ScannedRobotEvent e) {
    	_myLocation = new Point2D.Double(robot.getX(), robot.getY());
    	
    	if (e.getTime() == _lastScanTime) { return; }

        if (e.getEnergy() > 0.0) {
            _lastBearing = e.getBearingRadians();
            _lastHeading = e.getHeadingRadians();
            _lastDistance = e.getDistance();
            _lastVelocity = e.getVelocity();
            _lastEnergy = e.getEnergy();
        }

        if (robot.getEnergy() > 0.0) {
            _lastMyEnergy = robot.getEnergy();
        }

        ///// Update pseudo-scan log of our own movements //////////////////////
        double turnRate;
        double speed1;
        double speed2;
        int accel;
        double distLastEightTicks;
        double distLastFifteenTicks;
        double distLastTwentyFiveTicks;
        if (scanLog.size() > 1) {
            _travelTime =
                ((Math.abs(robot.getVelocity()) > .5)?(_travelTime + 1):0);
            _vchangeTime = ((Math.abs(robot.getVelocity() -
                            ((BotScan)moveLog.get(0)).getVelocity()) > .5)?
                            0:(_vchangeTime + 1));
            _sinceHitTime++;
            turnRate = Utils.normalRelativeAngle(robot.getHeadingRadians()
                            - ((BotScan)moveLog.get(0)).getHeadingRadians());

            speed1 = Math.abs(((BotScan)moveLog.get(0)).getVelocity());
            speed2 = Math.abs(robot.getVelocity());

            if (speed2 >= 7.5) {
                _sinceMaxSpeedTime = 0;
            } else {
                _sinceMaxSpeedTime++;
            }

            // 2 = accelerating, 1 = same, 0 = decelerating
            accel = ((speed2 > speed1 + .5)?2:((speed1 > speed2 + .5)?0:1));
            
            if (scanLog.size() >= 8) {
                distLastEightTicks = _myLocation.distance(
                	((BotScan)moveLog.get(7)).getLocation());
            } else {
                distLastEightTicks = _myLocation.distance(
                	((BotScan)moveLog.get(moveLog.size() - 1)).getLocation());
            }
            
            if (scanLog.size() >= 15) {
                distLastFifteenTicks = _myLocation.distance(
                	((BotScan)moveLog.get(14)).getLocation());
            } else {
                distLastFifteenTicks = _myLocation.distance(
                	((BotScan)moveLog.get(moveLog.size() - 1)).getLocation());
            }
            
            if (scanLog.size() >= 25) {
                distLastTwentyFiveTicks = _myLocation.distance(
                	((BotScan)moveLog.get(24)).getLocation());
            } else {
                distLastTwentyFiveTicks = _myLocation.distance(
                	((BotScan)moveLog.get(moveLog.size() - 1)).getLocation());
            }
        } else {
            _travelTime = 0;
            _vchangeTime = 0;
            _sinceHitTime = 0;
            _sinceMaxSpeedTime = 0;
            turnRate = 0;
            accel = 1;
            distLastEightTicks = 0;
            distLastFifteenTicks = 0;
            distLastTwentyFiveTicks = 0;
        }

        BotScan newMove =
            new BotScan(Utils.normalAbsoluteAngle(e.getBearingRadians()
            	+ Math.PI + robot.getHeadingRadians()),
            	Utils.normalRelativeAngle(e.getBearingRadians()
                + Math.PI + robot.getHeadingRadians() - e.getHeadingRadians()),
                robot.getHeadingRadians(), e.getDistance(), robot.getVelocity(),
                robot.getEnergy(), turnRate, accel, _myLocation, 
                _travelTime, _vchangeTime, _sinceHitTime,
                _sinceMaxSpeedTime, distLastEightTicks, 
                distLastFifteenTicks, distLastTwentyFiveTicks, e.getTime());
        moveLog.add(0, newMove);

        // if we are over the scan limit, drop the oldest data
        if (moveLog.size() > _scanLimit + 3) {
            moveLog.remove(_scanLimit);
            moveLog.remove(_scanLimit);
            moveLog.remove(_scanLimit);
        }
        ////////////////////////////////////////////////////////////////////////


        ///// Update scan log of enemy movements ///////////////////////////////
        _enemyLocation = DUtils.project(_myLocation, robot.getHeadingRadians()
        	+ e.getBearingRadians(), e.getDistance());

        if (scanLog.size() > 1) {
            _enemyTravelTime = ((Math.abs(e.getVelocity()) > .5)?
                                 (_enemyTravelTime + 1):0);
            _enemyVchangeTime = ((Math.abs(e.getVelocity() -
                            ((BotScan)scanLog.get(0)).getVelocity()) > .5)?
                            0:(_enemyVchangeTime + 1));
            _enemySinceHitTime++;
            turnRate = Utils.normalRelativeAngle(e.getHeadingRadians() -
                ((BotScan)scanLog.get(0)).getHeadingRadians());

            speed1 = Math.abs(((BotScan)scanLog.get(0)).getVelocity());
            speed2 = Math.abs(e.getVelocity());

            if (speed2 >= 7.5) {
                _enemySinceMaxSpeedTime = 0;
            } else {
                _enemySinceMaxSpeedTime++;
            }

            // 2 = accelerating, 1 = same, 0 = decelerating
            accel = ((speed2 > speed1 + .5)?2:((speed1 > speed2 + .5)?0:1));

            if (scanLog.size() >= 8) {
                distLastEightTicks = _enemyLocation.distance(
                	((BotScan)scanLog.get(7)).getLocation());
            } else {
                distLastEightTicks = _enemyLocation.distance(
                	((BotScan)scanLog.get(scanLog.size() - 1)).getLocation());
            }
            
            if (scanLog.size() >= 15) {
                distLastFifteenTicks = _enemyLocation.distance(
                	((BotScan)scanLog.get(14)).getLocation());
            } else {
                distLastFifteenTicks = _enemyLocation.distance(
                	((BotScan)scanLog.get(scanLog.size() - 1)).getLocation());
            }

            if (scanLog.size() >= 25) {
                distLastTwentyFiveTicks = _enemyLocation.distance(
                	((BotScan)scanLog.get(24)).getLocation());
            } else {
                distLastTwentyFiveTicks = _enemyLocation.distance(
                	((BotScan)scanLog.get(scanLog.size() - 1)).getLocation());
            }
        } else {
            _enemyTravelTime = 0;
            _enemyVchangeTime = 0;
            _enemySinceHitTime = 0;
            _enemySinceMaxSpeedTime = 0;
            turnRate = 0;
            accel = 1;
            distLastEightTicks = 0;
            distLastFifteenTicks = 0;
            distLastTwentyFiveTicks = 0;
        }

        BotScan newScan = new BotScan(
        	Utils.normalAbsoluteAngle(e.getBearingRadians() +
        	robot.getHeadingRadians()),	e.getBearingRadians(), 
        	e.getHeadingRadians(), e.getDistance(), e.getVelocity(), 
        	e.getEnergy(), turnRate, accel, _enemyLocation, _enemyTravelTime,
            _enemyVchangeTime, _enemySinceHitTime, _enemySinceMaxSpeedTime,
            distLastEightTicks, distLastFifteenTicks, distLastTwentyFiveTicks,
            e.getTime());
        scanLog.add(0, newScan);

         _lastScanTime = e.getTime();

        // if we are over the scan limit, drop the oldest data
        if (scanLog.size() > _scanLimit + 3) {
            scanLog.remove(_scanLimit);
            scanLog.remove(_scanLimit);
            scanLog.remove(_scanLimit);
        }
        ////////////////////////////////////////////////////////////////////////

        if (Math.abs(robot.getVelocity()) > _moveLogVThreshold) {
			double relativeHeadingRadians = Utils.normalRelativeAngle(
				newMove.getHeadingRadians() - newMove.getAbsBearingRadians() +
				(newMove.getVelocity() < 0 ? Math.PI : 0));
				
            _lastMoveLogOrientation = DUtils.sign(relativeHeadingRadians);
        }
        newMove.setOrientation(_lastMoveLogOrientation);
        newMove.setInverseScan(newScan);
        
        if (Math.abs(e.getVelocity()) > _scanLogVThreshold) {
			double relativeHeadingRadians = Utils.normalRelativeAngle(
				newScan.getHeadingRadians() - newScan.getAbsBearingRadians() +
				(newScan.getVelocity() < 0 ? Math.PI : 0));
					
	         _lastScanLogOrientation = DUtils.sign(relativeHeadingRadians);
        }
        newScan.setOrientation(_lastScanLogOrientation);
        newScan.setInverseScan(newMove);

        enemyRammerCheck();
    }

    public static void onBulletHit(BulletHitEvent e) {
        if (e.getTime() == _lastBulletHitTime) { return; }

        _lastBulletHitTime = e.getTime();
        _lastBulletDamage =
            Math.min(DUtils.bulletDamage(e.getBullet().getPower()),
                _lastEnergy);
        _enemySinceHitTime = 0;
        _bulletHits++;
        _bulletDamage += _lastBulletDamage;
        _bulletHitsThisRound++;
    }

    public static void onBulletMissed(BulletMissedEvent e) {
        if (e.getTime() == _lastBulletMissedTime) { return; }

        _lastBulletMissedTime = e.getTime();
//        if (!misser.isActive() && misser.getName().equals(this.getName())) {
        _bulletMisses++;
        _bulletMissesThisRound++;
    }

    public static void onBulletHitBullet(BulletHitBulletEvent e) {
        if (e.getTime() == _lastBulletHitBulletTime) { return; }

        _lastBulletHitBulletTime = e.getTime();
//        if (!e.getBullet().isActive() && e.getBullet().getName().equals(this.getName())) {
        _bulletMisses++;
        _bulletMissesThisRound++;

    }

    public static void onHitByBullet(HitByBulletEvent e) {
        if (e.getTime() == _lastHitByBulletTime) { return; }

        _lastHitByBulletTime = e.getTime();
        _lastEnemyBulletLifeGained =
            DUtils.bulletLifeGain(e.getBullet().getPower());
        _sinceHitTime = 0;
        _enemyBulletHits++;
        _enemyBulletDamage +=
            Math.min(DUtils.bulletDamage(e.getBullet().getPower()),
                _lastMyEnergy);
        _enemyBulletHitsThisRound++;
    }

    public static void onWin(WinEvent e) {
        if (e.getTime() == _lastWinTime) { return; }

        _lastWinTime = e.getTime();
//        _bulletDamage += _lastEnergy;
    }

    public static void onDeath(DeathEvent e) {
        if (e.getTime() == _lastDeathTime) { return; }

        _lastDeathTime = e.getTime();
    }

    public static void reset() {
        _lastScanTime = -1;
        _lastBulletHitTime = -1;
        _lastBulletMissedTime = -1;
        _lastBulletHitBulletTime = -1;
        _lastHitByBulletTime = -1;
        _lastWinTime = -1;
        _lastDeathTime = -1;
        _lastEnergyDrop = -1;
        _lastEnemyFireCheck = -1; // time of last check for enemy fire
        _travelTime = 0;
        _enemyTravelTime = 0;
        _vchangeTime = 0;
        _enemyVchangeTime = 0;
        _sinceHitTime = 0;
        _enemySinceHitTime = 0;
        _sinceMaxSpeedTime = 0;
        _enemySinceMaxSpeedTime = 0;
        _bulletHitsThisRound = 0;
        _bulletMissesThisRound = 0;
        _enemyBulletHitsThisRound = 0;
        _enemyBulletsFiredThisRound = 0;
        _lastBulletDamage = -1;
        _lastEnemyBulletLifeGained = -1;

        _lastBearing = 0;
        _lastHeading = 0;
        _lastDistance = 0;
        _lastVelocity = 0;
        _lastEnergy = 0;

        if (scanLog == null) {
            scanLog = new ArrayList();
            moveLog = new ArrayList();
        } else {
            scanLog.clear();
            moveLog.clear();
        }
    }

   /**
    * enemyFired: If energy of enemy went down and they weren't hit,
    *   we know they fired.
    *   - despite the precision lost, I'm content to miss fires that happened
    *     in the exact frame they were hit. If they lost energy <= 3,
    *     assume they fired.
    */
    public static boolean enemyFired(long now) {
        long scope = Math.min(4, (now - _lastEnemyFireCheck) + 1); // how many scans back to check...
        _lastEnemyFireCheck = now;
        if (ScanLog.scanLog.size() < scope) { return false; }

        //  loop through last #scope# scans, find any bullets fired.
        double energy1, energy2, energyDiff;
        double velocity1, velocity2, wallDamage;
        for (int x = 0; x < scope - 1; x++) {
            energy1 = ((BotScan)ScanLog.scanLog.get(x+1)).getEnergy();
            energy2 = ((BotScan)ScanLog.scanLog.get(x)).getEnergy();

            velocity1 = ((BotScan)ScanLog.scanLog.get(x+1)).getVelocity();
            velocity2 = ((BotScan)ScanLog.scanLog.get(x)).getVelocity();
            wallDamage =
                Math.max((Math.abs(velocity1 - velocity2) * .5) - 1, 0);

            if (energy1 != energy2 && energy2 != _lastEnergyDrop) {
                long fireTime =
                    ((BotScan)ScanLog.scanLog.get(x+1)).getTime();

                energyDiff = energy1 - energy2 - wallDamage;
                if (fireTime + 1 == _lastBulletHitTime) {
                    energyDiff -= _lastBulletDamage;
                }
                if (fireTime + 1 == _lastHitByBulletTime) {
                    energyDiff += _lastEnemyBulletLifeGained;
                }

                if (energyDiff < 3.01 && energyDiff > 0.09) {
                    _enemyPower = energyDiff;
                    _enemyPowerTotal += energyDiff;
                    _lastFireOffset = now - fireTime;
                    _lastEnergyDrop = energy2;
                    _enemyBulletsFired++;
                    _enemyBulletsFiredThisRound++;
                    return true;
                }
            }
        }

        return false;
    }

    public static void enemyRammerCheck() {
        BotScan lastScan = (BotScan)ScanLog.scanLog.get(0);
        BotScan lastBotScan = (BotScan)ScanLog.moveLog.get(0);

        if (((Math.abs(lastBotScan.getBearingRadians()) < 0.436332313 && lastScan.getVelocity() >= 0)
        || (Math.abs(Utils.normalRelativeAngle(lastBotScan.getBearingRadians() + Math.PI)) < 0.436332313 && lastScan.getVelocity() < 0)
        || (lastScan.getDistance() < 75))
        && Math.abs(lastScan.getVelocity()) > 0.5) {
            _enemyRammingCounter = Math.min(5000, _enemyRammingCounter + 2);
        } else {
            _enemyRammingCounter = Math.max(-5000, _enemyRammingCounter - 1);
        }

        _enemyIsRammer = (_enemyRammingCounter > 0)?true:false;
    }

    public static boolean enemyIsRammer() {
        return _enemyIsRammer;
    }

    public static double getLastBearing() { return _lastBearing; }
    public static double getLastHeading() { return _lastHeading; }
    public static double getLastDistance() { return _lastDistance; }
    public static double getLastVelocity() { return _lastVelocity; }
    public static double getLastEnergy() { return _lastEnergy; }
    public static long getBulletHits() { return _bulletHits; }
    public static long getBulletHitsThisRound() { return _bulletHitsThisRound; }
    public static long getBulletMissesThisRound() { return _bulletMissesThisRound; }
    public static long getBulletsFired() { return _bulletMisses + _bulletHits; }
    public static long getBulletsFiredThisRound() { return _bulletMissesThisRound + _bulletHitsThisRound; }
    public static double getHitPercentage() { return (((_bulletHits + _bulletMisses) == 0)?0:((double)_bulletHits / ((_bulletHits + _bulletMisses)) * 100)); }
    public static double getHitPercentageThisRound() { return (((_bulletHitsThisRound + _bulletMissesThisRound) == 0)?0:(((double)_bulletHitsThisRound / (_bulletHitsThisRound + _bulletMissesThisRound)) * 100)); }
    public static long getEnemyBulletHits() { return _enemyBulletHits; }
    public static long getEnemyBulletHitsThisRound() { return _enemyBulletHitsThisRound; }
    public static long getEnemyBulletsFired() { return _enemyBulletsFired; }
    public static long getEnemyBulletsFiredThisRound() { return _enemyBulletsFiredThisRound; }
    public static double getEnemyHitPercentage() { return ((_enemyBulletsFired == 0)?0:(((double)_enemyBulletHits / _enemyBulletsFired) * 100.0)); }
    public static double getEnemyHitPercentageThisRound() { return ((_enemyBulletsFiredThisRound == 0)?0:(((double)_enemyBulletHitsThisRound / _enemyBulletsFiredThisRound) * 100)); }
    public static double getBulletDamageGiven() { return _bulletDamage; }
    public static double getBulletDamageTaken() { return _enemyBulletDamage; }
    public static double getLastEnemyBulletPower() { return _enemyPower; } // enemy firepower
    public static double avgEnemyPower() { return ((_enemyBulletsFired == 0)?0:(_enemyPowerTotal / _enemyBulletsFired)); }
    public static long getLastFireOffset() { return _lastFireOffset; }
    public static BotScan getLastScan() { return (BotScan)scanLog.get(0); }
    public static BotScan getLastEnemyScan() { return (BotScan)moveLog.get(0); }
    public static Point2D.Double myLocation() { return _myLocation; }
    public static Point2D.Double enemyLocation() { return _enemyLocation; }
    public static double enemyX() { return ((BotScan)scanLog.get(0)).getLocation().x; }
    public static double enemyY() { return ((BotScan)scanLog.get(0)).getLocation().y; }
    public static BotScan getScan(int ticksAgo) { return (BotScan)scanLog.get(ticksAgo); }
    public static BotScan getEnemyScan(int ticksAgo) { return (BotScan)moveLog.get(ticksAgo); }
    public static Point2D.Double getLocation(int ticksAgo) { return ((BotScan)moveLog.get(ticksAgo)).getLocation(); }
    public static Point2D.Double getEnemyLocation(int ticksAgo) { return ((BotScan)scanLog.get(ticksAgo)).getLocation(); }

}
