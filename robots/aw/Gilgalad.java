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

package aw;

import java.awt.Color;
import java.awt.Graphics2D;

import robocode.*;
import robocode.control.events.RoundEndedEvent;
import robocode.util.Utils;
import java.awt.geom.Point2D;

import aw.gun.Aiglos;
import aw.gun.Gun;
import aw.movement.ShiningHelm;
import aw.movement.Movement;
import aw.utils.PrecisePredictor;
import aw.utils.RoboGeom;

public class Gilgalad extends AdvancedRobot {

	private boolean debugging = false;
	private boolean _isTC = false;
	private boolean _isMC = false;

	static Gun gun = null;
	static Movement movement = null;

	ShiningHelm DuelMovement = new ShiningHelm(this, true);

	double enemyHeading;
	double enemyVelocity;
	double enemyX;
	double enemyY;
	long lastScanTime = -1;
	long timeSpentOnGun = 0;
	long timeSpentOnMovement = 0;

	@Override
	public void run() {
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);

		Color bodyColor = new Color(0.84f, 0.84f, 0.93f, 0.9f);
		Color gunColor = new Color(0.1f, 0.43f, 0.1f, 0.9f);
		Color radarColor = Color.white;
		Color bulletColor = new Color(0.2f, 0.3f, 1.0f, 1.0f);
		Color scanArcColor = new Color(0.0f, 0.0f, 0.4f, 0.3f);

		setColors(bodyColor, gunColor, radarColor, bulletColor, scanArcColor);

		if (movement == null) {
			movement = new ShiningHelm(this, true);
		}
		if (gun == null) {
			gun = new Aiglos(this, true);
			gun.setMovement(movement);
		}

		movement.initRound();
		gun.initRound();

		// // TODO find a way to have the gun instanciated as a Gun, not Aiglos

		// // warm up some of the more important pieces of code
		// if(getTime() < 30) {
		// Point2D.Double pos1 = new Point2D.Double(0.0, 0.0);
		// Point2D.Double pos2;
		// for(int i = 0; i < 5000; i++) {
		// pos2 = RoboGeom.project(pos1, 500 * Math.random(), Math.PI * 2
		// *Math.random() - Math.PI);
		// double bearing = RoboGeom.getBearing(pos1, pos2);
		// pos1 = RoboGeom.project(pos1, 200 * Math.random(), bearing +
		// Math.PI);
		// }
		// }

		while (true) {
			if (getRadarTurnRemainingRadians() == 0) {
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			}
			if (!_isTC && getTime() != lastScanTime) {
				long startTime = System.nanoTime();
				movement.run(null);
				timeSpentOnMovement += System.nanoTime() - startTime;
			}
			execute();
		}
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		lastScanTime = getTime();
		double radarTurn = (Utils.normalRelativeAngle(getHeadingRadians()
				+ e.getBearingRadians() - getRadarHeadingRadians()));
		if (radarTurn > 0)
			radarTurn += 0.3;
		else
			radarTurn -= 0.3;

		setTurnRadarRightRadians(radarTurn);

		enemyHeading = e.getHeadingRadians();
		enemyVelocity = e.getVelocity();
		double enemyDistance = e.getDistance();
		double enemyAbsBearing = e.getBearingRadians() + getHeadingRadians();
		enemyX = getX() + enemyDistance * Math.sin(enemyAbsBearing);
		enemyY = getY() + enemyDistance * Math.cos(enemyAbsBearing);

		if (_isTC) {
			Point2D.Double robotCoordinates = new Point2D.Double(this.getX(),
					this.getY());
			long startTime = System.nanoTime();
			gun.run(e, robotCoordinates, null, 0);
			timeSpentOnGun += System.nanoTime() - startTime;

		} else if (_isMC) {
			long startTime = System.nanoTime();
			movement.run(e);
			timeSpentOnMovement += System.nanoTime() - startTime;
		} else {
			long startTime = System.nanoTime();
			movement.run(e);
			timeSpentOnMovement += System.nanoTime() - startTime;
			startTime = System.nanoTime();
			gun.run(e, movement.getOurPositionNextTurn(),
					movement.getOurMovementPath(), movement.getIndexEndAccuratePath());
			timeSpentOnGun += System.nanoTime() - startTime;

		}
	}

	@Override
	public void onHitByBullet(HitByBulletEvent e) {
		if (!_isTC) {
			long startTime = System.nanoTime();
			movement.onHitByBullet(e);
			timeSpentOnMovement += System.nanoTime() - startTime;
		}
	}

	@Override
	public void onBulletHitBullet(BulletHitBulletEvent e) {
		gun.onBulletHitBullet(e);
		if (!_isTC) {
			long startTime = System.nanoTime();
			movement.onBulletHitBullet(e);
			timeSpentOnMovement += System.nanoTime() - startTime;
		}
	}

	@Override
	public void onBulletHit(BulletHitEvent e) {
		gun.onBulletHit(e);
		if (!_isTC) {
			long startTime = System.nanoTime();
			movement.onBulletHit(e);
			timeSpentOnMovement += System.nanoTime() - startTime;
		}
	}

	@Override
	public void onHitRobot(HitRobotEvent e) {
		if (!_isTC) {
			long startTime = System.nanoTime();
			movement.onHitRobot(e);
			timeSpentOnMovement += System.nanoTime() - startTime;
		}
	}

	@Override
	public void onWin(WinEvent e) {
		if (!_isTC) {
			long startTime = System.nanoTime();
			movement.onWin(e);
			timeSpentOnMovement += System.nanoTime() - startTime;
		}
		if (!_isTC) {
			long startTime = System.nanoTime();
			movement.onRoundEnded();
			timeSpentOnMovement += System.nanoTime() - startTime;
		}
		if (!_isMC) {
			long startTime = System.nanoTime();
			gun.onRoundEnded();
			timeSpentOnGun += System.nanoTime() - startTime;
		}
		if (debugging) {
			System.out.println("Time spent on movement = "
					+ timeSpentOnMovement * 1E-6);
			System.out.println("Time spent on gun = " + timeSpentOnGun * 1E-6);
		}
	}

	public void onDeath(DeathEvent e) {
		if (!_isTC) {
			long startTime = System.nanoTime();
			movement.onRoundEnded();
			timeSpentOnMovement += System.nanoTime() - startTime;
		}
		if (!_isMC) {
			long startTime = System.nanoTime();
			gun.onRoundEnded();
			timeSpentOnGun += System.nanoTime() - startTime;
		}
		if (debugging) {
			System.out.println("Time spent on movement = "
					+ timeSpentOnMovement * 1E-6);
			System.out.println("Time spent on gun = " + timeSpentOnGun * 1E-6);
		}
	}

	@Override
	public void onSkippedTurn(SkippedTurnEvent e) {
		movement.onSkippedTurn(e);
	}

	@Override
	public void onHitWall(HitWallEvent e) {
		movement.onHitWall(e);
	}

	@Override
	public void onPaint(java.awt.Graphics2D g) {
		if (!_isTC)
			movement.onPaint(this.getGraphics(), getTime());
		if (!_isMC)
			gun.onPaint(this.getGraphics(), getTime());
	}

}
