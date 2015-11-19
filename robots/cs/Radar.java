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

import cs.geom.Vector;
import cs.utils.Simulate;
import robocode.Event;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 * Radar
 * <br/><br/>
 * In Japanese, Chikaku means "Perception"
 * 
 * @author Chase
 */
public abstract class Radar extends RobotBase {
	public boolean initialScan = true;
	public long initialTick = 2;
	public long lastScan = 0;
	public Vector simulatedEnemyPosition = new Vector();

	public void initialScan() {
		initialScan = true;
		initialTick = 2;
		lastScan = time;

		//angle difference between center of field and our robot radar heading
		double angleDifference = Utils.normalRelativeAngle(myPosition.angleTo(fieldCenter)
				- status.getRadarHeadingRadians());
		double turnDirection = Math.signum(angleDifference);

		// Set the turns a little over what they need to be
		setTurnRadar(turnDirection * Math.PI * 3.0);
		setTurnGun(turnDirection * Math.PI * 1.3);
	}


	@Override
	public void onRoundStarted(Event e) {
		super.onRoundStarted(e);
		initialScan();
	}

	@Override
	public void onTurnEnded(Event e) {
		super.onTurnEnded(e);
		if(isAlone) return;

		if(!initialScan && initialTick > 0)
			--initialTick;

		if(time - lastScan > 8) {
			//out.println("Error");
			debug("Scan failure, redoing initial scan.");
			initialScan();
		}
	}

	private Simulate sim = new Simulate();
	@Override
	public void onScannedRobot(ScannedRobotEvent e) {
		super.onScannedRobot(e);

		lastScan = time;

		double radarHeading = status.getRadarHeadingRadians();

		//How deep into the enemy robot do we want to scan?
		double scan_depth = 36.0;

		//Determine the angle to the enemy
		double angleToEnemy = status.getHeadingRadians() + e.getBearingRadians();

		if(initialTick == 0) {
			//calculate enemies very likely next position
			sim.position.setLocationAndProject(myPosition, angleToEnemy, e.getDistance());
			sim.velocity = e.getVelocity();
			sim.heading = e.getHeadingRadians();
			sim.direction = (int)Math.signum(sim.velocity);
			sim.step();

			Vector enemy = sim.position.clone();
			simulatedEnemyPosition = enemy;

			//calculate our very likely next position
			sim.position.setLocation(myPosition);
			sim.velocity = status.getVelocity();
			sim.heading = status.getHeadingRadians();
			sim.direction = (int)Math.signum(sim.velocity);
			sim.step();

			angleToEnemy = sim.position.angleTo(enemy);

			scan_depth = 4.0;
		}

		//Determine the farthest they could have traveled since we last saw them
		double travel = 8.0 * (time - lastScan) + scan_depth;

		// Calculate the smallest turn to its last position
		double turnToEnemy = Utils.normalRelativeAngle(angleToEnemy - radarHeading);

		// Determine the worst case lateral movement for them based on the last scan time
		double worstCaseOffset = Math.atan(travel / (e.getDistance() - (travel / 2.0)));

		// Recalculate the turn to get to the enemy based on the worst case movement
		//turn = Utils.normalRelativeAngle(angle + offset*Tools.sign(turn) - current.radarHeading);
		turnToEnemy = Utils.normalRelativeAngle(angleToEnemy + worstCaseOffset*Math.signum(turnToEnemy) - radarHeading);

		setTurnRadar(turnToEnemy);

		if(initialScan) {
			//TODO set this up so it can be used by melee as well when we have a high gun heat

			//set the gun turn to zero in case we do not go over
			setTurnGun(0);

			//here we handle supplementing the radar turn with gun and robot turn
			double absoluteTurnToEnemy = Math.abs(turnToEnemy);

			//check if we have to turn more then our radar can by itself
			if(absoluteTurnToEnemy > Rules.RADAR_TURN_RATE_RADIANS) {
				double amountOfTurnOverMaximumTurn = absoluteTurnToEnemy - Rules.RADAR_TURN_RATE_RADIANS;

				//supplement the turn
				angleToEnemy = Math.min(Rules.GUN_TURN_RATE_RADIANS, amountOfTurnOverMaximumTurn);
				setTurnGun(angleToEnemy*Math.signum(turnToEnemy));

				//check if we have to turn more then our gun can supplement
				if(amountOfTurnOverMaximumTurn > Rules.GUN_TURN_RATE_RADIANS) {
					amountOfTurnOverMaximumTurn = amountOfTurnOverMaximumTurn - Rules.GUN_TURN_RATE_RADIANS;

					//supplement the turn
					angleToEnemy = Math.min(Rules.MAX_TURN_RATE_RADIANS, amountOfTurnOverMaximumTurn);
					setTurnBody(angleToEnemy*Math.signum(turnToEnemy));

					//sometimes we require more turn then everything can give us, but usually it doesn't matter
				}
			}

			initialScan = false;
		}
	}
}
