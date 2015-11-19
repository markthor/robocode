package cs.move;

import static cs.utils.FastMath.abs;
import static cs.utils.FastMath.limit;
import static cs.utils.FastMath.min;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import robocode.Rules;
import robocode.util.Utils;
import cs.Radar;
import cs.geom.Line;
import cs.geom.Rectangle;
import cs.geom.Vector;
import cs.move.extra.Move;
import cs.move.extra.Wave;
import cs.utils.Simulate;
import cs.utils.Tools;

public abstract class MinimumRisk extends Radar {
	protected static final boolean DEBUG_WAVELESS_MOVEMENT = false;

	private static final int TimeBeforeFirstWaveToStartTurning = 4;
	
	/*
	 * Distancer Constants, control how the distancer acts,
	 * limiter is linear equation
	 */
	private static final double distBestDistance = 500.0;

	/* distancer angle limiting */
	private static final double distMinDistance = 300;
	private static final double distMinRotation = Math.PI/4.0;
	private static final double distMaxDistance = 800;
	private static final double distMaxRotation = Math.PI/10.0;
	private static final double distMultiplier = (distMaxRotation-distMinRotation)/(distMaxDistance-distMinDistance);
	private static final double distAdder = distMinRotation-distMultiplier*distMinDistance;
	
	
	protected Vector lastEnemyPosition = new Vector(-1,-1);
	protected int orbitDirection = 1;
	
	protected void doWavelessMovement() {
		final int initialTurns = (int)Math.ceil(3.0/coolingRate)+4;
		double safeTurns = status.getGunHeat()/coolingRate;

		if(time < initialTurns) {
			/* Do we have enough time to move around before they can start firing? */
			if(safeTurns > TimeBeforeFirstWaveToStartTurning || DEBUG_WAVELESS_MOVEMENT) {
				doMinimalRiskMovement();
			} else {
				Move data = predictMove(myPosition,lastEnemyPosition,
						status.getHeadingRadians(),status.getVelocity(),orbitDirection);
				
				setTurnBody(data.angleToTurn);
				setMaxVelocity(0);
				setMove(0);

				onMoveDecided(0,data.angleToTurn,0);
			}
		} else {
			/*
			 * Things are no longer safe, set our movement to `sane`
			 * 
			 * TODO: RAM if enemy is disabled
			 */
			if(status.getOthers() == 0) {
				/*
				 * No bullets in the air, enemy dead, we can do our victory DANCE (YAY!)
				 */
				doVictoryDance();
			} else {
				//Some other thing to do when nothing is in the air??
				doMinimalRiskMovement();
			}
		}
	}

	protected void doVictoryDance() {}

	protected void doMinimalRiskMovement() {
		double heading = status.getHeadingRadians();
		double velocity = status.getVelocity();

		Rectangle escapeField = new Rectangle(30,30,fieldWidth-60,fieldHeight-60);
		if(isPainting) {
			g.setColor(Color.WHITE);
			g.draw(escapeField);
		}


		if(DEBUG_WAVELESS_MOVEMENT) {
			Wave.hitbox.setFrame(lastEnemyPosition, 36, 36);
			if(isPainting) {
				g.setColor(Color.RED);
				g.draw(Wave.hitbox);
			}
		}

		//Do minimal risk movement
		Vector target = myPosition.clone();
		Vector bestTarget = myPosition;
		double angle = 0;
		double bestRisk = checkWavelessRisk(escapeField,bestTarget);
		double enemyDistance = myPosition.distance(lastEnemyPosition);

		//a little dynamic distancing
		//enemyDistance += 18*max((enemyDistance-36-50)/100.0,1.0);
		enemyDistance += limit(-18, -24.48+0.18*enemyDistance ,18);
		if(isPainting) {
			g.draw(new Line(
					target.projectNew(heading, Math.signum(velocity)*75),
					target.projectNew(heading, Math.signum(velocity)*150)));
		}

		while(angle < Math.PI*2) {
			double targetDistance = min(200,enemyDistance);

			target.setLocationAndProject(myPosition, angle, targetDistance);
			if(escapeField.contains(target)) {

				// a little nudge to perfer the direction its already going to
				// help avoid both equal opposite jam issues

				double risk = checkWavelessRisk(escapeField,target);

				if(risk < bestRisk) {
					bestRisk = risk;
					bestTarget = target.clone();
				}
				if(isPainting) {
					g.setColor(Color.BLUE);
					g.drawRect((int)target.x-2, (int)target.y-2, 4, 4);
				}

			}
			angle += Math.PI/32.0;
		}
		if(isPainting) {
			g.setColor(bodyColor);
			g.drawRect((int)bestTarget.x-2, (int)bestTarget.y-2, 4, 4);
		}

		double travelAngle = myPosition.angleTo(bestTarget);

		double forward = myPosition.distance(bestTarget);

		double angleToTurn = Utils.normalRelativeAngle(travelAngle - status.getHeadingRadians());
		int direction = 1;

		if(abs(angleToTurn) > Math.PI/2.0) {
			angleToTurn = Utils.normalRelativeAngle(angleToTurn - Math.PI);
			direction = -1;
		}

		//Slow down so we do not ram head long into the walls and can instead turn to avoid them
		double maxVelocity = Rules.MAX_VELOCITY;

		if(!field.contains(myPosition.projectNew(heading, velocity*3.25)))
			maxVelocity = 0;

		if(!field.contains(myPosition.projectNew(heading, velocity*5)))
			maxVelocity = 4;

		if(angleToTurn > 0.7 && velocity < 7) {
			maxVelocity = 0;
		}

		if(DEBUG_WAVELESS_MOVEMENT) {
			g.setColor(Color.WHITE);
			g.drawString("v "+status.getVelocity(), (float)myPosition.x-16, (float)myPosition.y-36-12);
		}

		setMaxVelocity(maxVelocity);
		setTurnBody(angleToTurn);
		setMove(forward*direction);

		onMoveDecided(maxVelocity,angleToTurn,forward*direction);
	}

	private double checkWavelessRisk(Rectangle field, Vector pos) {
		double risk = 100.0/pos.distanceSq(lastEnemyPosition);

		for(double[] edge : field.getEdges()) {
			risk += 5.0/(1.0+Line2D.ptSegDistSq(edge[0], edge[1], edge[2], edge[3], pos.x, pos.y));
		}

		/*
		 * get points between enemy location and corner and add risk!!!!
		 * these are bad places to be! Our hitbox is larger here if nothing else!
		 */
		for(double[] corner : field.getCorners()) {
			corner[0] = (corner[0]+lastEnemyPosition.x)/2.0;
			corner[1] = (corner[1]+lastEnemyPosition.y)/2.0;
			if(lastEnemyPosition.distanceSq(corner[0], corner[1]) < 22500 /* 150 */) {
				g.drawRect((int)corner[0]-2, (int)corner[1]-2, 4, 4);
				risk += 5.0/(1.0+pos.distanceSq(corner[0], corner[1]));
			}
		}

		return risk;
	}
	
	protected void onMoveDecided(double maxVelocity, double angleToTurn, double toMove) {
		Simulate sim = createSimulator();
		sim.maxVelocity = maxVelocity;
		sim.angleToTurn = angleToTurn;
		sim.direction = (int) Math.signum(toMove);
		sim.stepPrecise();
		onNextPosition(sim.position);
	}
	
	protected void onNextPosition(Point2D p) {
		
	}
	
	/**
	 * Calculate the driving
	 */
	protected Move predictMove(Vector myPosition, Vector orbitCenter,
			double myHeading, double myVelocity, int orbitDirection) {
		Move data = new Move();

		/* Better safe then very very sorry */
		if(orbitDirection == 0)
			orbitDirection = 1;

		double angleToRobot = orbitCenter.angleTo(myPosition);

		/*
		 * if the orbit direction is clockwise/counter, we want to try and
		 * point our robot in that direction, which is why we multiply by it
		 */
		double travelAngle = angleToRobot + (Math.PI/2.0) * orbitDirection;

		/* DONE add distancing to drive method */
		/* TODO add a better distancing method */
		double distance = myPosition.distance(orbitCenter);
		double distancing = ((distance-distBestDistance)/distBestDistance);

		double limit = limit(
				distMinRotation,
				distMultiplier*distance+distAdder,
				distMaxRotation);

		distancing = limit(-limit, distancing, limit);

		travelAngle += distancing*orbitDirection;

		travelAngle = Tools.fastSmooth(myPosition, travelAngle, orbitDirection,
				distance, fieldWidth, fieldHeight);

		data.angleToTurn = Utils.normalRelativeAngle(travelAngle - myHeading);
		data.direction = 1;

		/*
		 * If our backend is closer to direction, use that instead, and
		 * inform the caller that we are going to be going in reverse instead
		 */
		if(abs(data.angleToTurn) > Math.PI/2.0) {
			data.angleToTurn = Utils.normalRelativeAngle(data.angleToTurn - Math.PI);
			data.direction = -1;
		}

		/*
		 * Slow down so we do not ram head long into the walls and can instead turn to avoid them
		 * Still here (just in case)
		 */
		if(!field.contains(myPosition.projectNew(myHeading, myVelocity*3.25)))
			data.maxVelocity = 0;

		if(!field.contains(myPosition.projectNew(myHeading, myVelocity*5)))
			data.maxVelocity = 4;

		return data;
	}

}
