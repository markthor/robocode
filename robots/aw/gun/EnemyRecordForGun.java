/*
 * Copyright (c) 2011 Andrew Wells. All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or without modification, are
 *	permitted provided that the following conditions are met:
 *
 *		1. Redistributions of source code must retain the above copyright notice, this list of
 *     conditions and the following disclaimer.
 *
 *		2. Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 *	THIS SOFTWARE IS PROVIDED BY Andrew Wells ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *	WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *	FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Andrew Wells OR
 *	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *	CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *	SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *	ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *	ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * 	The views and conclusions contained in the software and documentation are those of the
 *	authors and should not be interpreted as representing official policies, either expressed
 *	or implied, of Andrew Wells.
 */

package aw.gun;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import aw.utils.*;
import aw.waves.GunDataWave;
import aw.waves.MovementDataWave;
import aw.waves.PreciseIntersectionWave;
import aw.waves.Wave;
import aw.waves.DataWavePassedRecord;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.control.events.RoundEndedEvent;
import robocode.util.Utils;

public class EnemyRecordForGun {

	public static boolean _isTC = false;
	public static boolean DEBUGGING = false;

	private final static int sizeOfEnemyMoveLog = 200;

	AdvancedRobot ourRobot;
	private static Rectangle2D battleField = new Rectangle(18, 18, 764, 564);
	private static WaveGunClassifier VirtualGunsClassifier = new VirtualGunsClassifier();

	private boolean _is1v1;

	private GunDataWave latestGDWave;
	private long timeLastRealWave;
	private ArrayList<GunDataWave> listOfWaves = new ArrayList<GunDataWave>();
	private ArrayList<RobotEPIFState> logEPIF = new ArrayList<RobotEPIFState>();
	private Point2D.Double[] logOfEnemyPositions = new Point2D.Double[sizeOfEnemyMoveLog];
	private int[] logOfEnemyAccel = new int[sizeOfEnemyMoveLog];
	private int[] _LogOfEnemyDir = new int[sizeOfEnemyMoveLog];
	private double[] _LogOfEnemyRelativeHeadings = new double[sizeOfEnemyMoveLog];
	private int indexEnemyMovementLogs = 0;
	private double enemyEnergy;
	private double enemyBearing;
	private double enemyAbsBearing;
	private double enemyHeading;
	private double practicalEnemyHeading;
	private double enemyDistance;
	private double enemyVelocity;
	private double oldEnemyBearing;
	private double oldEnemyHeading;
	private double oldPracticalEnemyHeading;
	private double oldEnemyDistance;
	private double oldEnemyVelocity;
	private double enemyX;
	private double enemyY;
	private double preciseDistanceToEnemy;
	private Point2D.Double enemyCoordinates;
	private Point2D.Double enemyGFOnePoint;
	private Point2D.Double enemyGFNegOnePoint;
	private double preciseGFOneBearing;
	private double preciseGFNegOneBearing;
	private double lastBulletPower;
	private int ticksSinceDirChange = 0;
	private int ticksSinceDecel = 0;
	private int ticksSinceVelocityChange = 0;
	private int orbitDir = 1;
	private int lastDir = 1;
	private int accel = 0;

	private int ourHits = 0;
	private int ourShots = 0;
	
	public EnemyRecordForGun(AdvancedRobot us, boolean is1v1) {
		ourRobot = us;
		this._is1v1 = is1v1;
	}

	public void initRound() {
		listOfWaves.clear();
		ticksSinceDirChange = 0;
		ticksSinceDecel = 0;
		ticksSinceVelocityChange = 0;
		orbitDir = 1;
		lastDir = 1;
		accel = 0;
		indexEnemyMovementLogs = 0;
		enemyEnergy = 0.0;
		enemyBearing = 0.0;
		enemyAbsBearing = 0.0;
		enemyHeading = 0.0;
		practicalEnemyHeading = 0.0;
		enemyDistance = 0.0;
		enemyVelocity = 0.0;
		oldEnemyBearing = 0.0;
		oldEnemyHeading = 0.0;
		oldPracticalEnemyHeading = 0.0;
		oldEnemyDistance = 0.0;
		oldEnemyVelocity = 0.0;
		lastBulletPower = 1.95;
		timeLastRealWave = -1;
	}

	public void onScannedRobot(ScannedRobotEvent e,
			Point2D.Double ourPositionNextTurn,
			ArrayList<RobotState> ourMovementPath, int indexEndAccuartePath,
			long ticksSinceFire, long AbsTime, double nextBulletPower) {
		oldEnemyBearing = enemyBearing;
		oldEnemyHeading = enemyHeading;
		oldPracticalEnemyHeading = practicalEnemyHeading;
		oldEnemyDistance = enemyDistance;
		oldEnemyVelocity = enemyVelocity;
		enemyEnergy = e.getEnergy();
		enemyBearing = e.getBearingRadians();
		enemyAbsBearing = enemyBearing + ourRobot.getHeadingRadians();
		enemyHeading = e.getHeadingRadians();
		practicalEnemyHeading = enemyVelocity >= 0 ? enemyHeading
				: enemyHeading + Math.PI;
		enemyDistance = e.getDistance();
		enemyVelocity = e.getVelocity();
		enemyX = ourRobot.getX() + enemyDistance * Math.sin(enemyAbsBearing);
		enemyY = ourRobot.getY() + enemyDistance * Math.cos(enemyAbsBearing);
		enemyCoordinates = new Point2D.Double(enemyX, enemyY);
		lastDir = orbitDir;
		double relativeHeading = Utils
				.normalRelativeAngle(practicalEnemyHeading - enemyAbsBearing);
		if (enemyVelocity != 0) {
			orbitDir = Math.sin(relativeHeading) < 0 ? -1 : 1;
		}
		ticksSinceDirChange++;
		ticksSinceVelocityChange++;
		ticksSinceDecel++;
		if (orbitDir != lastDir)
			ticksSinceDirChange = 0;
		if (Math.abs(oldEnemyVelocity) > Math.abs(enemyVelocity))
			ticksSinceDecel = 0;
		if (oldEnemyVelocity != enemyVelocity) {
			ticksSinceVelocityChange = 0;
			if (Math.abs(oldEnemyVelocity) < Math.abs(enemyVelocity))
				accel = -1;
			else
				accel = 1;
		} else
			accel = 0;
		
		RobotEPIFState EPIFState = new RobotEPIFState(enemyCoordinates, Math.abs(enemyVelocity), practicalEnemyHeading, Math.abs(relativeHeading), ourRobot.getTime());
		logEPIF.add(EPIFState);

		// TODO check this
		// This is calculated for the next turn since that is when we would
		// usually fire.

		// This should never be zero! The virtuality is set retroactively when
		// we fire.
		double virtuality;
		virtuality = (ticksSinceFire + 1) * ourRobot.getGunCoolingRate() / 3.0;
		double gunHeat = Math.max(0.0,
				ourRobot.getGunHeat() - ourRobot.getGunCoolingRate());
		if (gunHeat < (ticksSinceFire + 1) * ourRobot.getGunCoolingRate()) {
			if (ourRobot.getEnergy() > 0.11 || _isTC
					&& ourRobot.getEnergy() != 0.0) {
				// System.out.print("Using GunHeat   " + ourRobot.getGunHeat() +
				// "   " + (ticksSinceFire + 1) + "   ");
				virtuality = Math.max(gunHeat, ourRobot.getGunCoolingRate()) / 3.0;
				lastBulletPower = nextBulletPower;
			}
		}
		double bulletPower = lastBulletPower;

		double preciseDistanceToEnemy = Point2D.distance(ourPositionNextTurn.x,
				ourPositionNextTurn.y, enemyCoordinates.x, enemyCoordinates.y);
		double RelHeading = Math.abs(relativeHeading);

		double bulletTravelTime = preciseDistanceToEnemy
				/ (20 - 3.0 * bulletPower);

		double VChange = (Math.abs(enemyVelocity) - Math.abs(oldEnemyVelocity));
		double GFZeroAngle = RoboGeom.getBearing(ourPositionNextTurn,
				enemyCoordinates);

		double GFOneAngle;
		double GFNegOneAngle;

		double AheadWallSeverity;
		double ReverseWallSeverity;
		double AheadWallDist;
		double ReverseWallDist;

		double orbitingCollisionFactor;
		// if (ourMovementPath != null) {
		// int currentIndexOfOurMovementPath = 0;
		// for (currentIndexOfOurMovementPath = 0; currentIndexOfOurMovementPath
		// < ourMovementPath
		// .size(); currentIndexOfOurMovementPath++) {
		// if (ourPositionNextTurn.distance(ourMovementPath.get(
		// currentIndexOfOurMovementPath).getCoordinates()) < 0.1)
		// break;
		// }
		//
		// double ourRelativeHeading = Utils.normalRelativeAngle(RoboGeom
		// .getBearing(ourPositionNextTurn,
		// ourMovementPath.get(indexEndAccuartePath)
		// .getCoordinates())
		// - RoboGeom
		// .getBearing(enemyCoordinates, ourPositionNextTurn));
		//
		// orbitingCollisionFactor = 1.0
		// + Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
		// enemyCoordinates, ourPositionNextTurn)
		// - RoboGeom.getBearing(
		// enemyCoordinates,
		// ourMovementPath.get(indexEndAccuartePath)
		// .getCoordinates()))
		// / (Math.atan(8.0 * (indexEndAccuartePath -
		// currentIndexOfOurMovementPath)
		// / enemyDistance)))
		// * (orbitDir * (Math.sin(ourRelativeHeading) < 0 ? -1 : 1));
		//
		// }

		// If we don't do this here, we need to do it on some waves that have
		// the
		// predicted bullet power.
		if (true/* virtuality > 0.667 * ourRobot.getGunCoolingRate() */) {
			GFOneAngle = 0;
			GFNegOneAngle = 0;
			AheadWallSeverity = 0;
			ReverseWallSeverity = 0;
			AheadWallDist = 0;
			ReverseWallDist = 0;
			orbitingCollisionFactor = 0;
		} else {
			GFOneAngle = (preciseMaxEscapeAngle(ourPositionNextTurn,
					20 - 3 * bulletPower, orbitDir));
			GFNegOneAngle = (preciseMaxEscapeAngle(ourPositionNextTurn,
					20 - 3 * bulletPower, -orbitDir));

			preciseGFOneBearing = GFOneAngle;
			preciseGFNegOneBearing = GFNegOneAngle;

			AheadWallSeverity = wallSeverity(orbitDir, bulletTravelTime,
					bulletPower, ourPositionNextTurn);
			ReverseWallSeverity = wallSeverity(-orbitDir, bulletTravelTime,
					bulletPower, ourPositionNextTurn);
			AheadWallDist = wallDistance(ourPositionNextTurn,
					preciseDistanceToEnemy, orbitDir);
			ReverseWallDist = wallDistance(ourPositionNextTurn,
					preciseDistanceToEnemy, -orbitDir);

			orbitingCollisionFactor = 1.0;
			// if (ourMovementPath != null) {
			// int currentIndexOfOurMovementPath = 0;
			// for (currentIndexOfOurMovementPath = 0;
			// currentIndexOfOurMovementPath
			// < ourMovementPath
			// .size(); currentIndexOfOurMovementPath++) {
			// if (ourPositionNextTurn.distance(ourMovementPath.get(
			// currentIndexOfOurMovementPath).getCoordinates()) < 0.1)
			// break;
			// }
			//
			// double ourRelativeHeading = Utils.normalRelativeAngle(RoboGeom
			// .getBearing(ourPositionNextTurn,
			// ourMovementPath.get(indexEndAccuartePath)
			// .getCoordinates())
			// - RoboGeom
			// .getBearing(enemyCoordinates, ourPositionNextTurn));
			//
			// orbitingCollisionFactor = 1.0
			// + Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
			// enemyCoordinates, ourPositionNextTurn)
			// - RoboGeom.getBearing(
			// enemyCoordinates,
			// ourMovementPath.get(indexEndAccuartePath)
			// .getCoordinates()))
			// / (Math.atan(8.0 * (indexEndAccuartePath -
			// currentIndexOfOurMovementPath)
			// / enemyDistance)))
			// * (orbitDir * (Math.sin(ourRelativeHeading) < 0 ? -1 : 1));
			//
			// }
		}

		GunDataWave GDWave = new GunDataWave(e.getTime() + 1,
				ourPositionNextTurn, enemyCoordinates, bulletPower,
				bulletTravelTime, e.getHeadingRadians(), RelHeading,
				e.getVelocity(), AheadWallSeverity, ReverseWallSeverity,
				AheadWallDist, ReverseWallDist, VChange, GFZeroAngle,
				GFOneAngle, GFNegOneAngle, ticksSinceDirChange,
				ticksSinceVelocityChange, AbsTime, virtuality,
				orbitingCollisionFactor, orbitDir);
		if (virtuality < 0.54) {
			listOfWaves.add(GDWave);
			latestGDWave = GDWave;
		}
		manageWaves();
	}

	/**
	 * Always call onScannedRobot first!
	 * 
	 * @return The absolute robocode Angle to fire at
	 */
	public double getFiringAngle1v1() {
		return VirtualGunsClassifier.aim(latestGDWave);
	}

	public Point2D.Double getEnemyCoordinates() {
		return enemyCoordinates;
	}

	// public void record(GunDataWave wave, gunWavePassedRecord rec) {
	//
	// }

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		if (_is1v1) {
			long time = ourRobot.getTime();
			for (int i = 0; i < listOfWaves.size(); i++) {
				GunDataWave currentWave = listOfWaves.get(i);

				if (currentWave.getVirtuality() == 0.0
						&& currentWave.getBulletPower() == e.getBullet()
								.getPower()
						&& Math.abs((time - currentWave.getFireTime())
								* currentWave.getBulletVelocity()
								- currentWave.getSourcePosition().distance(
										e.getBullet().getX(),
										e.getBullet().getY())) < 50.0) {
					Point2D.Double coordinates = new Point2D.Double(e
							.getBullet().getX(), e.getBullet().getY());
					DataWavePassedRecord GWPRec = new DataWavePassedRecord(
							currentWave, coordinates);
					VirtualGunsClassifier.trainEnemyBulletDetection(
							currentWave, GWPRec);
					break;
				}
			}
		}
	}

	public void onBulletHit(BulletHitEvent e) {
		if (_is1v1) {
			ourHits++;
			long time = ourRobot.getTime();
			for (int i = 0; i < listOfWaves.size(); i++) {
				GunDataWave currentWave = listOfWaves.get(i);

				if (currentWave.getVirtuality() == 0.0
						&& currentWave.getBulletPower() == e.getBullet()
								.getPower()
						&& Math.abs((time - currentWave.getFireTime())
								* currentWave.getBulletVelocity()
								- currentWave.getSourcePosition().distance(
										e.getBullet().getX(),
										e.getBullet().getY())) < 50.0) {
					Point2D.Double coordinates = new Point2D.Double(e
							.getBullet().getX(), e.getBullet().getY());
					DataWavePassedRecord GWPRec = new DataWavePassedRecord(
							currentWave, coordinates);
					VirtualGunsClassifier.trainEnemyBulletDetection(
							currentWave, GWPRec);
					break;
				}
			}
		}
	}

	public void onRoundEnded() {
		System.out.println("OUR ACTUAL HIT RATE = " + (100.0 * ourHits)/ourShots + "%");
		VirtualGunsClassifier VGC = (VirtualGunsClassifier) VirtualGunsClassifier;
		VGC.listClassifierScores();
	}

	public void onBulletFired(double power, long time) {
		ourShots++;
		int nextRealWave = -1;
		int indexLastRealWave = -1;
		for (int i = 0; i < listOfWaves.size(); i++) {
			GunDataWave currentWave = listOfWaves.get(i);
			if (currentWave.getFireTime() == time) {
				currentWave.setBulletPower(power);
				currentWave.setVirtuality(0.0);
				nextRealWave = i;
			} else if(currentWave.getFireTime() == timeLastRealWave) {
				indexLastRealWave = i;
			}
		}
		if (nextRealWave != -1) {

//			for waves between two real waves, use a weighted average.
			if (indexLastRealWave != -1) {

				for (int i = indexLastRealWave + 1; i < nextRealWave; i++) {
					listOfWaves
							.get(i)
							.setBulletPower(
									((listOfWaves.get(indexLastRealWave)
											.getBulletPower() * Math.abs(nextRealWave - i)) + listOfWaves
											.get(nextRealWave).getBulletPower() * Math.abs(indexLastRealWave - i))
											/ (Math.abs(nextRealWave - i) + Math.abs(indexLastRealWave - i)));
//					TODO check this
					listOfWaves
					.get(i)
					.setVirtuality(Math.min(Math.abs(nextRealWave - i), Math.abs(indexLastRealWave - i)) * ourRobot.getGunCoolingRate() / 3.0);
				}
			} else {
				// virtual waves fired before the first real wave.
				for (int i = 0; i < nextRealWave; i++) {
					listOfWaves.get(i).setBulletPower(power);
				}
			}
			timeLastRealWave = time;
		}
	}

	private void manageWaves() {
		if (_is1v1) {
			long time = ourRobot.getTime();
			for (int i = 0; i < listOfWaves.size(); i++) {
				GunDataWave currentWave = listOfWaves.get(i);
//				currentWave.updateWave(enemyCoordinates, time);
				if (currentWave.getWavePassed(enemyCoordinates, time)
						&& currentWave.getBulletPowerSet()) {
//					there may be a better place for this
					currentWave.recalcAttributes();

					int index = (int) (logEPIF.size()-1-(time - currentWave.getFireTime()));
//					if(logEPIF.get(index).getTime() != currentWave.getFireTime())
//						System.out.println("Error TOfEPIF = " + logEPIF.get(index).getTime() + " TWave = " + currentWave.getFireTime());
					for(long t = currentWave.getFireTime(); t <= time; t++, index++) {
						currentWave.updateWave(logEPIF.get(index).getCoordinates(), t);
					}
					
					DataWavePassedRecord GWPRec = new DataWavePassedRecord(
							currentWave, enemyCoordinates);
					VirtualGunsClassifier.train(currentWave, GWPRec);
					listOfWaves.remove(i);
					i--;
				}
			}
		}
	}

	// NOTE: This must be synchronized with GunDataWave
	private double preciseMaxEscapeAngle(Point2D.Double ourCoordinatesNextTurn,
			double bulletVelocity, int orbitDirection) {

		Point2D.Double predictedPosition;
		boolean GFOnePrediction = true;
		if (orbitDir != orbitDirection)
			GFOnePrediction = false;

		predictedPosition = RoboGeom.precisePositionalMaxEscapeAnglePoint(
				ourCoordinatesNextTurn, enemyCoordinates, bulletVelocity,
				orbitDirection);

		double bearing = RoboGeom.getBearing(ourCoordinatesNextTurn,
				predictedPosition);
		if (GFOnePrediction)
			enemyGFOnePoint = predictedPosition;
		else
			enemyGFNegOnePoint = predictedPosition;

		return bearing;
	}

	// NOTE: This must be synchronized with GunDataWave
	private double wallSeverity(int dir, double actualBulletTravelTime,
			double bulletPower, Point2D.Double robotCoordinates) {
		// if(dir == orbitDir)
		// return (Math.abs(Utils
		// .normalRelativeAngle(preciseGFOneBearing
		// - RoboGeom.getBearing(robotCoordinates,
		// enemyCoordinates))) / RoboGeom.normMaxEscapeAngle(20 - 3 *
		// bulletPower));
		// else
		// return (Math.abs(Utils
		// .normalRelativeAngle(preciseGFNegOneBearing
		// - RoboGeom.getBearing(robotCoordinates,
		// enemyCoordinates))) / RoboGeom.normMaxEscapeAngle(20 - 3 *
		// bulletPower));

		if (dir == orbitDir)
			return (Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
					enemyCoordinates, enemyGFOnePoint)
					- RoboGeom.getBearing(enemyCoordinates, robotCoordinates))) / (Math.PI / 2));
		else
			return (Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
					enemyCoordinates, enemyGFNegOnePoint)
					- RoboGeom.getBearing(enemyCoordinates, robotCoordinates))) / (Math.PI / 2));

	}

	// NOTE: This must be synchronized with GunDataWave
	private double wallDistance(Point2D.Double ourCoordinatesNextTurn,
			double preciseDistToEnemy, int dir) {

		// if(Math.abs(Utils
		// .normalRelativeAngle(RoboGeom.getBearing(enemyCoordinates,
		// enemyGFOnePoint)
		// - RoboGeom.getBearing(enemyCoordinates, ourCoordinatesNextTurn))) ==
		// Math.PI / 2)
		// return Math.PI / 2;
		// else {
		// if(dir == orbitDir) {
		// return Math.min(Math.PI / 2, RoboGeom.wallDistance(
		// enemyGFOnePoint, enemyCoordinates,
		// enemyCoordinates.distance(enemyGFOnePoint),
		// dir))
		// / (Math.PI / 2);
		// }
		// else {
		// return Math.min(Math.PI / 2, RoboGeom.wallDistance(
		// enemyGFNegOnePoint, enemyCoordinates,
		// enemyCoordinates.distance(enemyGFNegOnePoint),
		// dir))
		// / (Math.PI / 2);
		// }
		// }
		return Math.min(Math.PI / 2, RoboGeom.wallDistance(
				ourCoordinatesNextTurn, enemyCoordinates, preciseDistToEnemy,
				dir))
				/ (Math.PI / 2);
	}

	// public void onPaint() {
	// painting = true;
	// }
	//
	// private void paintVector(Graphics2D g, Point2D.Double origin,
	// double length, double bearing, float red, float green, float blue,
	// float brightness) {
	// Point2D.Double endPoint = RoboGeom.project(origin, length,
	// bearing);
	// Point2D.Double arrowPoint1 = RoboGeom.project(endPoint,
	// length / 20, bearing + 3.44159);
	// Point2D.Double arrowPoint2 = RoboGeom.project(endPoint,
	// length / 20, bearing + 2.84159);
	// ;
	// Color color = new Color(red, green, blue, brightness);
	// g.setColor(color);
	// g.drawLine((int) origin.x, (int) origin.y, (int) endPoint.x,
	// (int) endPoint.y);
	// g.drawLine((int) endPoint.x, (int) endPoint.y, (int) arrowPoint1.x,
	// (int) arrowPoint1.y);
	// g.drawLine((int) endPoint.x, (int) endPoint.y, (int) arrowPoint2.x,
	// (int) arrowPoint2.y);
	// }
	//
	// private void paintWave(Graphics2D g, GunDataWave wave) {
	// Point2D.Double origin = wave.source;
	// double radius = (robot.getTime() - wave.startTime) * wave.velocity;
	// g.setColor(Color.gray);
	// g.drawOval((int) (origin.x - radius), (int) (origin.y - radius),
	// (int) (2 * radius), (int) (2 * radius));
	// Point2D.Double GFZeroPoint = RoboGeom.project(origin, radius,
	// wave.getGFZeroAngle());
	// Point2D.Double GFOnePoint = RoboGeom.project(origin, radius,
	// wave.getGFZeroAngle());
	// Point2D.Double GFNegOnePoint = RoboGeom.project(origin, radius,
	// wave.getGFZeroAngle());
	// Color color = new Color(0f, 0.4f, 0f, 0.3f);
	// g.setColor(color);
	// g.drawLine((int) origin.x, (int) origin.y, (int) GFZeroPoint.x,
	// (int) GFZeroPoint.y);
	// g.drawLine((int) origin.x, (int) origin.y, (int) GFOnePoint.x,
	// (int) GFOnePoint.y);
	// g.drawLine((int) origin.x, (int) origin.y, (int) GFNegOnePoint.x,
	// (int) GFNegOnePoint.y);
	// }
	//
	// private void paintBotWidthLine(Graphics2D g, Point2D.Double origin,
	// double length, double bearing, double angle, double distance) {
	// Point2D.Double midPoint = RoboGeom.project(origin, length - 20,
	// bearing);
	// double lengthOfBotWidthLine = (length - 20) * Math.tan(angle);
	// Point2D.Double point1 = RoboGeom.project(midPoint,
	// lengthOfBotWidthLine, bearing + 1.57079632675);
	// Point2D.Double point2 = RoboGeom.project(midPoint,
	// lengthOfBotWidthLine, bearing - 1.57079632675);
	// g.drawLine((int) point1.x, (int) point1.y, (int) point2.x,
	// (int) point2.y);
	// }
	//
	// // private double enemyDistanceTraveledLastNTicks(int n) {
	// // double enemyDistTraveledLastNTicks = 0;
	// // n %= _sizeOfEnemyMoveLog; // n must be smaller than
	// _sizeOfEnemyMoveLog
	// // int i;
	// // if ((i = indexEnemyMovementLogs - n) < 0)
	// // i += _sizeOfEnemyMoveLog;
	// // if (_LogOfEnemyPositions[i] == null) {
	// // i = 0;
	// // enemyDistTraveledLastNTicks = _sizeOfEnemyMoveLog
	// // * Point2D.distance(enemyX, enemyY,
	// // _LogOfEnemyPositions[i].x,
	// // _LogOfEnemyPositions[i].y) / (8 * n);
	// // } else
	// // enemyDistTraveledLastNTicks = Point2D.distance(enemyX, enemyY,
	// // _LogOfEnemyPositions[i].x, _LogOfEnemyPositions[i].y)
	// // / (8 * n);
	// // return enemyDistTraveledLastNTicks;
	// // }
	// //
	// // private double enemyAccelChangesLastNTicks(int n) {
	// // double enemyAccelChangesLastNTicks = 0;
	// // n %= _sizeOfEnemyMoveLog; // n must be smaller than
	// _sizeOfEnemyMoveLog
	// // int i;
	// // if ((i = indexEnemyMovementLogs - n) > 0) {
	// // for (; i < n; i++) {
	// // if (_LogOfEnemyAccel[i] != _LogOfEnemyAccel[i - 1])
	// // enemyAccelChangesLastNTicks++;
	// // }
	// // return enemyAccelChangesLastNTicks / n;
	// // } else {
	// // // loop through to the end of the movement log
	// // for (i += _sizeOfEnemyMoveLog; i < _sizeOfEnemyMoveLog; i++) {
	// // if (_LogOfEnemyPositions[i] == null) {
	// // if (_LogOfEnemyAccel[0] != _LogOfEnemyAccel[i - 1])
	// // enemyAccelChangesLastNTicks++;
	// // break;
	// // }
	// // if (_LogOfEnemyAccel[i] != _LogOfEnemyAccel[i - 1])
	// // enemyAccelChangesLastNTicks++;
	// // }
	// // // loop to the current position
	// // for (i = 1; i < indexEnemyMovementLogs; i++) {
	// // if (_LogOfEnemyAccel[i] != _LogOfEnemyAccel[i - 1])
	// // enemyAccelChangesLastNTicks++;
	// // }
	// // return enemyAccelChangesLastNTicks / n;
	// // }
	// // }
	// //
	// // private double enemyDirChangesLastNTicks(int n) {
	// // double enemyDirChangesLastNTicks = 0;
	// // n %= _sizeOfEnemyMoveLog; // n must be smaller than
	// _sizeOfEnemyMoveLog
	// // int i;
	// // if ((i = indexEnemyMovementLogs - n) > 0) {
	// // for (; i < n; i++) {
	// // if (_LogOfEnemyDir[i] != _LogOfEnemyDir[i - 1])
	// // enemyDirChangesLastNTicks++;
	// // }
	// // return enemyDirChangesLastNTicks / n;
	// // } else {
	// // // loop through to the end of the movement log
	// // for (i += _sizeOfEnemyMoveLog; i < _sizeOfEnemyMoveLog; i++) {
	// // if (_LogOfEnemyPositions[i] == null) {
	// // if (_LogOfEnemyDir[0] != _LogOfEnemyDir[i - 1])
	// // enemyDirChangesLastNTicks++;
	// // break;
	// // }
	// // if (_LogOfEnemyDir[i] != _LogOfEnemyDir[i - 1])
	// // enemyDirChangesLastNTicks++;
	// // }
	// // // loop to the current position
	// // for (i = 1; i < indexEnemyMovementLogs; i++) {
	// // if (_LogOfEnemyDir[i] != _LogOfEnemyDir[i - 1])
	// // enemyDirChangesLastNTicks++;
	// // }
	// // return enemyDirChangesLastNTicks / n;
	// // }
	// // }
	// //
	// // private double enemyAverageTurnLastNTicks(int n) {
	// // double enemyHeadingChangeLastNTicks = 0;
	// // n %= _sizeOfEnemyMoveLog; // n must be smaller than
	// _sizeOfEnemyMoveLog
	// // int i;
	// // if ((i = indexEnemyMovementLogs - n) > 0) {
	// // for (; i < n; i++) {
	// // if (_LogOfEnemyRelativeHeadings[i] != _LogOfEnemyRelativeHeadings[i -
	// 1])
	// // enemyHeadingChangeLastNTicks += _LogOfEnemyRelativeHeadings[i]
	// // - _LogOfEnemyRelativeHeadings[i - 1];
	// // }
	// // return enemyHeadingChangeLastNTicks / (0.174532925199432778 * n);
	// // } else {
	// // // loop through to the end of the movement log
	// // for (i += _sizeOfEnemyMoveLog; i < _sizeOfEnemyMoveLog; i++) {
	// // if (_LogOfEnemyPositions[i] == null) {
	// // if (_LogOfEnemyRelativeHeadings[0] != _LogOfEnemyRelativeHeadings[i -
	// 1])
	// // enemyHeadingChangeLastNTicks += _LogOfEnemyRelativeHeadings[i]
	// // - _LogOfEnemyRelativeHeadings[i - 1];
	// // ;
	// // break;
	// // }
	// // if (_LogOfEnemyRelativeHeadings[i] != _LogOfEnemyRelativeHeadings[i -
	// 1])
	// // enemyHeadingChangeLastNTicks += _LogOfEnemyRelativeHeadings[i]
	// // - _LogOfEnemyRelativeHeadings[i - 1];
	// // ;
	// // }
	// // // loop to the current position
	// // for (i = 1; i < indexEnemyMovementLogs; i++) {
	// // if (_LogOfEnemyRelativeHeadings[i] != _LogOfEnemyRelativeHeadings[i -
	// 1])
	// // enemyHeadingChangeLastNTicks += _LogOfEnemyRelativeHeadings[i]
	// // - _LogOfEnemyRelativeHeadings[i - 1];
	// // ;
	// // }
	// // return enemyHeadingChangeLastNTicks / (0.174532925199432778 * n);
	// // }
	// //
	// // }
	//
	// private double wallDistance(int dir, double actualBulletTravelTime,
	// double bulletPower, Point2D.Double robotCoordinates) {
	//
	// Wave wallDistWave = new Wave(robot.getTime() - 1, robotCoordinates, 20 -
	// 3 * bulletPower);
	//
	// Point2D.Double wallDistPoint = PrecisePredictor.predictPosition(
	// enemyCoordinates.x, enemyCoordinates.y, enemyHeading,
	// enemyVelocity, wallDistWave, robot.getTime(), 160, dir);
	//
	// double shortestPossibleBulletTravelTime = (actualBulletTravelTime *
	// bulletPower)
	// / (20.0 - 3.0 * bulletPower + 8.0);
	// return Math.max(0,
	// (Point2D.distance(enemyCoordinates.x, enemyCoordinates.y,
	// robotCoordinates.x, robotCoordinates.y) - Point2D
	// .distance(wallDistPoint.x, wallDistPoint.y,
	// robotCoordinates.x, robotCoordinates.y)))
	// / ((shortestPossibleBulletTravelTime) * 8.0);
	// //
	// // double ratioPredictedMaxGFOne = (Math.abs(Utils
	// // .normalRelativeAngle(preciseGFOneBearing
	// // - RoboGeom.getBearing(robotCoordinates,
	// // enemyCoordinates))) / normalMaxEscapeAngle(20 - 3 * bulletPower));
	// // double ratioPredictedMaxGFNegOne = (Math.abs(Utils
	// // .normalRelativeAngle(preciseGFNegOneBearing
	// // - RoboGeom.getBearing(robotCoordinates,
	// // enemyCoordinates))) / normalMaxEscapeAngle(20 - 3 * bulletPower));
	// // 0.6287963 is the maximum possible escape angle (in one direction)
	// // double firingAnglePreciseGFOneGFZero = Math.abs(Utils
	// // .normalRelativeAngle(preciseGFOneBearing
	// // - RoboGeom.getBearing(robotCoordinates,
	// // enemyCoordinates)))
	// // / 0.6287963;
	// // double firingAnglePreciseGFNegOneGFZero = Math.abs(Utils
	// // .normalRelativeAngle(preciseGFNegOneBearing
	// // - RoboGeom.getBearing(robotCoordinates,
	// // enemyCoordinates)))
	// // / 0.6287963;
	// //
	// // double moveAnglePreciseGFOneGFZero = Math.abs(Utils
	// // .normalRelativeAngle(RoboGeom.getBearing(
	// // enemyCoordinates, enemyGFOnePoint)
	// // - RoboGeom.getBearing(enemyCoordinates,
	// // robotCoordinates)))
	// // / Math.PI;
	// // double moveAnglePreciseGFNegOneGFZero = Math.abs(Utils
	// // .normalRelativeAngle(RoboGeom.getBearing(
	// // enemyCoordinates, enemyGFNegOnePoint)
	// // - RoboGeom.getBearing(enemyCoordinates,
	// // robotCoordinates)))
	// // / Math.PI;
	//
	// // double bearingFromEnemyToUs = Math.atan2(enemyX - robot.getX(),
	// // enemyY
	// // - robot.getY())
	// // / (2 * Math.PI);
	//
	// // if (dir == 1)
	// // return Point2D.distance(robotCoordinates.x, robotCoordinates.y,
	// // enemyGFOnePoint.x, enemyGFOnePoint.y)
	// // / preciseDistanceToEnemy;
	// // else
	// // return Point2D.distance(robotCoordinates.x, robotCoordinates.y,
	// // enemyGFNegOnePoint.x, enemyGFNegOnePoint.y)
	// // / preciseDistanceToEnemy;
	// //
	// // Point2D.Double normGFOnePoint = RoboGeom.project(
	// // robotCoordinates, enemyDistance, enemyAbsBearing + dir
	// // * normalMaxEscapeAngle(20 - 3 * bulletPower));
	// // Point2D.Double normGFNegOnePoint = RoboGeom.project(
	// // robotCoordinates, enemyDistance, enemyAbsBearing - dir
	// // * normalMaxEscapeAngle(20 - 3 * bulletPower));
	// //
	// // double ratioMoveAnglePreciseNormGFOne = moveAnglePreciseGFOneGFZero
	// // / (Math.abs(Utils.normalRelativeAngle(PrecisePredictor
	// // .getBearing(enemyCoordinates, normGFOnePoint)
	// // - RoboGeom.getBearing(enemyCoordinates,
	// // robotCoordinates))) / Math.PI);
	// // double ratioMoveAnglePreciseNormGFNegOne =
	// // moveAnglePreciseGFNegOneGFZero
	// // / (Math.abs(Utils.normalRelativeAngle(PrecisePredictor
	// // .getBearing(enemyCoordinates, normGFNegOnePoint)
	// // - RoboGeom.getBearing(enemyCoordinates,
	// // robotCoordinates))) / Math.PI);
	//
	// // moveAngle stuff might work nicely in Stat. C-targeting where the
	// // dimension could
	// // represent at what NBO the robot would turn around, but it doesn't
	// // seem to help here.
	// }

	public void onPaint(Graphics2D graphicsObject, long time) {
		VirtualGunsClassifier.onPaint(graphicsObject, time);
		
		if (DEBUGGING) {
			for(GunDataWave wave : listOfWaves) {
				if(wave.getBulletPowerSet())
					graphicsObject.setColor(Color.GREEN);
				else
					graphicsObject.setColor(Color.RED);

				double radius = wave.getBulletVelocity() * (time - wave.getFireTime());
				graphicsObject.drawOval((int) (wave.getSourcePosition().x-radius), (int) (wave.getSourcePosition().y-radius),  2 * (int) radius, 2 * (int) radius);
			}
			
			
			Point2D.Double ourCoordinates = new Point2D.Double(ourRobot.getX(),
					ourRobot.getY());
			Point2D.Double point = enemyGFOnePoint;
			graphicsObject.setColor(Color.GREEN);
			graphicsObject.drawLine((int) enemyCoordinates.x,
					(int) enemyCoordinates.y, (int) point.x, (int) point.y);
			point = enemyGFNegOnePoint;
			graphicsObject.setColor(Color.red);
			graphicsObject.drawLine((int) enemyCoordinates.x,
					(int) enemyCoordinates.y, (int) point.x, (int) point.y);

			Point2D.Double wallDistPoint = RoboGeom.project(
					ourCoordinates,
					enemyDistance,
					enemyAbsBearing
							+ orbitDir
							* Math.PI
							/ 2
							* wallDistance(ourCoordinates, enemyDistance,
									orbitDir));
			graphicsObject.setColor(Color.cyan);
			graphicsObject.drawLine((int) ourCoordinates.x,
					(int) ourCoordinates.y, (int) wallDistPoint.x,
					(int) wallDistPoint.y);

			wallDistPoint = RoboGeom.project(
					ourCoordinates,
					enemyDistance,
					enemyAbsBearing
							- orbitDir
							* Math.PI
							/ 2
							* wallDistance(ourCoordinates, enemyDistance,
									-orbitDir));
			graphicsObject.setColor(Color.magenta);
			graphicsObject.drawLine((int) ourCoordinates.x,
					(int) ourCoordinates.y, (int) wallDistPoint.x,
					(int) wallDistPoint.y);

			// Point2D.Double wallDistPoint = RoboGeom.project(enemyGFOnePoint,
			// enemyGFOnePoint.distance(enemyCoordinates),
			// RoboGeom.getBearing(enemyGFOnePoint, enemyCoordinates) + orbitDir
			// *
			// Math.PI / 2 * wallDistance(ourCoordinates, enemyDistance,
			// orbitDir));
			// graphicsObject.setColor(Color.cyan);
			// graphicsObject.drawLine((int) enemyGFOnePoint.x,
			// (int) enemyGFOnePoint.y, (int) wallDistPoint.x, (int)
			// wallDistPoint.y);
			//
			// wallDistPoint = RoboGeom.project(enemyGFNegOnePoint,
			// enemyGFNegOnePoint.distance(enemyCoordinates),
			// RoboGeom.getBearing(enemyGFNegOnePoint, enemyCoordinates) -
			// orbitDir
			// * Math.PI / 2 * wallDistance(ourCoordinates, enemyDistance,
			// -orbitDir));
			// graphicsObject.setColor(Color.magenta);
			// graphicsObject.drawLine((int) enemyGFNegOnePoint.x,
			// (int) enemyGFNegOnePoint.y, (int) wallDistPoint.x, (int)
			// wallDistPoint.y);
		}
	}
}
