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

package aw.utils;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.*;
import robocode.*;
import robocode.util.Utils;
import aw.waves.Wave;
import aw.utils.RoboGeom;
import aw.utils.RobotState;

public class PrecisePredictor {
	private static Rectangle2D battleField = new Rectangle(18, 18, 764, 564);
	private final static double ONE_HALF_PI = Math.PI / 2;
	private final static double NEG_ONE_HALF_PI = -Math.PI / 2;
	private final static double CONVERSION_DEGREES_TO_RADIANS = 0.0174532925199432778;
	private final static double MAXATTACKANGLESETTING = 1.1;
	private final static double MINATTACKANGLESETTING = 0.2;
	
	
	/**
	 * @param robot
	 *            The Advanced Robot to simulate the movement for.
	 * @param wave
	 *            Wave to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return Point robot will reach if it uses the specified parameters and
	 *         always goes at full speed.
	 */
	public static Point2D.Double predictPosition(AdvancedRobot robot,
			Wave wave, double stickLength, int dir) {
		return predictPosition(robot.getX(), robot.getY(),
				robot.getHeadingRadians(), robot.getVelocity(), wave,
				robot.getTime(), stickLength, dir);
	}

	

	// /**
	// * @param robotX
	// * X coordinate of robot
	// * @param robotY
	// * Y coordinate of robot
	// * @param robotHeading
	// * Heading (in robocode) of robot in radians.
	// * @param robotVelocity
	// * Velocity of robot.
	// * @param wave
	// * Wave to move for.
	// * @param pointToMoveTowards
	// * The point to simulate movement towards.
	// * @param stickLength
	// * Length of stick to use for wallsmoothing.
	// * @param dir
	// * Direction to move in (1 = clockwise, -1 = counter-clockwise)
	// * @return Point robot will reach if it uses the specified parameters and
	// * always goes at full speed.
	// */
	// public static Point2D.Double predictPosition(double robotX, double
	// robotY,
	// double robotHeading, double robotVelocity, Wave wave,
	// Point2D.Double pointToMoveTowards, long currentTick,
	// double stickLength, int dir) {
	// double turningFactor = 1.57079632679;
	// Point2D.Double position;
	// position = new Point2D.Double(robotX, robotY);
	// double currentHeading = robotHeading;
	// double currentVelocity = robotVelocity;
	// double moveDir;
	// boolean pastPoint = false;
	//
	// long futureTick = currentTick;
	//
	// while ((++futureTick - wave.getFireTime()) * wave.getBulletVelocity() <
	// Point2D
	// .distance(position.x, position.y, wave.getSource().x, wave.getSource().y)
	// && futureTick < 500 + currentTick) {
	// double desiredTurn;
	// if(!pastPoint && position.distance(pointToMoveTowards) < 50)
	// pastPoint = true;
	// if (!pastPoint) {
	// desiredTurn = noniterativeWallSmoothing(position,
	// RoboGeom.getBearing(position, pointToMoveTowards), dir,
	// stickLength) - currentHeading;
	// } else {
	// desiredTurn = noniterativeWallSmoothing(position,
	// (RoboGeom.getBearing(wave.startPosition, position) + dir
	// * turningFactor), dir, stickLength)
	// - currentHeading;
	// }
	//
	// moveDir = 1;
	// if (Math.cos(desiredTurn) < 0) {
	// desiredTurn += 3.1415926535;
	// moveDir = -1;
	// }
	// desiredTurn = Utils.normalRelativeAngle(desiredTurn);
	//
	// if (desiredTurn > 0) {
	// currentHeading += Math.min(
	// ((10 - 0.75 * Math.abs(currentVelocity))
	// * CONVERSION_DEGREES_TO_RADIANS), desiredTurn);
	// } else {
	// currentHeading += Math.max(
	// -((10 - 0.75 * Math.abs(currentVelocity))
	// * CONVERSION_DEGREES_TO_RADIANS), desiredTurn);
	// }
	//
	// currentHeading = Utils.normalRelativeAngle(currentHeading);
	//
	// if (moveDir * currentVelocity < 0) {
	// if (currentVelocity > 2) {
	// currentVelocity -= 2;
	// } else if (currentVelocity < -2) {
	// currentVelocity += 2;
	// } else {
	// currentVelocity = moveDir
	// * (1 - Math.abs(currentVelocity) / 2);
	// }
	// } else {
	// currentVelocity += 1 * moveDir;
	// }
	// currentVelocity = Math.max(-8, Math.min(currentVelocity, 8));
	// position = RoboGeom.project(position, currentVelocity, currentHeading);
	// }
	//
	// return position;
	// }

	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param wave
	 *            Wave to move for.
	 * @param currentTick
	 *            Current tick in robocode.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return Point robot will reach if it uses the specified parameters and
	 *         always goes at full speed.
	 */

	public static Point2D.Double predictPosition(double robotX, double robotY,
			double robotHeading, double robotVelocity, double angleToMoveAt,
			Wave wave, long currentTick, double stickLength, int dir) {
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;

		long futureTick = currentTick;

		while ((++futureTick - wave.getFireTime()) * wave.getBulletVelocity() < Point2D
				.distance(position.x, position.y, wave.getSourcePosition().x,
						wave.getSourcePosition().y)
				&& futureTick < 500 + currentTick) {

			double desiredTurn = noniterativeWallSmoothing(position,
					angleToMoveAt, dir, stickLength) - currentHeading;
			moveDir = 1;
			if (Math.cos(desiredTurn) < 0) {
				desiredTurn += 3.1415926535;
				moveDir = -1;
			}
			desiredTurn = Utils.normalRelativeAngle(desiredTurn);

			if (desiredTurn > 0) {
				currentHeading += Math
						.min(((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			} else {
				currentHeading += Math
						.max(-((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			}

			currentHeading = Utils.normalRelativeAngle(currentHeading);

			if (moveDir * currentVelocity < 0) {
				if (currentVelocity > 2) {
					currentVelocity -= 2;
				} else if (currentVelocity < -2) {
					currentVelocity += 2;
				} else {
					currentVelocity = moveDir
							* (1 - Math.abs(currentVelocity) / 2);
				}
			} else {
				currentVelocity += 1 * moveDir;
			}
			currentVelocity = Math.max(-8, Math.min(currentVelocity, 8));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
		}

		return position;
	}

	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param timeToMove
	 *            Ticks to simulate movement for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return Point robot will reach if it uses the specified parameters and
	 *         always goes at full speed.
	 */
	public static Point2D.Double predictPosition(double robotX, double robotY,
			double robotHeading, double robotVelocity, double angleToMoveAt,
			long timeToMove, double stickLength, int dir) {
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;

		while (timeToMove-- > 0) {

			double desiredTurn = noniterativeWallSmoothing(position,
					angleToMoveAt, dir, stickLength) - currentHeading;
			moveDir = 1;
			if (Math.cos(desiredTurn) < 0) {
				desiredTurn += 3.1415926535;
				moveDir = -1;
			}
			desiredTurn = Utils.normalRelativeAngle(desiredTurn);

			if (desiredTurn > 0) {
				currentHeading += Math
						.min(((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			} else {
				currentHeading += Math
						.max(-((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			}

			currentHeading = Utils.normalRelativeAngle(currentHeading);

			if (moveDir * currentVelocity < 0) {
				if (currentVelocity > 2) {
					currentVelocity -= 2;
				} else if (currentVelocity < -2) {
					currentVelocity += 2;
				} else {
					currentVelocity = moveDir
							* (1 - Math.abs(currentVelocity) / 2);
				}
			} else {
				currentVelocity += 1 * moveDir;
			}
			currentVelocity = Math.max(-8, Math.min(currentVelocity, 8));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
		}

		return position;
	}

	// /**
	// * @param robotX
	// * X coordinate of robot
	// * @param robotY
	// * Y coordinate of robot
	// * @param robotHeading
	// * Heading (in robocode) of robot in radians.
	// * @param robotVelocity
	// * Velocity of robot.
	// * @param wave
	// * Wave to move for.
	// * @param pointToMoveTowards
	// * The point to simulate movement towards.
	// * @param stickLength
	// * Length of stick to use for wallsmoothing.
	// * @param dir
	// * Direction to move in (1 = clockwise, -1 = counter-clockwise)
	// * @return ArrayList of points robot is at each turn for the specified
	// * parameters if the robot always tries to move at full speed.
	// */
	// public static ArrayList<Point2D.Double> predictPositions(double robotX,
	// double robotY,
	// double robotHeading, double robotVelocity, Wave wave,
	// Point2D.Double pointToMoveTowards, long currentTick,
	// double stickLength, int dir) {
	// ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();
	// double turningFactor = 1.57079632679;
	// Point2D.Double position;
	// position = new Point2D.Double(robotX, robotY);
	// double currentHeading = robotHeading;
	// double currentVelocity = robotVelocity;
	// double moveDir;
	// boolean pastPoint = false;
	//
	// long futureTick = currentTick;
	//
	// while ((++futureTick - wave.getFireTime()) * wave.getBulletVelocity() <
	// Point2D
	// .distance(position.x, position.y, wave.getSource().x, wave.getSource().y)
	// && futureTick < 500 + currentTick) {
	// double desiredTurn;
	// if(!pastPoint && position.distance(pointToMoveTowards) < 50)
	// pastPoint = true;
	// if (!pastPoint) {
	// desiredTurn = noniterativeWallSmoothing(position,
	// RoboGeom.getBearing(position, pointToMoveTowards), dir,
	// stickLength) - currentHeading;
	// } else {
	// desiredTurn = noniterativeWallSmoothing(position,
	// (RoboGeom.getBearing(wave.startPosition, position) + dir
	// * turningFactor), dir, stickLength)
	// - currentHeading;
	// }
	//
	// moveDir = 1;
	// if (Math.cos(desiredTurn) < 0) {
	// desiredTurn += 3.1415926535;
	// moveDir = -1;
	// }
	// desiredTurn = Utils.normalRelativeAngle(desiredTurn);
	//
	// if (desiredTurn > 0) {
	// currentHeading += Math.min(
	// ((10 - 0.75 * Math.abs(currentVelocity))
	// * CONVERSION_DEGREES_TO_RADIANS), desiredTurn);
	// } else {
	// currentHeading += Math.max(
	// -((10 - 0.75 * Math.abs(currentVelocity))
	// * CONVERSION_DEGREES_TO_RADIANS), desiredTurn);
	// }
	//
	// currentHeading = Utils.normalRelativeAngle(currentHeading);
	//
	// if (moveDir * currentVelocity < 0) {
	// if (currentVelocity > 2) {
	// currentVelocity -= 2;
	// } else if (currentVelocity < -2) {
	// currentVelocity += 2;
	// } else {
	// currentVelocity = moveDir
	// * (1 - Math.abs(currentVelocity) / 2);
	// }
	// } else {
	// currentVelocity += 1 * moveDir;
	// }
	// currentVelocity = Math.max(-8, Math.min(currentVelocity, 8));
	// position = RoboGeom.project(position, currentVelocity, currentHeading);
	// points.add(position);
	// }
	//
	// return points;
	// }

	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param wave
	 *            Wave to move for.
	 * @param currentTick
	 *            Current tick in robocode.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return ArrayList of points robot is at each turn for the specified
	 *         parameters if the robot always tries to move at full speed.
	 */
	public static ArrayList<Point2D.Double> predictPositions(double robotX,
			double robotY, double robotHeading, double robotVelocity,
			double angleToMoveAt, Wave wave, long currentTick,
			double stickLength, int dir) {
		ArrayList<Point2D.Double> positions = new ArrayList<Point2D.Double>();
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;

		long futureTick = currentTick;

		while ((futureTick - wave.getFireTime()) * wave.getBulletVelocity() < Point2D
				.distance(position.x, position.y, wave.getSourcePosition().x,
						wave.getSourcePosition().y)
				&& futureTick < 500 + currentTick) {
			futureTick++;

			double desiredTurn = noniterativeWallSmoothing(position,
					angleToMoveAt, dir, stickLength) - currentHeading;
			moveDir = 1;
			if (Math.cos(desiredTurn) < 0) {
				desiredTurn += 3.1415926535;
				moveDir = -1;
			}
			desiredTurn = Utils.normalRelativeAngle(desiredTurn);

			if (desiredTurn > 0) {
				currentHeading += Math
						.min(((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			} else {
				currentHeading += Math
						.max(-((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			}

			currentHeading = Utils.normalRelativeAngle(currentHeading);

			if (moveDir * currentVelocity < 0) {
				if (currentVelocity > 2) {
					currentVelocity -= 2;
				} else if (currentVelocity < -2) {
					currentVelocity += 2;
				} else {
					currentVelocity = moveDir
							* (1 - Math.abs(currentVelocity) / 2);
				}
			} else {
				currentVelocity += 1 * moveDir;
			}
			currentVelocity = Math.max(-8, Math.min(currentVelocity, 8));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
			positions.add(position);
		}

		return positions;
	}

	/**
	 * @param coordinates
	 *            Initial coordinates of the robot to simulate for.
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param timeToMove
	 *            Number of ticks to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return ArrayList of RobotStates at each turn for the specified
	 *         parameters. Each RobotState gives the coordinates, heading, and
	 *         velocity that are to be reached by the end of that turn.
	 */
	public static ArrayList<RobotState> simulateRawGoTo(
			Point2D.Double destination, Point2D.Double coordinates,
			double robotHeading, double robotVelocity, long timeToMove,
			double stickLength, int dir) {
		ArrayList<RobotState> path = new ArrayList<RobotState>();
		Point2D.Double position = coordinates;
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;

		RobotState state = new RobotState(position, currentVelocity,
				currentHeading);
		path.add(state);

		while (timeToMove-- > 0) {

			double driveDistance = Point2D.distance(position.x, position.y,
					destination.x, destination.y);
			double nextVelocity, nextHeading;

			if (driveDistance > 0.1 || currentVelocity > 2.0) {
				double desiredBearing = RoboGeom.getBearing(position,
						destination);
				double neededTurn = Utils.normalRelativeAngle(desiredBearing
						- currentHeading);
				if (Math.abs(neededTurn) > ONE_HALF_PI) {
					driveDistance *= -1;
					if (neededTurn > ONE_HALF_PI)
						neededTurn -= Math.PI;
					else
						neededTurn += Math.PI;
				}

				if (Math.signum(currentVelocity) != Math.signum(driveDistance)) {
					if (Math.abs(currentVelocity) > 2)
						nextVelocity = Math.signum(currentVelocity)
								* (Math.abs(currentVelocity) - 2);
					else {
						nextVelocity = Math.signum(driveDistance)
								* Math.min(Math.abs(driveDistance),
										(1 - Math.abs(currentVelocity) / 2));
					}
				} else
					nextVelocity = Math.signum(driveDistance)
							* Math.max(Math.min(getSemiLegalNeededVelocity(Math
									.abs(driveDistance)), Math
									.abs(currentVelocity) + 1.0), Math
									.abs(currentVelocity) - 2.0);
				if (neededTurn > 0) {
					nextHeading = currentHeading
							+ Math.min(
									((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
									neededTurn);
				} else {
					nextHeading = currentHeading
							+ Math.max(
									-((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
									neededTurn);
				}
			} else {
				nextVelocity = 0;
				double practicalHeading = currentHeading;
				if (currentVelocity < 0)
					practicalHeading += Math.PI;

				double neededTurn = Utils.normalRelativeAngle(PrecisePredictor
						.noniterativeWallSmoothing(position, practicalHeading,
								dir, 160)
						- currentHeading);

				if (Math.abs(neededTurn) > ONE_HALF_PI) {
					if (neededTurn > ONE_HALF_PI)
						neededTurn -= Math.PI;
					else
						neededTurn += Math.PI;
				}

				if (neededTurn > 0) {
					nextHeading = currentHeading
							+ Math.min(
									((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
									neededTurn);
				} else {
					nextHeading = currentHeading
							+ Math.max(
									-((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
									neededTurn);
				}
			}

			currentVelocity = nextVelocity;
			currentHeading = nextHeading;
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
			state = new RobotState(position, currentVelocity, currentHeading);
			path.add(state);
		}
		return path;
	}

	/**
	 * @param robot
	 *            The Advanced Robot to simulate the movement for.
	 * @param wave
	 *            Wave to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @param desiredDistance
	 *            The desired distance between the robot and the wave origin. 0
	 *            if no distancing is desired
	 * @return ArrayList of points robot is at each turn for the specified
	 *         parameters if the robot always tries to move at full speed.
	 */

	public static ArrayList<Point2D.Double> predictPositions(
			AdvancedRobot robot, Wave wave, double stickLength, int dir,
			double desiredDistance) {
		return predictPositions(robot.getX(), robot.getY(),
				robot.getHeadingRadians(), robot.getVelocity(), wave,
				robot.getTime(), stickLength, dir, desiredDistance);
	}

	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param wave
	 *            Wave to move for.
	 * @param currentTick
	 *            Current tick in robocode.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @param desiredDistance
	 *            The desired distance between the robot and the wave origin. 0
	 *            if no distancing is desired
	 * @return ArrayList of points robot is at each turn for the specified
	 *         parameters if the robot always tries to move at full speed.
	 */

	public static ArrayList<Point2D.Double> predictPositions(double robotX,
			double robotY, double robotHeading, double robotVelocity,
			Wave wave, long currentTime, double stickLength, int dir,
			double desiredDistance) {
		ArrayList<Point2D.Double> positions = new ArrayList<Point2D.Double>();
		double turningFactor = 1.57079632679;
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;
		double squareDesiredDist = desiredDistance * desiredDistance;

		long futureTick = currentTime;

		while ((futureTick - wave.getFireTime()) * wave.getBulletVelocity() < Point2D
				.distance(position.x, position.y, wave.getSourcePosition().x,
						wave.getSourcePosition().y)
				&& futureTick < 500 + currentTime) {
			futureTick++;
			double desiredTurn;
			if (desiredDistance == 0) {
				desiredTurn = noniterativeWallSmoothing(position,
						RoboGeom.getBearing(wave.getSourcePosition(), position)
								+ dir * turningFactor, dir, stickLength)
						- currentHeading;
			} else {
				double offset = turningFactor
						- 1
						+ Math.max(Math.min(
								Point2D.distanceSq(position.x, position.y,
										wave.getSourcePosition().x,
										wave.getSourcePosition().y)
										/ squareDesiredDist, 1.1), 0.8);

				desiredTurn = noniterativeWallSmoothing(position,
						RoboGeom.getBearing(wave.getSourcePosition(), position)
								+ dir * offset, dir, stickLength)
						- currentHeading;
			}
			moveDir = 1;
			if (Math.cos(desiredTurn) < 0) {
				desiredTurn += 3.1415926535;
				moveDir = -1;
			}
			desiredTurn = Utils.normalRelativeAngle(desiredTurn);

			if (desiredTurn > 0) {
				currentHeading += Math
						.min(((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			} else {
				currentHeading += Math
						.max(-((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			}

			currentHeading = Utils.normalRelativeAngle(currentHeading);

			if (moveDir * currentVelocity < 0) {
				if (currentVelocity > 2) {
					currentVelocity -= 2;
				} else if (currentVelocity < -2) {
					currentVelocity += 2;
				} else {
					currentVelocity = moveDir
							* (1 - Math.abs(currentVelocity) / 2);
				}
			} else {
				currentVelocity += 1 * moveDir;
			}
			currentVelocity = Math.max(-8, Math.min(currentVelocity, 8));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
			positions.add(position);
		}

		return positions;
	}
	
	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param wave
	 *            Wave to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return Point robot will reach if it uses the specified parameters and
	 *         always goes at full speed.
	 */
	public static Point2D.Double predictPosition(double robotX, double robotY,
			double robotHeading, double robotVelocity, Wave wave,
			long currentTick, double stickLength, int dir) {
		double turningFactor = 1.57079632679;
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;
	
		long futureTick = currentTick;
	
		while ((++futureTick - wave.getFireTime()) * wave.getBulletVelocity() < Point2D
				.distance(position.x, position.y, wave.getSourcePosition().x,
						wave.getSourcePosition().y)
				&& futureTick < 500 + currentTick) {
	
			double desiredTurn = noniterativeWallSmoothing(
					position,
					(RoboGeom.getBearing(wave.getSourcePosition(), position) + dir
							* turningFactor), dir, stickLength)
					- currentHeading;
			moveDir = 1;
			if (Math.cos(desiredTurn) < 0) {
				desiredTurn += 3.1415926535;
				moveDir = -1;
			}
			desiredTurn = Utils.normalRelativeAngle(desiredTurn);
	
			if (desiredTurn > 0) {
				currentHeading += Math
						.min(((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			} else {
				currentHeading += Math
						.max(-((10 - 0.75 * Math.abs(currentVelocity)) * CONVERSION_DEGREES_TO_RADIANS),
								desiredTurn);
			}
	
			currentHeading = Utils.normalRelativeAngle(currentHeading);
	
			if (moveDir * currentVelocity < 0) {
				if (currentVelocity > 2) {
					currentVelocity -= 2;
				} else if (currentVelocity < -2) {
					currentVelocity += 2;
				} else {
					currentVelocity = moveDir
							* (1 - Math.abs(currentVelocity) / 2);
				}
			} else {
				currentVelocity += 1 * moveDir;
			}
			currentVelocity = Math.max(-8, Math.min(currentVelocity, 8));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
		}
	
		return position;
	}

	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param timeToMove
	 *            Number of ticks to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return double[][] of the two points defining the MBR with 3 pixels of error 
	 * on each side.  [0][0] = minX  [0][1] = minY  [1][0] = maxX  [1][1] = maxY
	 * 
	 */
	public static double[][] predictEstimatePathMBR(double robotX,
			double robotY, double robotHeading, double robotVelocity,
			Wave wave, long initialTime, double stickLength,
			double desiredDistance, int dir) {
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double oneOverDesiredDist = 1.0 / (desiredDistance);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;
		
		double minX, minY, maxX, maxY;
		minX = minY = robotX;
		maxX = maxY = robotY;

		long futureTick = initialTime;

		while ((futureTick - wave.getFireTime() - 1) * wave.getBulletVelocity() < Point2D
				.distance(position.x, position.y, wave.getSourcePosition().x,
						wave.getSourcePosition().y)
				&& futureTick < 500 + initialTime) {
			futureTick++;
			double desiredTurn;
			if (desiredDistance == 0) {
				desiredTurn = Utils
						.normalRelativeAngle(noniterativeWallSmoothing(
								position,
								RoboGeom.getBearing(wave.getSourcePosition(),
										position) + dir * ONE_HALF_PI, dir,
								stickLength)
								- currentHeading);
			} else {
				// TODO test this more thouroughly
				double offset = ONE_HALF_PI
						- 1
						+ Math.max(Math.min(
								wave.getSourcePosition().distance(position)
										* oneOverDesiredDist, MAXATTACKANGLESETTING), MINATTACKANGLESETTING);

				desiredTurn = Utils
						.normalRelativeAngle(noniterativeWallSmoothing(
								position,
								RoboGeom.getBearing(wave.getSourcePosition(),
										position) + dir * offset, dir,
								stickLength)
								- currentHeading);
			}
			moveDir = 1;
			if (desiredTurn < NEG_ONE_HALF_PI) {
				desiredTurn += Math.PI;
				moveDir = -1;
			} else if (desiredTurn > ONE_HALF_PI) {
				desiredTurn -= Math.PI;
				moveDir = -1;
			}

			if (desiredTurn > 0) {
				currentHeading += Math.min(
						(0.174532925199 - 0.01308996939 * Math
								.abs(currentVelocity)), desiredTurn);
			} else {
				currentHeading += Math.max(
						(-0.174532925199 + 0.01308996939 * Math
								.abs(currentVelocity)), desiredTurn);
			}

			currentHeading = Utils.normalRelativeAngle(currentHeading);

			if (moveDir != Math.signum(currentVelocity)) {
				if (currentVelocity > 2.0) {
					currentVelocity -= 2.0;
				} else if (currentVelocity < -2.0) {
					currentVelocity += 2.0;
				} else {
					currentVelocity = moveDir
							* (1.0 - Math.abs(currentVelocity) * 0.5);
				}
			} else {
				if (moveDir > 0.0) {
					currentVelocity++;
				} else {
					currentVelocity--;
				}
			}
			currentVelocity = Math.max(-8.0, Math.min(currentVelocity, 8.0));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
			
			if(position.x < minX)
				minX = position.x;
			else if(position.x > maxX)
				maxX = position.x;
			if(position.y < minY)
				minY = position.y;
			else if(position.y > maxY)
				maxY = position.y;
			
		}
		
		double[][] MBRPoints = new double[2][2];
		MBRPoints[0][0] = minX - 3.0;
		MBRPoints[0][1] = minY - 3.0;
		MBRPoints[1][0] = maxX + 3.0;
		MBRPoints[1][1] = maxY + 3.0;

		return MBRPoints;
	}
	
	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param timeToMove
	 *            Number of ticks to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return ArrayList of RobotStates at each turn for the specified
	 *         parameters. Each RobotState gives the coordinates, heading, and
	 *         velocity that are to be reached by the end of that turn.
	 */
	public static Point2D.Double predictEstimatePathEndLocation(double robotX,
			double robotY, double robotHeading, double robotVelocity,
			Wave wave, long initialTime, double stickLength,
			double desiredDistance, int dir) {
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double oneOverDesiredDist = 1.0 / (desiredDistance);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;

		long futureTick = initialTime;

		while ((futureTick - wave.getFireTime() - 1) * wave.getBulletVelocity() < Point2D
				.distance(position.x, position.y, wave.getSourcePosition().x,
						wave.getSourcePosition().y)
				&& futureTick < 500 + initialTime) {
			futureTick++;
			double desiredTurn;
			if (desiredDistance == 0) {
				desiredTurn = Utils
						.normalRelativeAngle(noniterativeWallSmoothing(
								position,
								RoboGeom.getBearing(wave.getSourcePosition(),
										position) + dir * ONE_HALF_PI, dir,
								stickLength)
								- currentHeading);
			} else {
				// TODO test this more thouroughly
				double offset = ONE_HALF_PI
						- 1
						+ Math.max(Math.min(
								wave.getSourcePosition().distance(position)
										* oneOverDesiredDist, MAXATTACKANGLESETTING), MINATTACKANGLESETTING);

				desiredTurn = Utils
						.normalRelativeAngle(noniterativeWallSmoothing(
								position,
								RoboGeom.getBearing(wave.getSourcePosition(),
										position) + dir * offset, dir,
								stickLength)
								- currentHeading);
			}
			moveDir = 1;
			if (desiredTurn < NEG_ONE_HALF_PI) {
				desiredTurn += Math.PI;
				moveDir = -1;
			} else if (desiredTurn > ONE_HALF_PI) {
				desiredTurn -= Math.PI;
				moveDir = -1;
			}

			if (desiredTurn > 0) {
				currentHeading += Math.min(
						(0.174532925199 - 0.01308996939 * Math
								.abs(currentVelocity)), desiredTurn);
			} else {
				currentHeading += Math.max(
						(-0.174532925199 + 0.01308996939 * Math
								.abs(currentVelocity)), desiredTurn);
			}

			currentHeading = Utils.normalRelativeAngle(currentHeading);

			if (moveDir != Math.signum(currentVelocity)) {
				if (currentVelocity > 2.0) {
					currentVelocity -= 2.0;
				} else if (currentVelocity < -2.0) {
					currentVelocity += 2.0;
				} else {
					currentVelocity = moveDir
							* (1.0 - Math.abs(currentVelocity) * 0.5);
				}
			} else {
				if (moveDir > 0.0) {
					currentVelocity++;
				} else {
					currentVelocity--;
				}
			}
			currentVelocity = Math.max(-8.0, Math.min(currentVelocity, 8.0));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
		}

		return position;
	}
	
	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param timeToMove
	 *            Number of ticks to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return ArrayList of RobotStates at each turn for the specified
	 *         parameters. Each RobotState gives the coordinates, heading, and
	 *         velocity that are to be reached by the end of that turn.
	 */
	public static ArrayList<Point2D.Double> predictEstimatePathLocations(double robotX,
			double robotY, double robotHeading, double robotVelocity,
			Wave wave, long initialTime, double stickLength,
			double desiredDistance, int dir) {
		ArrayList<Point2D.Double> path = new ArrayList<Point2D.Double>();
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double oneOverDesiredDist = 1.0 / (desiredDistance);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;

		path.add(position);

		long futureTick = initialTime;

		while ((futureTick - wave.getFireTime() - 1) * wave.getBulletVelocity() < Point2D
				.distance(position.x, position.y, wave.getSourcePosition().x,
						wave.getSourcePosition().y)
				&& futureTick < 500 + initialTime) {
			futureTick++;
			double desiredTurn;
			if (desiredDistance == 0) {
				desiredTurn = Utils
						.normalRelativeAngle(noniterativeWallSmoothing(
								position,
								RoboGeom.getBearing(wave.getSourcePosition(),
										position) + dir * ONE_HALF_PI, dir,
								stickLength)
								- currentHeading);
			} else {
				// TODO test this more thouroughly
				double offset = ONE_HALF_PI
						- 1
						+ Math.max(Math.min(
								wave.getSourcePosition().distance(position)
										* oneOverDesiredDist, MAXATTACKANGLESETTING), MINATTACKANGLESETTING);

				desiredTurn = Utils
						.normalRelativeAngle(noniterativeWallSmoothing(
								position,
								RoboGeom.getBearing(wave.getSourcePosition(),
										position) + dir * offset, dir,
								stickLength)
								- currentHeading);
			}
			moveDir = 1;
			if (desiredTurn < NEG_ONE_HALF_PI) {
				desiredTurn += Math.PI;
				moveDir = -1;
			} else if (desiredTurn > ONE_HALF_PI) {
				desiredTurn -= Math.PI;
				moveDir = -1;
			}

			if (desiredTurn > 0) {
				currentHeading += Math.min(
						(0.174532925199 - 0.01308996939 * Math
								.abs(currentVelocity)), desiredTurn);
			} else {
				currentHeading += Math.max(
						(-0.174532925199 + 0.01308996939 * Math
								.abs(currentVelocity)), desiredTurn);
			}

			currentHeading = Utils.normalRelativeAngle(currentHeading);

			if (moveDir != Math.signum(currentVelocity)) {
				if (currentVelocity > 2.0) {
					currentVelocity -= 2.0;
				} else if (currentVelocity < -2.0) {
					currentVelocity += 2.0;
				} else {
					currentVelocity = moveDir
							* (1.0 - Math.abs(currentVelocity) * 0.5);
				}
			} else {
				if (moveDir > 0.0) {
					currentVelocity++;
				} else {
					currentVelocity--;
				}
			}
			currentVelocity = Math.max(-8.0, Math.min(currentVelocity, 8.0));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
			path.add(position);
		}

		return path;
	}

	/**
	 * @param robot
	 *            The AdvancedRobot to simulate the movement for.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param timeToMove
	 *            Number of ticks to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return ArrayList of RobotStates at each turn for the specified
	 *         parameters. Each RobotState gives the coordinates, heading, and
	 *         velocity that are to be reached by the end of that turn.
	 */

	public static ArrayList<RobotState> predictEstimatePath(
			AdvancedRobot robot, Wave wave, double stickLength,
			double desiredDistance, int dir) {
		return predictEstimatePath(robot.getX(), robot.getY(),
				robot.getHeadingRadians(), robot.getVelocity(), wave,
				robot.getTime(), stickLength, desiredDistance, dir);
	}

	/**
	 * @param robotX
	 *            X coordinate of robot
	 * @param robotY
	 *            Y coordinate of robot
	 * @param robotHeading
	 *            Heading (in robocode) of robot in radians.
	 * @param robotVelocity
	 *            Velocity of robot.
	 * @param angleToMoveAt
	 *            Angle (for robocode) the robot should move at in radians.
	 * @param timeToMove
	 *            Number of ticks to move for.
	 * @param stickLength
	 *            Length of stick to use for wallsmoothing.
	 * @param dir
	 *            Direction to move in (1 = clockwise, -1 = counter-clockwise)
	 * @return ArrayList of RobotStates at each turn for the specified
	 *         parameters. Each RobotState gives the coordinates, heading, and
	 *         velocity that are to be reached by the end of that turn.
	 */
	public static ArrayList<RobotState> predictEstimatePath(double robotX,
			double robotY, double robotHeading, double robotVelocity,
			Wave wave, long initialTime, double stickLength,
			double desiredDistance, int dir) {
		ArrayList<RobotState> path = new ArrayList<RobotState>();
		Point2D.Double position;
		position = new Point2D.Double(robotX, robotY);
		double oneOverDesiredDist = 1.0 / (desiredDistance);
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;
		double moveDir;

		RobotState state = new RobotState(position, currentVelocity,
				currentHeading);
		path.add(state);

		long futureTick = initialTime;

		while ((futureTick - wave.getFireTime() - 1) * wave.getBulletVelocity() < Point2D
				.distance(position.x, position.y, wave.getSourcePosition().x,
						wave.getSourcePosition().y)
				&& futureTick < 500 + initialTime) {
			futureTick++;
			double desiredTurn;
			if (desiredDistance == 0) {
				desiredTurn = Utils
						.normalRelativeAngle(noniterativeWallSmoothing(
								position,
								RoboGeom.getBearing(wave.getSourcePosition(),
										position) + dir * ONE_HALF_PI, dir,
								stickLength)
								- currentHeading);
			} else {
				// TODO test this more thouroughly
				double offset = ONE_HALF_PI
						- 1
						+ Math.max(Math.min(
								wave.getSourcePosition().distance(position)
										* oneOverDesiredDist, MAXATTACKANGLESETTING), MINATTACKANGLESETTING);

				desiredTurn = Utils
						.normalRelativeAngle(noniterativeWallSmoothing(
								position,
								RoboGeom.getBearing(wave.getSourcePosition(),
										position) + dir * offset, dir,
								stickLength)
								- currentHeading);
			}
			moveDir = 1;
			if (desiredTurn < NEG_ONE_HALF_PI) {
				desiredTurn += Math.PI;
				moveDir = -1;
			} else if (desiredTurn > ONE_HALF_PI) {
				desiredTurn -= Math.PI;
				moveDir = -1;
			}

			if (desiredTurn > 0) {
				currentHeading += Math.min(
						(0.174532925199 - 0.01308996939 * Math
								.abs(currentVelocity)), desiredTurn);
			} else {
				currentHeading += Math.max(
						(-0.174532925199 + 0.01308996939 * Math
								.abs(currentVelocity)), desiredTurn);
			}

			currentHeading = Utils.normalRelativeAngle(currentHeading);

			if (moveDir != Math.signum(currentVelocity)) {
				if (currentVelocity > 2.0) {
					currentVelocity -= 2.0;
				} else if (currentVelocity < -2.0) {
					currentVelocity += 2.0;
				} else {
					currentVelocity = moveDir
							* (1.0 - Math.abs(currentVelocity) * 0.5);
				}
			} else {
				if (moveDir > 0.0) {
					currentVelocity++;
				} else {
					currentVelocity--;
				}
			}
			currentVelocity = Math.max(-8.0, Math.min(currentVelocity, 8.0));
			position = RoboGeom.project(position, currentVelocity,
					currentHeading);
			state = new RobotState(position, currentVelocity, currentHeading);
			path.add(state);
		}

		return path;
	}

	/**
	 * 
	 * @param destination
	 *            Point to try to move to
	 * @param coordinates
	 *            Initial coordinates of robot
	 * @param wayPoints
	 *            Rough path to follow
	 * @param movementWave
	 *            Wave to stop movement for
	 * @param robotHeading
	 *            initial heading of robot in radians
	 * @param robotVelocity
	 *            Initial velocity of robot
	 * @param stickLength
	 *            Stick length to use for wall smoothing
	 * @param dir
	 *            Orbit Direction to use for wall smoothing
	 * @param initialTime
	 *            Initial time.
	 * @return Movement path. The path's first entry is the current turn. The
	 *         final entry is the turn on which the wave passes. Each RobotState
	 *         give the coordinates, heading, and velocity that are to be
	 *         attained on that turn.
	 */
	public static ArrayList<RobotState> simulateGoToFromRoughPath(
			Point2D.Double coordinates, ArrayList<RobotState> wayPoints,
			int indexOfDestination, Wave wave, double robotHeading,
			double robotVelocity, double stickLength, int dir, long initialTime) {
		// TODO verify this, make it work for 2 wave surfing
		ArrayList<RobotState> path = new ArrayList<RobotState>();
//		int i = 0;
//		while (i < indexOfDestination - 4) {
//			path.add(wayPoints.get(i));
//			i++;
//		}
		
		RobotState state;
		if (indexOfDestination > 4) {
			path.addAll(wayPoints.subList(0, indexOfDestination - 5));
			state = wayPoints.get(indexOfDestination - 5);
		}
		else
			state = new RobotState(coordinates, robotVelocity, robotHeading);
		// Debugging:
		// if(path.get(i) != state) {
		// System.out.println(wayPoints.size());
		// }

		path.addAll(simulateRawGoTo(wayPoints.get(indexOfDestination)
				.getCoordinates(), state.getCoordinates(), wave, state
				.getAbsHeading(), state.getVelocity(), stickLength, dir,
				initialTime + indexOfDestination - 5, (indexOfDestination == wayPoints.size() - 1)));

		return path;
	}

	// TODO optimize and fix
	// TODO verify that wall smoothing is working properly with goFullSpeed

	/**
	 * 
	 * @param destination
	 *            Point to try to move to
	 * @param coordinates
	 *            Initial coordinates of robot
	 * @param movementWave
	 *            Wave to stop movement for
	 * @param robotHeading
	 *            initial heading of robot in radians
	 * @param robotVelocity
	 *            Initial velocity of robot
	 * @param stickLength
	 *            Stick length to use for wall smoothing
	 * @param dir
	 *            Orbit Direction to use for wall smoothing
	 * @param initialTime
	 *            Initial time.
	 * @param goFullSpeed
	 *            Whether the path should try to stop on the final point or just
	 *            continue at full speed
	 * @return Movement path. The path's first entry is the current turn. The
	 *         final entry is the turn on which the wave passes. Each RobotState
	 *         give the coordinates for that turn and the heading and velocity
	 *         that are to be attained on that turn. So the first RobotState has
	 *         the initial position, but the heading and velocity are not
	 *         necessarily the initial heading and velocity.
	 */
	public static ArrayList<RobotState> simulateRawGoTo(
			Point2D.Double destination, Point2D.Double coordinates, Wave wave,
			double robotHeading, double robotVelocity, double stickLength,
			int dir, long initialTime, boolean goFullSpeed) {
		ArrayList<RobotState> path = new ArrayList<RobotState>();
		Point2D.Double position = coordinates;
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;

		RobotState state = new RobotState(position, currentVelocity,
				currentHeading);
		path.add(state);
		long currentTime = initialTime;

		double deltaX = Math.sin(currentHeading);
		double deltaY = Math.cos(currentHeading);

		while ((currentTime - wave.getFireTime() - 1)
				* wave.getBulletVelocity() < wave.getSourcePosition().distance(
				position.x, position.y)
				&& currentTime < 500 + initialTime) {
			currentTime++;
			double driveDistance = Point2D.distance(position.x, position.y,
					destination.x, destination.y);
			boolean pointedInRightDirection = false;
			double nextVelocity = currentVelocity;
			double nextHeading = currentHeading;
			double neededTurn;
			int moveDir = 1;

			if (driveDistance > 0.01 || Math.abs(currentVelocity) > 2.0) {
				if (!pointedInRightDirection) {
					double desiredBearing = RoboGeom.getBearing(position,
							destination);
					neededTurn = Utils.normalRelativeAngle(desiredBearing
							- currentHeading);
					if (neededTurn > ONE_HALF_PI) {
						neededTurn -= Math.PI;
						moveDir = -1;
					} else if (neededTurn < NEG_ONE_HALF_PI) {
						neededTurn += Math.PI;
						moveDir = -1;
					}
				} else {
					neededTurn = 0.0;
				}

				if (Math.signum(currentVelocity) != moveDir) {
					if (Math.abs(currentVelocity) > 2)
						nextVelocity = Math.signum(currentVelocity)
								* (Math.abs(currentVelocity) - 2);
					else {
						nextVelocity = moveDir
								* Math.min(Math.abs(driveDistance),
										(1 - Math.abs(currentVelocity) * 0.5));
					}
				} else {
					if (goFullSpeed) {
						double practicalHeading = currentHeading;
						if (currentVelocity < 0)
							practicalHeading += Math.PI;

						double wallSmoothedTurn = Utils
								.normalRelativeAngle(PrecisePredictor
										.noniterativeWallSmoothing(position,
												practicalHeading, dir, 160)
										- currentHeading);
						if (wallSmoothedTurn != 0)
							pointedInRightDirection = false;

						if (Math.abs(wallSmoothedTurn) < ONE_HALF_PI) {
							neededTurn = wallSmoothedTurn;
						}

						// TODO fix this
						nextVelocity = moveDir
								* Math.max(Math.min(8.0,
										Math.abs(currentVelocity) + 1.0), Math
										.abs(currentVelocity) - 2.0);
					} else {
						nextVelocity = moveDir
								* Math.max(Math.min(
										getSemiLegalNeededVelocity(Math
												.abs(driveDistance)), Math
												.abs(currentVelocity) + 1.0),
										Math.abs(currentVelocity) - 2.0);
					}
				}

				if (neededTurn > 0) {
//					nextHeading = currentHeading
//							+ Math.min((0.174532925199 - 0.01308996939 * Math
//									.abs(currentVelocity)), neededTurn);
					double limit = (0.174532925199 - 0.01308996939 * Math
							.abs(currentVelocity));
					if(neededTurn > limit)
						nextHeading = currentHeading + limit;
					else {
						nextHeading = currentHeading + neededTurn;
						pointedInRightDirection = true;
					}
				} else if (neededTurn < 0) {
//					nextHeading = currentHeading
//							+ Math.max((-0.174532925199 + 0.01308996939 * Math
//									.abs(currentVelocity)), neededTurn);
					double limit = (-0.174532925199 + 0.01308996939 * Math
							.abs(currentVelocity));
					if(neededTurn < limit)
						nextHeading = currentHeading + limit;
					else {
						nextHeading = currentHeading + neededTurn;
						pointedInRightDirection = true;
					}
				}
				
				currentVelocity = nextVelocity;
				if (neededTurn == 0) {
					pointedInRightDirection = true;
					position = new Point2D.Double(position.x + currentVelocity
							* deltaX, position.y + currentVelocity * deltaY);
				} else {
//					if (Utils.normalAbsoluteAngle(nextHeading) == Utils
//							.normalAbsoluteAngle(currentHeading + neededTurn))
//						pointedInRightDirection = true;
					currentHeading = nextHeading;
					if(currentVelocity != 0) {
					deltaX = Math.sin(currentHeading);
					deltaY = Math.cos(currentHeading);
					position = RoboGeom.project(position, currentVelocity,
							currentHeading);
					}
				}
				state = new RobotState(position, currentVelocity, currentHeading);
				path.add(state);
			} else {
				nextVelocity = 0.0;
				
//				double practicalHeading = currentHeading;
//
//				if (goFullSpeed) {
//					if (Math.signum(currentVelocity) != Math
//							.signum(driveDistance)) {
//						if (Math.abs(currentVelocity) > 2)
//							nextVelocity = Math.signum(currentVelocity)
//									* (Math.abs(currentVelocity) - 2);
//						else {
//							nextVelocity = moveDir
//									* Math.min(
//											Math.abs(driveDistance),
//											(1 - Math.abs(currentVelocity) * 0.5));
//						}
//					} else {
//						nextVelocity = moveDir
//								* Math.max(Math.min(8.0,
//										Math.abs(currentVelocity) + 1.0), Math
//										.abs(currentVelocity) - 2.0);
//
//					}
//					if (nextVelocity < 0)
//						practicalHeading += Math.PI;
//				} else {
//					nextVelocity = 0;
//					if (currentVelocity < 0)
//						practicalHeading += Math.PI;
//				}
//
//				neededTurn = Utils.normalRelativeAngle(PrecisePredictor
//						.noniterativeWallSmoothing(position, practicalHeading,
//								dir, 160)
//						- currentHeading);
//				pointedInRightDirection = false;
//
//				if (Math.abs(neededTurn) > ONE_HALF_PI) {
//					if (neededTurn > ONE_HALF_PI)
//						neededTurn -= Math.PI;
//					else
//						neededTurn += Math.PI;
//				}
//
//				if (neededTurn > 0) {
////					nextHeading = currentHeading
////							+ Math.min((0.174532925199 - 0.01308996939 * Math
////									.abs(currentVelocity)), neededTurn);
//					double limit = (0.174532925199 - 0.01308996939 * Math
//							.abs(currentVelocity));
//					if(neededTurn > limit)
//						nextHeading = currentHeading + limit;
//					else {
//						nextHeading = currentHeading + neededTurn;
//						pointedInRightDirection = true;
//					}
//				} else if (neededTurn < 0) {
////					nextHeading = currentHeading
////							+ Math.max((-0.174532925199 + 0.01308996939 * Math
////									.abs(currentVelocity)), neededTurn);
//					double limit = (-0.174532925199 + 0.01308996939 * Math
//							.abs(currentVelocity));
//					if(neededTurn < limit)
//						nextHeading = currentHeading + limit;
//					else {
//						nextHeading = currentHeading + neededTurn;
//						pointedInRightDirection = true;
//					}
//				}
				
				currentVelocity = nextVelocity;
				state = new RobotState(position, currentVelocity, currentHeading);
				path.add(state);
			}

		}
		return path;
	}
	
	
//	TODO optimize this!
	/**
	 * 
	 * @param destination
	 *            Point to try to move to
	 * @param coordinates
	 *            Initial coordinates of robot
	 * @param movementWave
	 *            Wave to stop movement for
	 * @param robotHeading
	 *            initial heading of robot in radians
	 * @param robotVelocity
	 *            Initial velocity of robot
	 * @param stickLength
	 *            Stick length to use for wall smoothing
	 * @param dir
	 *            Orbit Direction to use for wall smoothing
	 * @param initialTime
	 *            Initial time.
	 * @param goFullSpeed
	 *            Whether the path should try to stop on the final point or just
	 *            continue at full speed
	 * @return Movement path. The path's first entry is the current turn. The
	 *         final entry is the turn on which the wave passes. Each RobotState
	 *         give the coordinates for that turn and the heading and velocity
	 *         that are to be attained on that turn. So the first RobotState has
	 *         the initial position, but the heading and velocity are not
	 *         necessarily the initial heading and velocity.
	 */
	public static ArrayList<RobotState> simulateRawGoToForSecondWave(
			Point2D.Double destination, Point2D.Double coordinates, Wave wave,
			double robotHeading, double robotVelocity, long initialTime) {
		ArrayList<RobotState> path = new ArrayList<RobotState>();
		Point2D.Double position = coordinates;
		double currentHeading = robotHeading;
		double currentVelocity = robotVelocity;

		RobotState state = new RobotState(position, currentVelocity,
				currentHeading);
		path.add(state);
		long currentTime = initialTime;

		double deltaX = Math.sin(currentHeading);
		double deltaY = Math.cos(currentHeading);

		while ((currentTime - wave.getFireTime() - 1)
				* wave.getBulletVelocity() < wave.getSourcePosition().distance(
				position.x, position.y)
				&& currentTime < 500 + initialTime) {
			currentTime++;
			double driveDistance = Point2D.distance(position.x, position.y,
					destination.x, destination.y);
			boolean pointedInRightDirection = false;
			double nextVelocity = currentVelocity;
			double nextHeading = currentHeading;
			double neededTurn;
			int moveDir = 1;

			if (driveDistance > 0.01 || Math.abs(currentVelocity) > 2.0) {
				if (!pointedInRightDirection) {
					double desiredBearing = RoboGeom.getBearing(position,
							destination);
					neededTurn = Utils.normalRelativeAngle(desiredBearing
							- currentHeading);
					if (neededTurn > ONE_HALF_PI) {
						neededTurn -= Math.PI;
						moveDir = -1;
					} else if (neededTurn < NEG_ONE_HALF_PI) {
						neededTurn += Math.PI;
						moveDir = -1;
					}
				} else {
					neededTurn = 0.0;
				}

				if (Math.signum(currentVelocity) != moveDir) {
					if (Math.abs(currentVelocity) > 2)
						nextVelocity = Math.signum(currentVelocity)
								* (Math.abs(currentVelocity) - 2);
					else {
						nextVelocity = moveDir
								* Math.min(Math.abs(driveDistance),
										(1 - Math.abs(currentVelocity) * 0.5));
					}
				} else {

						nextVelocity = moveDir
								* Math.max(Math.min(
										getSemiLegalNeededVelocity(Math
												.abs(driveDistance)), Math
												.abs(currentVelocity) + 1.0),
										Math.abs(currentVelocity) - 2.0);
				}

				if (neededTurn > 0) {
//					nextHeading = currentHeading
//							+ Math.min((0.174532925199 - 0.01308996939 * Math
//									.abs(currentVelocity)), neededTurn);
					double limit = (0.174532925199 - 0.01308996939 * Math
							.abs(currentVelocity));
					if(neededTurn > limit)
						nextHeading = currentHeading + limit;
					else {
						nextHeading = currentHeading + neededTurn;
						pointedInRightDirection = true;
					}
				} else if (neededTurn < 0) {
//					nextHeading = currentHeading
//							+ Math.max((-0.174532925199 + 0.01308996939 * Math
//									.abs(currentVelocity)), neededTurn);
					double limit = (-0.174532925199 + 0.01308996939 * Math
							.abs(currentVelocity));
					if(neededTurn < limit)
						nextHeading = currentHeading + limit;
					else {
						nextHeading = currentHeading + neededTurn;
						pointedInRightDirection = true;
					}
				}
			} else {
				double practicalHeading = currentHeading;
					nextVelocity = 0;
					if (currentVelocity < 0)
						practicalHeading += Math.PI;
					neededTurn = 0;
			}
			currentVelocity = nextVelocity;
			if (neededTurn == 0) {
				pointedInRightDirection = true;
				position = new Point2D.Double(position.x + currentVelocity
						* deltaX, position.y + currentVelocity * deltaY);
			} else {
//				if (Utils.normalAbsoluteAngle(nextHeading) == Utils
//						.normalAbsoluteAngle(currentHeading + neededTurn))
//					pointedInRightDirection = true;
				currentHeading = nextHeading;
				deltaX = Math.sin(currentHeading);
				deltaY = Math.cos(currentHeading);
				position = RoboGeom.project(position, currentVelocity,
						currentHeading);
			}
			state = new RobotState(position, currentVelocity, currentHeading);
			path.add(state);
		}
		return path;
	}

	/**
	 * 
	 * @param robotLoc
	 *            Location of the robot to wall-smooth for.
	 * @param heading
	 *            The initial robocode absolute bearing angle to wall-smooth
	 *            for.
	 * @param orbitDir
	 *            1 if we are orbiting clockwise, -1 if we are orbiting
	 *            counterclockwise.
	 * @param stickLength
	 *            Minimum of normal stick length and distance to the point we
	 *            are orbiting.
	 * @return The absolute robocode bearing the robot should head at for the
	 *         specified parameters.
	 */
	public static double noniterativeWallSmoothing(Point2D.Double robotLoc,
			double heading, double orbitDir, double stickLength) {
		final int RIGHT_WALL = 1, LEFT_WALL = -1, TOP_WALL = 1, BOTTOM_WALL = -1, NO_WALL = 0;
		int wallToSmoothHorizontal = 0;
		int wallToSmoothVertical = 0;
		// TODO special case not within wall stick length of wall
		Point2D.Double projectedPosition = RoboGeom.project(robotLoc,
				stickLength, heading);

		if (projectedPosition.x > battleField.getMaxX()) {
			wallToSmoothHorizontal = RIGHT_WALL;
		} else if (projectedPosition.x < battleField.getMinX()) {
			wallToSmoothHorizontal = LEFT_WALL;
		}
		if (projectedPosition.y > battleField.getMaxY()) {
			wallToSmoothVertical = TOP_WALL;
		} else if (projectedPosition.y < battleField.getMinY()) {
			wallToSmoothVertical = BOTTOM_WALL;
		}

		if (wallToSmoothHorizontal == NO_WALL
				&& wallToSmoothVertical == NO_WALL)
			return heading;
		else if (wallToSmoothHorizontal != NO_WALL
				&& wallToSmoothVertical != NO_WALL) {
			if (orbitDir == 1) {
				if (wallToSmoothHorizontal == RIGHT_WALL) {
					if (wallToSmoothVertical == TOP_WALL)
						wallToSmoothVertical = NO_WALL;
					else
						wallToSmoothHorizontal = NO_WALL;
				} else {
					if (wallToSmoothVertical == TOP_WALL)
						wallToSmoothHorizontal = NO_WALL;
					else
						wallToSmoothVertical = NO_WALL;
				}
			} else {
				if (wallToSmoothHorizontal == RIGHT_WALL) {
					if (wallToSmoothVertical == TOP_WALL)
						wallToSmoothHorizontal = NO_WALL;
					else
						wallToSmoothVertical = NO_WALL;
				} else {
					if (wallToSmoothVertical == TOP_WALL)
						wallToSmoothVertical = NO_WALL;
					else
						wallToSmoothHorizontal = NO_WALL;
				}
			}
		}

		if (wallToSmoothVertical == 1)
			return (orbitDir * Math.acos((battleField.getMaxY() - robotLoc.y)
					/ stickLength));
		else if (wallToSmoothVertical == -1)
			return (3.1415926535 + orbitDir
					* Math.acos((robotLoc.y - battleField.getMinY())
							/ stickLength));
		else if (wallToSmoothHorizontal == 1)
			return (1.5707963267949 + orbitDir
					* Math.acos((battleField.getMaxX() - robotLoc.x)
							/ stickLength));
		else if (wallToSmoothHorizontal == -1)
			return (4.7123889803847 + orbitDir
					* Math.acos((robotLoc.x - battleField.getMinX())
							/ stickLength));
		else
			throw new RuntimeException("This code should be unreachable.");
	}

	public double distNeededToStop(double velocity) {
		if (velocity > 6) {
			return 4.0 * velocity - 12.0;
		} else if (velocity <= 2) {
			return velocity;
		} else if (velocity > 4) {
			return 3.0 * velocity - 6.0;
		} else {
			return 2.0 * velocity - 2.0;
		}
	}

	private static double getSemiLegalNeededVelocity(double desiredDistance) {
		if (desiredDistance > 18)
			return Math.min(8, 0.2 * (desiredDistance + 12.0));
		else if (desiredDistance <= 2)
			return desiredDistance;
		else if (desiredDistance > 10)
			return 0.25 * (desiredDistance + 6.0);
		else
			return 0.3333333333 * (desiredDistance + 2.0);
	}

	private static double getNeededVelocity(double desiredDistance) {
		if (desiredDistance > 18)
			return 0.2 * (desiredDistance + 12.0);
		else if (desiredDistance <= 2)
			return desiredDistance;
		else if (desiredDistance > 10)
			return 0.25 * (desiredDistance + 6.0);
		else
			return 0.3333333333 * (desiredDistance + 2.0);
	}
}
