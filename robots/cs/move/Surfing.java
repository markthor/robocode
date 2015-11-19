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
/*
 * Dear Future Chase,
 * 
 * I cannot even begin to express how deeply sorry I am.
 * But I assure you, at the time, I (probably) knew what I was doing.
 * 
 * -Chase
 * 
 * P.S. I am trying to leave comments,
 *      but you know how bad I am at those.
 */
package cs.move;

import static cs.utils.FastMath.abs;
import static cs.utils.FastMath.min;
import static cs.utils.FastMath.sign;
import java.awt.Color;
import java.awt.geom.Arc2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.Event;
import robocode.HitByBulletEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import ags.utils.KdTree;
import ags.utils.KdTree.Entry;
import cs.geom.Line;
import cs.geom.Vector;
import cs.move.extra.Data;
import cs.move.extra.DuelSeed;
import cs.move.extra.Move;
import cs.move.extra.Risk;
import cs.move.extra.State;
import cs.move.extra.Wave;
import cs.utils.MaximumDeque;
import cs.utils.Simulate;
import cs.utils.Tools;

/**
 * Movement <br/>
 * <br/>
 * In Japanese, Kakeru means "Dash" (in some cases)
 * 
 * @author Chase
 */
public abstract class Surfing extends MinimumRisk {
	/**
	 * Keep in mind with heat waves we will have waves 2 rounds earlier the the
	 * gun heat would otherwise indicate
	 */
	private static final int MaxTurnHistoryLength = 32;
	private static final int MaxHeatWaveQueueLength = 4;
	private static final int AverageSingleRoundTime = 1200;
	private static final boolean DEBUG_BULLET_HANDLING = false;
	private static final MaximumDeque<Double> enemyBulletPower;
	private static final KdTree<Data> tree;
	static {
		enemyBulletPower = new MaximumDeque<Double>(MaxHeatWaveQueueLength);
		enemyBulletPower.addLast(3.0);
		final int dimensions = new Data().getDataArray().length;
		tree = new KdTree.SqrEuclid<Data>(dimensions, 0);
		DuelSeed.seed(tree);
	}

	private static void prepareWaveData(final Wave<Data> wave) {
		wave.data.bulletFlightTime = wave.data.distance / wave.speed;
	}

	private final LinkedList<Wave<Data>> enemyWaves;
	private final MaximumDeque<State> history;
	private double lastEnemyEnergy;
	private double lastEnemyVelocity;
	private long timeSinceDirectionChange;
	private double enemyGunHeatx10;
	
	/* Inactivity Counter stuff */
	private Wave<Data> lastWave = null;
	private long lastWaveFireTime = 0;
	private double lastWaveFirePower = 0;
	
	private static int enemyBulletFired = 0;
	private static int enemyBulletIntercepted = 0;
	private static int enemyBulletHit = 0;
	private final LinkedList<Bullet> bullets = new LinkedList<Bullet>();
	private long cacheTime = -1;
	private Wave<Data> cachedListsWave = null;
	private List<Entry<Data>> cachedList = null;

	public Surfing() {
		enemyWaves = new LinkedList<Wave<Data>>();
		history = new MaximumDeque<State>(MaxTurnHistoryLength);
		lastEnemyEnergy = 100;
	}

	private void calculateShadow(final Wave<Data> wave, final Bullet b) {
		// until bullet is past wave calculate ahead
		long timeOffset = 0;
		final double x = b.getX();
		final double y = b.getY();
		final double h = b.getHeadingRadians();
		final double v = b.getVelocity();
		do {
			final double r = wave.getRadius(time + timeOffset);
			final Line line = Line.projection(x, y, h, v * timeOffset, v * (timeOffset + 1));
			if(myPosition.distanceSq(line.x1, line.y1) > wave.distanceSq(myPosition) - r * r) break;
			wave.shadowBullet(b, line, time + timeOffset);
		}
		while(++timeOffset < 110);
	}

	private void calculateShadowsForBullet(final Bullet b) {
		for(final Wave<Data> wave : enemyWaves) {
			if(wave.imaginary) continue;
			calculateShadow(wave, b);
		}
	}

	private void calculateShadowsForWave(final Wave<Data> wave) {
		if(wave.imaginary) return;
		final Iterator<Bullet> it = bullets.iterator();
		while(it.hasNext()) {
			final Bullet b = it.next();
			if(!b.isActive()) {
				it.remove();
				continue;
			}
			calculateShadow(wave, b);
		}
	}

	private double checkIntersectionRisk(final Wave<Data> wave) {
		double risk = 0;
		final double factorCenter = (wave.minFactor + wave.maxFactor) / 2.0;
		List<Entry<Data>> list = null;
		if(cacheTime == time && cachedListsWave == wave) list = cachedList;
		else list = tree.nearestNeighbor(wave.data.getWeightedDataArray(), 64, false);
		int iterate = 0;
		final double hitrate = enemyBulletHit / (double) enemyBulletFired;
		for(final Entry<Data> e : list) {
			final Data data = e.value;
			if(e.distance > 8 + hitrate * 24 && iterate != 0) continue;
			++iterate;
			double eRisk = 0;
			eRisk = 0.15 / (1.0 + abs(data.guessfactor - factorCenter));
			if(wave.minFactor < data.guessfactor && wave.maxFactor > data.guessfactor) eRisk += 0.8;
			eRisk += 0.05 / (1.0 + min(wave.distance(wave.lastTarget), wave.lastTarget.distance(lastEnemyPosition)));
			// wave.distance();
			// min(distance_to_wave_centre,distance_to_enemy)
			final double factorRange = wave.maxFactor - wave.minFactor;
			// how much do the shadows cover our min to max risk area
			double coveredRange = 0;
			for(final double[] intr : wave.bulletShadows) {
				if(intr[0] >= wave.maxFactor || intr[1] <= wave.minFactor) continue;
				double minf = intr[0];
				double maxf = intr[1];
				if(minf < wave.minFactor) minf = wave.minFactor;
				if(maxf > wave.maxFactor) maxf = wave.maxFactor;
				coveredRange += maxf - minf;
			}
			final double ratio = coveredRange / factorRange;
			eRisk *= 1.0 - ratio;
			final double timeWeight = AverageSingleRoundTime / (double) (AverageSingleRoundTime + globalTime - data.globalTime);
			final double weight = data.weight / (1.0 + e.distance) * timeWeight;
			risk += eRisk * weight;
		}
		cachedList = list;
		cachedListsWave = wave;
		cacheTime = time;
		return risk / list.size();
	}

	private void checkMove(final Simulate sim, final Wave<Data> wave, final int orbitDirection) {
		final Move move = predictMove(sim.position, wave, sim.heading, sim.velocity, orbitDirection);
		sim.angleToTurn = move.angleToTurn;
		sim.direction = move.direction;
		sim.maxVelocity = min(sim.maxVelocity, move.maxVelocity);
	}

	private double checkStandingRisk(final Wave<Data> wave, final Vector target) {
		wave.resetFactors();
		wave.standingIntersection(target);
		return checkIntersectionRisk(wave);
	}

	private Risk checkWaveRisk(final Wave<Data> wave, final int orbitDirection, final double maxVelocity) {
		/*
		 * Simplify the output to our direction chooser
		 */
		final Risk waveRisk = new Risk();
		waveRisk.orbitDirection = orbitDirection;
		waveRisk.maxVelocity = maxVelocity;
		/*
		 * Create a simulator for our movement
		 */
		final Simulate sim = createSimulator();
		sim.maxVelocity = maxVelocity;
		sim.direction = orbitDirection;
		/*
		 * Used for our distance danger
		 */
		final double currentDistance = wave.distance(sim.position);
		double predictedDistance = 0;
		int intersected = 0;
		long timeOffset = 0;
		/*
		 * Reset the factors, so we do not get cross risk check contamination
		 */
		wave.resetFactors();
		if(isPainting)
			g.setColor(Color.ORANGE);
		/*
		 * Since our radar is limited to 1200 and the slowest bullet moves at 11
		 * per second, calculating past 110 is pointlessly impossible, so it is
		 * a safe number to use as our break point.
		 */
		while(timeOffset < 110) {
			// DRAW DNGER MOVEMENT
			if(isPainting)
				g.drawOval((int) sim.position.x - 3, (int) sim.position.y - 3, 6, 6);
			if(wave.doesIntersect(sim.position, time + timeOffset)) {
				++intersected;
				predictedDistance += wave.distance(sim.position);
			} else if(intersected > 0) {
				predictedDistance /= intersected;
				waveRisk.risk += checkIntersectionRisk(wave);
				final Wave<Data> second = findBestSurfableWave(wave);
				if(second != null) {
					// do second wave checking
					final Simulate reverse = sim.copy();
					reverse.direction = -orbitDirection;
					double bestRisk = Double.POSITIVE_INFINITY;
					boolean doBreak = false;
					final long timeLimit = timeOffset + 30;
					if(isPainting)
						g.setColor(Color.RED);
					int n0 = 0;
					int n1 = 0;
					while(timeOffset < timeLimit) {
						// for every position along reverse and sim, calculate
						// the PI
						double risk = checkStandingRisk(second, reverse.position);
						if(risk > 0 && risk < bestRisk) bestRisk = risk;
						if(second.intersects(reverse.position, time + timeOffset)) ++n0;
						else if(n0 != 0) doBreak = true;
						risk = checkStandingRisk(second, sim.position);
						if(risk > 0 && risk < bestRisk) bestRisk = risk;
						if(second.intersects(reverse.position, time + timeOffset)) ++n1;
						else if(n1 != 0 && doBreak) break;
						if(isPainting) {
							g.drawRect((int) reverse.position.x - 2, (int) reverse.position.y - 2, 4, 4);
							g.drawRect((int) sim.position.x - 2, (int) sim.position.y - 2, 4, 4);
						}
						checkMove(reverse, wave, -orbitDirection);
						reverse.step();
						reverse.maxVelocity = maxVelocity;
						checkMove(sim, wave, orbitDirection);
						sim.step();
						sim.maxVelocity = maxVelocity;
						++timeOffset;
					}
					if(bestRisk < 4) waveRisk.risk += bestRisk / 2.0;
				}
				break;
			}
			checkMove(sim, wave, orbitDirection);
			sim.step();
			sim.maxVelocity = maxVelocity;
			++timeOffset;
		}
		double distanceRisk = currentDistance / predictedDistance;
		distanceRisk *= distanceRisk;
		waveRisk.risk *= distanceRisk;
		return waveRisk;
	}

	private void checkWaves() {
		final Iterator<Wave<Data>> waveIterator = enemyWaves.iterator();
		while(waveIterator.hasNext()) {
			final Wave<Data> wave = waveIterator.next();
			final double radius = wave.getRadius(time);
			/* /////////////////////// */
			/* DRAW WAVE */
			
			//disable draw if we are not painting
			if(isPainting) {
				g.setColor(new Color(128, 128, 128));
				final double escape = abs(wave.escapeAngle);
				if(wave.imaginary) g.setColor(new Color(255, 0, 0));
				
				g.draw(new Arc2D.Double(wave.x-radius,wave.y-radius,radius*2,radius*2,
						Math.toDegrees(wave.directAngle - escape)-90, Math.toDegrees(escape * 2), Arc2D.OPEN));
				
				final double len = 6;
				
				g.setColor(Color.CYAN);
				for(final double[] range : wave.bulletShadows) {
					g.draw(Line.projection(wave.x, wave.y, range[0] * wave.escapeAngle + wave.directAngle, radius, radius + len));
					g.draw(Line.projection(wave.x, wave.y, range[1] * wave.escapeAngle + wave.directAngle, radius, radius + len));
					
					double start = Math.toDegrees(range[0] * wave.escapeAngle + wave.directAngle) - 90;
					double extend = Math.toDegrees(range[1] * wave.escapeAngle + wave.directAngle) - 90;
					
					double cLen = len / 2.0;
					double offset = radius + cLen;
					double r = radius*2+2*cLen;
					
					g.draw(new Arc2D.Double(wave.x-offset,wave.y-offset,r,r,start, extend-start, Arc2D.OPEN));
				}
				
			}
			/* END DRAW WAVE */
			/* /////////////////////// */
			
			if(wave.imaginary) {
				/*
				 * if the wave is imaginary (a heat wave) we should remove it
				 * automatically after 2 ticks which is when we will detect
				 * their actual wave, if they fire
				 */
				if(!isAlone && time - wave.fireTime > 2) waveIterator.remove();
			} else
			/* Clean up waves that can no longer hit us */
			if(wave.didIntersect(myPosition, time)) {
				onWaveRemoval(wave, false);
				waveIterator.remove();
			}
		}
	}

	private Wave<Data> createMovementWave(final double power, final int direction) {
		final Wave<Data> wave = new Wave<Data>();
		wave.power = power;
		wave.speed = Rules.getBulletSpeed(wave.power);
		wave.escapeAngle = Math.asin(8.0 / wave.speed) * direction;
		return wave;
	}

	private void doMovement() {
		final Wave<Data> wave = findBestSurfableWave(null);
		if(wave == null || DEBUG_WAVELESS_MOVEMENT) {
			doWavelessMovement();
			return;
		}
		if(isPainting) {
			g.setColor(Color.WHITE);
			g.draw(field);
		}
		doSurfingMovement(wave);
	}

	private void doSurfingMovement(final Wave<Data> wave) {
		/* Do the actual surfing and such */
		setMaxVelocity(Rules.MAX_VELOCITY);
		final Risk[] directions = new Risk[] { 
				checkWaveRisk(wave, orbitDirection, 0),
				checkWaveRisk(wave, orbitDirection, Rules.MAX_VELOCITY),
				checkWaveRisk(wave, -orbitDirection, Rules.MAX_VELOCITY)
			};
		int bestIndex = 0;
		double minRisk = Double.POSITIVE_INFINITY;
		for(int i = 0; i < directions.length; ++i)
			if(directions[i].risk < minRisk) {
				bestIndex = i;
				minRisk = directions[i].risk;
			}
		final Move move = predictMove(myPosition, wave, status.getHeadingRadians(), status.getVelocity(),
				directions[bestIndex].orbitDirection);
		final double maxVelocity = min(directions[bestIndex].maxVelocity, move.maxVelocity);
		setMaxVelocity(maxVelocity);
		setTurnBody(move.angleToTurn);
		setMove(1000 * move.direction);
		onMoveDecided(maxVelocity, move.angleToTurn, 1000 * move.direction);
	}

/*	private Wave<Data> findBestSurfableWave(final Wave<Data> exclude) {
		final double halfBotWidth = 18 + Math.sin(lastEnemyPosition.angleTo(myPosition)) * 7.4558441;
		// find the wave soonest to hit us
		Wave<Data> wave = null;
		double bestETA = Double.POSITIVE_INFINITY;
		for(final Wave<Data> check : enemyWaves) {
			if(check == exclude) continue;
			final double distance = myPosition.distance(check) - check.getRadius(time);
			final double ETA = distance / check.speed;
			if(distance < 0 && Math.abs(distance) + check.speed > halfBotWidth) continue;
			if(ETA < bestETA) {
				wave = check;
				bestETA = ETA;
			}
		}
		return wave;
	}*/
	
	private Wave<Data> findBestSurfableWave(final Wave<Data> exclude) {
		final double halfBotWidth = 18 + Math.sin(lastEnemyPosition.angleTo(myPosition)) * 7.4558441;
		// find the wave soonest to hit us
		Wave<Data> wave = null;
		double bestFitness = Double.NEGATIVE_INFINITY;
		for(final Wave<Data> check : enemyWaves) {
			if(check == exclude) continue;
			final double distance = myPosition.distance(check) - check.getRadius(time);
			final double ETA = distance / check.speed;
			if(distance < 0 && Math.abs(distance) + check.speed > halfBotWidth) continue;
			final double fitness = check.power / ETA;
			if(fitness > bestFitness) {
				wave = check;
				bestFitness = fitness;
			}
		}
		return wave;
	}

	/**
	 * Handle an actual enemy bullet
	 */
	private void handleBullet(final Bullet b, final boolean collision) {
		final Vector bulletPosition = new Vector(b.getX(), b.getY());
		if(DEBUG_BULLET_HANDLING) if(collision) println(time + "- Bullet Bullet Collision");
		else println(time + "- Bullet Robot Collision");
		final double bulletDistToEnemy = lastEnemyPosition.distance(bulletPosition);
		/* Find the matching wave */
		final Iterator<Wave<Data>> wi = enemyWaves.iterator();
		while(wi.hasNext()) {
			final Wave<Data> wave = wi.next();
			if(wave.imaginary && time - wave.fireTime < 3) if(!collision || bulletDistToEnemy > 2) continue;
			/*
			 * check if the power is close enough to be our wave. This margin is
			 * small, only to allow room for rounding errors
			 */
			if(abs(b.getPower() - wave.power) < 0.001 || wave.imaginary && abs(b.getPower() - wave.power) < 0.5) {
				final double d = wave.distanceSq(bulletPosition);
				/* the current distance of the wave */
				boolean match = false;
				if(collision) {
					final double rad1 = wave.speed * (time - 1 - wave.fireTime);
					final double rad2 = wave.speed * (time - 2 - wave.fireTime);
					if(abs(rad1 * rad1 - d) < 128) {
						if(DEBUG_BULLET_HANDLING) println("Match on BD1");
						match = true;
					} else if(abs(rad2 * rad2 - d) < 128) {
						if(DEBUG_BULLET_HANDLING) println("Match on BD2");
						match = true;
					}
				} else {
					final double rad0 = wave.speed * (time - wave.fireTime);
					if(abs(d - rad0 * rad0) < 128) match = true;
				}
				if(match) {
					/*
					 * Alternate method, but why recalculate something? Could be
					 * used as an extra check.
					 */
					/* updateTree(wave,wave.angleTo(bulletPosition)); */
					updateTree(wave, b.getHeadingRadians());
					onWaveRemoval(wave, true);
					wi.remove();
					if(wave.imaginary) // just to keep everything nice and
										// balanced
						++enemyBulletFired;
					return;
				}
			}
		}
		/* If not found say so, cause something f*cked up! */
		// println("Error: Unknown Bullet Collision");
		// println("\tBullet:["+b.getX()+","+b.getY()+"] @ h"+b.getHeadingRadians()+" @ p"
		// + b.getPower());
		printf("Error: Unknown Bullet Collision\n\tBullet:[%.4f,%.4f] @ h%.4f @ p%.4f\n\tEnemy:[%.4f,%.4f] D To B %.4f\nWaves:\n",
				b.getX(), b.getY(), b.getHeadingRadians(), b.getPower(), lastEnemyPosition.x, lastEnemyPosition.y, bulletDistToEnemy);
		for(final Wave<Data> wave : enemyWaves) {
			final double rad = wave.speed * (time - wave.fireTime);
			final double rad1 = wave.speed * (time - 1 - wave.fireTime);
			final double d = wave.distanceSq(bulletPosition);
			printf("\tp %.4f  d %.4f  d1 %.4f  i %b\n", wave.power, abs(rad * rad - d), abs(rad1 * rad1 - d), wave.imaginary);
		}
		// enemyEnergyHistory
	}

	@Override
	public void onBulletFired(final Bullet b) {
		super.onBulletFired(b);
		bullets.add(b);
		// calculate where it will be on all future waves
		calculateShadowsForBullet(b);
	}

	/**
	 * Account for our bullets hitting the enemy
	 */
	@Override
	public void onBulletHit(final BulletHitEvent e) {
		super.onBulletHit(e);
		/* Get the power of the bullet of ours that hit */
		final double bulletPower = e.getBullet().getPower();
		/*
		 * Determine how much damage our bullet does to the enemy and adjust our
		 * stored copy to reflect the amount lost
		 */
		lastEnemyEnergy -= Rules.getBulletDamage(bulletPower);
	}

	/**
	 * Account for one of our bullets hitting an enemy bullet.
	 */
	@Override
	public void onBulletHitBullet(final BulletHitBulletEvent e) {
		super.onBulletHitBullet(e);
		++enemyBulletIntercepted;
		removeShadow(e.getBullet());
		handleBullet(e.getHitBullet(), true);
	}

	/**
	 * Account for one of the enemies bullets hitting us.
	 */
	@Override
	public void onHitByBullet(final HitByBulletEvent e) {
		super.onHitByBullet(e);
		/*
		 * Increase the enemy energy based on how powerful the bullet that hit
		 * us was this is so we can get reliable energy drop detection
		 */
		lastEnemyEnergy += Rules.getBulletHitBonus(e.getPower());
		++enemyBulletHit;
		handleBullet(e.getBullet(), false);
	}

	@Override
	public void onRoundStarted(final Event e) {
		super.onRoundStarted(e);
		/* Set it to be the same as our gun heat on round start! */
		enemyGunHeatx10 = status.getGunHeat() * 10;
		/* Our scans are 1 round late */
		enemyGunHeatx10 -= coolingRate * 10;
	}

	@Override
	public void onScannedRobot(final ScannedRobotEvent e) {
		super.onScannedRobot(e);
		final double angleToEnemy = e.getBearingRadians() + status.getHeadingRadians();
		final double enemyVelocity = e.getVelocity();
		final State state = new State();
		state.myPosition = myPosition;
		state.enemyPosition = myPosition.projectNew(angleToEnemy, e.getDistance());
		state.data.lateralVelocity = status.getVelocity() * Math.sin(e.getBearingRadians());
		state.data.advancingVelcity = status.getVelocity() * Math.cos(e.getBearingRadians());
		state.data.distance = e.getDistance();
		state.data.globalTime = globalTime;
		int direction = sign(state.data.lateralVelocity);
		if(direction == orbitDirection) ++timeSinceDirectionChange;
		else timeSinceDirectionChange = 0;
		orbitDirection = direction;
		history.addFirst(state);
		state.data.timeSinceDirectionChange = timeSinceDirectionChange;
		state.data.wallForward = Tools.getWallDistance(state.enemyPosition, field, state.data.distance,
				state.enemyPosition.angleTo(state.myPosition), direction);
		state.data.wallReverse = Tools.getWallDistance(state.enemyPosition, field, state.data.distance,
				state.enemyPosition.angleTo(state.myPosition), -direction);
		state.data.distLast10 = myPosition.distance(history.get(min(10, history.size() - 1)).myPosition);
		if(abs(enemyVelocity) == 0.0) if(abs(lastEnemyVelocity) > 2.0) // Hit
																		// the
																		// wall!
			lastEnemyEnergy -= Rules.getWallHitDamage(abs(lastEnemyVelocity));
			else {
				// TODO slightly more complicated check near wall and heading
				// check
			}
		final double enemyEnergy = e.getEnergy();
		/*
		 * Fire new imaginary wave if we can, heat waves are always 2 ticks
		 * previous to a real one 'possibly' being fired
		 */
		if(enemyGunHeatx10 <= 20 * coolingRate && enemyGunHeatx10 > 10 * coolingRate) {
			double power = 0;
			for(final Double d : enemyBulletPower)
				power += d;
			power /= enemyBulletPower.size();
			power = min(enemyEnergy, power);
			/* Create the Wave */
			final Wave<Data> wave = createMovementWave(power, direction);
			wave.setLocation(simulatedEnemyPosition);
			wave.imaginary = true;
			/*
			 * Only +1 because real waves are -1 in time, meaning that by the
			 * time we detect them, it has already been 1 turn since it was
			 * fired, meaning heat waves +2 makes it only +1 here. :-)
			 */
			wave.fireTime = time + 1;
			wave.directAngle = wave.angleTo(state.myPosition);
			wave.data = state.data;
			prepareWaveData(wave);
			enemyWaves.add(wave);
		}
		/* Check for new enemy wave */
		double energyDelta = lastEnemyEnergy - enemyEnergy;
		if(energyDelta > 0 && energyDelta <= 3.0 && history.size() > 2) {
			boolean addWave = true;
			
			if(time - lastWaveFireTime == 1) {
				//assume we have inactivity counter
				
				//remove last wave if it didn't exist
				if(Math.abs(lastWaveFirePower - 0.1) < 0.001) {
					enemyWaves.remove(lastWave);
					--enemyBulletFired;
				}
				
				//adjust our current wave for the inactivity counter
				if(Math.abs(energyDelta - 0.1) < 0.001) {
					addWave = false;
				} else if(energyDelta - 0.1 > 0) {
					energyDelta -= 0.1;
				}
			}
			
			lastWaveFireTime = time;
			lastWaveFirePower = energyDelta;
			
			if(addWave) {
				enemyGunHeatx10 = Rules.getGunHeat(energyDelta) * 10;
				final State old = history.get(2);
				direction = sign(old.data.lateralVelocity);
				
				final Wave<Data> wave = createMovementWave(energyDelta, direction);
				wave.setLocation(lastEnemyPosition);
				wave.fireTime = time - 1;
				wave.directAngle = wave.angleTo(old.myPosition);
				wave.data = old.data;
				enemyBulletPower.addLast(wave.power);
				
				prepareWaveData(wave);
				calculateShadowsForWave(wave);
				enemyWaves.add(wave);
				++enemyBulletFired;
				
				lastWave = wave;
			}
		}
		if(DEBUG_WAVELESS_MOVEMENT) {
			if(lastEnemyPosition.x < 0) lastEnemyPosition = myPosition.projectNew(angleToEnemy, 50);
		} else lastEnemyPosition = state.enemyPosition;
		lastEnemyVelocity = enemyVelocity;
		lastEnemyEnergy = enemyEnergy;
	}

	@Override
	public void onTurnEnded(final Event e) {
		super.onTurnEnded(e);
		/*
		 * Don't move if we are scanning, this is mostly start of round fast
		 * radar finding protection
		 */
		if(initialTick != 0) return;
		/*
		 * We track the gun heat because it is important for our heat/imaginary
		 * waves
		 */
		enemyGunHeatx10 -= coolingRate * 10;
		if(enemyGunHeatx10 <= 0) enemyGunHeatx10 = 0;
		/*
		 * Take are of all waves that cannot possibly even conceivably hit us
		 * anymore
		 */
		checkWaves();
		/* Run our 'actual' movement */
		doMovement();
		if(isPainting && enemyBulletFired > 0) {
			g.setColor(Color.WHITE);
			g.drawString(sprintf("Enemy Bullet Intercept Rate: %.4f%%", enemyBulletIntercepted / (double) enemyBulletFired * 100), 20,
					8 + 18);
			g.drawString(sprintf("Enemy Bullet Hit Rate: %.4f%%", enemyBulletHit / (double) enemyBulletFired * 100), 20, 8 + 12 + 18);
		}
	}

	protected void onWaveRemoval(final Wave<Data> wave, final boolean hit) {}

	private void removeShadow(final Bullet b) {
		for(final Wave<Data> wave : enemyWaves) {
			// check if bullet has yet to pass through wave
			final double r = wave.getRadius(time);
			final double d = wave.distanceSq(myPosition) - r * r;
			if(d > myPosition.distanceSq(b.getX(), b.getY())) // if it hasn't,
																// remove it
																// from the wave
				wave.removeShadow(b);
		}
	}

	private void updateTree(final Wave<Data> wave, final double angleToBullet) {
		final double angleOffset = Utils.normalRelativeAngle(angleToBullet - wave.directAngle);
		wave.data.guessfactor = angleOffset / wave.escapeAngle;
		tree.addPoint(wave.data.getWeightedDataArray(), wave.data);
	}
}
