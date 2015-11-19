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

package aw.waves;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import aw.utils.RoboGeom;
import aw.utils.RobotState;
import aw.utils.misc;
import robocode.Rules;
import robocode.util.Utils;
import robocode.Bullet;

public class MovementDataWave extends DataWave {
	private static final double ONE_HALF_BOT_DIAGONAL = 25.4558441227;

	private ArrayList<double[]> anglesWeightsAndBandwidths;
	
	private double virtuality;
	private double bulletPower;
	private double robotDistLast5Ticks;
	private double robotDistLast10Ticks;
	private double robotDistLast20Ticks;
	private double maxEscapeAngle;
	private double preciseGFOneAngle;
	private double preciseGFNegOneAngle;
	private boolean surfingThisWave;
	private ArrayList<BulletShadow> bulletShadows = new ArrayList<BulletShadow>();
	private ArrayList<EffectiveBulletShadow> effectiveBulletShadows = new ArrayList<EffectiveBulletShadow>();

	public double[] anglePassedDebugging;

	
	/**
	 * 
	 * @param time
	 *            Time to start wave
	 * @param x
	 *            X coordinate of wave center
	 * @param y
	 *            Y coordinate of wave center
	 * @param bulletPower
	 *            Wave bullet power
	 * @param dir
	 *            Direction to base GF's on (clockwise = 1, counter-clockwise =
	 *            -1)
	 * @param GFOne
	 *            Max absolute angle of GF 1
	 * @param GFNegOne
	 *            Max absolute angle of GF -1
	 * @param GFZero
	 *            GF 0
	 * @param dataPointCoordinates
	 *            coordinates for the k-dimensional data point
	 */
	public MovementDataWave(long time, Point2D.Double sourcePosition,
			Point2D.Double targetPosition, double bulletPower,
			double bulletTravelTime, double Heading, double RelHeading,
			double targetVelocity, double AheadWallSeverity,
			double ReverseWallSeverity, double AheadWallDist,
			double ReverseWallDist, double Accel, double GFZeroAngle,
			double GFOneAngle, double GFNegOneAngle, double preciseGFOneAngle,
			double preciseGFNegOneAngle, long ticksSinceDirChange,
			long ticksSinceVelocityChange, long absFireTime, double virtuality,
			double robotDistLast5Ticks, double robotDistLast10Ticks,
			double robotDistLast20Ticks, double orbitingCollisionFactor, int orbitDir) {
		super(time, sourcePosition, targetPosition, Rules
				.getBulletSpeed(bulletPower), bulletTravelTime, Heading,
				RelHeading, targetVelocity, AheadWallSeverity,
				ReverseWallSeverity, AheadWallDist, ReverseWallDist, Accel,
				ticksSinceDirChange, ticksSinceVelocityChange, absFireTime,
				GFZeroAngle, GFOneAngle, GFNegOneAngle, orbitingCollisionFactor, orbitDir);
		this.virtuality = virtuality;
		this.bulletPower = bulletPower;
		this.robotDistLast5Ticks = robotDistLast5Ticks;
		this.robotDistLast10Ticks = robotDistLast10Ticks;
		this.robotDistLast20Ticks = robotDistLast20Ticks;
		this.preciseGFOneAngle = preciseGFOneAngle;
		this.preciseGFNegOneAngle = preciseGFNegOneAngle;
		this.maxEscapeAngle = Math.asin(8 / this.getBulletVelocity());
		surfingThisWave = true;
	}

	public double getGF(Point2D.Double point, long time) {
		if (getAnglesAssigned() && getWavePassed(point, time)) {

			double angle = getMeanMaxAngles();
			double offset = getOrbitDir()
					* Utils.normalRelativeAngle(angle - getGFZeroAngle());

			if (offset <= 0) {
				return Math.max(
						-1.0,
						offset
								/ Math.abs(Utils
										.normalRelativeAngle(getGFNegOneAngle()
												- getGFZeroAngle())));
			} else {
				return Math.min(
						1.0,
						offset
								/ Math.abs(Utils
										.normalRelativeAngle(getGFOneAngle()
												- getGFZeroAngle())));
			}

		}
		return 2.0; // return two if the wave has not intercepted the target
					// yet.
	}

	public double convertGFToAngle(double GF) {
		return this.getGFZeroAngle() + GF * this.getOrbitDir() * maxEscapeAngle;
	}

	public double getSourceToPointBearing(Point2D.Double point) {
		return Math.atan2((point.x - this.getSourcePosition().x),
				(point.y - this.getSourcePosition().y));
	}

	public double[] getIntersectionAngles(ArrayList<RobotState> path,
			long initialTime) {
		boolean waveIntersected = false;
		double maxClockwiseAngle = 0.0; // garbage value
		double maxCounterClockwiseAngle = 0.0; // garbage value
		boolean clockwiseAssigned = false;
		boolean counterClockwiseAssigned = false;
		double radius = this.getBulletVelocity()
				* (initialTime + 1 - this.getFireTime());

		for (int counter = 1; counter < path.size(); counter++) {
			double smallRadiusSqr = radius * radius;
			radius += this.getBulletVelocity();
			double largeRadiusSqr = radius * radius;
			if (waveIntersected
					|| Point2D.distance(this.getSourcePosition().x, this
							.getSourcePosition().y, path.get(counter)
							.getCoordinates().x, path.get(counter)
							.getCoordinates().y)
							- ONE_HALF_BOT_DIAGONAL <= radius) {
				waveIntersected = true;

				boolean pointInSmall = false;
				boolean pointOutSmall = false;
				boolean pointInLargeOutSmall = false;
				boolean pointOutLarge = false;

				ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();

				Point2D.Double cornerPoint = new Point2D.Double(path.get(
						counter).getCoordinates().x - 18, path.get(counter)
						.getCoordinates().y - 18);
				double cornerDist = (this.getSourcePosition()
						.distanceSq(cornerPoint));
				if (cornerDist >= smallRadiusSqr) {
					pointOutSmall = true;
					if (cornerDist <= largeRadiusSqr) {
						pointInLargeOutSmall = true;
						points.add(cornerPoint);
					} else
						pointOutLarge = true;
				} else
					pointInSmall = true;
				cornerPoint = new Point2D.Double(path.get(counter)
						.getCoordinates().x - 18, path.get(counter)
						.getCoordinates().y + 18);
				cornerDist = (this.getSourcePosition().distanceSq(cornerPoint));
				if (cornerDist >= smallRadiusSqr) {
					pointOutSmall = true;
					if (cornerDist <= largeRadiusSqr) {
						pointInLargeOutSmall = true;
						points.add(cornerPoint);
					} else
						pointOutLarge = true;
				} else
					pointInSmall = true;
				cornerPoint = new Point2D.Double(path.get(counter)
						.getCoordinates().x + 18, path.get(counter)
						.getCoordinates().y - 18);
				cornerDist = (this.getSourcePosition().distanceSq(cornerPoint));
				if (cornerDist >= smallRadiusSqr) {
					pointOutSmall = true;
					if (cornerDist <= largeRadiusSqr) {
						pointInLargeOutSmall = true;
						points.add(cornerPoint);
					} else
						pointOutLarge = true;
				} else
					pointInSmall = true;
				cornerPoint = new Point2D.Double(path.get(counter)
						.getCoordinates().x + 18, path.get(counter)
						.getCoordinates().y + 18);
				cornerDist = (this.getSourcePosition().distanceSq(cornerPoint));
				if (cornerDist >= smallRadiusSqr) {
					pointOutSmall = true;
					if (cornerDist <= largeRadiusSqr) {
						pointInLargeOutSmall = true;
						points.add(cornerPoint);
					} else
						pointOutLarge = true;
				} else
					pointInSmall = true;

				if ((pointInLargeOutSmall || pointInSmall) && pointOutLarge)
					points.addAll(RoboGeom.robotCircleIntersection(
							path.get(counter).getCoordinates(),
							this.getSourcePosition(), radius));

				if (pointInSmall && pointOutSmall)
					points.addAll(RoboGeom.robotCircleIntersection(
							path.get(counter).getCoordinates(),
							this.getSourcePosition(),
							radius - this.getBulletVelocity()));

				for (Point2D.Double point : points) {
					double angle = RoboGeom.getBearing(
							this.getSourcePosition(), point);
					if (!clockwiseAssigned
							|| Utils.normalRelativeAngle(angle
									- maxClockwiseAngle) > 0) {
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

				// The precise intersection range should not get any larger
				// after the wave passes this point
				// if (Point2D.distance(this.getSourcePosition().x, this
				// .getSourcePosition().y, path.get(counter)
				// .getCoordinates().x,
				// path.get(counter).getCoordinates().y)
				// + ONE_HALF_BOT_DIAGONAL < radius - this.getBulletVelocity())
				// {
				// wavePassed = true;
				// break;
				// }
			}
		}

		// if (!wavePassed && waveIntersected) {
		// double radius = this.getBulletVelocity()
		// * (currentTime - this.getFireTime());
		//
		// ArrayList<Point2D.Double> points = RoboGeom
		// .robotCircleIntersection(path.get(path.size() - 1)
		// .getCoordinates(), this.getSourcePosition(), radius);
		// points.addAll(RoboGeom.robotCircleIntersection(
		// path.get(path.size() - 1).getCoordinates(),
		// this.getSourcePosition(), radius - this.getBulletVelocity()));
		//
		// Point2D.Double cornerPoint = new Point2D.Double(path.get(
		// path.size() - 1).getCoordinates().x - 18, path.get(
		// path.size() - 1).getCoordinates().y - 18);
		// double cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
		// this.getSourcePosition().x, this.getSourcePosition().y));
		// if (cornerDist <= radius
		// && cornerDist >= radius - this.getBulletVelocity())
		// points.add(cornerPoint);
		// cornerPoint = new Point2D.Double(path.get(path.size() - 1)
		// .getCoordinates().x - 18, path.get(path.size() - 1)
		// .getCoordinates().y + 18);
		// cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
		// this.getSourcePosition().x, this.getSourcePosition().y));
		// if (cornerDist <= radius
		// && cornerDist >= radius - this.getBulletVelocity())
		// points.add(cornerPoint);
		// cornerPoint = new Point2D.Double(path.get(path.size() - 1)
		// .getCoordinates().x + 18, path.get(path.size() - 1)
		// .getCoordinates().y - 18);
		// cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
		// this.getSourcePosition().x, this.getSourcePosition().y));
		// if (cornerDist <= radius
		// && cornerDist >= radius - this.getBulletVelocity())
		// points.add(cornerPoint);
		// cornerPoint = new Point2D.Double(path.get(path.size() - 1)
		// .getCoordinates().x + 18, path.get(path.size() - 1)
		// .getCoordinates().y + 18);
		// cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
		// this.getSourcePosition().x, this.getSourcePosition().y));
		// if (cornerDist <= radius
		// && cornerDist >= radius - this.getBulletVelocity())
		// points.add(cornerPoint);
		//
		// for (Point2D.Double point : points) {
		// double angle = RoboGeom.getBearing(this.getSourcePosition(),
		// point);
		// if (!clockwiseAssigned
		// || Utils.normalRelativeAngle(angle - maxClockwiseAngle) > 0) {
		// clockwiseAssigned = true;
		// maxClockwiseAngle = angle;
		// }
		// if (!counterClockwiseAssigned
		// || Utils.normalRelativeAngle(angle
		// - maxCounterClockwiseAngle) < 0) {
		// counterClockwiseAssigned = true;
		// maxCounterClockwiseAngle = angle;
		// }
		// }
		// }

		double[] anglesOfIntersection = new double[2];
		anglesOfIntersection[0] = maxClockwiseAngle;
		anglesOfIntersection[1] = maxCounterClockwiseAngle;

		return anglesOfIntersection;
	}

	/**
	 * Warning! This skips the first turn of the movement path!
	 * 
	 * @param path
	 * @param initialTime
	 * @param clockwiseAngle
	 * @param counterClockwiseAngle
	 * @return
	 */
	public double[] getIntersectionAngles(ArrayList<RobotState> path,
			long initialTime, double clockwiseAngle,
			double counterClockwiseAngle) {
		double maxClockwiseAngle = clockwiseAngle;
		double maxCounterClockwiseAngle = counterClockwiseAngle;

		// long currentTime = initialTime + 2;
		double radius = this.getBulletVelocity()
				* (initialTime + 1 - this.getFireTime());

		for (int counter = 1; counter < path.size(); counter++) {
			double smallRadiusSqr = radius * radius;
			radius += this.getBulletVelocity();
			double largeRadiusSqr = radius * radius;

			boolean pointInSmall = false;
			boolean pointOutSmall = false;
			boolean pointInLargeOutSmall = false;
			boolean pointOutLarge = false;

			ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();

			Point2D.Double cornerPoint = new Point2D.Double(path.get(counter)
					.getCoordinates().x - 18, path.get(counter)
					.getCoordinates().y - 18);
			double cornerDist = (this.getSourcePosition()
					.distanceSq(cornerPoint));
			if (cornerDist >= smallRadiusSqr) {
				pointOutSmall = true;
				if (cornerDist <= largeRadiusSqr) {
					pointInLargeOutSmall = true;
					points.add(cornerPoint);
				} else
					pointOutLarge = true;
			} else
				pointInSmall = true;
			cornerPoint = new Point2D.Double(
					path.get(counter).getCoordinates().x - 18, path
							.get(counter).getCoordinates().y + 18);
			cornerDist = (this.getSourcePosition().distanceSq(cornerPoint));
			if (cornerDist >= smallRadiusSqr) {
				pointOutSmall = true;
				if (cornerDist <= largeRadiusSqr) {
					pointInLargeOutSmall = true;
					points.add(cornerPoint);
				} else
					pointOutLarge = true;
			} else
				pointInSmall = true;
			cornerPoint = new Point2D.Double(
					path.get(counter).getCoordinates().x + 18, path
							.get(counter).getCoordinates().y - 18);
			cornerDist = (this.getSourcePosition().distanceSq(cornerPoint));
			if (cornerDist >= smallRadiusSqr) {
				pointOutSmall = true;
				if (cornerDist <= largeRadiusSqr) {
					pointInLargeOutSmall = true;
					points.add(cornerPoint);
				} else
					pointOutLarge = true;
			} else
				pointInSmall = true;
			cornerPoint = new Point2D.Double(
					path.get(counter).getCoordinates().x + 18, path
							.get(counter).getCoordinates().y + 18);
			cornerDist = (this.getSourcePosition().distanceSq(cornerPoint));
			if (cornerDist >= smallRadiusSqr) {
				pointOutSmall = true;
				if (cornerDist <= largeRadiusSqr) {
					pointInLargeOutSmall = true;
					points.add(cornerPoint);
				} else
					pointOutLarge = true;
			} else
				pointInSmall = true;

			if ((pointInLargeOutSmall || pointInSmall) && pointOutLarge)
				points.addAll(RoboGeom.robotCircleIntersection(path
						.get(counter).getCoordinates(), this
						.getSourcePosition(), radius));

			if (pointInSmall && pointOutSmall)
				points.addAll(RoboGeom.robotCircleIntersection(path
						.get(counter).getCoordinates(), this
						.getSourcePosition(), radius - this.getBulletVelocity()));

			for (Point2D.Double point : points) {
				double angle = RoboGeom.getBearing(this.getSourcePosition(),
						point);
				if (Utils.normalRelativeAngle(angle - maxClockwiseAngle) > 0) {
					maxClockwiseAngle = angle;
				} else if (Utils.normalRelativeAngle(angle
						- maxCounterClockwiseAngle) < 0) {
					maxCounterClockwiseAngle = angle;
				}
			}

			// The precise intersection range should not get any larger
			// after the wave passes this point
			if (this.getSourcePosition().distance(
					path.get(counter).getCoordinates())
					+ ONE_HALF_BOT_DIAGONAL < radius - this.getBulletVelocity()) {
				break;
			}
		}

		// if (!wavePassed && waveIntersected) {
		// double radius = this.getBulletVelocity()
		// * (currentTime - this.getFireTime());
		//
		// ArrayList<Point2D.Double> points = RoboGeom
		// .robotCircleIntersection(path.get(path.size() - 1)
		// .getCoordinates(), this.getSourcePosition(), radius);
		// points.addAll(RoboGeom.robotCircleIntersection(
		// path.get(path.size() - 1).getCoordinates(),
		// this.getSourcePosition(), radius - this.getBulletVelocity()));
		//
		// Point2D.Double cornerPoint = new Point2D.Double(path.get(
		// path.size() - 1).getCoordinates().x - 18, path.get(
		// path.size() - 1).getCoordinates().y - 18);
		// double cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
		// this.getSourcePosition().x, this.getSourcePosition().y));
		// if (cornerDist <= radius
		// && cornerDist >= radius - this.getBulletVelocity())
		// points.add(cornerPoint);
		// cornerPoint = new Point2D.Double(path.get(path.size() - 1)
		// .getCoordinates().x - 18, path.get(path.size() - 1)
		// .getCoordinates().y + 18);
		// cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
		// this.getSourcePosition().x, this.getSourcePosition().y));
		// if (cornerDist <= radius
		// && cornerDist >= radius - this.getBulletVelocity())
		// points.add(cornerPoint);
		// cornerPoint = new Point2D.Double(path.get(path.size() - 1)
		// .getCoordinates().x + 18, path.get(path.size() - 1)
		// .getCoordinates().y - 18);
		// cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
		// this.getSourcePosition().x, this.getSourcePosition().y));
		// if (cornerDist <= radius
		// && cornerDist >= radius - this.getBulletVelocity())
		// points.add(cornerPoint);
		// cornerPoint = new Point2D.Double(path.get(path.size() - 1)
		// .getCoordinates().x + 18, path.get(path.size() - 1)
		// .getCoordinates().y + 18);
		// cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
		// this.getSourcePosition().x, this.getSourcePosition().y));
		// if (cornerDist <= radius
		// && cornerDist >= radius - this.getBulletVelocity())
		// points.add(cornerPoint);
		//
		// for (Point2D.Double point : points) {
		// double angle = RoboGeom.getBearing(this.getSourcePosition(),
		// point);
		// if (!clockwiseAssigned
		// || Utils.normalRelativeAngle(angle - maxClockwiseAngle) > 0) {
		// clockwiseAssigned = true;
		// maxClockwiseAngle = angle;
		// }
		// if (!counterClockwiseAssigned
		// || Utils.normalRelativeAngle(angle
		// - maxCounterClockwiseAngle) < 0) {
		// counterClockwiseAssigned = true;
		// maxCounterClockwiseAngle = angle;
		// }
		// }
		// }

		double[] anglesOfIntersection = new double[2];
		anglesOfIntersection[0] = maxClockwiseAngle;
		anglesOfIntersection[1] = maxCounterClockwiseAngle;

		return anglesOfIntersection;
	}

	
	// Warning! This skips the first turn of the path.
	public double checkDangerEstimate(ArrayList<RobotState> path,
			long initialTime) {
		double danger = Double.MIN_VALUE;

		double maxClockwiseAngle = 0.0; // garbage value
		double maxCounterClockwiseAngle = 0.0; // garbage value
		boolean clockwiseAssigned = false;
		boolean counterClockwiseAssigned = false;
		long currentTime = path.size() + initialTime - 1;
		double radius = this.getBulletVelocity() * (currentTime - this.getFireTime());
		for (int counter = path.size() - 2; counter >= 0; currentTime--, counter--) {
			radius -= this.getBulletVelocity();
			if (Point2D.distance(this.getSourcePosition().x, this
					.getSourcePosition().y,
					path.get(counter).getCoordinates().x, path.get(counter)
							.getCoordinates().y) >= radius) {
				double angle = RoboGeom.getBearing(getSourcePosition(), path
						.get(counter + 1).getCoordinates());
				double angleOffset = 0.5 * RoboGeom
						.preciseBotWidthAngle(getSourcePosition(),
								path.get(counter + 1).getCoordinates());
				maxClockwiseAngle = Utils.normalAbsoluteAngle(angle
						+ angleOffset);
				maxCounterClockwiseAngle = Utils.normalAbsoluteAngle(angle
						- angleOffset);
				clockwiseAssigned = true;
				counterClockwiseAssigned = true;
				break;
			}
		}

		if (clockwiseAssigned && counterClockwiseAssigned) {

			ArrayList<double[]> AWB = anglesWeightsAndBandwidths;
			for (int i = 0; i < AWB.size(); i++) {
				double a, b;
				double denominator = 1.0 / AWB.get(i)[2];
				a = Utils.normalRelativeAngle(AWB.get(i)[0] - maxClockwiseAngle) * denominator;
				b = Utils.normalRelativeAngle(AWB.get(i)[0]
						- maxCounterClockwiseAngle) * denominator;
				danger += AWB.get(i)[1] * Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
			}

			for (EffectiveBulletShadow shadow : effectiveBulletShadows) {
				// boolean CAngleShadowOverRobot = false;
				boolean CAngleShadowPassesRobot = false;
				// boolean CCAngleShadowOverRobot = false;
				boolean CCAngleShadowPassesRobot = false;

				if (Utils.normalRelativeAngle(maxCounterClockwiseAngle
						- shadow.getClockwiseAngle()) >= 0.0) {
					continue;
				} else {
					if (Utils.normalRelativeAngle(maxClockwiseAngle
							- shadow.getClockwiseAngle()) <= 0.0) {
						CAngleShadowPassesRobot = true;
					} else {
						// CAngleShadowOverRobot = true;
					}
				}

				if (Utils.normalRelativeAngle(maxClockwiseAngle
						- shadow.getCounterClockwiseAngle()) <= 0.0) {
					continue;
				} else {
					if (Utils.normalRelativeAngle(maxCounterClockwiseAngle
							- shadow.getCounterClockwiseAngle()) >= 0.0) {
						CCAngleShadowPassesRobot = true;
					} else {
						// CCAngleShadowOverRobot = true;
					}
				}

				if (CAngleShadowPassesRobot && CCAngleShadowPassesRobot) {
					danger = Double.MIN_VALUE;
					break;
				} else if (CAngleShadowPassesRobot) {
					for (int i = 0; i < AWB.size(); i++) {
						double a, b;
						double denominator = 1.0 / AWB.get(i)[2];
						a = Utils.normalRelativeAngle(AWB.get(i)[0]
								- maxClockwiseAngle) * denominator;
						b = Utils.normalRelativeAngle(AWB.get(i)[0]
								- shadow.getCounterClockwiseAngle()) * denominator;
						danger -= AWB.get(i)[1]
								* Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
					}
				} else if (CCAngleShadowPassesRobot) {
					for (int i = 0; i < AWB.size(); i++) {
						double a, b;
						double denominator = 1.0 / AWB.get(i)[2];
						a = Utils.normalRelativeAngle(AWB.get(i)[0]
								- shadow.getClockwiseAngle()) * denominator;
						b = Utils.normalRelativeAngle(AWB.get(i)[0]
								- maxCounterClockwiseAngle) * denominator;
						danger -= AWB.get(i)[1]
								* Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
					}
				} else {
					for (int i = 0; i < AWB.size(); i++) {
						double a, b;
						double denominator = 1.0 / AWB.get(i)[2];
						a = Utils.normalRelativeAngle(AWB.get(i)[0]
								- shadow.getClockwiseAngle()) * denominator;
						b = Utils.normalRelativeAngle(AWB.get(i)[0]
								- shadow.getCounterClockwiseAngle()) * denominator;
						danger -= AWB.get(i)[1]
								* Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
					}
				}
			}
		}
		return danger;
	}
	
	public double checkDangerEstimateForPoint(Point2D.Double point) {
		double danger = Double.MIN_VALUE;

		double maxClockwiseAngle = 0.0; // garbage value
		double maxCounterClockwiseAngle = 0.0; // garbage value

				double angle = RoboGeom.getBearing(getSourcePosition(), point);
				double angleOffset = 0.5 * RoboGeom
						.preciseBotWidthAngle(getSourcePosition(), point);
				maxClockwiseAngle = Utils.normalAbsoluteAngle(angle
						+ angleOffset);
				maxCounterClockwiseAngle = Utils.normalAbsoluteAngle(angle
						- angleOffset);

				ArrayList<double[]> AWB = anglesWeightsAndBandwidths;
			for (int i = 0; i < AWB.size(); i++) {
				double a, b;
				double denominator = 1.0 / AWB.get(i)[2];
				a = Utils.normalRelativeAngle(AWB.get(i)[0] - maxClockwiseAngle) * denominator;
				b = Utils.normalRelativeAngle(AWB.get(i)[0]
						- maxCounterClockwiseAngle) * denominator;
				danger += AWB.get(i)[1] * Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
			}

			for (EffectiveBulletShadow shadow : effectiveBulletShadows) {
				// boolean CAngleShadowOverRobot = false;
				boolean CAngleShadowPassesRobot = false;
				// boolean CCAngleShadowOverRobot = false;
				boolean CCAngleShadowPassesRobot = false;

				if (Utils.normalRelativeAngle(maxCounterClockwiseAngle
						- shadow.getClockwiseAngle()) >= 0.0) {
					continue;
				} else {
					if (Utils.normalRelativeAngle(maxClockwiseAngle
							- shadow.getClockwiseAngle()) <= 0.0) {
						CAngleShadowPassesRobot = true;
					} else {
						// CAngleShadowOverRobot = true;
					}
				}

				if (Utils.normalRelativeAngle(maxClockwiseAngle
						- shadow.getCounterClockwiseAngle()) <= 0.0) {
					continue;
				} else {
					if (Utils.normalRelativeAngle(maxCounterClockwiseAngle
							- shadow.getCounterClockwiseAngle()) >= 0.0) {
						CCAngleShadowPassesRobot = true;
					} else {
						// CCAngleShadowOverRobot = true;
					}
				}

				if (CAngleShadowPassesRobot && CCAngleShadowPassesRobot) {
					danger = Double.MIN_VALUE;
					break;
				} else if (CAngleShadowPassesRobot) {
					for (int i = 0; i < AWB.size(); i++) {
						double a, b;
						double denominator = 1.0 / AWB.get(i)[2];
						a = Utils.normalRelativeAngle(AWB.get(i)[0]
								- maxClockwiseAngle) * denominator;
						b = Utils.normalRelativeAngle(AWB.get(i)[0]
								- shadow.getCounterClockwiseAngle()) * denominator;
						danger -= AWB.get(i)[1]
								* Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
					}
				} else if (CCAngleShadowPassesRobot) {
					for (int i = 0; i < AWB.size(); i++) {
						double a, b;
						double denominator = 1.0 / AWB.get(i)[2];
						a = Utils.normalRelativeAngle(AWB.get(i)[0]
								- shadow.getClockwiseAngle()) * denominator;
						b = Utils.normalRelativeAngle(AWB.get(i)[0]
								- maxCounterClockwiseAngle) * denominator;
						danger -= AWB.get(i)[1]
								* Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
					}
				} else {
					for (int i = 0; i < AWB.size(); i++) {
						double a, b;
						double denominator = 1.0 / AWB.get(i)[2];
						a = Utils.normalRelativeAngle(AWB.get(i)[0]
								- shadow.getClockwiseAngle()) * denominator;
						b = Utils.normalRelativeAngle(AWB.get(i)[0]
								- shadow.getCounterClockwiseAngle()) * denominator;
						danger -= AWB.get(i)[1]
								* Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
					}
				}
			}
		return danger;
	}

	public double getDangerForAngles(double angleA, double angleB) {
		double danger = Double.MIN_VALUE;
		ArrayList<double[]> AWB = anglesWeightsAndBandwidths;
		for (int i = 0; i < AWB.size(); i++) {
			double a, b;
			double denominator = 1.0 / AWB.get(i)[2];
			a = Utils.normalRelativeAngle(AWB.get(i)[0] - angleA) * denominator;
			b = Utils.normalRelativeAngle(AWB.get(i)[0] - angleB) * denominator;
			danger += AWB.get(i)[1] * Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
		}
		for (EffectiveBulletShadow shadow : effectiveBulletShadows) {
			// boolean CAngleShadowOverRobot = false;
			boolean CAngleShadowPassesRobot = false;
			// boolean CCAngleShadowOverRobot = false;
			boolean CCAngleShadowPassesRobot = false;

			if (Utils.normalRelativeAngle(angleB - shadow.getClockwiseAngle()) >= 0.0) {
				continue;
			} else {
				if (Utils.normalRelativeAngle(angleA
						- shadow.getClockwiseAngle()) <= 0.0) {
					CAngleShadowPassesRobot = true;
				} else {
					// CAngleShadowOverRobot = true;
				}
			}

			if (Utils.normalRelativeAngle(angleA
					- shadow.getCounterClockwiseAngle()) <= 0.0) {
				continue;
			} else {
				if (Utils.normalRelativeAngle(angleB
						- shadow.getCounterClockwiseAngle()) >= 0.0) {
					CCAngleShadowPassesRobot = true;
				} else {
					// CCAngleShadowOverRobot = true;
				}
			}

			if (CAngleShadowPassesRobot && CCAngleShadowPassesRobot) {
				danger = Double.MIN_VALUE;
			} else if (CAngleShadowPassesRobot) {
				for (int i = 0; i < AWB.size(); i++) {
					double a, b;
					double denominator = 1.0 / AWB.get(i)[2];
					a = Utils.normalRelativeAngle(AWB.get(i)[0] - angleA) * denominator;
					b = Utils.normalRelativeAngle(AWB.get(i)[0]
							- shadow.getCounterClockwiseAngle()) * denominator;
					danger -= AWB.get(i)[1] * Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
				}
			} else if (CCAngleShadowPassesRobot) {
				for (int i = 0; i < AWB.size(); i++) {
					double a, b;
					double denominator = 1.0 / AWB.get(i)[2];
					a = Utils.normalRelativeAngle(AWB.get(i)[0]
							- shadow.getClockwiseAngle()) * denominator;
					b = Utils.normalRelativeAngle(AWB.get(i)[0] - angleB) * denominator;
					danger -= AWB.get(i)[1] * Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
				}
			} else {
				for (int i = 0; i < AWB.size(); i++) {
					double a, b;
					double denominator = 1.0 / AWB.get(i)[2];
					a = Utils.normalRelativeAngle(AWB.get(i)[0]
							- shadow.getClockwiseAngle()) * denominator;
					b = Utils.normalRelativeAngle(AWB.get(i)[0]
							- shadow.getCounterClockwiseAngle()) * denominator;
					danger -= AWB.get(i)[1] * Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
				}
			}
		}
		return danger;
	}

	public double getDangerWithoutBulletShadowsForAngles(double angleA,
			double angleB) {
		double danger = Double.MIN_VALUE;
		ArrayList<double[]> AWB = anglesWeightsAndBandwidths;
		for (int i = 0; i < AWB.size(); i++) {
			double a, b;
			double denominator = 1.0 / AWB.get(i)[2];
			a = Utils.normalRelativeAngle(AWB.get(i)[0] - angleA) * denominator;
			b = Utils.normalRelativeAngle(AWB.get(i)[0] - angleB) * denominator;
			danger += AWB.get(i)[1] * Math.abs(misc.kernelIntegralFunction(a) - misc.kernelIntegralFunction(b));
		}
		return danger;
	}

	public void setAnglesWeightsAndBandwidths(
			ArrayList<double[]> anglesWeightsAndBandwidths) {
		this.anglesWeightsAndBandwidths = anglesWeightsAndBandwidths;
	}

	public void addBulletShadows(double bulletVelocity, double bulletHeading,
			Point2D.Double bulletStartingPosition, long fireTime, Bullet bullet) {
		bulletShadows.add(RoboGeom.getBulletShadow(this, bulletVelocity,
				bulletHeading, bulletStartingPosition, fireTime, bullet));
		processBulletShadows();
	}

	public void removeBulletShadows(double velocityOfBullet,
			double absHeadingOfBullet, Point2D.Double coordinatesOfBullet,
			long timeOfCollision, Bullet bullet) {
		// boolean found = false;
		for (int i = 0; i < bulletShadows.size(); i++) {
			// if(coordinatesOfBullet.distance(RoboGeom.project(bulletShadows.get(i).getBulletStartPosition(),
			// velocityOfBullet * (timeOfCollision -
			// bulletShadows.get(i).getBulletFireTime() - 1),
			// bulletShadows.get(i).getBulletHeading())) < 0.1) {
			if (bullet.equals(bulletShadows.get(i).getBullet())) {
				// found = true;
				if (bulletShadows.get(i).getCollisionEndTime() >= timeOfCollision) {
					bulletShadows.remove(i);
				}
				break;
			}
		}
		// if (!found) {
		// System.out.println("Bullet shadow not found!");
		// } else
		// System.out.println("Bullet shadow found!");
		processBulletShadows();

	}

	private void processBulletShadows() {
		effectiveBulletShadows = new ArrayList<EffectiveBulletShadow>();
		if (bulletShadows.size() > 0) {
			EffectiveBulletShadow eft = new EffectiveBulletShadow(bulletShadows
					.get(0).getClockwiseAngle(), bulletShadows.get(0)
					.getCounterClockwiseAngle());
			effectiveBulletShadows.add(eft);

			for (int i = 1; i < bulletShadows.size(); i++) {
				double clockwiseLimit = bulletShadows.get(i)
						.getClockwiseAngle();
				double counterClockwiseLimit = bulletShadows.get(i)
						.getCounterClockwiseAngle();
				ArrayList<Integer> shadowsToRemove = new ArrayList<Integer>();

				boolean addThisShadow = true;
				for (int j = 0; j < effectiveBulletShadows.size(); j++) {
					// boolean CAngleShadowOverRobot = false;
					boolean CAngleShadowOverlaps = false;
					// boolean CCAngleShadowOverRobot = false;
					boolean CCAngleShadowOverlaps = false;

					if (Utils.normalRelativeAngle(effectiveBulletShadows.get(j)
							.getCounterClockwiseAngle()
							- bulletShadows.get(i).getClockwiseAngle()) >= 0.0) {
						// eft = new
						// EffectiveBulletShadow(bulletShadows.get(i).getClockwiseAngle(),
						// bulletShadows.get(i).getCounterClockwiseAngle());
						// effectiveBulletShadows.add(eft);
						continue;
					} else {
						if (Utils.normalRelativeAngle(effectiveBulletShadows
								.get(j).getClockwiseAngle()
								- bulletShadows.get(i).getClockwiseAngle()) <= 0.0) {
							CAngleShadowOverlaps = true;
						} else {
							// CAngleShadowOverRobot = true;
						}
					}

					if (Utils.normalRelativeAngle(effectiveBulletShadows.get(j)
							.getClockwiseAngle()
							- bulletShadows.get(i).getCounterClockwiseAngle()) <= 0.0) {
						continue;
					} else {
						if (Utils.normalRelativeAngle(effectiveBulletShadows
								.get(j).getCounterClockwiseAngle()
								- bulletShadows.get(i)
										.getCounterClockwiseAngle()) >= 0.0) {
							CCAngleShadowOverlaps = true;
						} else {
							// CCAngleShadowOverRobot = true;
						}
					}

					// addThisShadow = true;
					if (CAngleShadowOverlaps && CCAngleShadowOverlaps) {
						shadowsToRemove.add(j);
					} else if (CAngleShadowOverlaps) {
						counterClockwiseLimit = effectiveBulletShadows.get(j)
								.getCounterClockwiseAngle();
						shadowsToRemove.add(j);
					} else if (CCAngleShadowOverlaps) {
						clockwiseLimit = effectiveBulletShadows.get(j)
								.getClockwiseAngle();
						shadowsToRemove.add(j);
					} else {
						addThisShadow = false;
					}
				}
				if (shadowsToRemove.size() == 0) {
					if (addThisShadow) {
						eft = new EffectiveBulletShadow(clockwiseLimit,
								counterClockwiseLimit);
						effectiveBulletShadows.add(eft);
					}
				} else {
					eft = new EffectiveBulletShadow(clockwiseLimit,
							counterClockwiseLimit);
					effectiveBulletShadows.set(shadowsToRemove.get(0), eft);
					// shadowsToRemove.remove(0);
					// java.util.Collections.sort(shadowsToRemove);
					int offset = 0;
					for (int j = 1; j < shadowsToRemove.size(); j++) {
						effectiveBulletShadows.remove(shadowsToRemove.get(j)
								- offset);
						offset++;
					}
				}
			}
		}
	}

	public boolean surfingThisWave() {
		return surfingThisWave;
	}

	public void stopSurfing() {
		surfingThisWave = false;
	}

	public ArrayList<double[]> getAnglesWeightsAndBandwidths() {
		return anglesWeightsAndBandwidths;
	}

	public ArrayList<BulletShadow> getBulletShadows() {
		return bulletShadows;
	}

	public ArrayList<EffectiveBulletShadow> getEffectiveBulletShadows() {
		return effectiveBulletShadows;
	}

	public double getPreciseGFOneAngle() {
		return preciseGFOneAngle;
	}

	public double getPreciseGFNegOneAngle() {
		return preciseGFNegOneAngle;
	}

	public double getVirtuality() {
		return virtuality;
	}

	public double getBulletPower() {
		return bulletPower;
	}

	public double getRobotDistLast5Ticks() {
		return robotDistLast5Ticks;
	}

	public double getRobotDistLast10Ticks() {
		return robotDistLast10Ticks;
	}

	public double getRobotDistLast20Ticks() {
		return robotDistLast20Ticks;
	}
}
