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
import java.lang.Double;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import aw.waves.BulletShadow;
import aw.waves.MovementDataWave;
import robocode.util.Utils;
import robocode.Bullet;

public final class RoboGeom {
	private static final double ONE_HALF_BOT_DIAGONAL = 25.4558441227;
	public static final double ONE_HALF_PIE = Math.PI / 2;
	public static final double THREE_HALVES_PIE = 3.0 * Math.PI / 2;

	public static double preciseBotWidthAngle(Point2D.Double firingPosition,
			double distance, double absBearing) {
		Point2D.Double enemyPredictedPosition = project(firingPosition,
				distance, absBearing);

		absBearing = Utils.normalAbsoluteAngle(absBearing);
		Point2D.Double CPoint, CCPoint;

		if (absBearing > 0 && absBearing < ONE_HALF_PIE) {
			if (enemyPredictedPosition.x - firingPosition.x < 18)
				CCPoint = new Point2D.Double(
						enemyPredictedPosition.x - 18, enemyPredictedPosition.y - 18);
			else
				CCPoint = new Point2D.Double(
						enemyPredictedPosition.x - 18, enemyPredictedPosition.y + 18);
			if (enemyPredictedPosition.y - firingPosition.y < 18)
				CPoint = new Point2D.Double(
						enemyPredictedPosition.x - 18, enemyPredictedPosition.y - 18);
			else
				CPoint = new Point2D.Double(
						enemyPredictedPosition.x + 18, enemyPredictedPosition.y - 18);
		} else if (absBearing > ONE_HALF_PIE && absBearing < Math.PI) {
			if (enemyPredictedPosition.y - firingPosition.y > -18)
				CCPoint = new Point2D.Double(
						enemyPredictedPosition.x - 18, enemyPredictedPosition.y + 18);
			else
				CCPoint = new Point2D.Double(
						enemyPredictedPosition.x + 18, enemyPredictedPosition.y + 18);
			if (enemyPredictedPosition.x - firingPosition.x < 18)
				CPoint = new Point2D.Double(
						enemyPredictedPosition.x - 18, enemyPredictedPosition.y + 18);
			else
				CPoint = new Point2D.Double(
						enemyPredictedPosition.x - 18, enemyPredictedPosition.y - 18);
		} else if (absBearing > Math.PI && absBearing < THREE_HALVES_PIE) {
			if (enemyPredictedPosition.x - firingPosition.x > -18)
				CCPoint = new Point2D.Double(
						enemyPredictedPosition.x + 18, enemyPredictedPosition.y + 18);
			else
				CCPoint = new Point2D.Double(
						enemyPredictedPosition.x + 18, enemyPredictedPosition.y - 18);
			if (enemyPredictedPosition.y - firingPosition.y > -18)
				CPoint = new Point2D.Double(
						enemyPredictedPosition.x + 18, enemyPredictedPosition.y + 18);
			else
				CPoint = new Point2D.Double(
						enemyPredictedPosition.x - 18, enemyPredictedPosition.y + 18);
		} else {
			if (enemyPredictedPosition.x - firingPosition.x > -18)
				CPoint = new Point2D.Double(
						enemyPredictedPosition.x + 18, enemyPredictedPosition.y - 18);
			else
				CPoint = new Point2D.Double(
						enemyPredictedPosition.x + 18, enemyPredictedPosition.y + 18);
			if (enemyPredictedPosition.y - firingPosition.y < 18)
				CCPoint = new Point2D.Double(
						enemyPredictedPosition.x + 18, enemyPredictedPosition.y - 18);
			else
				CCPoint = new Point2D.Double(
						enemyPredictedPosition.x - 18, enemyPredictedPosition.y - 18);
		}

		return Math.abs(Utils.normalRelativeAngle(getBearing(firingPosition, CPoint)
				- getBearing(firingPosition, CCPoint)));
	}
	
	public static double preciseBotWidthAngle(Point2D.Double firingPosition, Point2D.Double targetPredictedPosition) {
		Point2D.Double CPoint, CCPoint;

		if (targetPredictedPosition.x > firingPosition.x && targetPredictedPosition.y > firingPosition.y) {
			if (targetPredictedPosition.x - firingPosition.x < 18)
				CCPoint = new Point2D.Double(
						targetPredictedPosition.x - 18, targetPredictedPosition.y - 18);
			else
				CCPoint = new Point2D.Double(
						targetPredictedPosition.x - 18, targetPredictedPosition.y + 18);
			if (targetPredictedPosition.y - firingPosition.y < 18)
				CPoint = new Point2D.Double(
						targetPredictedPosition.x - 18, targetPredictedPosition.y - 18);
			else
				CPoint = new Point2D.Double(
						targetPredictedPosition.x + 18, targetPredictedPosition.y - 18);
		} else if (targetPredictedPosition.x > firingPosition.x && targetPredictedPosition.y < firingPosition.y) {
			if (targetPredictedPosition.y - firingPosition.y > -18)
				CCPoint = new Point2D.Double(
						targetPredictedPosition.x - 18, targetPredictedPosition.y + 18);
			else
				CCPoint = new Point2D.Double(
						targetPredictedPosition.x + 18, targetPredictedPosition.y + 18);
			if (targetPredictedPosition.x - firingPosition.x < 18)
				CPoint = new Point2D.Double(
						targetPredictedPosition.x - 18, targetPredictedPosition.y + 18);
			else
				CPoint = new Point2D.Double(
						targetPredictedPosition.x - 18, targetPredictedPosition.y - 18);
		} else if (targetPredictedPosition.x < firingPosition.x && targetPredictedPosition.y < firingPosition.y) {
			if (targetPredictedPosition.x - firingPosition.x > -18)
				CCPoint = new Point2D.Double(
						targetPredictedPosition.x + 18, targetPredictedPosition.y + 18);
			else
				CCPoint = new Point2D.Double(
						targetPredictedPosition.x + 18, targetPredictedPosition.y - 18);
			if (targetPredictedPosition.y - firingPosition.y > -18)
				CPoint = new Point2D.Double(
						targetPredictedPosition.x + 18, targetPredictedPosition.y + 18);
			else
				CPoint = new Point2D.Double(
						targetPredictedPosition.x - 18, targetPredictedPosition.y + 18);
		} else {
			if (targetPredictedPosition.x - firingPosition.x > -18)
				CPoint = new Point2D.Double(
						targetPredictedPosition.x + 18, targetPredictedPosition.y - 18);
			else
				CPoint = new Point2D.Double(
						targetPredictedPosition.x + 18, targetPredictedPosition.y + 18);
			if (targetPredictedPosition.y - firingPosition.y < 18)
				CCPoint = new Point2D.Double(
						targetPredictedPosition.x + 18, targetPredictedPosition.y - 18);
			else
				CCPoint = new Point2D.Double(
						targetPredictedPosition.x - 18, targetPredictedPosition.y - 18);
		}

		return Math.abs(Utils.normalRelativeAngle(getBearing(firingPosition, CPoint)
				- getBearing(firingPosition, CCPoint)));
	}

	public static ArrayList<Point2D.Double> robotCircleIntersection(
			Point2D.Double robotCoordinates, Point2D.Double circleCenter,
			double radius) {
		double distance = Point2D.distance(robotCoordinates.x,
				robotCoordinates.y, circleCenter.x, circleCenter.y);
		ArrayList<Point2D.Double> intersectionPoints = new ArrayList<Point2D.Double>();

		if (radius < distance + ONE_HALF_BOT_DIAGONAL
				&& radius > distance - ONE_HALF_BOT_DIAGONAL) {
			intersectionPoints = horizontalLineIntersectsCircle(circleCenter,
					radius, robotCoordinates.y + 18.0);
			intersectionPoints.addAll(horizontalLineIntersectsCircle(
					circleCenter, radius, robotCoordinates.y - 18.0));
			int i;
			for (i = 0; i < intersectionPoints.size(); i++) {
				if (Math.abs(robotCoordinates.x - intersectionPoints.get(i).x) > 18)
					intersectionPoints.remove(i--);
			}
			intersectionPoints.addAll(verticalLineIntersectsCircle(
					circleCenter, radius, robotCoordinates.x + 18.0));
			intersectionPoints.addAll(verticalLineIntersectsCircle(
					circleCenter, radius, robotCoordinates.x - 18.0));
			for (; i < intersectionPoints.size(); i++) {
				if (Math.abs(robotCoordinates.y - intersectionPoints.get(i).y) > 18)
					intersectionPoints.remove(i--);
			}
		}

		return intersectionPoints;
	}

	private static ArrayList<Point2D.Double> horizontalLineIntersectsCircle(
			Point2D.Double circleOrigin, double radius, double lineY) {
		ArrayList<Point2D.Double> intersectionPoints = new ArrayList<Point2D.Double>();

		double n = lineY - circleOrigin.y;
		double temp = radius * radius - n * n;
		if (temp < 0)
			return intersectionPoints;

		double x1 = Math.sqrt(temp) + circleOrigin.x;
		double x2 = -Math.sqrt(temp) + circleOrigin.x;

		Point2D.Double point = new Point2D.Double(x1, lineY);
		intersectionPoints.add(point);
		point = new Point2D.Double(x2, lineY);
		intersectionPoints.add(point);

		return intersectionPoints;
	}

	private static ArrayList<Point2D.Double> verticalLineIntersectsCircle(
			Point2D.Double circleOrigin, double radius, double lineX) {
		ArrayList<Point2D.Double> intersectionPoints = new ArrayList<Point2D.Double>();

		double n = lineX - circleOrigin.x;
		double temp = radius * radius - n * n;
		if (temp < 0)
			return intersectionPoints;

		double y1 = Math.sqrt(temp) + circleOrigin.y;
		double y2 = -Math.sqrt(temp) + circleOrigin.y;

		Point2D.Double point = new Point2D.Double(lineX, y1);
		intersectionPoints.add(point);
		point = new Point2D.Double(lineX, y2);
		intersectionPoints.add(point);

		return intersectionPoints;
	}

	public static BulletShadow getBulletShadow(MovementDataWave wave,
			double bulletVelocity, double bulletAbsHeading,
			Point2D.Double bulletStartingPosition, long fireTime, Bullet bullet) {
		BulletShadow shadow = null;
		double maxClockwiseAngle = 0.0; // Garbage value
		double maxCounterClockwiseAngle = 0.0; // Garbage value
		boolean clockwiseAssigned = false;
		boolean counterClockwiseAssigned = false;

		double bulletDistTraveled = 0;
		double waveRadius = (fireTime - wave.getFireTime())
				* wave.getBulletVelocity();
		ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();

		boolean bulletFrontIn = false;
		boolean bulletFrontPassed = false;
		boolean bulletBackIn = false;

		long startTimeOffset = 0;
		long stopTimeOffset = 0;

		for (int i = 0; i < 100; i++) {
			waveRadius += wave.getBulletVelocity();
			if (waveRadius <= 0.0) {
				bulletDistTraveled += bulletVelocity;
			} else {

				Point2D.Double bulletPositionBeginningOfTurn = project(
						bulletStartingPosition, bulletDistTraveled,
						bulletAbsHeading);
				bulletDistTraveled += bulletVelocity;
				Point2D.Double bulletPositionEndOfTurn = project(
						bulletStartingPosition, bulletDistTraveled,
						bulletAbsHeading);

				bulletFrontIn = false;
				bulletFrontPassed = false;
				bulletBackIn = false;

				if (wave.getSourcePosition().distance(bulletPositionEndOfTurn) < waveRadius) {
					if (!bulletFrontIn) {
						startTimeOffset = i;
					}
					bulletFrontIn = true;
					if (wave.getSourcePosition().distance(
							bulletPositionEndOfTurn) < waveRadius
							- wave.getBulletVelocity()) {
						bulletFrontPassed = true;
					}
				}
				if (wave.getSourcePosition().distance(
						bulletPositionBeginningOfTurn) < waveRadius) {
					bulletBackIn = true;
					if (wave.getSourcePosition().distance(
							bulletPositionBeginningOfTurn) < waveRadius
							- wave.getBulletVelocity()) {
						stopTimeOffset = i;
						break;
					}
				}

				if (bulletFrontIn) {
					if (bulletFrontPassed) {
						if (bulletBackIn) {
							points.add(bulletPositionBeginningOfTurn);
							points.addAll(bulletCircleIntersection(
									wave.getSourcePosition(),
									waveRadius - wave.getBulletVelocity(),
									bulletPositionEndOfTurn, bulletVelocity,
									bulletAbsHeading));
						} else {
							points.addAll(bulletCircleIntersection(
									wave.getSourcePosition(),
									waveRadius - wave.getBulletVelocity(),
									bulletPositionEndOfTurn, bulletVelocity,
									bulletAbsHeading));
							points.addAll(bulletCircleIntersection(
									wave.getSourcePosition(), waveRadius,
									bulletPositionEndOfTurn, bulletVelocity,
									bulletAbsHeading));
						}
					} else {
						if (bulletBackIn) {
							points.add(bulletPositionBeginningOfTurn);
							points.add(bulletPositionEndOfTurn);
						} else {
							points.add(bulletPositionEndOfTurn);
							points.addAll(bulletCircleIntersection(
									wave.getSourcePosition(), waveRadius,
									bulletPositionEndOfTurn, bulletVelocity,
									bulletAbsHeading));
						}
					}
				}
			}
		}

		for (Point2D.Double point : points) {
			double angle = RoboGeom.getBearing(wave.getSourcePosition(), point);
			if (!clockwiseAssigned
					|| Utils.normalRelativeAngle(angle - maxClockwiseAngle) > 0) {
				clockwiseAssigned = true;
				maxClockwiseAngle = angle;
			}
			if (!counterClockwiseAssigned
					|| Utils.normalRelativeAngle(angle
							- maxCounterClockwiseAngle) < 0) {
				counterClockwiseAssigned = true;
				maxCounterClockwiseAngle = angle;
			}
		}

		shadow = new BulletShadow(maxClockwiseAngle, maxCounterClockwiseAngle,
				startTimeOffset + fireTime, stopTimeOffset + fireTime,
				fireTime, bulletAbsHeading, bulletVelocity,
				bulletStartingPosition, bullet);
		return shadow;
	}

	/**
	 * @param circleRadiusEndOfTurn
	 *            Radius of circle at the end of the turn.
	 * @param bulletPositionEndOfTurn
	 *            Bullet position at the end of the turn.
	 * @param bulletVelocity
	 *            Bullet velocity.
	 * @param bulletAbsHeading
	 *            Bullet absolute heading in radians.
	 * @return Points of intersection between the circle and the line.
	 */
	private static ArrayList<Point2D.Double> bulletCircleIntersection(
			Point2D.Double circleOrigin, double circleRadiusEndOfTurn,
			Point2D.Double bulletPositionEndOfTurn, double bulletVelocity,
			double bulletAbsHeading) {
		ArrayList<Point2D.Double> intersectionPoints = new ArrayList<Point2D.Double>();
		double deltaX = bulletVelocity * Math.sin(bulletAbsHeading);
		double deltaY = bulletVelocity * Math.cos(bulletAbsHeading);
		if (deltaX != 0.0 && deltaX != bulletVelocity) {
			double slope = deltaY / deltaX;
			double yIntercept = (bulletPositionEndOfTurn.y - circleOrigin.y)
					- (bulletPositionEndOfTurn.x - circleOrigin.x) * slope;
			intersectionPoints = lineIntersectsCircle(circleRadiusEndOfTurn,
					slope, yIntercept);

			for (int i = 0; i < intersectionPoints.size(); i++) {
				intersectionPoints.get(i).setLocation(
						intersectionPoints.get(i).x + circleOrigin.x,
						intersectionPoints.get(i).y + circleOrigin.y);

				if (deltaX > 0) {
					if (intersectionPoints.get(i).x > bulletPositionEndOfTurn.x
							|| intersectionPoints.get(i).x < bulletPositionEndOfTurn.x
									- deltaX)
						intersectionPoints.remove(i--);
				} else {
					if (intersectionPoints.get(i).x < bulletPositionEndOfTurn.x
							|| intersectionPoints.get(i).x > bulletPositionEndOfTurn.x
									- deltaX)
						intersectionPoints.remove(i--);
				}
			}
		} else {
			if (deltaX == 0.0) {
				intersectionPoints = verticalLineIntersectsCircle(circleOrigin,
						circleRadiusEndOfTurn, bulletPositionEndOfTurn.x);

				for (int i = 0; i < intersectionPoints.size(); i++) {
					if (deltaY > 0) {
						if (intersectionPoints.get(i).y > bulletPositionEndOfTurn.y
								|| intersectionPoints.get(i).y < bulletPositionEndOfTurn.y
										- deltaY)
							intersectionPoints.remove(i);
					} else {
						if (intersectionPoints.get(i).y < bulletPositionEndOfTurn.y
								|| intersectionPoints.get(i).y > bulletPositionEndOfTurn.y
										- deltaY)
							intersectionPoints.remove(i);
					}
				}
			} else {
				intersectionPoints = horizontalLineIntersectsCircle(
						circleOrigin, circleRadiusEndOfTurn,
						bulletPositionEndOfTurn.y);
				for (int i = 0; i < intersectionPoints.size(); i++) {
					if (deltaX > 0) {
						if (intersectionPoints.get(i).x > bulletPositionEndOfTurn.x
								|| intersectionPoints.get(i).x < bulletPositionEndOfTurn.x
										- deltaX)
							intersectionPoints.remove(i);
					} else {
						if (intersectionPoints.get(i).x < bulletPositionEndOfTurn.x
								|| intersectionPoints.get(i).x > bulletPositionEndOfTurn.x
										- deltaX)
							intersectionPoints.remove(i);
					}
				}
			}
		}

		return intersectionPoints;
	}

	/**
	 * @param circleRadius
	 *            Radius of circle.
	 * @param m
	 *            Slope of line.
	 * @param b
	 *            Y intercept of line. This must be adjusted so that the center
	 *            of the circle is the origin.
	 * @return Points of intersection between the circle and the line.
	 */
	private static ArrayList<Point2D.Double> lineIntersectsCircle(
			double circleRadius, double m, double b) {
		ArrayList<Point2D.Double> intersectionPoints = new ArrayList<Point2D.Double>();
		double quadA = 1.0 + m * m;
		double quadB = 2.0 * m * b;
		double quadC = b * b - circleRadius * circleRadius;

		double discriminant = quadB * quadB - 4.0 * quadA * quadC;
		if (discriminant > 0.0) {
			double q = -0.5
					* (quadB + Math.signum(quadB) * Math.sqrt(discriminant));
			double x1 = q / quadA;
			double x2 = quadC / q;
			double y1 = m * x1 + b;
			double y2 = m * x2 + b;
			Point2D.Double point = new Point2D.Double(x1, y1);
			intersectionPoints.add(point);
			point = new Point2D.Double(x2, y2);
			intersectionPoints.add(point);
		}

		return intersectionPoints;
	}

	/**
	 * returns the robocode absolute bearing from the first point to the second.
	 * 
	 * @param point1
	 *            the first point
	 * @param point2
	 *            the second point
	 * @return The bearing from point1 to point2.
	 */
	public static double getBearing(Point2D.Double point1, Point2D.Double point2) {
		return Math.atan2(point2.x - point1.x, point2.y - point1.y);
	}

	/**
	 * Projects a distance and heading form a point in robocode coordinates.
	 * 
	 * @param position
	 *            Source of projection.
	 * @param dist
	 *            Distance to project.
	 * @param heading
	 *            Heading of projection. (in robocode coordinates)
	 * @return The point that is dist away from position with a robocode heading
	 *         of heading.
	 */
	public static Point2D.Double project(Point2D.Double position, double dist,
			double heading) {
		return new Point2D.Double(position.x + Math.sin(heading) * dist,
				position.y + Math.cos(heading) * dist);
	}

	public static double normMaxEscapeAngle(double bulletVelocity) {
		return Math.asin(8 / bulletVelocity);
	}

	private static Rectangle2D battleField = new Rectangle(18, 18, 764, 564);

	public static double wallDistance(Point2D.Double sourcePosition,
			Point2D.Double targetPosition, double distance, int orbitDir) {
		final int RIGHT_WALL = 1, LEFT_WALL = -1, TOP_WALL = 1, BOTTOM_WALL = -1, NO_WALL = 0;
		int wallToSmoothHorizontal = 0;
		int wallToSmoothVertical = 0;
		double topDist, bottomDist, leftDist, rightDist;
		topDist = battleField.getMaxY() - sourcePosition.y;
		bottomDist = sourcePosition.y - battleField.getMinY();
		rightDist = battleField.getMaxX() - sourcePosition.x;
		leftDist = sourcePosition.x - battleField.getMinX();

		double targetBearing = getBearing(sourcePosition, targetPosition);
		if (rightDist < distance) {
			if (leftDist < distance) {
				if (Math.abs(Utils.normalRelativeAngle(targetBearing)) > Math.PI / 2) {
					if (orbitDir == 1)
						wallToSmoothHorizontal = LEFT_WALL;
					else
						wallToSmoothHorizontal = RIGHT_WALL;
				} else {
					if (orbitDir == 1)
						wallToSmoothHorizontal = RIGHT_WALL;
					else
						wallToSmoothHorizontal = LEFT_WALL;
				}
			} else
				wallToSmoothHorizontal = RIGHT_WALL;
		} else if (leftDist < distance) {
			wallToSmoothHorizontal = LEFT_WALL;
		}

		if (topDist < distance) {
			if (bottomDist < distance) {
				if (Utils.normalRelativeAngle(targetBearing) < 0) {
					if (orbitDir == 1)
						wallToSmoothVertical = TOP_WALL;
					else
						wallToSmoothVertical = BOTTOM_WALL;
				} else {
					if (orbitDir == 1)
						wallToSmoothVertical = BOTTOM_WALL;
					else
						wallToSmoothVertical = TOP_WALL;
				}
			} else
				wallToSmoothVertical = TOP_WALL;
		} else if (bottomDist < distance) {
			wallToSmoothVertical = BOTTOM_WALL;
		}

		if (wallToSmoothHorizontal != NO_WALL
				&& wallToSmoothVertical != NO_WALL) {
			if (orbitDir == 1) {
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
			} else {
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
			}
		}
		if (wallToSmoothHorizontal != NO_WALL) {
			if (wallToSmoothHorizontal == LEFT_WALL)
				return Utils
						.normalAbsoluteAngle(orbitDir
								* ((1.5 * Math.PI - orbitDir
										* Math.acos(leftDist / distance)) - targetBearing));
			else
				return Utils
						.normalAbsoluteAngle(orbitDir
								* ((0.5 * Math.PI - orbitDir
										* Math.acos(rightDist / distance)) - targetBearing));
		} else if (wallToSmoothVertical != NO_WALL) {
			if (wallToSmoothVertical == TOP_WALL)
				return Utils
						.normalAbsoluteAngle(orbitDir
								* ((0.0 * Math.PI - orbitDir
										* Math.acos(topDist / distance)) - targetBearing));
			else
				return Utils
						.normalAbsoluteAngle(orbitDir
								* ((1.0 * Math.PI - orbitDir
										* Math.acos(bottomDist / distance)) - targetBearing));
		} else
			return Double.POSITIVE_INFINITY;
	}

	// TODO optimize
	public static Point2D.Double precisePositionalMaxEscapeAnglePoint(
			Point2D.Double sourceCoordinates, Point2D.Double targetCoordinates,
			double bulletVelocity, int orbitDirection) {

		Point2D.Double predictedPosition;

		double targetDistance = sourceCoordinates.distance(targetCoordinates);
		double sourcePointBearing = getBearing(sourceCoordinates,
				targetCoordinates);

		double maxAngleOffset = Math.PI / 2;
		double minAngleOffset = 0.0;

		double predictedAngle;
		double rawAngle = sourcePointBearing + orbitDirection * Math.PI / 2;

		int count = 0;
		do {
			double offset = (minAngleOffset + maxAngleOffset) * 0.5;
			predictedAngle = rawAngle + orbitDirection * offset;
			double deltaX = 8.0 * Math.sin(predictedAngle);
			double deltaY = 8.0 * Math.cos(predictedAngle);
			double length = distanceTraveledForAngleDistanceAndBulletVelocity(
					-8.0 * Math.sin(offset), 8.0 * Math.cos(offset),
					targetDistance, bulletVelocity);

			predictedPosition = project(targetCoordinates, length,
					predictedAngle);

			boolean adjusted = false;
			double newX = predictedPosition.x;
			double newY = predictedPosition.y;
			if (newX > battleField.getMaxX()) {
				newY = verticalLineIntersectsLineY(battleField.getMaxX(),
						deltaY / deltaX, targetCoordinates.x,
						targetCoordinates.y);
				newX = battleField.getMaxX();
				adjusted = true;
			} else if (newX < battleField.getMinX()) {
				newY = verticalLineIntersectsLineY(battleField.getMinX(),
						deltaY / deltaX, targetCoordinates.x,
						targetCoordinates.y);
				newX = battleField.getMinX();
				adjusted = true;
			}
			if (newY > battleField.getMaxY()) {
				newX = horizontalLineIntersectsLineX(battleField.getMaxY(),
						deltaY / deltaX, targetCoordinates.x,
						targetCoordinates.y);
				newY = battleField.getMaxY();
				adjusted = true;
			} else if (newY < battleField.getMinY()) {
				newX = horizontalLineIntersectsLineX(battleField.getMinY(),
						deltaY / deltaX, targetCoordinates.x,
						targetCoordinates.y);
				newY = battleField.getMinY();
				adjusted = true;
			}
			predictedPosition = new Point2D.Double(newX, newY);

			if (adjusted)
				minAngleOffset = offset;
			else
				maxAngleOffset = offset;

			count++;
		} while (count < 20);

		return predictedPosition;
	}

	private static double distanceTraveledForAngleDistanceAndBulletVelocity(
			double normalizedDeltaY, double normalizedDeltaX,
			double targetDistance, double bulletVelocity) {
		double a = normalizedDeltaY * normalizedDeltaY + normalizedDeltaX
				* normalizedDeltaX - bulletVelocity * bulletVelocity;
		double b = 2.0 * targetDistance * normalizedDeltaY;
		double c = targetDistance * targetDistance;
		double discriminant = b * b - 4.0 * a * c;
		double x1;

		if (b < 0) {
			x1 = (-b + Math.sqrt(discriminant)) / (2.0 * a);
		} else {
			x1 = (-b - Math.sqrt(discriminant)) / (2.0 * a);
		}

		double t = Math.max(x1, c / (a * x1));
		return 8.0 * t;
	}

	private static double horizontalLineIntersectsLineX(double horizontalLineY,
			double slope, double xPoint, double yPoint) {
		return xPoint + (horizontalLineY - yPoint) / slope;
	}

	private static double verticalLineIntersectsLineY(double verticalLineX,
			double slope, double xPoint, double yPoint) {
		return yPoint + (verticalLineX - xPoint) * slope;
	}
}
