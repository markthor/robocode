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
import java.awt.geom.Point2D;
import robocode.BulletHitEvent;
import robocode.Event;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import cs.GunBase;
import cs.mint.gun.CloverGun;
import cs.mint.util.Rectangle;
import cs.mint.util.Vector;

public class Mint extends RobotBase {
	public MintState lastState;
	public MintState state;
	public CloverGun gun = new CloverGun(this);

	public Mint(GunBase base) {
		this.setPeer(base);
	}
	
	private void doBattleStart() {
		MintState.battlefieldWidth = (int)getBattleFieldWidth();
		MintState.battlefieldHeight = (int)getBattleFieldHeight();
		MintState.coolingRate = getGunCoolingRate();
		MintState.battlefield = new Rectangle(18, 18, MintState.battlefieldWidth - 36, MintState.battlefieldHeight - 36);
	}
	
	boolean scanned = false;
	
	@Override
	public void onScannedRobot(final ScannedRobotEvent e) {
		scanned = true;
		state.update(e,lastState);
	}

	@Override
	public void onStatus(final StatusEvent e) {
		lastState = state;
		state = new MintState(e,lastState);
		updateGraphics();
		if(0 == state.time) {
			if(0 == state.roundNum)
				doBattleStart();
		}
	}
	
	public void draw() {
		g.setColor(Color.GREEN);
		g.drawRect((int)state.position.x-18, (int)state.position.y-18, 36, 36);
		
		if(state.targetPosition != null) {
			g.setColor(Color.RED);
			g.drawRect((int)state.targetPosition.x-18, (int)state.targetPosition.y-18, 36, 36);
		}
	}

	public void onBulletHit(final BulletHitEvent e) {
		state.update(e);
	}

	public void onHitByBullet(final HitByBulletEvent e) {
		state.update(e);
	}
	
	public void setNextPosition(Point2D p) {
		gun.setNextPosition(new Vector(p));
	}
	
	@Override
	public void onTurnEnded(final Event e) {
//		draw();
//		gun.setNextPosition(state.position);
		if(!scanned)
			return;
		
		gun.update(state);
//		execute();
	}
}
