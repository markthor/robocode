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

import aw.utils.RoboGeom;

import robocode.Rules;
import robocode.util.Utils;

public class GunDataWave extends DataWave {
	private double virtuality;
	private double bulletPower;
	private Point2D.Double enemyGFOnePoint;
	private Point2D.Double enemyGFNegOnePoint;

	private boolean bulletPowerSet = false;

	public GunDataWave(long time, Point2D.Double sourcePosition,
			Point2D.Double targetPosition, double bulletPower,
			double bulletTravelTime, double Heading, double RelHeading,
			double Velocity, double AheadWallSeverity,
			double ReverseWallSeverity, double AheadWallDist,
			double ReverseWallDist, double Accel, double GFZeroAngle,
			double GFOneAngle, double GFNegOneAngle, long ticksSinceDirChange,
			long ticksSinceVelocityChange, long absFireTime, double virtuality,
			double orbitingCollisionFactor, int orbitDir) {
		super(time, sourcePosition, targetPosition, Rules
				.getBulletSpeed(bulletPower), bulletTravelTime, Heading,
				RelHeading, Velocity, AheadWallSeverity, ReverseWallSeverity,
				AheadWallDist, ReverseWallDist, Accel, ticksSinceDirChange,
				ticksSinceVelocityChange, absFireTime, GFZeroAngle, GFOneAngle,
				GFNegOneAngle, orbitingCollisionFactor, orbitDir);

		this.virtuality = virtuality;
		this.bulletPower = bulletPower;
	}

	public double getVirtuality() {
		return virtuality;
	}

	public double getBulletPower() {
		return bulletPower;
	}

	public void setVirtuality(double virtuality) {
		this.virtuality = virtuality;
	}

	public void setBulletPower(double power) {
		if (power != bulletPower) {
			bulletPower = power;
//			we do this at training and aiming instead
//			recalcAttributes();
		}
		bulletPowerSet = true;
	}

	public boolean getBulletPowerSet() {
		return bulletPowerSet;
	}

	public void recalcAttributes() {
		super.setGFOneAngle(preciseMaxEscapeAngle(getSourcePosition(),
				20 - 3 * bulletPower, getOrbitDir()));
		super.setGFNegOneAngle(preciseMaxEscapeAngle(getSourcePosition(),
				20 - 3 * bulletPower, -getOrbitDir()));
		super.setAheadWallSeverity(wallSeverity(getOrbitDir()));
		super.setReverseWallSeverity(wallSeverity(-getOrbitDir()));
		super.setAheadWallDist(wallDistance(getOrbitDir()));
		super.setReverseWallDist(wallDistance(-getOrbitDir()));
		super.setBulletTravelTime(getSourcePosition().distance(targetPosition)
				/ (20 - 3.0 * bulletPower));
	}

	private double preciseMaxEscapeAngle(Point2D.Double ourCoordinatesNextTurn,
			double bulletVelocity, int orbitDirection) {

		Point2D.Double predictedPosition;
		boolean GFOnePrediction = true;
		if (getOrbitDir() != orbitDirection)
			GFOnePrediction = false;

		predictedPosition = RoboGeom.precisePositionalMaxEscapeAnglePoint(
				ourCoordinatesNextTurn, targetPosition, bulletVelocity,
				orbitDirection);

		double bearing = RoboGeom.getBearing(ourCoordinatesNextTurn,
				predictedPosition);
		if (GFOnePrediction)
			enemyGFOnePoint = predictedPosition;
		else
			enemyGFNegOnePoint = predictedPosition;

		return bearing;
	}

	private double wallSeverity(int dir) {
		if (dir == getOrbitDir())
			return (Math
					.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
							targetPosition, enemyGFOnePoint)
							- RoboGeom.getBearing(targetPosition,
									getSourcePosition()))) / (Math.PI / 2));
		else
			return (Math
					.abs(Utils.normalRelativeAngle(RoboGeom.getBearing(
							targetPosition, enemyGFNegOnePoint)
							- RoboGeom.getBearing(targetPosition,
									getSourcePosition()))) / (Math.PI / 2));

	}

	private double wallDistance(int dir) {
		return Math.min(Math.PI / 2, RoboGeom.wallDistance(getSourcePosition(),
				targetPosition, getSourcePosition().distance(targetPosition),
				dir))
				/ (Math.PI / 2);
	}

}
