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

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.geom.Point2D;

import robocode.control.events.RoundEndedEvent;
import robocode.util.*;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import aw.movement.Movement;
import aw.movement.ShiningHelm;
import aw.utils.*;
import robocode.Bullet;

public class Aiglos extends Gun {
	private boolean _is1v1;
	private boolean _isTC = false;

	private ArrayList<EnemyRecordForGun> _enemyRecords = new ArrayList<EnemyRecordForGun>();
	private HashMap<String, EnemyRecordForGun> _enemies = new HashMap<String, EnemyRecordForGun>();

	static long absTime = 0;
	long ticksSinceFire = Long.MAX_VALUE;

	public Aiglos(AdvancedRobot robot, boolean is1v1) {
		super(robot);
		this._is1v1 = is1v1;
		if (is1v1) {
			EnemyRecordForGun ERecord = new EnemyRecordForGun(getRobot(), is1v1);
			_enemyRecords.add(ERecord);
		}
	}

	public void initRound() {
		for (EnemyRecordForGun ERG : _enemyRecords) {
			ERG.initRound();
		}
		ticksSinceFire = 1000;
	}

	// public void initializeGraphics(Graphics2D graphicsObj) {
	// EnemyRecordForGun ERecord = _enemyRecords.get(0);
	// ERecord.initializeGraphics(graphicsObj);
	// }

	@Override
	public void run(ScannedRobotEvent e, Point2D.Double ourPositionNextTurn,
			ArrayList<RobotState> ourMovementPath, int indexEndAccuratePath) {
		absTime++;
		ticksSinceFire++;
		if (_is1v1) {
			double bulletPower = firingPower1v1(e.getDistance(), e.getEnergy());
			EnemyRecordForGun ERG = _enemyRecords.get(0);
			ERG.onScannedRobot(e, ourPositionNextTurn, ourMovementPath,
					indexEndAccuratePath, ticksSinceFire, absTime, bulletPower);
			Bullet bullet;
			if ((getRobot().getEnergy() > 0.11 || _isTC) && (bullet = getRobot().setFireBullet(bulletPower)) != null) {
					ticksSinceFire = 0;
					Point2D.Double ourCoordinates = new Point2D.Double(
							getRobot().getX(), getRobot().getY());
					getMovement().ourBulletFired(
							Rules.getBulletSpeed(bulletPower),
							getRobot().getGunHeadingRadians(), ourCoordinates,
							getRobot().getTime(), bullet);
					getRobot().setTurnGunRightRadians(
							Utils.normalRelativeAngle(RoboGeom.getBearing(
									ourPositionNextTurn,
									ERG.getEnemyCoordinates())
									- getRobot().getGunHeadingRadians()));
					for (EnemyRecordForGun enemy : _enemyRecords) {
						enemy.onBulletFired(bullet.getPower(), getRobot()
								.getTime());
					}
			} else if (getRobot().getGunHeat() < getRobot().getGunCoolingRate() * 2.0
					&& (getRobot().getEnergy() > 0.11 || _isTC)) {
				getRobot().setTurnGunRightRadians(
						Utils.normalRelativeAngle(ERG.getFiringAngle1v1()
								- getRobot().getGunHeadingRadians()));
			} else {
				getRobot().setTurnGunRightRadians(
						Utils.normalRelativeAngle(RoboGeom.getBearing(
								ourPositionNextTurn, ERG.getEnemyCoordinates())
								- getRobot().getGunHeadingRadians()));
			}
		} else {

		}

	}
	
	private double firingPower1v1(double enemyDistance, double enemyEnergy) {
		if (this.getRobot() == null) {
			throw new RuntimeException("Robot is null");
		}
		if (_isTC)
			return Math.min(getRobot().getEnergy(), 3.0);

		double bulletPower;
		if (getRobot().getEnergy() > 40) {
				bulletPower = 1.95;
		} else if (getRobot().getEnergy() > 10 && enemyDistance < 400) {
			bulletPower = (getRobot().getEnergy() / 40) * 1.99;
		} else
			bulletPower = (getRobot().getEnergy() / 40)
					* (getRobot().getEnergy() / 40)
					* (getRobot().getEnergy() / 40) * 1.99;

		if (enemyDistance < 120)
			bulletPower = Math.min(getRobot().getEnergy() * 0.5 - 0.2, 2.99);

		if (enemyEnergy < 18) {
			bulletPower = Math.min(((enemyEnergy + 2) / 6), bulletPower);
		}
		if (enemyEnergy < 4) {
			bulletPower = Math.min((enemyEnergy / 4), bulletPower);
		}

		if (bulletPower > getRobot().getEnergy() - 0.1)
			bulletPower = getRobot().getEnergy() - 0.1;
		bulletPower = Math.min(3.0, Math.max(0.1, bulletPower));

		return bulletPower;
	}


//	private double firingPower1v1(double enemyDistance, double enemyEnergy) {
//		if (this.getRobot() == null) {
//			throw new RuntimeException("Robot is null");
//		}
//		if (_isTC)
//			return Math.min(getRobot().getEnergy(), 3.0);
//
//		double bulletPower;
//		double reduceFirepowerThreshold = Math.min(
//				Math.max(35.0, (enemyEnergy - getRobot().getEnergy()) * 7),
//				75.0);
//		if (getRobot().getEnergy() > reduceFirepowerThreshold) {
//			bulletPower = 1.9;
//		} else {
//			bulletPower = 1.90 * Math.pow(getRobot().getEnergy()
//					/ reduceFirepowerThreshold, 4);
//		}
//		bulletPower *= Math.min(1.5, 400 / enemyDistance);
//		bulletPower = Math.round(bulletPower * 10) / 10.0 + 0.05;
//
//		if (enemyDistance < 120)
//			bulletPower = Math.min(getRobot().getEnergy() * 0.5 - 0.2, 2.99);
//
//		if (enemyEnergy < 18) {
//			bulletPower = Math.min(((enemyEnergy + 2) / 6), bulletPower);
//		}
//		if (enemyEnergy < 4) {
//			bulletPower = Math.min((enemyEnergy / 4), bulletPower);
//		}
//
//		if (bulletPower > getRobot().getEnergy() - 0.1)
//			bulletPower = getRobot().getEnergy() - 0.1;
//		bulletPower = Math.min(3.0, Math.max(0.1, bulletPower));
//
//		return bulletPower;
//	}

	public void onHitByBullet(HitByBulletEvent e) {

	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		_enemyRecords.get(0).onBulletHitBullet(e);
	}

	public void onBulletHit(BulletHitEvent e) {
		_enemyRecords.get(0).onBulletHit(e);
	}

	public void onWin(WinEvent e) {

	}

	public void onRoundEnded() {
		System.out.println("-----GUN-----");
		_enemyRecords.get(0).onRoundEnded();
	}

	public void onPaint(Graphics2D graphicsObject, long time) {
		_enemyRecords.get(0).onPaint(graphicsObject, time);
	}

}
