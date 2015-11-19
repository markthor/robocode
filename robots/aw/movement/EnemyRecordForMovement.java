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

package aw.movement;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.lang.Double;
//import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import aw.Mallorn.tree.*;
//import aw.Mallorn.tree.KDTreeBucket;
//import aw.gun.VirtualGunsClassifier;
import aw.utils.*;
import aw.waves.EffectiveBulletShadow;
//import aw.waves.KNNGFMovementWave;
import aw.waves.MovementDataWave;
import aw.waves.PreciseIntersectionWave;
import aw.waves.Wave;
import aw.waves.DataWavePassedRecord;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
//import robocode.control.events.RoundEndedEvent;
import robocode.util.Utils;
import robocode.Bullet;

public class EnemyRecordForMovement {

	public static boolean debugging = false;
	public static boolean _isMC = false;
	private long waveDangerTime = 0;
	private long onScannedRobotTime = 0;
	private long waveManagementTime = 0;

	private final static int _sizeLongLog = 24;

	AdvancedRobot ourRobot;
	private static VirtualMovementsClassifier VirtualMovementsClassifier = new VirtualMovementsClassifier();

	private ArrayList<PreciseIntersectionWave> _ListOfPreciseWaves = new ArrayList<PreciseIntersectionWave>();

	private ArrayList<Bullet> ourBulletsInAir = new ArrayList<Bullet>();
	private ArrayList<Long> fireTimesOurBulletsInAir = new ArrayList<Long>();

	private static KDTree<Double> enemyBulletPowersTree = new KDTree<Double>(3);

	private boolean _is1v1;

	private long lastMoveRecalcTime = -1;
	// private boolean useNonWaveSurfing = false;
	private MovementDataWave latestMDWave;
	private ArrayList<MovementDataWave> listOfWaves = new ArrayList<MovementDataWave>();
	private ArrayList<MovementDataWave> listOfVirtualWaves = new ArrayList<MovementDataWave>();
	private int indexMovementLogs = 0;
	private int indexLongLog = 0;
	private Point2D.Double[] enemyPreviousPositions = new Point2D.Double[2];
	private Point2D.Double[] ourPreviousPositions = new Point2D.Double[2];
	private Point2D.Double[] longLogPreviousPositions = new Point2D.Double[_sizeLongLog];
	private int[] ourOrbitDirections = new int[2];
	private double[] ourVelocities = new double[2];
	private double[] ourHeadings = new double[2];
	private double[] ourVChange = new double[2];
	private int[] ourPreviousTicksSinceVelocityChange = new int[2];
	private int[] ourPreviousTicksSinceDirChange = new int[2];
	private double preciseDistanceToEnemy;
	private Point2D.Double enemyCoordinates;
	private Point2D.Double ourGFOnePoint;
	private Point2D.Double ourGFNegOnePoint;
	private Point2D.Double ourCoordinates;
	private int ticksSinceDirChange = 0;
	private int ticksSinceDecel = 0;
	private int ticksSinceVelocityChange = 0;
	private int orbitDir = 1;
	private int lastDir = 1;
	private int VChangeInt = 0;
	private double desiredDistance = 650;
	private double enemyEnergy;
	private double enemyGunHeat;// TODO track gun heat.
	private double enemyPracticalHeading;
	private boolean enemyShootsAtGunHeatZero = true;

	private double heading = 0;
	private double velocity = 0;
	private double VChange = 0;
	private double ticksSinceFire = 31;
	private double lastBulletPower;

	private double numberOfBulletHitBulletEvents = 0.0;
	private int numberOfBulletHitBulletEventsThisRound = 0;
	private int numberOfBulletsThatHitUsThisRound = 0;
	private int numberOfOurBulletsThatHitThisRound = 0;

	private double enemyHits = 0.0;
	private double enemyMissPenalties = 0.0;
	private double enemyRawMissPenalties = 0.0;
	private double numOfShotsLogged = 0.0;

	public EnemyRecordForMovement(AdvancedRobot us, boolean is1v1) {
		ourRobot = us;
		this._is1v1 = is1v1;

	}

	public void initRound() {
		listOfWaves.clear();
		listOfVirtualWaves.clear();
		_ListOfPreciseWaves.clear();
		ourBulletsInAir.clear();
		fireTimesOurBulletsInAir.clear();
		lastMoveRecalcTime = -1;
		latestMDWave = null;
		indexMovementLogs = 0;
		indexLongLog = 0;
		longLogPreviousPositions = new Point2D.Double[_sizeLongLog];
		ticksSinceFire = 31;
		enemyGunHeat = 3.0;
		velocity = 0;
		VChange = 0;
		ticksSinceDirChange = 0;
		orbitDir = 1;
		lastDir = 1;
		enemyPracticalHeading = 0.0;
		lastBulletPower = 2.0;
		numberOfBulletHitBulletEventsThisRound = 0;
		numberOfBulletsThatHitUsThisRound = 0;
		numberOfOurBulletsThatHitThisRound = 0;
		waveDangerTime = 0;
		onScannedRobotTime = 0;
		waveManagementTime = 0;
	}

	public void onScannedRobot(ScannedRobotEvent e, double ourVelocity,
			double ourHeading, double ourEnergy, Point2D.Double ourCoordinates,
			long currentTime, long absTime) {
		long startOnScannedTime = System.nanoTime();

		ticksSinceFire++;

		enemyGunHeat -= ourRobot.getGunCoolingRate();
		
//		TODO get gun heat waves working

//		if (enemyGunHeat < -0.001 - 2.0 * ourRobot.getGunCoolingRate())
//			enemyShootsAtGunHeatZero = false;

		ourOrbitDirections[indexMovementLogs] = orbitDir;
		ourVelocities[indexMovementLogs] = velocity;
		ourVChange[indexMovementLogs] = VChange;
		ourHeadings[indexMovementLogs] = heading;
		enemyPreviousPositions[indexMovementLogs] = enemyCoordinates;
		ourPreviousPositions[indexMovementLogs] = ourCoordinates;
		ourPreviousTicksSinceVelocityChange[indexMovementLogs] = ticksSinceVelocityChange;
		ourPreviousTicksSinceDirChange[indexMovementLogs] = ticksSinceDirChange;
		if (indexMovementLogs == 0)
			indexMovementLogs = 1;
		else
			indexMovementLogs = 0;

		longLogPreviousPositions[indexLongLog] = ourCoordinates;
		if (++indexLongLog >= _sizeLongLog)
			indexLongLog = 0;

		ticksSinceVelocityChange++;
		if (velocity != ourVelocity)
			ticksSinceVelocityChange = 0;
		ticksSinceDirChange++;
		if (lastDir != orbitDir)
			ticksSinceDirChange = 0;

		VChange = Math.abs(ourVelocity) - Math.abs(velocity);
		velocity = ourVelocity;
		heading = ourHeading;
		this.ourCoordinates = ourCoordinates;

		if (e != null) {
			enemyPracticalHeading = e.getHeadingRadians();
			if (e.getVelocity() < 0)
				enemyPracticalHeading = Utils
						.normalAbsoluteAngle(enemyPracticalHeading + Math.PI);

			lastDir = orbitDir;
			if (ourVelocity != 0) {
				if (Math.sin(e.getBearingRadians()) * ourVelocity > 0)
					orbitDir = 1;
				else
					orbitDir = -1;
			}
			// TODO track gunheat to check this with too. is the > 0.099 a good
			// idea?
			if (enemyEnergy - e.getEnergy() > 0.099
					&& enemyEnergy - e.getEnergy() < 3.01) {

				ticksSinceFire = 0;
				enemyGunHeat = Rules.getGunHeat(enemyEnergy - e.getEnergy())
						- ourRobot.getGunCoolingRate();

				lastBulletPower = enemyEnergy - e.getEnergy();
				double bulletVelocity = 20.0 - 3.0 * (enemyEnergy - e
						.getEnergy());
				double bulletTravelTime = (Point2D.distance(
						ourPreviousPositions[indexMovementLogs].x,
						ourPreviousPositions[indexMovementLogs].y,
						enemyPreviousPositions[indexMovementLogs].x,
						enemyPreviousPositions[indexMovementLogs].y) / bulletVelocity);
				double relativeHeading = Utils
						.normalRelativeAngle(ourHeadings[indexMovementLogs]
								- RoboGeom
										.getBearing(
												enemyCoordinates,
												ourPreviousPositions[indexMovementLogs]));
				double RelHeading = Math.abs(relativeHeading);

				double GFZeroAngle = RoboGeom.getBearing(enemyCoordinates,
						ourPreviousPositions[indexMovementLogs]);
				double GFOneAngle = GFZeroAngle
						+ ourOrbitDirections[indexMovementLogs]
						* RoboGeom.normMaxEscapeAngle(bulletVelocity);
				double GFNegOneAngle = GFZeroAngle
						- ourOrbitDirections[indexMovementLogs]
						* RoboGeom.normMaxEscapeAngle(bulletVelocity);
				double preciseGFOneAngle = preciseMaxEscapeAngle(
						bulletVelocity, ourOrbitDirections[indexMovementLogs]);
				double preciseGFNegOneAngle = preciseMaxEscapeAngle(
						bulletVelocity, -ourOrbitDirections[indexMovementLogs]);
				double AheadWallSeverity = wallSeverity(
						ourOrbitDirections[indexMovementLogs],
						bulletTravelTime, (enemyEnergy - e.getEnergy()),
						ourPreviousPositions[indexMovementLogs],
						enemyPreviousPositions[indexMovementLogs]);
				double ReverseWallSeverity = wallSeverity(
						-ourOrbitDirections[indexMovementLogs],
						bulletTravelTime, (enemyEnergy - e.getEnergy()),
						ourPreviousPositions[indexMovementLogs],
						enemyPreviousPositions[indexMovementLogs]);
				double AheadWallDist = wallDistance(enemyCoordinates,
						ourOrbitDirections[indexMovementLogs]);
				double ReverseWallDist = wallDistance(enemyCoordinates,
						-ourOrbitDirections[indexMovementLogs]);
				double virtuality = Math.min(
						ticksSinceFire * ourRobot.getGunCoolingRate(),
						enemyGunHeat) * 0.33333333333;

				double orbitingCollisionFactor = 0.0;

				MovementDataWave addWave = new MovementDataWave(
						currentTime - 1, enemyCoordinates,
						ourPreviousPositions[indexMovementLogs], enemyEnergy
								- e.getEnergy(), bulletTravelTime,
						ourHeadings[indexMovementLogs], RelHeading,
						ourVelocities[indexMovementLogs], AheadWallSeverity,
						ReverseWallSeverity, AheadWallDist, ReverseWallDist,
						ourVChange[indexMovementLogs], GFZeroAngle, GFOneAngle,
						GFNegOneAngle, preciseGFOneAngle, preciseGFNegOneAngle,
						ticksSinceDirChange, ticksSinceVelocityChange,
						absTime - 1, virtuality, ourDistLastNTicks(5),
						ourDistLastNTicks(10), ourDistLastNTicks(20),
						orbitingCollisionFactor,
						ourOrbitDirections[indexMovementLogs]);

				long startTime = System.nanoTime();
				addWave.setAnglesWeightsAndBandwidths(VirtualMovementsClassifier
						.getRawAnglesWeightsAndBandwidths(addWave, enemyHits,
								enemyMissPenalties, enemyRawMissPenalties,
								numOfShotsLogged, numberOfBulletHitBulletEvents));
				waveDangerTime += System.nanoTime() - startTime;

				PreciseIntersectionWave preciseAddWave = new PreciseIntersectionWave(
						currentTime - 1, enemyCoordinates,
						20.0 - 3.0 * (enemyEnergy - e.getEnergy()),
						RoboGeom.getBearing(enemyCoordinates,
								ourPreviousPositions[indexMovementLogs]));

				_ListOfPreciseWaves.add(preciseAddWave);

				// TODO sometimes we might need to surf these waves!!!
				listOfWaves.add(addWave);
				addShadowsBulletsInAir(addWave, currentTime);
				int otherWaves = 0;
				for (MovementDataWave wave : listOfWaves) {
					if (wave.surfingThisWave())
						otherWaves++;
				}

				if (otherWaves < 2)
					lastMoveRecalcTime = currentTime;
				listOfVirtualWaves.add(addWave);

				double[] bulletPowersData = new double[3];
				bulletPowersData[0] = Point2D.distance(
						ourPreviousPositions[indexMovementLogs].x,
						ourPreviousPositions[indexMovementLogs].y,
						enemyPreviousPositions[indexMovementLogs].x,
						enemyPreviousPositions[indexMovementLogs].y);
				bulletPowersData[1] = enemyEnergy;
				bulletPowersData[2] = ourEnergy;

				enemyBulletPowersTree.addPoint(bulletPowersData, enemyEnergy
						- e.getEnergy());
			} else if (enemyPreviousPositions[indexMovementLogs] != null) {

				double bulletPower = lastBulletPower;
				double bulletVelocity = 20.0 - 3.0 * (bulletPower);
				double bulletTravelTime = (Point2D.distance(
						ourPreviousPositions[indexMovementLogs].x,
						ourPreviousPositions[indexMovementLogs].y,
						enemyPreviousPositions[indexMovementLogs].x,
						enemyPreviousPositions[indexMovementLogs].y) / bulletVelocity);
				double relativeHeading = Utils
						.normalRelativeAngle(ourHeadings[indexMovementLogs]
								- RoboGeom
										.getBearing(
												enemyCoordinates,
												ourPreviousPositions[indexMovementLogs]));
				double RelHeading = Math.abs(relativeHeading);

				double GFZeroAngle = RoboGeom.getBearing(enemyCoordinates,
						ourPreviousPositions[indexMovementLogs]);
				double GFOneAngle = GFZeroAngle
						+ ourOrbitDirections[indexMovementLogs]
						* RoboGeom.normMaxEscapeAngle(bulletVelocity);
				double GFNegOneAngle = GFZeroAngle
						- ourOrbitDirections[indexMovementLogs]
						* RoboGeom.normMaxEscapeAngle(bulletVelocity);
				double preciseGFOneAngle = preciseMaxEscapeAngle(
						bulletVelocity, ourOrbitDirections[indexMovementLogs]);
				double preciseGFNegOneAngle = preciseMaxEscapeAngle(
						bulletVelocity, -ourOrbitDirections[indexMovementLogs]);
				double AheadWallSeverity = wallSeverity(
						ourOrbitDirections[indexMovementLogs],
						bulletTravelTime, (enemyEnergy - e.getEnergy()),
						ourPreviousPositions[indexMovementLogs],
						enemyPreviousPositions[indexMovementLogs]);
				double ReverseWallSeverity = wallSeverity(
						-ourOrbitDirections[indexMovementLogs],
						bulletTravelTime, (enemyEnergy - e.getEnergy()),
						ourPreviousPositions[indexMovementLogs],
						enemyPreviousPositions[indexMovementLogs]);
				double AheadWallDist = wallDistance(enemyCoordinates,
						ourOrbitDirections[indexMovementLogs]);
				double ReverseWallDist = wallDistance(enemyCoordinates,
						-ourOrbitDirections[indexMovementLogs]);
				double virtuality = Math.min(
						ticksSinceFire * ourRobot.getGunCoolingRate(),
						enemyGunHeat) * 0.33333333333;

				double orbitingCollisionFactor = 0.0;

				MovementDataWave addWave = new MovementDataWave(
						currentTime - 1, enemyCoordinates,
						ourPreviousPositions[indexMovementLogs], bulletPower,
						bulletTravelTime, ourHeadings[indexMovementLogs],
						RelHeading, ourVelocities[indexMovementLogs],
						AheadWallSeverity, ReverseWallSeverity, AheadWallDist,
						ReverseWallDist, ourVChange[indexMovementLogs],
						GFZeroAngle, GFOneAngle, GFNegOneAngle,
						preciseGFOneAngle, preciseGFNegOneAngle,
						ticksSinceDirChange, ticksSinceVelocityChange,
						absTime - 1, virtuality, ourDistLastNTicks(5),
						ourDistLastNTicks(10), ourDistLastNTicks(20),
						orbitingCollisionFactor,
						ourOrbitDirections[indexMovementLogs]);

				listOfVirtualWaves.add(addWave);
				if (enemyGunHeat <= 0 && enemyShootsAtGunHeatZero) {
					int otherWaves = 0;
					for (MovementDataWave wave : listOfWaves) {
						if (wave.surfingThisWave())
							otherWaves++;
					}
					if (otherWaves < 2)
						lastMoveRecalcTime = currentTime;
				}
			}

			double enemyAbsBearing = e.getBearingRadians() + ourHeading;
			enemyCoordinates = new Point2D.Double(ourCoordinates.x
					+ Math.sin(enemyAbsBearing) * e.getDistance(),
					ourCoordinates.y + Math.cos(enemyAbsBearing)
							* e.getDistance());

			// Needed because of the order of event (onWin) calls.
			if (e.getEnergy() >= 0)
				enemyEnergy = e.getEnergy();

		}
		long startMWTime = System.nanoTime();
		manageWaves(currentTime);
		manageOurBullets(currentTime);
		waveManagementTime += System.nanoTime() - startMWTime;
		onScannedRobotTime += System.nanoTime() - startOnScannedTime;

	}

	// /**
	// * Always call onScannedRobot first!
	// *
	// * @return The absolute robocode Angle to fire at
	// */
	// public double[][] getRawAnglesWeightsAndBandwidths() {
	// return VirtualMovementsClassifier
	// .getRawAnglesWeightsAndBandwidths(_LatestMDWave);
	// }

	// TODO: should you surf the wave for the last tick? this must be
	// synchronized with
	// the precise predictor.
	/**
	 * 
	 * @param time
	 *            current time.
	 * @return The wave that will pass our current location first. Null if no
	 *         current waves will pass it.
	 */
	public MovementDataWave getSoonestBreaksSurfingWave(long time) {
		MovementDataWave bestWave = null;
		double timeBestWaveBreaks = 100000.0;
		for (MovementDataWave wave : listOfWaves) {
			double currentTimeWaveBreaks;
			if ((currentTimeWaveBreaks = (wave.getSourcePosition().distance(
					ourCoordinates) - (time - wave.getFireTime())
					* wave.getBulletVelocity())
					/ wave.getBulletVelocity()) < timeBestWaveBreaks
					&& currentTimeWaveBreaks > 0.0) {
				timeBestWaveBreaks = currentTimeWaveBreaks;
				bestWave = wave;
			}
		}
		return bestWave;
	}

	// TODO: should you surf the wave for the last tick? this must be
	// synchronized with
	// the precise predictor.
	/**
	 * 
	 * @param time
	 *            current time.
	 * @param absTime
	 *            absolute time
	 * @return The gun heat wave from the current tick if it should be surfed.
	 *         Otherwise it returns null.
	 */
	public MovementDataWave getGunHeatWave(long time, long absTime) {
		MovementDataWave bestWave = null;
		if (enemyShootsAtGunHeatZero && enemyGunHeat <= 0.0) {
			double bulletVelocity = 20.0 - 3.0 * (lastBulletPower);
			double bulletTravelTime = (Point2D.distance(ourCoordinates.x,
					ourCoordinates.y, enemyCoordinates.x, enemyCoordinates.y) / bulletVelocity);
			double relativeHeading = Utils.normalRelativeAngle(heading
					- RoboGeom.getBearing(enemyCoordinates, ourCoordinates));
			double RelHeading = Math.abs(relativeHeading);

			double GFZeroAngle = RoboGeom.getBearing(enemyCoordinates,
					ourCoordinates);
			double GFOneAngle = GFZeroAngle + orbitDir
					* RoboGeom.normMaxEscapeAngle(bulletVelocity);
			double GFNegOneAngle = GFZeroAngle - orbitDir
					* RoboGeom.normMaxEscapeAngle(bulletVelocity);
			double preciseGFOneAngle = preciseMaxEscapeAngle(bulletVelocity,
					orbitDir);
			double preciseGFNegOneAngle = preciseMaxEscapeAngle(bulletVelocity,
					-orbitDir);
			double AheadWallSeverity = wallSeverity(orbitDir, bulletTravelTime,
					(lastBulletPower), ourCoordinates, enemyCoordinates);
			double ReverseWallSeverity = wallSeverity(-orbitDir,
					bulletTravelTime, (lastBulletPower), ourCoordinates,
					enemyCoordinates);
			double AheadWallDist = wallDistance(enemyCoordinates, orbitDir);
			double ReverseWallDist = wallDistance(enemyCoordinates, -orbitDir);
			double virtuality = Math
					.min(ticksSinceFire * ourRobot.getGunCoolingRate(),
							enemyGunHeat) * 0.33333333333;

			double orbitingCollisionFactor = 0.0;

			bestWave = new MovementDataWave(time, enemyCoordinates,
					ourPreviousPositions[indexMovementLogs], lastBulletPower,
					bulletTravelTime, ourHeadings[indexMovementLogs],
					RelHeading, ourVelocities[indexMovementLogs],
					AheadWallSeverity, ReverseWallSeverity, AheadWallDist,
					ReverseWallDist, ourVChange[indexMovementLogs],
					GFZeroAngle, GFOneAngle, GFNegOneAngle, preciseGFOneAngle,
					preciseGFNegOneAngle, ticksSinceDirChange,
					ticksSinceVelocityChange, absTime, virtuality,
					ourDistLastNTicks(5), ourDistLastNTicks(10),
					ourDistLastNTicks(20), orbitingCollisionFactor,
					ourOrbitDirections[indexMovementLogs]);
			long startTime = System.nanoTime();
			bestWave.setAnglesWeightsAndBandwidths(VirtualMovementsClassifier
					.getRawAnglesWeightsAndBandwidths(bestWave, enemyHits,
							enemyMissPenalties, enemyRawMissPenalties,
							numOfShotsLogged, numberOfBulletHitBulletEvents));
			waveDangerTime += System.nanoTime() - startTime;
		}
//		return bestWave;
		return null;
	}

	public MovementDataWave getSoonestBreaksSurfingWave(long time,
			Point2D.Double point) {
		MovementDataWave bestWave = null;
		double timeBestWaveBreaks = 100000;
		for (MovementDataWave wave : listOfWaves) {
			double currentTimeWaveBreaks;
			if ((currentTimeWaveBreaks = (wave.getSourcePosition().distance(
					point) - (time - wave.getFireTime())
					* wave.getBulletVelocity())
					/ wave.getBulletVelocity()) < timeBestWaveBreaks
					&& currentTimeWaveBreaks > 0.0) {
				timeBestWaveBreaks = currentTimeWaveBreaks;
				bestWave = wave;
			}
		}
		return bestWave;
	}

	/**
	 * Doesn't work for one wave.
	 * 
	 * @param n
	 * @param ourFuturePosition
	 * @param time
	 * @return
	 */
	public MovementDataWave[] getNNearestSurifingWavesForPoint(int n,
			Point2D.Double ourFuturePosition, long time) {
		MovementDataWave[] currentWaves = new MovementDataWave[n];

		double[] timesWavesBreak = new double[n];
		for (int i = 0; i < timesWavesBreak.length; i++) {
			timesWavesBreak[i] = 100000.0;
		}

		for (MovementDataWave wave : listOfWaves) {
			double currentTimeWaveBreaks;
			if ((currentTimeWaveBreaks = (wave.getSourcePosition().distance(
					ourFuturePosition) - (time - wave.getFireTime())
					* wave.getBulletVelocity())
					/ wave.getBulletVelocity()) < timesWavesBreak[n - 1]
					&& currentTimeWaveBreaks > 0) {
				for (int i = n - 2; i >= 0; i--) {
					if (currentTimeWaveBreaks > timesWavesBreak[i]) {
						timesWavesBreak[i + 1] = currentTimeWaveBreaks;
						currentWaves[i + 1] = wave;
						break;
					} else if (i == 0) {
						timesWavesBreak[i + 1] = timesWavesBreak[i];
						currentWaves[i + 1] = currentWaves[i];
						timesWavesBreak[i] = currentTimeWaveBreaks;
						currentWaves[i] = wave;
						break;
					} else {
						timesWavesBreak[i + 1] = timesWavesBreak[i];
						currentWaves[i + 1] = currentWaves[i];
					}
				}
			}
		}
		return currentWaves;
	}

	public Point2D.Double getEnemyCoordinates() {
		return enemyCoordinates;
	}

	private void addShadowsBulletsInAir(MovementDataWave wave, long currentTime) {
		for (int i = 0; i < ourBulletsInAir.size(); i++) {
			Bullet bullet = ourBulletsInAir.get(i);
			Point2D.Double bulletPos = new Point2D.Double(bullet.getX(),
					bullet.getY());
			Point2D.Double bulletStartPos = RoboGeom.project(bulletPos,
					bullet.getVelocity()
							* (currentTime - fireTimesOurBulletsInAir.get(i)),
					bullet.getHeadingRadians() + Math.PI);
			wave.addBulletShadows(bullet.getVelocity(),
					bullet.getHeadingRadians(), bulletStartPos,
					fireTimesOurBulletsInAir.get(i), bullet);
		}
		lastMoveRecalcTime = currentTime;
	}

	public void ourBulletFired(double bulletAbsHeading, double bulletVelocity,
			Point2D.Double bulletSource, long fireTime, long currentTime,
			Bullet bullet) {
		if (_is1v1) {
			ourBulletsInAir.add(bullet);
			fireTimesOurBulletsInAir.add(fireTime);
			for (MovementDataWave wave : listOfWaves) {
				wave.addBulletShadows(bulletVelocity, bulletAbsHeading,
						bulletSource, fireTime, bullet);
			}

			// TODO this is probably worse but it I'll keep it for now because
			// it makes everything correct.
			// Delay move recalc because firing angle assumed that we would use
			// old movement path.

			lastMoveRecalcTime = currentTime + 1;
		}
	}

	public void onHitByBullet(HitByBulletEvent e, long time) {
		if (_is1v1) {
			numberOfBulletsThatHitUsThisRound++;
			Point2D.Double point = new Point2D.Double(e.getBullet().getX(), e
					.getBullet().getY());
			logBullet(point, e.getBullet().getVelocity(), e.getBullet()
					.getPower(), time);
			updateEnergy(Rules.getBulletHitBonus(e.getBullet().getPower()));
			lastMoveRecalcTime = time;
			enemyHits++;
		}
	}

	public void onBulletHitBullet(BulletHitBulletEvent e, long time) {
		if (_is1v1) {
			numberOfBulletHitBulletEvents++;
			numberOfBulletHitBulletEventsThisRound++;
			Point2D.Double point = new Point2D.Double(e.getHitBullet().getX(),
					e.getHitBullet().getY());
			// use (time - 1) because the event is from the previous turn
			logBullet(point, e.getHitBullet().getVelocity(), e.getHitBullet()
					.getPower(), time - 1);
			Point2D.Double bulletCoordinates = new Point2D.Double(e.getBullet()
					.getX(), e.getBullet().getY());
			for (MovementDataWave wave : listOfWaves) {
				wave.removeBulletShadows(e.getBullet().getVelocity(), e
						.getBullet().getHeadingRadians(), bulletCoordinates, e
						.getTime(), e.getBullet());
			}
			//TODO change this to time?
			lastMoveRecalcTime = time + 1;
		}
	}

	public void onBulletHit(BulletHitEvent e) {
		if (_is1v1) {
			numberOfOurBulletsThatHitThisRound++;
			if ((getEnemyEnergy() - Rules.getBulletDamage(e.getBullet()
					.getPower())) >= 0)
				updateEnergy(-Rules.getBulletDamage(e.getBullet().getPower()));
		}
	}

	public void onHitRobot(HitRobotEvent e) {
		if (_is1v1) {
			if (getEnemyEnergy() - Rules.ROBOT_HIT_DAMAGE >= 0)
				updateEnergy(-Rules.ROBOT_HIT_DAMAGE);
		}
	}

	public void onWin(WinEvent e, double ourEnergy, long currentTime,
			long absTime) {
		double[] bulletPowersData = new double[3];
		bulletPowersData[0] = Point2D.distance(
				ourPreviousPositions[indexMovementLogs].x,
				ourPreviousPositions[indexMovementLogs].y,
				enemyPreviousPositions[indexMovementLogs].x,
				enemyPreviousPositions[indexMovementLogs].y);
		bulletPowersData[1] = enemyEnergy;
		bulletPowersData[2] = ourEnergy;

		double bulletPower;

		if (enemyBulletPowersTree.size > 0) {
			HeapEntry<PointEntry<Double>>[] points = enemyBulletPowersTree
					.getNNearestPoints(bulletPowersData,
							Math.min(5, enemyBulletPowersTree.size));
			double sum = 0.0;
			for (int i = 0; i < points.length; i++) {
				sum += points[i].entryData.dataObject;
			}
			bulletPower = Math.min(enemyEnergy, sum / points.length);
		} else
			bulletPower = 0.1;

		double bulletVelocity = 20.0 - 3.0 * (bulletPower);
		double bulletTravelTime = (Point2D.distance(
				ourPreviousPositions[indexMovementLogs].x,
				ourPreviousPositions[indexMovementLogs].y,
				enemyPreviousPositions[indexMovementLogs].x,
				enemyPreviousPositions[indexMovementLogs].y) / bulletVelocity);
		double relativeHeading = Utils
				.normalRelativeAngle(ourHeadings[indexMovementLogs]
						- RoboGeom.getBearing(enemyCoordinates,
								ourPreviousPositions[indexMovementLogs]));
		double RelHeading = Math.abs(relativeHeading);

		double GFZeroAngle = RoboGeom.getBearing(enemyCoordinates,
				ourPreviousPositions[indexMovementLogs]);
		double GFOneAngle = GFZeroAngle + ourOrbitDirections[indexMovementLogs]
				* RoboGeom.normMaxEscapeAngle(bulletVelocity);
		double GFNegOneAngle = GFZeroAngle
				- ourOrbitDirections[indexMovementLogs]
				* RoboGeom.normMaxEscapeAngle(bulletVelocity);
		double preciseGFOneAngle = preciseMaxEscapeAngle(bulletVelocity,
				ourOrbitDirections[indexMovementLogs]);
		double preciseGFNegOneAngle = preciseMaxEscapeAngle(bulletVelocity,
				-ourOrbitDirections[indexMovementLogs]);
		double AheadWallSeverity = wallSeverity(
				ourOrbitDirections[indexMovementLogs], bulletTravelTime,
				(bulletPower), ourPreviousPositions[indexMovementLogs],
				enemyPreviousPositions[indexMovementLogs]);
		double ReverseWallSeverity = wallSeverity(
				-ourOrbitDirections[indexMovementLogs], bulletTravelTime,
				(bulletPower), ourPreviousPositions[indexMovementLogs],
				enemyPreviousPositions[indexMovementLogs]);
		double AheadWallDist = wallDistance(enemyCoordinates,
				ourOrbitDirections[indexMovementLogs]);
		double ReverseWallDist = wallDistance(enemyCoordinates,
				-ourOrbitDirections[indexMovementLogs]);
		double virtuality = Math.min(
				ticksSinceFire * ourRobot.getGunCoolingRate(), enemyGunHeat) * 0.33333333333;

		double orbitingCollisionFactor = 0.0;

		MovementDataWave addWave = new MovementDataWave(currentTime - 1,
				enemyCoordinates, ourPreviousPositions[indexMovementLogs],
				bulletPower, bulletTravelTime, ourHeadings[indexMovementLogs],
				RelHeading, ourVelocities[indexMovementLogs],
				AheadWallSeverity, ReverseWallSeverity, AheadWallDist,
				ReverseWallDist, ourVChange[indexMovementLogs], GFZeroAngle,
				GFOneAngle, GFNegOneAngle, preciseGFOneAngle,
				preciseGFNegOneAngle, ticksSinceDirChange,
				ticksSinceVelocityChange, absTime - 1, virtuality,
				ourDistLastNTicks(5), ourDistLastNTicks(10),
				ourDistLastNTicks(20), orbitingCollisionFactor,
				ourOrbitDirections[indexMovementLogs]);
		long startTime = System.nanoTime();

		addWave.setAnglesWeightsAndBandwidths(VirtualMovementsClassifier
				.getRawAnglesWeightsAndBandwidths(addWave, enemyHits,
						enemyMissPenalties, enemyRawMissPenalties,
						numOfShotsLogged, numberOfBulletHitBulletEvents));
		waveDangerTime += System.nanoTime() - startTime;

		listOfWaves.add(addWave);
		addShadowsBulletsInAir(addWave, currentTime);
		lastMoveRecalcTime = currentTime;
		System.out.println("OnWin at " + currentTime);
	}

	private void logBullet(Point2D.Double hitCoordinates, double hitVelocity,
			double hitEnergy, long time) {
		for (int i = 0; i < listOfWaves.size(); i++) {
			MovementDataWave wave = listOfWaves.get(i);
			double d;
			if ((d = hitCoordinates.distance(wave.getSourcePosition())) < (time - wave
					.getFireTime()) * wave.getBulletVelocity() + 0.1
					&& d > (time - wave.getFireTime() - 1)
							* wave.getBulletVelocity() - 0.1
					&& wave.getBulletPower() + 0.001 > hitEnergy
					&& wave.getBulletPower() - 0.001 < hitEnergy) {

				DataWavePassedRecord rec = new DataWavePassedRecord(wave,
						hitCoordinates);

				VirtualMovementsClassifier.train(wave, rec);

				listOfWaves.remove(i);
				numOfShotsLogged++;
				// TODO fix this
				double botAngle = Math.abs(Utils.normalRelativeAngle(wave
						.getMaxClockwiseAngle()
						- wave.getMaxCounterClockwiseAngle()));
				enemyMissPenalties += botAngle
						/ (Math.abs(Utils.normalRelativeAngle(wave
								.getPreciseGFOneAngle()
								- wave.getPreciseGFNegOneAngle())) + botAngle);
				enemyRawMissPenalties += botAngle
						/ (Math.abs(Utils.normalRelativeAngle(wave
								.getGFOneAngle() - wave.getGFNegOneAngle())) + botAngle);
				for (MovementDataWave oldWave : listOfWaves) {
					long startTime = System.nanoTime();

					oldWave.setAnglesWeightsAndBandwidths(VirtualMovementsClassifier
							.getRawAnglesWeightsAndBandwidths(oldWave,
									enemyHits, enemyMissPenalties,
									enemyRawMissPenalties, numOfShotsLogged,
									numberOfBulletHitBulletEvents));
					waveDangerTime += System.nanoTime() - startTime;

				}
				break;
			} /*
			 * else { System.out.println(d + ", " + (time - wave.getFireTime() -
			 * 1) wave.getBulletVelocity() + "; " + hitEnergy + ", " +
			 * wave.getBulletPower()); System.out.println(time); }
			 */
		}
	}

	public void onRoundEnded() {
		System.out
				.println("Estimated Raw Enemy Hit rate = "
						+ ((enemyHits + numberOfBulletHitBulletEvents
								* (enemyHits / numOfShotsLogged) - enemyRawMissPenalties) / numOfShotsLogged));
		System.out
				.println("Estimated Enemy Hit rate = "
						+ ((enemyHits + numberOfBulletHitBulletEvents
								* (enemyHits / numOfShotsLogged) - enemyMissPenalties) / numOfShotsLogged));
		System.out.println("Known Enemy Hit rate: "
				+ (enemyHits - enemyMissPenalties) / numOfShotsLogged + ", "
				+ numOfShotsLogged + "  Raw = " + 100.0 * enemyHits
				/ numOfShotsLogged + "%");
		VirtualMovementsClassifier VMC = (VirtualMovementsClassifier) VirtualMovementsClassifier;
		VMC.listClassifierScores();
		System.out.println("Flattener enabled = "
				+ VMC.flattenerEnabled(enemyHits, enemyMissPenalties,
						enemyRawMissPenalties, numOfShotsLogged,
						numberOfBulletHitBulletEvents));
		if (debugging) {
			System.out.println("Wave Danger Time = " + waveDangerTime * 1E-6);
			System.out.println("OnScannedRobot Time = " + onScannedRobotTime
					* 1E-6);
			System.out.println("Wave Management Time = " + waveManagementTime
					* 1E-6);
		}

	}

	private void updateEnergy(double deltaEnergy) {
		enemyEnergy += deltaEnergy;
	}

	private void manageWaves(long time) {
		for (int i = 0; i < listOfWaves.size(); i++) {
			listOfWaves.get(i).updateWave(ourCoordinates, time);
			MovementDataWave wave = listOfWaves.get(i);
			if (wave.getSourcePosition().distance(ourCoordinates.x,
					ourCoordinates.y) < wave.getBulletVelocity()
					* (time - wave.getFireTime()) - 50.0) {

				// if (Math.abs(wave.anglePassedDebugging[0]
				// - wave.getMaxClockwiseAngle()) > 0.01
				// || Math.abs(wave.anglePassedDebugging[1]
				// - wave.getMaxCounterClockwiseAngle()) > 0.01) {
				// System.out.println("Predicted: "
				// + wave.anglePassedDebugging[0] + ", "
				// + wave.anglePassedDebugging[1]);
				// System.out.println("Real       : "
				// + wave.getMaxClockwiseAngle() + ", "
				// + wave.getMaxCounterClockwiseAngle());
				// System.out.println(Math.asin(36 / wave
				// .getSourceToTargetDistance()));
				// }

				VirtualMovementsClassifier
						.trainSafelyPassedOfBullet(listOfWaves.get(i));
				listOfWaves.remove(i--);
				numOfShotsLogged++;
				double botAngle = Math.abs(Utils.normalRelativeAngle(wave
						.getMaxClockwiseAngle()
						- wave.getMaxCounterClockwiseAngle()));
				enemyMissPenalties += botAngle
						/ (Math.abs(Utils.normalRelativeAngle(wave
								.getPreciseGFOneAngle()
								- wave.getPreciseGFNegOneAngle())));
				enemyRawMissPenalties += botAngle
						/ (Math.abs(Utils.normalRelativeAngle(wave
								.getGFOneAngle() - wave.getGFNegOneAngle())));
			} else if (wave.surfingThisWave()
					&& wave.getSourcePosition().distance(ourCoordinates.x,
							ourCoordinates.y) < wave.getBulletVelocity()
							* (time - wave.getFireTime())) {
				lastMoveRecalcTime = time;
				// TODO check this
				wave.stopSurfing();
			}
		}
		for (int i = 0; i < listOfVirtualWaves.size(); i++) {
			MovementDataWave wave = listOfVirtualWaves.get(i);
			if (wave.getWavePassed(ourCoordinates, time)) {

				DataWavePassedRecord rec = new DataWavePassedRecord(wave,
						ourCoordinates);
				VirtualMovementsClassifier.trainVirtualWave(wave, rec);
				// TODO use places we know did not contain bullets to train
				// classifiers.
				listOfVirtualWaves.remove(i--);
			}
		}

		for (int i = 0; i < _ListOfPreciseWaves.size(); i++) {
			_ListOfPreciseWaves.get(i).updateWave(ourCoordinates, time);
			if (_ListOfPreciseWaves.get(i).getWavePassed(ourCoordinates, time)) {
				_ListOfPreciseWaves.remove(i--);
			}
		}
	}

	private void manageOurBullets(long currentTime) {
		for (int i = 0; i < ourBulletsInAir.size(); i++) {
			Point2D.Double bulletCoordinates = new Point2D.Double(
					ourBulletsInAir.get(i).getX(), ourBulletsInAir.get(i)
							.getY());
			if (ourCoordinates.distance(enemyCoordinates) < ourCoordinates
					.distance(bulletCoordinates)
					|| !ourBulletsInAir.get(i).isActive()) {
				fireTimesOurBulletsInAir.remove(i);
				ourBulletsInAir.remove(i--);
			}
		}
	}

	public long getLastMoveRecalcTime() {
		return lastMoveRecalcTime;
	}

	// public boolean getUseNonWaveSurfing() {
	// // return useNonWaveSurfing;
	// if (_ListOfWaves.size() == 0)
	// return true;
	// else
	// return false;
	// }

	public double getEnemyEnergy() {
		return enemyEnergy;
	}

	public double getEnemyPracticalHeading() {
		return enemyPracticalHeading;
	}

	// This should only be called if the movement path is already being
	// recalculated
	public double getDesiredDistance() {
		if (VirtualMovementsClassifier.flattenerEnabled(enemyHits,
				enemyMissPenalties, enemyRawMissPenalties, numOfShotsLogged,
				numberOfBulletHitBulletEvents))
			return 400.0; // a distance of zero means that we don't try to
							// control the distance
		else if (numberOfBulletHitBulletEventsThisRound > 5 * (numberOfBulletsThatHitUsThisRound + 1)
				&& numberOfBulletHitBulletEventsThisRound > 5 * (numberOfOurBulletsThatHitThisRound + 1))
			return 120.0;
		else if ((enemyHits - enemyMissPenalties) / numOfShotsLogged < -0.1)
			return 400.0;
		else
			return desiredDistance;
	}

	// TODO improve
	private double preciseMaxEscapeAngle(double bulletVelocity,
			int orbitDirection) {
		Wave predictionWave = new Wave(ourRobot.getTime() + 1,
				enemyCoordinates, bulletVelocity);
		Point2D.Double predictedPosition;
		boolean GFOnePrediction = true;
		if (orbitDir * orbitDirection == -1)
			GFOnePrediction = false;

		// double GFZeroAngle = RoboGeom.getBearing(enemyCoordinates,
		// ourCoordinates);
		predictedPosition = PrecisePredictor.predictPosition(ourCoordinates.x,
				ourCoordinates.y, heading, velocity, predictionWave,
				ourRobot.getTime(), 160, orbitDirection);
		// double bearingToMoveAt =
		// Utils.normalAbsoluteAngle(RoboGeom.getBearing(
		// enemyCoordinates, predictedPosition));
		double maxBearing = RoboGeom.getBearing(enemyCoordinates,
				predictedPosition);
		// double maxAngleSize = Math.abs(Utils.normalRelativeAngle(maxBearing
		// - GFZeroAngle));
		// if (GFOnePrediction)
		// enemyGFOnePoint = predictedPosition;
		// else
		// enemyGFNegOnePoint = predictedPosition;
		//
		// predictedPosition =
		// PrecisePredictor.predictPosition(ourCoordinates.x,
		// ourCoordinates.y, heading, velocity, bearingToMoveAt,
		// predictionWave, ourRobot.getTime(), 100, orbitDirection);
		// bearingToMoveAt = Utils.normalAbsoluteAngle(RoboGeom.getBearing(
		// enemyCoordinates, predictedPosition));
		// if (Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
		// enemyCoordinates, predictedPosition) - GFZeroAngle)) > maxAngleSize)
		// {
		// maxBearing = RoboGeom.getBearing(enemyCoordinates,
		// predictedPosition);
		// maxAngleSize = Math.abs(Utils.normalRelativeAngle(maxBearing
		// - GFZeroAngle));
		// if (GFOnePrediction)
		// enemyGFOnePoint = predictedPosition;
		// else
		// enemyGFNegOnePoint = predictedPosition;
		// }
		//
		// predictedPosition =
		// PrecisePredictor.predictPosition(ourCoordinates.x,
		// ourCoordinates.y, heading, velocity, bearingToMoveAt,
		// predictionWave, ourRobot.getTime(), 100, orbitDir);
		// bearingToMoveAt = Utils.normalAbsoluteAngle(RoboGeom.getBearing(
		// enemyCoordinates, predictedPosition));
		// if (Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
		// enemyCoordinates, predictedPosition) - GFZeroAngle)) > maxAngleSize)
		// {
		// maxBearing = RoboGeom.getBearing(enemyCoordinates,
		// predictedPosition);
		// maxAngleSize = Math.abs(Utils.normalRelativeAngle(maxBearing
		// - GFZeroAngle));
		if (GFOnePrediction)
			ourGFOnePoint = predictedPosition;
		else
			ourGFNegOnePoint = predictedPosition;
		// }

		return maxBearing;
	}

	private double wallSeverity(int dir, double actualBulletTravelTime,
			double bulletPower, Point2D.Double ourPrevCoordinates,
			Point2D.Double enemyPrevCoordinates) {
		double stickLength = Math.min(295.0, actualBulletTravelTime * 10.0);
		return Math
				.min(0.5 * Math.PI,
						Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
								ourPreviousPositions[indexMovementLogs],
								enemyCoordinates)
								- PrecisePredictor
										.noniterativeWallSmoothing(
												ourPreviousPositions[indexMovementLogs],
												RoboGeom.getBearing(
														enemyCoordinates,
														ourPreviousPositions[indexMovementLogs])
														+ dir * Math.PI / 2,
												dir, stickLength)))
								/ (0.5 * Math.PI));

		// Wave wallDistWave = new Wave(ourRobot.getTime() - 1,
		// enemyPrevCoordinates, 20 - 3 * bulletPower);
		//
		// Point2D.Double wallDistPoint = PrecisePredictor.predictPosition(
		// ourPrevCoordinates.x, ourPrevCoordinates.y, heading, velocity,
		// wallDistWave, ourRobot.getTime(), 160, dir);
		//
		// double bulletVelocity = 20 - 3.0 * bulletPower;
		// double shortestPossibleBulletTravelTime = (actualBulletTravelTime *
		// bulletVelocity)
		// / (bulletVelocity + 8.0);
		// return Math.max(0, (Point2D.distance(enemyPrevCoordinates.x,
		// enemyPrevCoordinates.y, ourPrevCoordinates.x,
		// ourPrevCoordinates.y) - Point2D.distance(wallDistPoint.x,
		// wallDistPoint.y, enemyCoordinates.x, enemyCoordinates.y)))
		// / ((shortestPossibleBulletTravelTime) * 8.0);

		// if(dir == ourOrbitDirections[indexMovementLogs])
		// return
		// Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(enemyPrevCoordinates,
		// ourGFOnePoint) - RoboGeom.getBearing(enemyPrevCoordinates,
		// ourPrevCoordinates))) /
		// RoboGeom.normMaxEscapeAngle(Rules.getBulletSpeed(bulletPower));
		// else
		// return
		// Math.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(enemyPrevCoordinates,
		// ourGFNegOnePoint) - RoboGeom.getBearing(enemyPrevCoordinates,
		// ourPrevCoordinates))) /
		// RoboGeom.normMaxEscapeAngle(Rules.getBulletSpeed(bulletPower));

		//
		// double ratioPredictedMaxGFOne = (Math.abs(Utils
		// .normalRelativeAngle(preciseGFOneBearing
		// - RoboGeom.getBearing(robotCoordinates,
		// enemyCoordinates))) / normalMaxEscapeAngle(20 - 3 * bulletPower));
		// double ratioPredictedMaxGFNegOne = (Math.abs(Utils
		// .normalRelativeAngle(preciseGFNegOneBearing
		// - RoboGeom.getBearing(robotCoordinates,
		// enemyCoordinates))) / normalMaxEscapeAngle(20 - 3 * bulletPower));
		// 0.6287963 is the maximum possible escape angle (in one direction)
		// double firingAnglePreciseGFOneGFZero = Math.abs(Utils
		// .normalRelativeAngle(preciseGFOneBearing
		// - RoboGeom.getBearing(robotCoordinates,
		// enemyCoordinates)))
		// / 0.6287963;
		// double firingAnglePreciseGFNegOneGFZero = Math.abs(Utils
		// .normalRelativeAngle(preciseGFNegOneBearing
		// - RoboGeom.getBearing(robotCoordinates,
		// enemyCoordinates)))
		// / 0.6287963;
		//
		// double moveAnglePreciseGFOneGFZero = Math.abs(Utils
		// .normalRelativeAngle(RoboGeom.getBearing(
		// enemyCoordinates, enemyGFOnePoint)
		// - RoboGeom.getBearing(enemyCoordinates,
		// robotCoordinates)))
		// / Math.PI;
		// double moveAnglePreciseGFNegOneGFZero = Math.abs(Utils
		// .normalRelativeAngle(RoboGeom.getBearing(
		// enemyCoordinates, enemyGFNegOnePoint)
		// - RoboGeom.getBearing(enemyCoordinates,
		// robotCoordinates)))
		// / Math.PI;

		// double bearingFromEnemyToUs = Math.atan2(enemyX - robot.getX(),
		// enemyY
		// - robot.getY())
		// / (2 * Math.PI);

		// if (dir == 1)
		// return Point2D.distance(robotCoordinates.x, robotCoordinates.y,
		// enemyGFOnePoint.x, enemyGFOnePoint.y)
		// / preciseDistanceToEnemy;
		// else
		// return Point2D.distance(robotCoordinates.x, robotCoordinates.y,
		// enemyGFNegOnePoint.x, enemyGFNegOnePoint.y)
		// / preciseDistanceToEnemy;
		//
		// Point2D.Double normGFOnePoint = RoboGeom.project(
		// robotCoordinates, enemyDistance, enemyAbsBearing + dir
		// * normalMaxEscapeAngle(20 - 3 * bulletPower));
		// Point2D.Double normGFNegOnePoint = RoboGeom.project(
		// robotCoordinates, enemyDistance, enemyAbsBearing - dir
		// * normalMaxEscapeAngle(20 - 3 * bulletPower));
		//
		// double ratioMoveAnglePreciseNormGFOne = moveAnglePreciseGFOneGFZero
		// / (Math.abs(Utils.normalRelativeAngle(PrecisePredictor
		// .getBearing(enemyCoordinates, normGFOnePoint)
		// - RoboGeom.getBearing(enemyCoordinates,
		// robotCoordinates))) / Math.PI);
		// double ratioMoveAnglePreciseNormGFNegOne =
		// moveAnglePreciseGFNegOneGFZero
		// / (Math.abs(Utils.normalRelativeAngle(PrecisePredictor
		// .getBearing(enemyCoordinates, normGFNegOnePoint)
		// - RoboGeom.getBearing(enemyCoordinates,
		// robotCoordinates))) / Math.PI);

		// moveAngle stuff might work nicely in Stat. C-targeting where the
		// dimension could
		// represent at what NBO the robot would turn around, but it doesn't
		// seem to help here.
	}

	private double wallDistance(Point2D.Double enemyCoordinates, int dir) {
		return Math
				.min(Math.PI / 2,
						RoboGeom.wallDistance(
								enemyCoordinates,
								ourPreviousPositions[indexMovementLogs],
								enemyCoordinates
										.distance(ourPreviousPositions[indexMovementLogs]),
								dir))
				/ (Math.PI / 2);
	}

	private double ourDistLastNTicks(int n) {
		if (n + 2 > indexLongLog
				&& longLogPreviousPositions[longLogPreviousPositions.length - 1] == null) {
			return longLogPreviousPositions[0]
					.distance(ourPreviousPositions[indexMovementLogs]);
		}
		int num = n + 2;
		int indexOfPosition;
		if (indexLongLog - num < 0)
			indexOfPosition = longLogPreviousPositions.length
					+ (indexLongLog - num);
		else
			indexOfPosition = indexLongLog - num;
		return longLogPreviousPositions[indexOfPosition]
				.distance(ourPreviousPositions[indexMovementLogs]);
	}

	public void onPaint(Graphics2D graphicsObject, long time) {
		if (_is1v1) {
			for (MovementDataWave wave : listOfWaves) {
				paintWave(graphicsObject, wave, time);
			}
			markCurrentMovementWave(graphicsObject, time);

			for (PreciseIntersectionWave wave : _ListOfPreciseWaves) {
				if (wave.getWaveIntersected())
					paintPreciseWave(graphicsObject, wave, time);
			}

			graphicsObject.setColor(Color.green);
			graphicsObject.drawRect((int) ourCoordinates.x - 18,
					(int) ourCoordinates.y - 18, 36, 36);
		}
	}

	private void paintPreciseWave(Graphics2D graphicsObject,
			PreciseIntersectionWave wave, long time) {

		if (debugging) {
			double radius = wave.getBulletVelocity()
					* (time - wave.getFireTime());

			Point2D.Double GFZeroPoint = RoboGeom.project(
					wave.getSourcePosition(), radius, wave.getGFZeroAngle());

			Point2D.Double MaxClockwisePoint = RoboGeom.project(
					wave.getSourcePosition(), radius,
					wave.getMaxClockwiseAngle());
			Point2D.Double MaxCounterClockwisePoint = RoboGeom.project(
					wave.getSourcePosition(), radius,
					wave.getMaxCounterClockwiseAngle());

			graphicsObject.setColor(Color.RED);

			graphicsObject.drawLine((int) wave.getSourcePosition().x,
					(int) wave.getSourcePosition().y,
					(int) MaxClockwisePoint.x, (int) MaxClockwisePoint.y);

			graphicsObject.setColor(Color.GREEN);

			graphicsObject.drawLine((int) wave.getSourcePosition().x,
					(int) wave.getSourcePosition().y,
					(int) MaxCounterClockwisePoint.x,
					(int) MaxCounterClockwisePoint.y);
		}

	}

	private void paintWave(Graphics2D graphicsObject, MovementDataWave wave,
			long time) {
		double radius = wave.getBulletVelocity() * (time - wave.getFireTime());
		graphicsObject.setColor(Color.GRAY);
		graphicsObject.drawOval((int) (wave.getSourcePosition().x - radius),
				(int) (wave.getSourcePosition().y - radius),
				(int) (2 * radius), (int) (2 * radius));
		Point2D.Double GFZeroPoint = RoboGeom.project(wave.getSourcePosition(),
				radius, wave.getGFZeroAngle());
		graphicsObject.drawLine((int) wave.getSourcePosition().x,
				(int) wave.getSourcePosition().y, (int) GFZeroPoint.x,
				(int) GFZeroPoint.y);
		graphicsObject.fillOval((int) wave.getSourcePosition().x - 4,
				(int) wave.getSourcePosition().y - 4, 8, 8);
		graphicsObject.drawOval((int) GFZeroPoint.x - 4,
				(int) GFZeroPoint.y - 4, 8, 8);
		float maxWeight = 0.0000000000001f;
		for (double[] angleWeightAndBandwidth : wave
				.getAnglesWeightsAndBandwidths()) {
			if (angleWeightAndBandwidth[1] > maxWeight)
				maxWeight = (float) angleWeightAndBandwidth[1];
		}
		for (double[] angleWeightAndBandwidth : wave
				.getAnglesWeightsAndBandwidths()) {
			Point2D.Double GFEndPoint = RoboGeom.project(
					wave.getSourcePosition(), radius,
					angleWeightAndBandwidth[0]);
			Color color = new Color(0.7f, 0.1f, 0.1f,
					0.1f + 0.9f * (float) Math.sqrt(angleWeightAndBandwidth[1]
							/ maxWeight));
			graphicsObject.setColor(color);
			graphicsObject.drawOval((int) GFEndPoint.x - 3,
					(int) GFEndPoint.y - 3, 6, 6);
		}
		graphicsObject.setColor(Color.green);
		for (EffectiveBulletShadow shadow : wave.getEffectiveBulletShadows()) {
			Point2D.Double point1 = RoboGeom.project(wave.getSourcePosition(),
					radius, shadow.getClockwiseAngle());
			Point2D.Double point2 = RoboGeom.project(wave.getSourcePosition(),
					radius, shadow.getCounterClockwiseAngle());
			graphicsObject.drawLine((int) point1.x, (int) point1.y,
					(int) point2.x, (int) point2.y);
		}

		double[] anglesToPaint = new double[51];
		for (int i = 0; i < 25; i++) {
			anglesToPaint[i] = wave.getGFZeroAngle() + (i / 25.0)
					* (wave.getGFOneAngle() - wave.getGFZeroAngle());
			anglesToPaint[50 - i] = wave.getGFZeroAngle() + (i / 25.0)
					* (wave.getGFNegOneAngle() - wave.getGFZeroAngle());
		}
		anglesToPaint[25] = wave.getGFZeroAngle();
		double[] dangersToPaint = new double[51];
		double halfSliceWidth = (wave.getGFOneAngle() - wave.getGFNegOneAngle()) / 102.0;
		for (int i = 0; i < 51; i++) {
			dangersToPaint[i] = wave.getDangerForAngles(anglesToPaint[i]
					+ halfSliceWidth, anglesToPaint[i] - halfSliceWidth);
		}

		double maxDanger = 1E-10;
		for (double d : dangersToPaint) {
			if (d > maxDanger)
				maxDanger = d;
		}

		for (int i = 0; i < 51; i++) {
			Point2D.Double GFEndPoint = RoboGeom.project(
					wave.getSourcePosition(),
					radius - wave.getBulletVelocity(), anglesToPaint[i]);
			double danger = dangersToPaint[i];
			Color color = new Color((float) Math.sqrt(danger / maxDanger),
					1.0f - (float) Math.sqrt(danger / maxDanger), 0.1f, 1.0f);
			graphicsObject.setColor(color);
			graphicsObject.fillOval((int) GFEndPoint.x - 2,
					(int) GFEndPoint.y - 2, 4, 4);
			// graphicsObject.drawString("" + danger,(int) GFEndPoint.x + 3,
			// (int) GFEndPoint.y);
		}
	}

	private void markCurrentMovementWave(Graphics2D graphicsObject, long time) {
		if (listOfWaves.size() > 0) {
			MovementDataWave wave = getSoonestBreaksSurfingWave(time);
			if (wave != null) {
				graphicsObject.setColor(Color.darkGray);
				Point2D.Double point1, point2;
				double radius = wave.getBulletVelocity()
						* (time - wave.getFireTime());
				point1 = RoboGeom.project(wave.getSourcePosition(),
						radius - 10, wave.getGFOneAngle());
				point2 = RoboGeom.project(wave.getSourcePosition(),
						radius + 10, wave.getGFOneAngle());
				graphicsObject.drawLine((int) point1.x, (int) point1.y,
						(int) point2.x, (int) point2.y);

				point1 = RoboGeom.project(wave.getSourcePosition(),
						radius - 10, wave.getGFNegOneAngle());
				point2 = RoboGeom.project(wave.getSourcePosition(),
						radius + 10, wave.getGFNegOneAngle());
				graphicsObject.drawLine((int) point1.x, (int) point1.y,
						(int) point2.x, (int) point2.y);
			}

		}

	}
}
