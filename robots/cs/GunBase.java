/**
 * Copyright (c) 2011 Chase
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

package cs;

import java.awt.geom.Point2D;
import cs.mint.Mint;
import cs.move.Surfing;
import robocode.BulletHitEvent;
import robocode.Event;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

/**
 * Weapon
 * <br/><br/>
 * In Classical Japanese, Daikama means "Scythe", Dai (large), Kama (sickle)
 * 
 * @author Chase
 */
public abstract class GunBase extends Surfing {
	private Mint mint = new Mint(this);
	
	@Override
	public void onScannedRobot(final ScannedRobotEvent e) {
		super.onScannedRobot(e);
		if(initialTick != 0)
			return;
		mint.onScannedRobot(e);
	}

	@Override
	public void onStatus(final StatusEvent e) {
		super.onStatus(e);
		mint.onStatus(e);
	}

	public void onBulletHit(final BulletHitEvent e) {
		super.onBulletHit(e);
		mint.onBulletHit(e);
	}

	public void onHitByBullet(final HitByBulletEvent e) {
		super.onHitByBullet(e);
		mint.onHitByBullet(e);
	}
	
	@Override
	public void onTurnEnded(final Event e) {
		super.onTurnEnded(e);
		mint.onTurnEnded(e);
	}
	
	protected void onNextPosition(Point2D p) {
		super.onNextPosition(p);
		mint.setNextPosition(p);
	}
}
