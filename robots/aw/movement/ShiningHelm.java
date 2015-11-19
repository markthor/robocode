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
import java.util.ArrayList;
import java.util.HashMap;

import javax.management.RuntimeErrorException;

import aw.gun.EnemyRecordForGun;
import aw.utils.RoboGeom;
import aw.waves.MovementDataWave;
import aw.waves.PreciseIntersectionWave;
import aw.waves.Wave;
import aw.utils.misc;
import aw.utils.PointDangerRecord;
import aw.utils.RobotState;
import aw.utils.PrecisePredictor;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.AdvancedRobot;
import robocode.SkippedTurnEvent;
import robocode.WinEvent;
import robocode.control.events.RoundEndedEvent;
import robocode.util.Utils;
import robocode.Bullet;

public class ShiningHelm extends Movement {

	private boolean debugging = false;
	private boolean _is1v1 = true;
	private boolean _isMC = false;
	private long calcTime = 0;
	private long checkDangerTime = 0;
	private long pathGenerationTime = 0;
	private long secondWavePathGenerationTime = 0;
	private long firstWaveAnglesTime = 0;
	private long secondWaveDangerTime = 0;
	private long secondWaveAnglesTime = 0;

	
	double battleFieldWidth;
	double battleFieldHeight;
	private static ArrayList<EnemyRecordForMovement> _enemyRecords = new ArrayList<EnemyRecordForMovement>();
	private static HashMap<String, EnemyRecordForMovement> _enemies = new HashMap<String, EnemyRecordForMovement>();

	private static int absTurnNum = 0;

	private Point2D.Double ourCoordinates;
	private Point2D.Double destinationPoint = null;
	private Point2D.Double ourPositionNextTurn;
	private ArrayList<RobotState> ourMovementPath = null;
	private int indexFinalFirstWaveOfPath;
	private int ourIndexMovementPath = 1;

	private ArrayList<Point2D.Double> latestCanidatePoints = new ArrayList<Point2D.Double>();
	private ArrayList<RobotState> latestFirstWaveEstimatePath = new ArrayList<RobotState>();
	private ArrayList<ArrayList<RobotState>> latestFirstWaveCanidatePaths = new ArrayList<ArrayList<RobotState>>();
	private ArrayList<Point2D.Double> latestSecondWaveCanidatePoints = new ArrayList<Point2D.Double>();
	private double[] ourAnglesIntersection = null;
	private double[] dangersOfCanidatePoints = new double[0];
	private double minDanger;
	private long currentTime = 0;
	private long ourLastBulletLoggedTime = 0;
	private long ourLastMoveRecalcTime = 0;

	public ShiningHelm(AdvancedRobot robot, boolean is1v1) {
		super(robot);
		this._is1v1 = is1v1;
		if (is1v1) {
			EnemyRecordForMovement ERecord = new EnemyRecordForMovement(robot,
					is1v1);
			_enemyRecords.add(ERecord);
		}
	}

	public void initRound() {
		battleFieldWidth = robot.getBattleFieldWidth();
		battleFieldHeight = robot.getBattleFieldHeight();
		ourLastBulletLoggedTime = 0;
		ourLastMoveRecalcTime = 0;
		currentTime = 0;
		calcTime = 0;
		checkDangerTime = 0;
		pathGenerationTime = 0;
		secondWavePathGenerationTime = 0;
		firstWaveAnglesTime = 0;
		secondWaveDangerTime = 0;
		secondWaveAnglesTime = 0;
		_enemyRecords.get(0).initRound();
	}

	@Override
	public void run(ScannedRobotEvent e) {
		absTurnNum++;
		ourCoordinates = new Point2D.Double(robot.getX(), robot.getY());
		currentTime = robot.getTime();

		if (_is1v1) {
			EnemyRecordForMovement erm = _enemyRecords.get(0);
			erm.onScannedRobot(e, robot.getVelocity(),
					robot.getHeadingRadians(), robot.getEnergy(),
					ourCoordinates, currentTime, absTurnNum);
			if (erm.getSoonestBreaksSurfingWave(currentTime) == null) {
				moveToGoodPosition();
			} else if (erm.getLastMoveRecalcTime() == currentTime) {
				calculateMovementPath();
			}
			moveOnPath();
		}
		// if (!_enemies.containsKey(e.getName())) {
		// _enemyRecords.add(new EnemyRecordForMovement(robot, _is1v1));
		// }
	}

	private void moveOnPath() {
		if (ourMovementPath == null
				|| ourIndexMovementPath >= ourMovementPath.size()) {

			robot.out.println("movement needed to emergency-regenerate path.");
			if (ourMovementPath == null)
				robot.out.println("ourMovementPath == null");
			else
				robot.out.println("ourIndexMovementPath = "
						+ ourIndexMovementPath + " ourMovementPath.size() = "
						+ ourMovementPath.size());

			EnemyRecordForMovement erm = _enemyRecords.get(0);
			if (erm.getSoonestBreaksSurfingWave(currentTime) == null && erm.getGunHeatWave(currentTime, absTurnNum) == null) {
				moveToGoodPosition();
			} else {
				if (erm.getLastMoveRecalcTime() != currentTime)
					robot.out.println("Last recalc time = "
							+ erm.getLastMoveRecalcTime() + ", current time = "
							+ currentTime);

				MovementDataWave CWave = erm
						.getSoonestBreaksSurfingWave(currentTime);
				robot.out
						.println("time until wave breaks = "
								+ (CWave.getSourcePosition().distance(
										ourCoordinates) - (currentTime - CWave
										.getFireTime())
										* CWave.getBulletVelocity())
								/ CWave.getBulletVelocity());
				calculateMovementPath();
			}
			// throw new RuntimeException(
			// "movement needed to emergency-regenerate ourMovementPath.");
		}

		double neededTurn = Utils.normalRelativeAngle(ourMovementPath.get(
				ourIndexMovementPath).getAbsHeading()
				- robot.getHeadingRadians());
		robot.setMaxVelocity(ourMovementPath.get(ourIndexMovementPath)
				.getVelocity());
		robot.setAhead(Math.signum(ourMovementPath.get(ourIndexMovementPath)
				.getVelocity()) * 40);
		robot.setTurnRightRadians(Utils.normalRelativeAngle(neededTurn));

		// ourPositionNextTurn = ourMovementPath.get(++ourIndexMovementPath)
		// .getCoordinates();

		ourPositionNextTurn = RoboGeom.project(ourCoordinates, ourMovementPath
				.get(ourIndexMovementPath).getVelocity(),
				ourMovementPath.get(ourIndexMovementPath).getAbsHeading());

		// Check path sanity

		int i = ourIndexMovementPath;
		// if
		// (ourMovementPath.get(i).getCoordinates().distance(ourPositionNextTurn)
		// > 0.0001) {
		// robot.out.println("our Path Appears broken : " + i + " out of " +
		// (ourMovementPath.size()-1));
		// robot.out.println("Our actual    State : " + ourCoordinates + "; "
		// + robot.getVelocity() + "; " + robot.getHeadingRadians());
		// robot.out.println("Our current   State : "
		// + ourMovementPath.get(i).getCoordinates() + "; "
		// + ourMovementPath.get(i).getVelocity() + "; "
		// + ourMovementPath.get(i).getAbsHeading());
		// robot.out.println("Our current-1 State : "
		// + ourMovementPath.get(Math.max(i - 1, 0)).getCoordinates()
		// + "; "
		// + ourMovementPath.get(Math.max(i - 1, 0)).getVelocity()
		// + "; "
		// + ourMovementPath.get(Math.max(i - 1, 0)).getAbsHeading());
		// robot.out.println("Our current+1 State : "
		// + ourMovementPath.get(
		// Math.min(i + 1, ourMovementPath.size() - 1))
		// .getCoordinates()
		// + "; "
		// + ourMovementPath.get(
		// Math.min(i + 1, ourMovementPath.size() - 1))
		// .getVelocity()
		// + "; "
		// + ourMovementPath.get(
		// Math.min(i + 1, ourMovementPath.size() - 1))
		// .getAbsHeading() + "\n");
		// }

		ourIndexMovementPath++;

		// robot.out.println("Our Index on Path = " + (ourIndexMovementPath-1) +
		// " ,  Current Time = " + currentTime);
	}

	// unused code. It should be slightly more precise than the current method,
	// so I
	// am keeping it for the moment.

	private void calculateMovementPath() {
		long startTime = System.nanoTime();
		ourLastMoveRecalcTime = currentTime;
		/*
		 * Algorithm:
		 * 
		 * 1) Simulate move until wave passes 2) Get Dangers 3) Sort 4) simulate
		 * move on next wave 5) add new dangers (include extra dangers incurred
		 * on previous wave) 6) do 4-5 until can quit (danger of two waves
		 * greater than danger of one)
		 * 
		 * (for more than 2 waves repeat 2-6 for each wave apply to appropriate
		 * 'level' of movement option (ie. the cutoff could apply to the second
		 * wave but not the first))
		 */

		ArrayList<RobotState> bestPath = new ArrayList<RobotState>();
		double dangerBestPath = Double.POSITIVE_INFINITY;
		if (_is1v1) {
			double ourHeading = robot.getHeadingRadians();
			double ourVelocity = robot.getVelocity();
			EnemyRecordForMovement erm = _enemyRecords.get(0);
			MovementDataWave moveCalcWave = erm
					.getSoonestBreaksSurfingWave(robot.getTime());
			if (moveCalcWave == null) {
				moveCalcWave = erm.getGunHeatWave(robot.getTime(), absTurnNum);
				System.out.println("surfing gun heat wave.");
			}
			if (moveCalcWave != null) {
				MovementDataWave firstWave = moveCalcWave;
				ArrayList<RobotState> firstWaveEstimatePath = new ArrayList<RobotState>();
				ArrayList<ArrayList<RobotState>> firstWaveMovementPaths = new ArrayList<ArrayList<RobotState>>();

				long startT = System.nanoTime();

				firstWaveEstimatePath = PrecisePredictor.predictEstimatePath(
						ourCoordinates.x, ourCoordinates.y, ourHeading,
						ourVelocity, moveCalcWave, currentTime, 160,
						erm.getDesiredDistance(), 1);
				latestFirstWaveEstimatePath = firstWaveEstimatePath;

				for (int index = 0; index < firstWaveEstimatePath.size(); index++) {
					firstWaveMovementPaths.add(PrecisePredictor
							.simulateGoToFromRoughPath(ourCoordinates,
									firstWaveEstimatePath, index, moveCalcWave,
									ourHeading, ourVelocity, 160, 1,
									currentTime));
				}

				firstWaveEstimatePath = PrecisePredictor.predictEstimatePath(
						ourCoordinates.x, ourCoordinates.y, ourHeading,
						ourVelocity, moveCalcWave, currentTime, 160,
						erm.getDesiredDistance(), -1);
				latestFirstWaveEstimatePath.addAll(firstWaveEstimatePath);

				for (int index = 0; index < firstWaveEstimatePath.size(); index++) {
					firstWaveMovementPaths.add(PrecisePredictor
							.simulateGoToFromRoughPath(ourCoordinates,
									firstWaveEstimatePath, index, moveCalcWave,
									ourHeading, ourVelocity, 160, -1,
									currentTime));
				}

				latestFirstWaveCanidatePaths = firstWaveMovementPaths;

				pathGenerationTime += System.nanoTime() - startT;

				double[] dangersOfPaths = new double[firstWaveMovementPaths
						.size()];
				double[][] anglesOfPaths = new double[firstWaveMovementPaths
						.size()][2];

				for (int i = 0; i < dangersOfPaths.length; i++) {
					long ST = System.nanoTime();
					anglesOfPaths[i] = moveCalcWave.getIntersectionAngles(
							firstWaveMovementPaths.get(i), currentTime);
					firstWaveAnglesTime += System.nanoTime() - ST;

					dangersOfPaths[i] = getDangerAngles(anglesOfPaths[i][0],
							anglesOfPaths[i][1], moveCalcWave);
				}

				int[] order = misc.getOrderAscendingDangers(dangersOfPaths);

				// bestPath = firstWaveMovementPaths.get(order[0]);
				// destinationPoint = bestPath.get(bestPath.size() - 1)
				// .getCoordinates();
				// ourAnglesIntersection = anglesOfPaths[order[0]];
				// if (bestPath.size() == 0) {
				// robot.out
				// .println("path length = 0  First wave estimate points = "
				// + latestCanidatePoints.size());
				// }

				// Limit to 5 best points so that it cannot take too long. (I
				// think using more would be rare anyways.)

				// PointDangerRecord dangersPointsSecondWave = new
				// PointDangerRecord();

				for (int i = 0; i < 20 && i < order.length; i++) {

					// for (int i = 0; i < order.length; i++) {
					if (dangerBestPath <= dangersOfPaths[order[i]])
						break;

					ArrayList<RobotState> movementPath = firstWaveMovementPaths
							.get(order[i]);
					long secondWaveStartTime = currentTime
							+ movementPath.size() - 1;
					moveCalcWave = erm.getSoonestBreaksSurfingWave(
							secondWaveStartTime,
							movementPath.get(movementPath.size() - 1)
									.getCoordinates());
					if (moveCalcWave == firstWave) {
						System.out
								.println("Error:  We tried to surf the same wave twice!");
					}
					if (moveCalcWave == null)
						moveCalcWave = erm.getGunHeatWave(
								secondWaveStartTime,
								absTurnNum
										+ (secondWaveStartTime - robot
												.getTime()));
					if (moveCalcWave != null) {

						RobotState initialStateStartSecondPath = movementPath
								.get(movementPath.size() - 1);

						ArrayList<RobotState> secondWaveEstimatePath = new ArrayList<RobotState>();
						ArrayList<ArrayList<RobotState>> secondWaveMovementPaths = new ArrayList<ArrayList<RobotState>>();

						startT = System.nanoTime();

						secondWaveEstimatePath = PrecisePredictor
								.predictEstimatePath(
										initialStateStartSecondPath
												.getCoordinates().x,
										initialStateStartSecondPath
												.getCoordinates().y,
										initialStateStartSecondPath
												.getAbsHeading(),
										initialStateStartSecondPath
												.getVelocity(), moveCalcWave,
										secondWaveStartTime, 160, erm
												.getDesiredDistance(), 1);

						secondWaveMovementPaths.add(PrecisePredictor
								.simulateGoToFromRoughPath(
										initialStateStartSecondPath
												.getCoordinates(),
										secondWaveEstimatePath, 0,
										moveCalcWave,
										initialStateStartSecondPath
												.getAbsHeading(),
										initialStateStartSecondPath
												.getVelocity(), 160, 1,
										secondWaveStartTime));
						secondWaveMovementPaths.add(PrecisePredictor
								.simulateGoToFromRoughPath(
										initialStateStartSecondPath
												.getCoordinates(),
										secondWaveEstimatePath,
										secondWaveEstimatePath.size() - 1,
										moveCalcWave,
										initialStateStartSecondPath
												.getAbsHeading(),
										initialStateStartSecondPath
												.getVelocity(), 160, 1,
										secondWaveStartTime));

						// for (int index = 0; index < secondWaveEstimatePath
						// .size() - 1; index += 7) {
						// secondWaveMovementPaths.add(PrecisePredictor
						// .simulateGoToFromRoughPath(ourCoordinates,
						// secondWaveEstimatePath, index,
						// moveCalcWave, ourHeading,
						// ourVelocity, 160, 1,
						// secondWaveStartTime));
						// }
						//
						// if ((secondWaveEstimatePath.size() - 1) % 7 != 0
						// && secondWaveEstimatePath.size() > 2) {
						// secondWaveMovementPaths.add(PrecisePredictor
						// .simulateGoToFromRoughPath(ourCoordinates,
						// secondWaveEstimatePath,
						// secondWaveEstimatePath.size() - 2,
						// moveCalcWave, ourHeading,
						// ourVelocity, 160, 1,
						// secondWaveStartTime));
						// }
						//
						secondWaveEstimatePath = PrecisePredictor
								.predictEstimatePath(
										initialStateStartSecondPath
												.getCoordinates().x,
										initialStateStartSecondPath
												.getCoordinates().y,
										initialStateStartSecondPath
												.getAbsHeading(),
										initialStateStartSecondPath
												.getVelocity(), moveCalcWave,
										secondWaveStartTime, 160, erm
												.getDesiredDistance(), -1);

						secondWaveMovementPaths.add(PrecisePredictor
								.simulateGoToFromRoughPath(
										initialStateStartSecondPath
												.getCoordinates(),
										secondWaveEstimatePath,
										secondWaveEstimatePath.size() - 1,
										moveCalcWave,
										initialStateStartSecondPath
												.getAbsHeading(),
										initialStateStartSecondPath
												.getVelocity(), 160, -1,
										secondWaveStartTime));

						//
						// for (int index = 7; index < secondWaveEstimatePath
						// .size() - 1; index += 7) {
						// secondWaveMovementPaths.add(PrecisePredictor
						// .simulateGoToFromRoughPath(ourCoordinates,
						// secondWaveEstimatePath, index,
						// moveCalcWave, ourHeading,
						// ourVelocity, 160, -1,
						// secondWaveStartTime));
						// }
						//
						// if ((secondWaveEstimatePath.size() - 1) % 7 != 0
						// && secondWaveEstimatePath.size() > 2) {
						// secondWaveMovementPaths.add(PrecisePredictor
						// .simulateGoToFromRoughPath(ourCoordinates,
						// secondWaveEstimatePath,
						// secondWaveEstimatePath.size() - 2,
						// moveCalcWave, ourHeading,
						// ourVelocity, 160, -1,
						// secondWaveStartTime));
						// }

						secondWavePathGenerationTime += System.nanoTime()
								- startT;
						pathGenerationTime += System.nanoTime() - startT;

						for (ArrayList<RobotState> secondPath : secondWaveMovementPaths) {
							ArrayList<RobotState> fullPath = new ArrayList<RobotState>();
							fullPath.addAll(movementPath);
							fullPath.remove(fullPath.size() - 1);
							fullPath.addAll(secondPath);
							// TODO this should use the two most important waves
							// it can also be optimized
							// TODO bug hunt
							long ST = System.nanoTime();
							double[] newAngles = firstWave
									.getIntersectionAngles(secondPath,
											secondWaveStartTime,
											anglesOfPaths[order[i]][0],
											anglesOfPaths[order[i]][1]);
							firstWaveAnglesTime += System.nanoTime() - ST;

							double danger;
							if (newAngles[0] == anglesOfPaths[order[i]][0]
									&& newAngles[1] == anglesOfPaths[order[i]][1])
								danger = dangersOfPaths[order[i]];
							else
								danger = getDangerAngles(newAngles[0],
										newAngles[1], firstWave);
							long SWSTime = System.nanoTime();

							// Double SWDanger = dangersPointsSecondWave
							// .getDanger(fullPath.get(fullPath.size() -
							// 1).getCoordinates());
							// if (SWDanger == -1.0) {
							// SWDanger = getDangerMovementPath(fullPath,
							// moveCalcWave, currentTime);
							// dangersPointsSecondWave
							// .add(fullPath.get(fullPath.size() -
							// 1).getCoordinates(), SWDanger);
							// }
							// danger += SWDanger;
							danger += getDangerMovementPath(fullPath,
									moveCalcWave, currentTime);

							secondWaveDangerTime += System.nanoTime() - SWSTime;
							if (danger < dangerBestPath) {
								dangerBestPath = danger;
								bestPath = fullPath;
								destinationPoint = movementPath.get(
										movementPath.size() - 1)
										.getCoordinates();
								indexFinalFirstWaveOfPath = movementPath.size() - 1;
								ourAnglesIntersection = newAngles;
								if (bestPath.size() == 0) {
									robot.out
											.println("path length = 0  First wave estimate points = "
													+ latestCanidatePoints
															.size());
								}
							}
						}
					} else {
						MovementDataWave[] MDWaves = erm
								.getNNearestSurifingWavesForPoint(
										2,
										movementPath.get(
												movementPath.size() - 1)
												.getCoordinates(), currentTime);

						if (MDWaves[0] == null) {
							System.out
									.println("Error: Our path is broken! Found while trying to surf two waves.");
						} else if (MDWaves[0] != firstWave) {
							moveCalcWave = MDWaves[0];
						} else if (MDWaves[1] != null) {
							moveCalcWave = MDWaves[1];
						} else {
							bestPath = firstWaveMovementPaths.get(order[0]);
							destinationPoint = bestPath
									.get(bestPath.size() - 1).getCoordinates();
							indexFinalFirstWaveOfPath = bestPath.size() - 1;
							ourAnglesIntersection = anglesOfPaths[order[0]];
							if (bestPath.size() == 0) {
								robot.out
										.println("path length = 0  First wave estimate points = "
												+ latestCanidatePoints.size());
								break;
							}
							break;
						}

						double danger;
						if ((danger = getDangerMovementPath(
								firstWaveMovementPaths.get(order[i]),
								firstWave, currentTime)
								+ getDangerMovementPath(
										firstWaveMovementPaths.get(order[i]),
										moveCalcWave, currentTime)) < dangerBestPath) {
							dangerBestPath = danger;
							bestPath = firstWaveMovementPaths.get(order[i]);
							destinationPoint = movementPath.get(
									movementPath.size() - 1).getCoordinates();
							indexFinalFirstWaveOfPath = movementPath.size() - 1;
							ourAnglesIntersection = firstWave
									.getIntersectionAngles(
											firstWaveMovementPaths
													.get(order[i]), currentTime);
							if (bestPath.size() == 0) {
								robot.out
										.println("path length = 0  First wave estimate points = "
												+ latestCanidatePoints.size());
							}
						}
					}
				}
				firstWave.anglePassedDebugging = ourAnglesIntersection;

			} else {
				// what to do if wave surfing with no waves. This should never
				// happen at present.

				robot.out.println("error movementWave is null");
			}

		}

		// RobotState previousState = bestPath.get(0);
		// for (int i = 1; i < bestPath.size(); i++) {
		// if (bestPath
		// .get(i)
		// .getCoordinates()
		// .distance(RoboGeom.project(previousState.getCoordinates(),
		// bestPath.get(i).getVelocity(), bestPath.get(i)
		// .getAbsHeading())) > 0.1) {
		// System.out.println("Our movement Path is broken. " + i + " of "
		// + (bestPath.size() - 1));
		// System.out.println("PreviousState : "
		// + previousState.getCoordinates() + ", "
		// + previousState.getVelocity() + ", "
		// + previousState.getAbsHeading());
		// System.out.println("CurrentState : "
		// + bestPath.get(i).getCoordinates() + ", "
		// + bestPath.get(i).getVelocity() + ", "
		// + bestPath.get(i).getAbsHeading());
		//
		// // break;
		// }
		// previousState = bestPath.get(i);
		// }
		ourIndexMovementPath = 1;
		ourMovementPath = bestPath;
		calcTime += System.nanoTime() - startTime;
	}

	private void calculateMovementPathImprecise() {

		boolean firstWaveCalcStarted = false;
		boolean secondWaveCalcStarted = false;
		boolean firstWaveLoopRun = false;
		boolean secondWaveLoopRun = false;
		boolean pointInSecondWaveRectangles = false;

		long startTime = System.nanoTime();
		ourLastMoveRecalcTime = currentTime;

		ArrayList<RobotState> bestPath = new ArrayList<RobotState>();
		double dangerBestPath = Double.POSITIVE_INFINITY;
		if (_is1v1) {
			double ourHeading = robot.getHeadingRadians();
			double ourVelocity = robot.getVelocity();
			EnemyRecordForMovement erm = _enemyRecords.get(0);
			MovementDataWave moveCalcWave = erm
					.getSoonestBreaksSurfingWave(robot.getTime());
			if (moveCalcWave != null) {

				firstWaveCalcStarted = true;

				MovementDataWave firstWave = moveCalcWave;
				ArrayList<RobotState> firstWaveEstimatePath = new ArrayList<RobotState>();
				ArrayList<ArrayList<RobotState>> firstWaveMovementPaths = new ArrayList<ArrayList<RobotState>>();

				long startT = System.nanoTime();

				firstWaveEstimatePath = PrecisePredictor.predictEstimatePath(
						ourCoordinates.x, ourCoordinates.y, ourHeading,
						ourVelocity, moveCalcWave, currentTime, 160,
						erm.getDesiredDistance(), 1);
				latestFirstWaveEstimatePath = firstWaveEstimatePath;

				for (int index = 0; index < firstWaveEstimatePath.size(); index++) {
					firstWaveMovementPaths.add(PrecisePredictor
							.simulateGoToFromRoughPath(ourCoordinates,
									firstWaveEstimatePath, index, moveCalcWave,
									ourHeading, ourVelocity, 160, 1,
									currentTime));
				}

				int firstWaveMovementPathGFOneIndex = firstWaveMovementPaths
						.size() - 1;

				firstWaveEstimatePath = PrecisePredictor.predictEstimatePath(
						ourCoordinates.x, ourCoordinates.y, ourHeading,
						ourVelocity, moveCalcWave, currentTime, 160,
						erm.getDesiredDistance(), -1);
				latestFirstWaveEstimatePath.addAll(firstWaveEstimatePath);

				for (int index = 0; index < firstWaveEstimatePath.size(); index++) {
					firstWaveMovementPaths.add(PrecisePredictor
							.simulateGoToFromRoughPath(ourCoordinates,
									firstWaveEstimatePath, index, moveCalcWave,
									ourHeading, ourVelocity, 160, -1,
									currentTime));
				}

				latestFirstWaveCanidatePaths = firstWaveMovementPaths;

				pathGenerationTime += System.nanoTime() - startT;

				double[] dangersOfPaths = new double[firstWaveMovementPaths
						.size()];
				double[][] anglesOfPaths = new double[firstWaveMovementPaths
						.size()][2];

				for (int i = 0; i < dangersOfPaths.length; i++) {
					long ST = System.nanoTime();
					anglesOfPaths[i] = moveCalcWave.getIntersectionAngles(
							firstWaveMovementPaths.get(i), currentTime);
					firstWaveAnglesTime += System.nanoTime() - ST;

					dangersOfPaths[i] = getDangerAngles(anglesOfPaths[i][0],
							anglesOfPaths[i][1], moveCalcWave);
				}

				int[] order = misc.getOrderAscendingDangers(dangersOfPaths);

				// bestPath = firstWaveMovementPaths.get(order[0]);
				// destinationPoint = bestPath.get(bestPath.size() - 1)
				// .getCoordinates();
				// ourAnglesIntersection = anglesOfPaths[order[0]];
				// if (bestPath.size() == 0) {
				// robot.out
				// .println("path length = 0  First wave estimate points = "
				// + latestCanidatePoints.size());
				// }

				// Limit to 5 best points so that it cannot take too long. (I
				// think using more would be rare anyways.)

				// PointDangerRecord dangersPointsSecondWave = new
				// PointDangerRecord();

				ArrayList<RobotState> movementPath = firstWaveMovementPaths
						.get(order[0]);
				long secondWaveStartTime = currentTime + movementPath.size()
						- 1;

				MovementDataWave secondWave = null;
				MovementDataWave[] wavesToSurf = erm
						.getNNearestSurifingWavesForPoint(2, ourCoordinates,
								currentTime);
				// System.out.println((wavesToSurf[0] == wavesToSurf[1]));
				if (wavesToSurf[1] != null) {
					secondWave = wavesToSurf[1];
				}
				if (secondWave != null) {

					secondWaveCalcStarted = true;

					ArrayList<Point2D.Double> secondWavePoints = new ArrayList<Point2D.Double>();

					for (int i = 0; i < latestFirstWaveEstimatePath.size(); i++) {
						secondWavePoints.add(latestFirstWaveEstimatePath.get(i)
								.getCoordinates());
					}

					RobotState state = firstWaveMovementPaths
							.get(firstWaveMovementPathGFOneIndex)
							.get(firstWaveMovementPaths.get(
									firstWaveMovementPathGFOneIndex).size() - 1);

					secondWavePoints.addAll(PrecisePredictor
							.predictEstimatePathLocations(
									state.getCoordinates().x,
									state.getCoordinates().y,
									state.getAbsHeading(), state.getVelocity(),
									secondWave, secondWaveStartTime, 160,
									erm.getDesiredDistance(), 1));
					secondWavePoints.addAll(PrecisePredictor
							.predictEstimatePathLocations(
									state.getCoordinates().x,
									state.getCoordinates().y,
									state.getAbsHeading(), state.getVelocity(),
									secondWave, secondWaveStartTime, 160,
									erm.getDesiredDistance(), -1));

					state = firstWaveMovementPaths
							.get(firstWaveMovementPaths.size() - 1)
							.get(firstWaveMovementPaths.get(
									firstWaveMovementPaths.size() - 1).size() - 1);

					secondWavePoints.addAll(PrecisePredictor
							.predictEstimatePathLocations(
									state.getCoordinates().x,
									state.getCoordinates().y,
									state.getAbsHeading(), state.getVelocity(),
									secondWave, secondWaveStartTime, 160,
									erm.getDesiredDistance(), 1));
					secondWavePoints.addAll(PrecisePredictor
							.predictEstimatePathLocations(
									state.getCoordinates().x,
									state.getCoordinates().y,
									state.getAbsHeading(), state.getVelocity(),
									secondWave, secondWaveStartTime, 160,
									erm.getDesiredDistance(), -1));

					// secondWavePoints.addAll(PrecisePredictor
					// .predictEstimatePathLocations(ourCoordinates.x,
					// ourCoordinates.y, ourHeading, ourVelocity,
					// secondWave, currentTime, 160,
					// erm.getDesiredDistance(), 1));
					// secondWavePoints.addAll(PrecisePredictor
					// .predictEstimatePathLocations(ourCoordinates.x,
					// ourCoordinates.y, ourHeading, ourVelocity,
					// secondWave, currentTime, 160,
					// erm.getDesiredDistance(), -1));
					//
					// for (int i = secondWavePoints.size() - 1; i > 0; i--) {
					// if (secondWavePoints.get(i).distanceSq(
					// secondWavePoints.get(i - 1)) < 4.0)
					// secondWavePoints.remove(i);
					// }

					latestSecondWaveCanidatePoints = secondWavePoints;

					double[] dangersPointsSecondWave = new double[secondWavePoints
							.size()];
					for (int i = 0; i < secondWavePoints.size(); i++) {
						dangersPointsSecondWave[i] = getDangerPoint(
								secondWavePoints.get(i), secondWave);
					}

					int[] orderSecondWavePoints = misc
							.getOrderAscendingDangers(dangersPointsSecondWave);

					for (int i = 0; i < order.length; i++) {

						firstWaveLoopRun = true;

						if (dangerBestPath <= dangersOfPaths[order[i]]
								+ dangersPointsSecondWave[orderSecondWavePoints[0]])
							break;

						movementPath = firstWaveMovementPaths.get(order[i]);
						RobotState initialStateStartSecondPath = movementPath
								.get(movementPath.size() - 1);

						double[][] GFOneArray = PrecisePredictor
								.predictEstimatePathMBR(
										initialStateStartSecondPath
												.getCoordinates().x,
										initialStateStartSecondPath
												.getCoordinates().y,
										initialStateStartSecondPath
												.getAbsHeading(),
										initialStateStartSecondPath
												.getVelocity(), moveCalcWave,
										secondWaveStartTime, 160, erm
												.getDesiredDistance(), 1);
						double[][] GFNegOneArray = PrecisePredictor
								.predictEstimatePathMBR(
										initialStateStartSecondPath
												.getCoordinates().x,
										initialStateStartSecondPath
												.getCoordinates().y,
										initialStateStartSecondPath
												.getAbsHeading(),
										initialStateStartSecondPath
												.getVelocity(), moveCalcWave,
										secondWaveStartTime, 160, erm
												.getDesiredDistance(), -1);

						double[][] finalArray = new double[2][2];
						finalArray[0][0] = Math.min(GFOneArray[0][0],
								GFNegOneArray[0][0]);
						finalArray[0][1] = Math.min(GFOneArray[0][1],
								GFNegOneArray[0][1]);
						finalArray[1][0] = Math.max(GFOneArray[1][0],
								GFNegOneArray[1][0]);
						finalArray[1][1] = Math.max(GFOneArray[1][1],
								GFNegOneArray[1][1]);

						int pointsInRectangle = 0;
						for (int j = 0; j < orderSecondWavePoints.length; j++) {

							secondWaveLoopRun = true;

							if (dangerBestPath <= dangersOfPaths[order[i]]
									+ dangersPointsSecondWave[orderSecondWavePoints[j]])
								break;

							Point2D.Double secondWaveDestPoint = secondWavePoints
									.get(orderSecondWavePoints[j]);
							double xCoord = secondWaveDestPoint.x;
							double yCoord = secondWaveDestPoint.y;
							// TODO fix this, we need to be able to guarantee
							// that a reasonable number of points will be in
							// this rectangle.
							if (xCoord > finalArray[0][0]
									&& yCoord > finalArray[0][1]
									&& xCoord < finalArray[1][0]
									&& yCoord < finalArray[1][1]) {

								pointsInRectangle++;

								pointInSecondWaveRectangles = true;

								ArrayList<RobotState> fullPath = new ArrayList<RobotState>();
								fullPath.addAll(movementPath);
								fullPath.remove(fullPath.size() - 1);
								ArrayList<RobotState> secondPath = PrecisePredictor
										.simulateRawGoToForSecondWave(
												secondWavePoints
														.get(orderSecondWavePoints[j]),
												initialStateStartSecondPath
														.getCoordinates(),
												secondWave,
												initialStateStartSecondPath
														.getAbsHeading(),
												initialStateStartSecondPath
														.getVelocity(),
												secondWave.getFireTime()
														+ timeUntilWaveReachesPoint(
																secondWave,
																secondWaveDestPoint)
														- secondWaveStartTime);
								fullPath.addAll(secondPath);

								long ST = System.nanoTime();
								double[] newAngles = firstWave
										.getIntersectionAngles(secondPath,
												secondWaveStartTime,
												anglesOfPaths[order[i]][0],
												anglesOfPaths[order[i]][1]);
								firstWaveAnglesTime += System.nanoTime() - ST;

								double danger;
								if (newAngles[0] == anglesOfPaths[order[i]][0]
										&& newAngles[1] == anglesOfPaths[order[i]][1])
									danger = dangersOfPaths[order[i]];
								else {
									danger = getDangerAngles(newAngles[0],
											newAngles[1], firstWave);
								}

								if (secondWaveDestPoint.distance(secondPath
										.get(secondPath.size() - 1)
										.getCoordinates()) > 1.0) {
									danger += getDangerMovementPath(fullPath,
											secondWave, currentTime);
									System.out
											.println("Destination on second wave wasn't reached!");
								} else {
									danger += dangersPointsSecondWave[orderSecondWavePoints[j]];
								}

								if (danger < dangerBestPath) {
									dangerBestPath = danger;
									bestPath = fullPath;
									destinationPoint = movementPath.get(
											movementPath.size() - 1)
											.getCoordinates();
									indexFinalFirstWaveOfPath = movementPath
											.size() - 1;
									ourAnglesIntersection = newAngles;
									if (bestPath.size() == 0) {
										robot.out
												.println("path length = 0  First wave estimate points = "
														+ latestCanidatePoints
																.size());
									}
								}
							} else
								continue;
						}

						if (pointsInRectangle == 0 || pointsInRectangle < 3
								&& secondWavePoints.size() > 15) {

							// System.out.println("We had to use extra points!");

							Point2D.Double secondWaveDestPoint = initialStateStartSecondPath
									.getCoordinates();

							ArrayList<RobotState> fullPath = new ArrayList<RobotState>();
							fullPath.addAll(movementPath);
							fullPath.remove(fullPath.size() - 1);
							ArrayList<RobotState> secondPath = PrecisePredictor
									.simulateRawGoToForSecondWave(
											secondWaveDestPoint,
											initialStateStartSecondPath
													.getCoordinates(),
											secondWave,
											initialStateStartSecondPath
													.getAbsHeading(),
											initialStateStartSecondPath
													.getVelocity(),
											secondWave.getFireTime()
													+ timeUntilWaveReachesPoint(
															secondWave,
															secondWaveDestPoint)
													- secondWaveStartTime);
							fullPath.addAll(secondPath);

							long ST = System.nanoTime();
							double[] newAngles = firstWave
									.getIntersectionAngles(secondPath,
											secondWaveStartTime,
											anglesOfPaths[order[i]][0],
											anglesOfPaths[order[i]][1]);
							firstWaveAnglesTime += System.nanoTime() - ST;

							double danger;
							if (newAngles[0] == anglesOfPaths[order[i]][0]
									&& newAngles[1] == anglesOfPaths[order[i]][1])
								danger = dangersOfPaths[order[i]];
							else {
								danger = getDangerAngles(newAngles[0],
										newAngles[1], firstWave);
							}

							danger += getDangerMovementPath(fullPath,
									secondWave, currentTime);

							if (danger < dangerBestPath) {
								dangerBestPath = danger;
								bestPath = fullPath;
								destinationPoint = movementPath.get(
										movementPath.size() - 1)
										.getCoordinates();
								indexFinalFirstWaveOfPath = movementPath.size() - 1;
								ourAnglesIntersection = newAngles;
								if (bestPath.size() == 0) {
									robot.out
											.println("path length = 0  First wave estimate points = "
													+ latestCanidatePoints
															.size());
								}
							}
						}
					}
				} else {

					bestPath = firstWaveMovementPaths.get(order[0]);
					destinationPoint = bestPath.get(bestPath.size() - 1)
							.getCoordinates();
					indexFinalFirstWaveOfPath = bestPath.size() - 1;
					ourAnglesIntersection = anglesOfPaths[order[0]];
					if (bestPath.size() == 0) {
						robot.out
								.println("path length = 0  First wave estimate points = "
										+ latestCanidatePoints.size());
					}
				}

			} else {
				// what to do if wave surfing with no waves. This should never
				// happen at present.

				robot.out.println("error movementWave is null");
			}
		}

		// RobotState previousState = bestPath.get(0);
		// for (int i = 1; i < bestPath.size(); i++) {
		// if (!bestPath
		// .get(i)
		// .getCoordinates()
		// .equals(RoboGeom.project(previousState.getCoordinates(),
		// bestPath.get(i).getVelocity(), bestPath.get(i)
		// .getAbsHeading()))) {
		// System.out.println("Our movement Path is broken. " + i + " of "
		// + (bestPath.size() - 1));
		// System.out.println("PreviousState : "
		// + previousState.getCoordinates() + ", "
		// + previousState.getVelocity() + ", "
		// + previousState.getAbsHeading());
		// System.out.println("CurrentState : "
		// + bestPath.get(i).getCoordinates() + ", "
		// + bestPath.get(i).getVelocity() + ", "
		// + bestPath.get(i).getAbsHeading());
		//
		// // break;
		// }
		// previousState = bestPath.get(i);
		// }

		if (bestPath.size() == 0)
			System.out
					.println("calcMovePathCalled but path size = 0      First:"
							+ firstWaveCalcStarted + "    Second: "
							+ secondWaveCalcStarted + "    FirstWaveLoop: "
							+ firstWaveLoopRun + "    SecondWaveLoop: "
							+ secondWaveLoopRun
							+ "    PointInSecondWaveRectangle: "
							+ pointInSecondWaveRectangles);

		ourIndexMovementPath = 1;
		ourMovementPath = bestPath;
		calcTime += System.nanoTime() - startTime;
	}

	private double getDangerAngles(double clockwiseAngle,
			double counterClockwiseAngle, MovementDataWave wave) {
		long startTime = System.nanoTime();
		double danger = 0;
		// TODO use actual times for movement path?
		danger = Rules.getBulletDamage(wave.getBulletPower())
				* wave.getDangerForAngles(clockwiseAngle, counterClockwiseAngle)
				/ Math.max(1.0, timeUntilWaveBreaks(wave));
		// if (timeUntilWaveBreaks(wave) < 0) {
		// robot.out.println("Time Until Wave Breaks is Negative");
		// }
		checkDangerTime += System.nanoTime() - startTime;
		return danger;
	}

	private double getDangerMovementPath(ArrayList<RobotState> path,
			MovementDataWave wave, long pathStartTime) {
		long startTime = System.nanoTime();
		double danger = 0;
		// TODO use actual times for movement path?
		danger = Rules.getBulletDamage(wave.getBulletPower())
				* wave.checkDangerEstimate(path, pathStartTime)
				/ Math.max(1.0, timeUntilWaveBreaks(wave));
		// if (timeUntilWaveBreaks(wave) < 0) {
		// robot.out.println("Time Until Wave Breaks is Negative");
		// }
		checkDangerTime += System.nanoTime() - startTime;
		return danger;
	}

	private double getDangerPoint(Point2D.Double point, MovementDataWave wave) {
		long startTime = System.nanoTime();
		double danger = 0;
		danger = Rules.getBulletDamage(wave.getBulletPower())
				* wave.checkDangerEstimateForPoint(point)
				/ Math.max(1.0, timeUntilWaveBreaks(wave));
		checkDangerTime += System.nanoTime() - startTime;
		return danger;
	}

	private double timeUntilWaveBreaks(Wave wave) {
		return ((wave.getSourcePosition().distance(ourPositionNextTurn) - (robot
				.getTime() - wave.getFireTime()) * wave.getBulletVelocity()) / wave
					.getBulletVelocity());
	}

	public void ourBulletFired(double bulletVelocity, double bulletAbsHeading,
			Point2D.Double bulletStartingPosition, long fireTime, Bullet bullet) {
		ourLastBulletLoggedTime = fireTime;
		if (_is1v1) {
			_enemyRecords.get(0).ourBulletFired(bulletAbsHeading,
					bulletVelocity, bulletStartingPosition, fireTime,
					robot.getTime(), bullet);
		}
	}

	public void onHitByBullet(HitByBulletEvent e) {
		if (_is1v1) {
			_enemyRecords.get(0).onHitByBullet(e, e.getTime());
		}
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		if (_is1v1) {
			_enemyRecords.get(0).onBulletHitBullet(e, e.getTime());
		}
	}

	public void onBulletHit(BulletHitEvent e) {
		if (_is1v1) {
			_enemyRecords.get(0).onBulletHit(e);
		}
	}

	public void onHitRobot(HitRobotEvent e) {
		if (_is1v1) {
			_enemyRecords.get(0).onHitRobot(e);
		}
		if (_enemyRecords.get(0).getSoonestBreaksSurfingWave(currentTime) == null)
			moveToGoodPosition();
		else
			calculateMovementPath();
	}

	public void onHitWall(HitWallEvent e) {
		if (_is1v1) {

		}
		if (_enemyRecords.get(0).getSoonestBreaksSurfingWave(currentTime) == null)
			moveToGoodPosition();
		else
			calculateMovementPath();
	}

	public void onWin(WinEvent e) {
		if (_is1v1) {
			_enemyRecords.get(0).onWin(e, robot.getEnergy(), e.getTime(),
					absTurnNum);
		}
	}

	public void onSkippedTurn(SkippedTurnEvent e) {
		if (_is1v1) {
			if (ourLastMoveRecalcTime < _enemyRecords.get(0)
					.getLastMoveRecalcTime()) {
				calculateMovementPath();
			}
		}
	}

	public void onRoundEnded() {
		System.out.println("-----MOVEMENT-----");
		_enemyRecords.get(0).onRoundEnded();
		if (debugging) {
			System.out.println("Move Calc time = " + calcTime * 1E-6);
			System.out.println("Path generation time = " + pathGenerationTime
					* 1E-6);
			System.out.println("Second wave path generation time = "
					+ secondWavePathGenerationTime * 1E-6);
			System.out.println("First wave angles time = "
					+ firstWaveAnglesTime * 1E-6);
			System.out.println("Second wave danger time = "
					+ secondWaveDangerTime * 1E-6);
			System.out.println("Check Danger time = " + checkDangerTime * 1E-6);
		}
	}

	private long timeUntilWaveReachesPoint(MovementDataWave wave,
			Point2D.Double point) {
		return 1 + (long) ((wave.getSourcePosition().distance(point) - (robot
				.getTime() - wave.getFireTime()) * wave.getBulletVelocity()) / wave
					.getBulletVelocity());
	}

	public Point2D.Double getOurPositionNextTurn() {
		return ourPositionNextTurn;
	}

	public ArrayList<RobotState> getOurMovementPath() {
		return ourMovementPath;
	}

	public int getIndexEndAccuratePath() {
		return indexFinalFirstWaveOfPath;
	}

//	TODO good antiRam movement
	private void moveToGoodPosition() {
		ArrayList<Point2D.Double> canidatePositionPoints = new ArrayList<Point2D.Double>();
		for (int i = 0; i < 60; i++) {
			double projectionAngle = i / 30.0 * Math.PI;
			double maxProjectionDistance;
			EnemyRecordForMovement erm = _enemyRecords.get(0);
			if (erm.getEnemyCoordinates() == null)
				maxProjectionDistance = 160.0;
			else
				maxProjectionDistance = Math.min(_enemyRecords.get(0)
						.getEnemyCoordinates().distance(ourCoordinates), 250.0);
			for (int j = 1; j < 10; j++) {
				double projectionDistance = j / 10.0 * maxProjectionDistance;
				double projectedX, projectedY;
				if ((projectedX = ourCoordinates.x + Math.sin(projectionAngle)
						* projectionDistance) > battleFieldWidth - 18)
					projectedX = battleFieldWidth - 18;
				else if (projectedX < 18)
					projectedX = 18;
				if ((projectedY = ourCoordinates.y + Math.cos(projectionAngle)
						* projectionDistance) > battleFieldHeight - 18)
					projectedY = battleFieldHeight - 18;
				else if (projectedY < 18)
					projectedY = 18;

				canidatePositionPoints.add(new Point2D.Double(projectedX,
						projectedY));
			}
		}
		latestCanidatePoints = canidatePositionPoints;
		dangersOfCanidatePoints = new double[canidatePositionPoints.size()];
		minDanger = evaluatePosition(canidatePositionPoints.get(0));
		dangersOfCanidatePoints[0] = evaluatePosition(canidatePositionPoints
				.get(0));
		destinationPoint = canidatePositionPoints.get(0);
		for (int i = 1; i < canidatePositionPoints.size(); i++) {
			dangersOfCanidatePoints[i] = evaluatePosition(canidatePositionPoints
					.get(i));
			if (dangersOfCanidatePoints[i] < minDanger) {
				minDanger = dangersOfCanidatePoints[i];
				destinationPoint = canidatePositionPoints.get(i);
			}
		}

		if (Utils.normalRelativeAngle(robot.getHeadingRadians()
				- RoboGeom.getBearing(ourCoordinates, destinationPoint)) <= Math.PI / 2.0)
			ourMovementPath = PrecisePredictor.simulateRawGoTo(
					destinationPoint, ourCoordinates,
					robot.getHeadingRadians(), robot.getVelocity(), 20, 160, 1);
		else
			ourMovementPath = PrecisePredictor
					.simulateRawGoTo(destinationPoint, ourCoordinates,
							robot.getHeadingRadians(), robot.getVelocity(), 50,
							160, -1);
		ourIndexMovementPath = 1;

		indexFinalFirstWaveOfPath = ourMovementPath.size() - 1;

		ourAnglesIntersection = null;

	}

	private double evaluatePosition(Point2D.Double position) {
		double danger = 0.00001;
		if (_is1v1) {
			EnemyRecordForMovement erm = _enemyRecords.get(0);
			
			double ourPracticalHeading = erm.ourRobot.getHeadingRadians();
			if (erm.ourRobot.getVelocity() < 0)
				ourPracticalHeading = Utils
						.normalAbsoluteAngle(ourPracticalHeading + Math.PI);
			
			double vertAngle = Math.cos(ourPracticalHeading);
			double horAngle = Math.sin(ourPracticalHeading);
			
			double dist = position.x - 17.0;
			danger += Math.abs(horAngle) / (dist * dist);
			dist = battleFieldWidth - position.x - 17.0;
			danger += Math.abs(horAngle) / (dist * dist);
			dist = position.y - 17.0;
			danger += Math.abs(vertAngle) / (dist * dist);
			dist = battleFieldHeight - position.y - 17.0;
			danger += Math.abs(vertAngle) / (dist * dist);
			
			
			dist = position.x - 17.9;
			danger += 1 / (dist * dist);
			dist = battleFieldWidth - position.x - 17.9;
			danger += 1 / (dist * dist);
			dist = position.y - 17.9;
			danger += 1 / (dist * dist);
			dist = battleFieldHeight - position.y - 17.9;
			danger += 1 / (dist * dist);
			



			
			 Point2D.Double dangerPoint = new Point2D.Double(17.9, 17.9);
			 danger += 100.0 / dangerPoint.distance(ourCoordinates);
			 dangerPoint = new Point2D.Double(17.9, 582.1);
			 danger += 100.0 / dangerPoint.distance(ourCoordinates);
			 dangerPoint = new Point2D.Double(782.1, 17.9);
			 danger += 100.0 / dangerPoint.distance(ourCoordinates);
			 dangerPoint = new Point2D.Double(782.1, 582.1);
			 danger += 100.0 / dangerPoint.distance(ourCoordinates);

			// TODO use desired distance
			if (erm.getEnemyCoordinates() != null) {
				danger += (erm.getEnemyEnergy())
						/ Point2D.distanceSq(position.x, position.y,
								erm.getEnemyCoordinates().x,
								erm.getEnemyCoordinates().y);
				if (erm.getEnemyCoordinates().distance(ourCoordinates) > 75.0) {
					danger += (erm.getEnemyEnergy())
							/ RoboGeom.project(
									erm.getEnemyCoordinates(),
									erm.getEnemyCoordinates().distance(
											ourCoordinates) * 0.9,
									erm.getEnemyPracticalHeading()).distanceSq(
									position);

					danger += (erm.getEnemyEnergy())
							/ RoboGeom.project(
									erm.getEnemyCoordinates(),
									erm.getEnemyCoordinates().distance(
											ourCoordinates) * 2.1,
									RoboGeom.getBearing(
											erm.getEnemyCoordinates(),
											ourCoordinates)).distanceSq(position);
				}

			}
		}

		return danger;
	}

	public void onPaint(Graphics2D graphicsObject, long time) {
		paintCanidatePoints(graphicsObject);
		paintPath(graphicsObject);

		_enemyRecords.get(0).onPaint(graphicsObject, time);
	}

	private void paintCanidatePoints(Graphics2D graphicsObject) {
		EnemyRecordForMovement erm = _enemyRecords.get(0);
		if (erm.getSoonestBreaksSurfingWave(robot.getTime()) != null) {
			graphicsObject.setColor(Color.green);
			for (ArrayList<RobotState> path : latestFirstWaveCanidatePaths) {
				Point2D.Double point = path.get(path.size() - 1)
						.getCoordinates();
				graphicsObject.drawOval((int) point.x - 3, (int) point.y - 3,
						6, 6);
			}
			graphicsObject.setColor(Color.lightGray);
			for (RobotState state : latestFirstWaveEstimatePath) {
				graphicsObject.drawOval((int) state.getCoordinates().x - 2,
						(int) state.getCoordinates().y - 2, 4, 4);
			}

			if (latestSecondWaveCanidatePoints != null) {
				graphicsObject.setColor(Color.cyan);

				// Point2D.Double previousPoint = ourCoordinates;

				for (Point2D.Double point : latestSecondWaveCanidatePoints) {
					graphicsObject.drawOval((int) point.x - 2,
							(int) point.y - 2, 4, 4);
					// graphicsObject.drawLine((int) previousPoint.x, (int)
					// previousPoint.y, (int) state.getCoordinates().x, (int)
					// state.getCoordinates().y);
					// previousPoint = state.getCoordinates();
				}
			}

		} else {
			for (int i = 0; i < latestCanidatePoints.size(); i++) {
				if ((float) (minDanger / dangersOfCanidatePoints[i]) > 1 // TODO
																			// find
																			// bug
						|| (float) (minDanger / dangersOfCanidatePoints[i]) < 0) {
					robot.out
							.println((float) (minDanger / dangersOfCanidatePoints[i]));
					robot.out.println(minDanger);
					robot.out.println(dangersOfCanidatePoints[i]);
				}
				Color color = new Color(
						0.1f,
						0.7f,
						0.0f,
						0.1f + 0.9f * (float) (minDanger / dangersOfCanidatePoints[i]));
				graphicsObject.setColor(color);
				graphicsObject.drawOval(
						(int) latestCanidatePoints.get(i).x - 2,
						(int) latestCanidatePoints.get(i).y - 2, 4, 4);
			}
		}
	}

	private void paintPath(Graphics2D graphicsObject) {
		if (destinationPoint != null) {
			Color color = new Color(0.62f, 0.62f, 0.7f, 0.2f);
			graphicsObject.setColor(color);

			// Point2D.Double previousPoint = ourCoordinates;

			for (RobotState state : ourMovementPath) {
				graphicsObject.drawOval((int) state.getCoordinates().x - 6,
						(int) state.getCoordinates().y - 6, 12, 12);
				// graphicsObject.drawLine((int) previousPoint.x, (int)
				// previousPoint.y, (int) state.getCoordinates().x, (int)
				// state.getCoordinates().y);
				// previousPoint = state.getCoordinates();
			}

		}

		Color color = new Color(0.62f, 0.62f, 0.7f, 0.9f);
		graphicsObject.setColor(color);
		graphicsObject.drawRect((int) destinationPoint.x - 18,
				(int) destinationPoint.y - 18, 36, 36);
		EnemyRecordForMovement erm = _enemyRecords.get(0);
		if (erm.getSoonestBreaksSurfingWave(robot.getTime()) != null) {
			if (ourAnglesIntersection == null) {
				robot.out.println("Error: currentTime = "
						+ robot.getTime()
						+ " bestSurfingWave time until break: "
						+ timeUntilWaveBreaks(erm
								.getSoonestBreaksSurfingWave(robot.getTime())));
			}
			Point2D.Double point1 = RoboGeom
					.project(
							erm.getSoonestBreaksSurfingWave(robot.getTime())
									.getSourcePosition(),
							erm.getSoonestBreaksSurfingWave(robot.getTime())
									.getBulletVelocity()
									* (robot.getTime()
											- erm.getSoonestBreaksSurfingWave(
													robot.getTime())
													.getFireTime() + 1),
							ourAnglesIntersection[0]); // TODO find bug
			Point2D.Double point2 = RoboGeom
					.project(
							erm.getSoonestBreaksSurfingWave(robot.getTime())
									.getSourcePosition(),
							erm.getSoonestBreaksSurfingWave(robot.getTime())
									.getBulletVelocity()
									* (robot.getTime()
											- erm.getSoonestBreaksSurfingWave(
													robot.getTime())
													.getFireTime() - 1),
							ourAnglesIntersection[0]);
			graphicsObject.drawLine((int) point1.x, (int) point1.y,
					(int) point2.x, (int) point2.y);
			point1 = RoboGeom
					.project(
							erm.getSoonestBreaksSurfingWave(robot.getTime())
									.getSourcePosition(),
							erm.getSoonestBreaksSurfingWave(robot.getTime())
									.getBulletVelocity()
									* (robot.getTime()
											- erm.getSoonestBreaksSurfingWave(
													robot.getTime())
													.getFireTime() + 1),
							ourAnglesIntersection[1]);
			point2 = RoboGeom
					.project(
							erm.getSoonestBreaksSurfingWave(robot.getTime())
									.getSourcePosition(),
							erm.getSoonestBreaksSurfingWave(robot.getTime())
									.getBulletVelocity()
									* (robot.getTime()
											- erm.getSoonestBreaksSurfingWave(
													robot.getTime())
													.getFireTime() - 1),
							ourAnglesIntersection[1]);

			graphicsObject.drawLine((int) point1.x, (int) point1.y,
					(int) point2.x, (int) point2.y);
		}
	}
}