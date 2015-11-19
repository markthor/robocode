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

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.PrintStream;
import cs.GunBase;
import robocode.BattleEndedEvent;
import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.Event;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.SkippedTurnEvent;
import robocode.StatusEvent;
import robocode.WinEvent;

/**
 * The normal AdvancedRobot class has years of clutter within it.
 * This class has no robot specific code in it, meaning you can safely ignore it.
 * 
 * @author Chase
 * 
 */
public abstract class RobotBase {
	public PrintStream out;
	public Graphics2D g;
	private GunBase peer;

	public final void execute() {
		peer.peer.execute();
	}

	public final double getBattleFieldHeight() {
		return peer.peer.getBattleFieldHeight();
	}

	public final double getBattleFieldWidth() {
		return peer.peer.getBattleFieldWidth();
	}

	public final Graphics2D getGraphics() {
		return peer.peer.getGraphics();
	}

	public final double getGunCoolingRate() {
		return peer.peer.getGunCoolingRate();
	}

	public final String getName() {
		return peer.peer.getName();
	}

	
	public Runnable getRobotRunnable() {
		return null;
	}

	public final boolean isAdjustGunForBodyTurn() {
		return peer.peer.isAdjustGunForBodyTurn();
	}

	public final boolean isAdjustRadarForBodyTurn() {
		return peer.peer.isAdjustRadarForBodyTurn();
	}

	public final boolean isAdjustRadarForGunTurn() {
		return peer.peer.isAdjustRadarForGunTurn();
	}

	
	public void onBattleEnded(final BattleEndedEvent e) {}

	public void onBulletFired(final Bullet b) {}

	
	public void onBulletHit(final BulletHitEvent e) {}

	
	public void onBulletHitBullet(final BulletHitBulletEvent e) {}

	
	public void onBulletMissed(final BulletMissedEvent e) {}

	
//	public final void onCustomEvent(final CustomEvent e) {
//		if(e.getCondition() instanceof TurnEndedEventCondition) onTurnEnded(e);
//	}

	
	public void onDeath(final DeathEvent e) {}

	
	public void onHitByBullet(final HitByBulletEvent e) {}

	
	public void onHitRobot(final HitRobotEvent e) {}

	
	public void onHitWall(final HitWallEvent e) {}

	
	public void onRobotDeath(final RobotDeathEvent e) {}

	
	public void onRoundEnded(final RoundEndedEvent e) {}

	
	public void onScannedRobot(final ScannedRobotEvent e) {}

	
	public void onSkippedTurn(final SkippedTurnEvent e) {}

	
	public void onStatus(final StatusEvent e) {}

	public void onTurnEnded(final Event e) {}

	
	public void onWin(final WinEvent e) {}

	public final void rescan() {
		peer.peer.rescan();
	}

	public final void setAdjustGunForBodyTurn(final boolean independent) {
		peer.peer.setAdjustGunForBodyTurn(independent);
	}

	public final void setAdjustRadarForBodyTurn(final boolean independent) {
		peer.peer.setAdjustRadarForBodyTurn(independent);
	}

	public final void setAdjustRadarForGunTurn(final boolean independent) {
		peer.peer.setAdjustRadarForGunTurn(independent);
	}

	public final void setAllColors(final Color color) {
		peer.peer.setBodyColor(color);
		peer.peer.setBulletColor(color);
		peer.peer.setGunColor(color);
		peer.peer.setRadarColor(color);
		peer.peer.setScanColor(color);
	}

	public final void setBodyColor(final Color color) {
		peer.peer.setBodyColor(color);
	}

	public final void setBulletColor(final Color color) {
		peer.peer.setBulletColor(color);
	}

	public final void setDebugProperty(final String key, final String value) {
		peer.peer.setDebugProperty(key, value);
	}

	public final Bullet setFire(final double power) {
		final Bullet b = peer.setFire(power);
		if(b != null) {
			onBulletFired(b);
		}
		return b;
	}

	public final void setGunColor(final Color color) {
		peer.peer.setGunColor(color);
	}

	public final void setMaxTurnRate(final double newMaxTurnRate) {
		peer.peer.setMaxTurnRate(newMaxTurnRate);
	}

	public final void setMaxVelocity(final double newMaxVelocity) {
		peer.peer.setMaxVelocity(newMaxVelocity);
	}

	public final void setMove(final double distance) {
		peer.peer.setMove(distance);
	}

	
	public void setOut(final PrintStream out) {
		this.out = out;
	}

	
	public void setPeer(final GunBase peer) {
		this.peer = peer;
//		this.peer.peer.addCustomEvent(new TurnEndedEventCondition());
//		g = this.peer.peer.getGraphics();
	}
	
	protected void updateGraphics() {
		g = this.peer.peer.getGraphics();
	}

	public final void setRadarColor(final Color color) {
		peer.peer.setRadarColor(color);
	}

	public final void setScanColor(final Color color) {
		peer.peer.setScanColor(color);
	}

	public final void setTurnBody(final double radians) {
		peer.peer.setTurnBody(radians);
	}

	public final void setTurnGun(final double radians) {
		peer.peer.setTurnGun(radians);
	}

	public final void setTurnRadar(final double radians) {
		peer.peer.setTurnRadar(radians);
	}
}
