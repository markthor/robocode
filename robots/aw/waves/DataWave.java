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

import robocode.util.Utils;

public abstract class DataWave extends PreciseIntersectionWave {

	private double GFOneAngle;
	private double GFNegOneAngle;
	private double orbitingCollisionFactor;
	Point2D.Double targetPosition;
	private double sourceToTargetDistance;
	private double bulletTravelTime;
	private double Heading;
	private double RelHeading;
	private double targetVelocity;
	private double AheadWallSeverity;
	private double ReverseWallSeverity;
	private double AheadWallDist;
	private double ReverseWallDist;
	private double VChange;
	private long ticksSinceDirChange;
	private long ticksSinceVelocityChange;
	private long absFireTime;
	private int orbitDir;

	public DataWave(long time, Point2D.Double startPosition,
			Point2D.Double targetPosition, double bulletVelocity,
			double bulletTravelTime, double Heading, double RelHeading,
			double Velocity, double AheadWallSeverity,
			double ReverseWallSeverity, double AheadWallDist,
			double ReverseWallDist, double VChange, long ticksSinceDirChange,
			long ticksSinceVelocityChange, long absFireTime,
			double GFZeroAngle, double GFOneAngle, double GFNegOneAngle, double orbitingCollisionFactor,
			int orbitDir) {
		super(time, startPosition, bulletVelocity, GFZeroAngle);
		this.GFOneAngle = GFOneAngle;
		this.GFNegOneAngle = GFNegOneAngle;
		this.orbitingCollisionFactor = orbitingCollisionFactor;
		this.targetPosition = targetPosition;
		this.sourceToTargetDistance = Point2D.distance(startPosition.x,
				startPosition.y, targetPosition.x, targetPosition.y);
		this.bulletTravelTime = bulletTravelTime;
		this.Heading = Heading;
		this.RelHeading = RelHeading;
		this.targetVelocity = Velocity;
		this.AheadWallSeverity = AheadWallSeverity;
		this.ReverseWallSeverity = ReverseWallSeverity;
		this.AheadWallDist = AheadWallDist;
		this.ReverseWallDist = ReverseWallDist;
		this.VChange = VChange;
		this.ticksSinceDirChange = ticksSinceDirChange;
		this.ticksSinceVelocityChange = ticksSinceVelocityChange;
		this.absFireTime = absFireTime;
		this.orbitDir = orbitDir;
	}

	public double getGF(double angle) {

		double offset = orbitDir
				* Utils.normalRelativeAngle(angle - getGFZeroAngle());
		if (offset <= 0) {
			return Math.max(
					-1,
					offset
							/ Math.abs(Utils.normalRelativeAngle(GFNegOneAngle
									- getGFZeroAngle())));
		} else {
			return Math.min(
					1,
					offset
							/ Math.abs(Utils.normalRelativeAngle(GFOneAngle
									- getGFZeroAngle())));
		}
	}

	public double getSourceToPointBearing(Point2D.Double point) {
		return Math.atan2((point.x - this.getSourcePosition().x),
				(point.y - this.getSourcePosition().y));
	}

	public Point2D.Double getTargetPosition() {
		return targetPosition;
	}

	public double getGFOneAngle() {
		return GFOneAngle;
	}
	
	public void setGFOneAngle(double GFOneAngle) {
		this.GFOneAngle = GFOneAngle;
	}

	public double getGFNegOneAngle() {
		return GFNegOneAngle;
	}
	
	public void setGFNegOneAngle(double GFNegOneAngle) {
		this.GFNegOneAngle = GFNegOneAngle;
	}

	public double getOrbitingCollisionFactor() {
		return orbitingCollisionFactor;
	}
	
	public double getSourceToTargetDistance() {
		return sourceToTargetDistance;
	}

	public double getBulletTravelTime() {
		return bulletTravelTime;
	}
	
	public void setBulletTravelTime(double bulletTravelTime) {
		this.bulletTravelTime = bulletTravelTime;
	}

	public double getHeading() {
		return Heading;
	}

	public double getRelHeading() {
		return RelHeading;
	}

	public double getTargetVelocity() {
		return targetVelocity;
	}

	public double getAheadWallSeverity() {
		return AheadWallSeverity;
	}
	
	public void setAheadWallSeverity(double aheadWallSeverity) {
		this.AheadWallSeverity = aheadWallSeverity;
	}

	public double getReverseWallSeverity() {
		return ReverseWallSeverity;
	}
	
	public void setReverseWallSeverity(double reverseWallSeverity) {
		this.ReverseWallSeverity = reverseWallSeverity;
	}

	public double getAheadWallDist() {
		return AheadWallDist;
	}
	
	public void setAheadWallDist(double aheadWallDist) {
		this.AheadWallDist = aheadWallDist;
	}

	public double getReverseWallDist() {
		return ReverseWallDist;
	}
	
	public void setReverseWallDist(double reverseWallDist) {
		this.ReverseWallDist = reverseWallDist;
	}

	public double getVChange() {
		return VChange;
	}

	public long getTicksSinceDirChange() {
		return ticksSinceDirChange;
	}

	public long getTicksSinceVelocityChange() {
		return ticksSinceVelocityChange;
	}

	public long getAbsFireTime() {
		return absFireTime;
	}

	public int getOrbitDir() {
		return orbitDir;
	}

}
