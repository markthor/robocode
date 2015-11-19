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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.awt.Graphics2D;
import aw.utils.RobotState;
import robocode.*;
import robocode.control.events.RoundEndedEvent;

public abstract class Movement {
	AdvancedRobot robot;
	public Movement(AdvancedRobot robot) {
		this.robot = robot;
	}
	
	public abstract void run(ScannedRobotEvent e);
	
	public abstract void initRound();
	
	public abstract void ourBulletFired(double bulletVelocity, double bulletAbsHeading,
			Point2D.Double bulletStartingPosition, long fireTime, Bullet bullet);
	
	public abstract void onHitByBullet(HitByBulletEvent e);

	public abstract void onBulletHitBullet(BulletHitBulletEvent e);

	public abstract void onBulletHit(BulletHitEvent e);
	
	public abstract void onHitRobot(HitRobotEvent e);

	public abstract void onWin(WinEvent e);
	
	public abstract void onRoundEnded();

	public abstract Point2D.Double getOurPositionNextTurn();
	
	public abstract ArrayList<RobotState> getOurMovementPath();
	
	public abstract int getIndexEndAccuratePath();
	
	public abstract void onSkippedTurn(SkippedTurnEvent e);
	
	public abstract void onHitWall(HitWallEvent e);

	public abstract void onPaint(Graphics2D g, long time);
}
