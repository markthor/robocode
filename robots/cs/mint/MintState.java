/**
 * Copyright (c) 2012-2013 Robert Maupin (Chase)
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *    1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 
 *    2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 
 *    3. This notice may not be removed or altered from any source
 *    distribution.
 */
package cs.mint;

import java.util.ArrayDeque;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.RobotStatus;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import cs.mint.util.Rectangle;
import cs.mint.util.Vector;

/**
 * A state of a single round.
 * 
 * @author Chase
 * 
 */
public class MintState {
	public static final int CLOCKWISE = 1;
	public static final int COUNTERCLOCKWISE = -1;
	public static Rectangle battlefield;
	public static double coolingRate;
	public static int battlefieldHeight;
	public static int battlefieldWidth;
	public static ArrayDeque<Vector> pastTargetPosition = new ArrayDeque<Vector>();
	public final Vector position;
	public final double energy;
	public final double bodyHeading;
	public final double bodyTurnRemaining;
	public final double gunHeading;
	public final double gunHeat;
	public final double moveRemaining;
	public final double gunTurnRemaining;
	public final double radarHeading;
	public final double velocity;
	public final long time;
	public final int others;
	public final int roundNum;
	public Vector targetPosition = null;
	public double bodyHeadingDelta;
	public double targetAngle;
	public double velocityDelta;
	public double targetDistance;
	public double targetEnergy;
	public double targetHeading;
	public double targetHeadingDelta;
	public double targetLateralVelocity;
	public double targetRelativeAngle;
	public double targetVelocity;
	public double targetVelocityDelta;
	public double targetDistanceLast16;
	public long targetTimeSinceVelocityChange;
	public int targetOrbitDirection;

	public MintState(final StatusEvent e, final MintState lastState) {
		final RobotStatus status = e.getStatus();
		time = status.getTime();
		if(time == 0)
			MintState.pastTargetPosition.clear();
		roundNum = status.getRoundNum();
		position = new Vector(status.getX(), status.getY());
		bodyHeading = status.getHeadingRadians();
		gunHeading = status.getGunHeadingRadians();
		radarHeading = status.getRadarHeadingRadians();
		energy = status.getEnergy();
		gunHeat = status.getGunHeat();
		velocity = status.getVelocity();
		moveRemaining = status.getDistanceRemaining();
		gunTurnRemaining = status.getGunTurnRemainingRadians();
		bodyTurnRemaining = status.getTurnRemainingRadians();
		others = status.getOthers();
		if(lastState != null) {
			bodyHeadingDelta = bodyHeading - lastState.bodyHeading;
			velocityDelta = velocity - lastState.velocity;
		}
	}

	public void update(final BulletHitEvent e) {
		final double bulletPower = e.getBullet().getPower();
		targetEnergy -= Rules.getBulletDamage(bulletPower);
	}

	public void update(final HitByBulletEvent e) {
		targetEnergy += Rules.getBulletHitBonus(e.getPower());
	}

	public void update(final ScannedRobotEvent e, final MintState lastState) {
		targetRelativeAngle = e.getBearingRadians();
		targetAngle = bodyHeading + targetRelativeAngle;
		targetVelocity = e.getVelocity();
		final double bearing = e.getHeadingRadians() - targetAngle;
		targetLateralVelocity = targetVelocity * Math.sin(bearing);
		targetOrbitDirection = targetLateralVelocity > 0 ? CLOCKWISE : COUNTERCLOCKWISE;
		targetHeading = e.getHeadingRadians();
		targetEnergy = e.getEnergy();
		targetPosition = position.clone().project(targetAngle, targetDistance = e.getDistance());
		
		pastTargetPosition.addFirst(targetPosition);
		if(lastState != null) {
			targetHeadingDelta = targetHeading - lastState.targetHeading;
			targetVelocityDelta = targetVelocity - lastState.targetVelocity;
			
			++targetTimeSinceVelocityChange;
			if(Math.abs(targetLateralVelocity - lastState.targetLateralVelocity) > 0.5) {
				targetTimeSinceVelocityChange = 0;
			}
			
			if(pastTargetPosition.size() < 16) {
				targetDistanceLast16 = targetPosition.distance(pastTargetPosition.getLast());
			} else {
				targetDistanceLast16 = targetPosition.distance(pastTargetPosition.removeLast());
			}
		}
	}
}
