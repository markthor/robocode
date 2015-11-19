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

import aw.utils.RoboGeom;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import robocode.util.Utils;

public class PreciseIntersectionWave extends Wave {
	private static final double ONE_HALF_BOT_DIAGONAL = 25.4558441227;

	private double maxClockwiseAngle;
	private double maxCounterClockwiseAngle;
	private boolean clockwiseAssigned = false;
	private boolean counterClockwiseAssigned = false;
	private double measureMaxAngles = 0.0;
	private double GFZeroAngle;
	private boolean wavePassed = false;
	private boolean waveIntersected = false;

	public PreciseIntersectionWave(long time, Point2D.Double startPosition,
			double bulletVelocity, double GFZeroAngle) {
		super(time, startPosition, bulletVelocity);
		this.GFZeroAngle = GFZeroAngle;
	}

	/**
	 * currentTime must be >= all previously 
	 * passed currentTimes for this wave because of the wave intersected optimization.
	 * 
	 * @param targetPosition Current position of the wave's target
	 * @param currentTime Current robocode time.
	 */
	public void updateWave(Point2D.Double targetPosition, long currentTime) {

		if (waveIntersected
				|| Point2D.distance(this.getSourcePosition().x, this.getSourcePosition().y,
						targetPosition.x, targetPosition.y)
						- ONE_HALF_BOT_DIAGONAL <= this.getBulletVelocity()
						* (currentTime - this.getFireTime() + 1)) {
			double radius = this.getBulletVelocity()
					* (currentTime - this.getFireTime() + 1);
			waveIntersected = true;

			ArrayList<Point2D.Double> points = RoboGeom
					.robotCircleIntersection(targetPosition, this.getSourcePosition(),
							radius);
			points.addAll(RoboGeom.robotCircleIntersection(targetPosition,
					this.getSourcePosition(), radius - this.getBulletVelocity()));

			Point2D.Double cornerPoint = new Point2D.Double(
					targetPosition.x - 18, targetPosition.y - 18);
			double cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
					this.getSourcePosition().x, this.getSourcePosition().y));
			if (cornerDist <= radius
					&& cornerDist >= radius - this.getBulletVelocity())
				points.add(cornerPoint);
			cornerPoint = new Point2D.Double(targetPosition.x - 18,
					targetPosition.y + 18);
			cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
					this.getSourcePosition().x, this.getSourcePosition().y));
			if (cornerDist <= radius
					&& cornerDist >= radius - this.getBulletVelocity())
				points.add(cornerPoint);
			cornerPoint = new Point2D.Double(targetPosition.x + 18,
					targetPosition.y - 18);
			cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
					this.getSourcePosition().x, this.getSourcePosition().y));
			if (cornerDist <= radius
					&& cornerDist >= radius - this.getBulletVelocity())
				points.add(cornerPoint);
			cornerPoint = new Point2D.Double(targetPosition.x + 18,
					targetPosition.y + 18);
			cornerDist = (Point2D.distance(cornerPoint.x, cornerPoint.y,
					this.getSourcePosition().x, this.getSourcePosition().y));
			if (cornerDist <= radius
					&& cornerDist >= radius - this.getBulletVelocity())
				points.add(cornerPoint);

			for (Point2D.Double point : points) {
				double angle = RoboGeom.getBearing(this.getSourcePosition(), point);
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
		}
	}

	public double getGFZeroAngle() {
		return GFZeroAngle;
	}

	public boolean getWavePassed(Point2D.Double targetPosition, long currentTime) {
		// The precise intersection range should not get any larger after
		// the wave passes this point
		double radius = this.getBulletVelocity()
				* (currentTime - this.getFireTime() + 1);
		return Point2D.distance(this.getSourcePosition().x, this.getSourcePosition().y,
				targetPosition.x, targetPosition.y) + ONE_HALF_BOT_DIAGONAL < radius;
	}

	public boolean getWaveIntersected() {
		return waveIntersected;
	}

	public double getMaxClockwiseAngle() {
		return maxClockwiseAngle;
	}

	public double getMaxCounterClockwiseAngle() {
		return maxCounterClockwiseAngle;
	}
	
	public boolean getAnglesAssigned() {
		return clockwiseAssigned && counterClockwiseAssigned;
	}

	public double getMeanMaxAngles() {
		return maxCounterClockwiseAngle + Utils.normalRelativeAngle(maxClockwiseAngle - maxCounterClockwiseAngle) * 0.5;
	}
}
