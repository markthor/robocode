package ar;

import java.awt.*;
import java.util.*;
import java.util.List;

import robocode.*;
import robocode.util.*;

public class TheoryOfEverything extends AdvancedRobot {
	private class VirtualBullet extends Condition {
		private Gun gun;
		private Point2D location;
		private double heading;
		private double velocity;

		public VirtualBullet(Gun gun, Point2D location, double heading,
				double velocity) {
			this.gun = gun;
			this.location = location;
			this.heading = heading;
			this.velocity = velocity;
		}

		public boolean test() {
			location.x += Math.sin(heading) * velocity;
			location.y += Math.cos(heading) * velocity;

			if (Util.distance(location, enemyLocation) < Util.ROBOT_WIDTH) {
				handle();
				virtualBullets.remove(this);
				removeCustomEvent(this);
			} else if (location.x < 0 || location.x > getBattleFieldWidth()
					|| location.y < 0 || location.y > getBattleFieldHeight()) {
				virtualBullets.remove(this);
				removeCustomEvent(this);
			}

			return false;
		}

		private void handle() {
			double proximity = Util.distance(getX(), getY(), location.x,
					location.y);
			boolean leftWall = location.x < 130;
			boolean rightWall = getBattleFieldWidth() - location.x < 130;
			boolean topWall = location.y < 130;
			boolean bottomWall = getBattleFieldHeight() - location.y < 130;
			boolean corner = (leftWall && topWall) || (rightWall && topWall)
					|| (leftWall && bottomWall) || (rightWall && bottomWall);
			boolean wall = leftWall || rightWall || topWall || bottomWall;

			int proximityPosition;
			int wallPosition;
			if (proximity < 200) {
				proximityPosition = 0;
			} else if (proximity < 400) {
				proximityPosition = 1;
			} else {
				proximityPosition = 2;
			}
			if (corner) {
				wallPosition = 2;
			} else if (wall) {
				wallPosition = 1;
			} else {
				wallPosition = 0;
			}

			List<Double> segment = guns.get(gun).get(proximityPosition);
			segment.set(wallPosition, segment.get(wallPosition) + 1);
		}
	}

	/*
	 * Dimension 1: Gun. Dimension 2: Proximity - 200, 400, +. Dimension 3: Near
	 * walls - center, wall, corner.
	 */
	private static Map<Gun, List<List<Double>>> guns = new HashMap<Gun, List<List<Double>>>();
	private List<VirtualBullet> virtualBullets = new LinkedList<VirtualBullet>();

	static {
		guns.put(new HeadOnTargetingGun(), new ArrayList<List<Double>>());
		guns.put(new LinearTargetingGun(), new ArrayList<List<Double>>());
		guns.put(new CircularTargetingGun(), new ArrayList<List<Double>>());
		guns.put(new AntiOscillatorGun(), new ArrayList<List<Double>>());

		for (List<List<Double>> proximity : guns.values()) {
			for (int i = 0; i < 3; i++) {
				List<Double> walls = new ArrayList<Double>();
				proximity.add(walls);
				for (int j = 0; j < 3; j++) {
					walls.add(1.0);
				}
			}
		}
	}

	private Point2D enemyLocation = new Point2D(0, 0);
	private double lastEnergy = 100;
	private Point2D target = new Point2D(0, 0);
	private int targeted = 0;
	// Bugfix for double-fire bug.
	private boolean justFired = false;
	private String enemyName = null;
	private boolean printedMessage = false;

	public void run() {
		setColors(new Color(0, 100, 100), Color.gray, Color.black);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setTurnRadarRight(Double.POSITIVE_INFINITY);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		boolean hasFired = false;
		double energyDifference = e.getEnergy() - lastEnergy;
		if (energyDifference > 0 && energyDifference <= 3) {
			hasFired = true;
		}
		lastEnergy = e.getEnergy();

		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double distance = e.getDistance();
		double enemyX = getX() + distance * Math.sin(absoluteBearing);
		double enemyY = getY() + distance * Math.cos(absoluteBearing);
		enemyLocation = new Point2D(enemyX, enemyY);
		enemyName = e.getName();

		updateMovement(e, hasFired);
		if (e.getEnergy() > 0 && this.getEnergy() > 0) {
			doVirtualBullets(e, hasFired);
		}
		goTo(target);
	}

	private void updateMovement(ScannedRobotEvent e, boolean hasFired) {
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double distance = e.getDistance();

		setTurnRadarLeft(getRadarTurnRemaining());

		if (e.getEnergy() > 0) {
			distance -= ((Math.random() * 200) + 100);
		}
		if ((distance < 300 || hasFired) && e.getEnergy() > 0) {
			absoluteBearing += (Math.random() < 0.5 ? 1 : -1) * Math.PI / 2;
		}

		double enemyX = getX() + distance * Math.sin(absoluteBearing);
		double enemyY = getY() + distance * Math.cos(absoluteBearing);

		if (targeted == 0) {
			target = new Point2D(enemyX, enemyY);
			targeted = 10;
		} else {
			targeted--;
		}
	}

	private void doVirtualBullets(ScannedRobotEvent e, boolean hasFired) {
		if (Math.random() > 0.1) {
			double absoluteBearing = getHeadingRadians()
					+ e.getBearingRadians();
			double distance = e.getDistance();
			double enemyX = getX() + distance * Math.sin(absoluteBearing);
			double enemyY = getY() + distance * Math.cos(absoluteBearing);

			double proximity = Util.distance(getX(), getY(), enemyX, enemyY);
			boolean leftWall = enemyX < 130;
			boolean rightWall = getBattleFieldWidth() - enemyX < 130;
			boolean topWall = enemyY < 130;
			boolean bottomWall = getBattleFieldHeight() - enemyY < 130;
			boolean corner = (leftWall && topWall) || (rightWall && topWall)
					|| (leftWall && bottomWall) || (rightWall && bottomWall);
			boolean wall = leftWall || rightWall || topWall || bottomWall;

			int proximityPosition;
			int wallPosition;
			if (proximity < 200) {
				proximityPosition = 0;
			} else if (proximity < 400) {
				proximityPosition = 1;
			} else {
				proximityPosition = 2;
			}
			if (corner) {
				wallPosition = 2;
			} else if (wall) {
				wallPosition = 1;
			} else {
				wallPosition = 0;
			}

			double bulletPower = Math.min(3.0, getEnergy());
			// double bulletPower = Math.min(Math.min(e.getEnergy() / 10,
			// getEnergy()), Rules.MAX_BULLET_POWER);
			// double bulletPower = Math.max(Math.sin((Math.PI / 100) *
			// e.getEnergy())
			// * Rules.MAX_BULLET_POWER, 1.1);
			// double bulletPower = Math.min(Rules.MAX_BULLET_POWER / 2,
			// getEnergy());
			// double bulletPower = Math.max(Math.min(Rules.MAX_BULLET_POWER
			// * (2.0 / (double) getRoundNum()), Rules.MAX_BULLET_POWER),
			// 1.5);
			// double bulletPower = (getRoundNum() < 3 ? 3 : 2);

			Map<Gun, Double> thetas = new HashMap<Gun, Double>();
			Gun bestGun = null;
			double bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				// out.println(thisGun.toString());

				if (bestGun == null
						|| thisGun.getValue().get(proximityPosition).get(
								wallPosition) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(proximityPosition).get(
							wallPosition);
				}

				double theta = thisGun.getKey().getTheta(this, e, bulletPower,
						hasFired);
				thetas.put(thisGun.getKey(), theta);
				if (getGunHeat() <= getGunCoolingRate() && !justFired) {
					Point2D bulletOrigin = new Point2D(getX(), getY());
					VirtualBullet virtualBullet = new VirtualBullet(
							thisGun.getKey(), bulletOrigin, theta,
							20 - bulletPower*3);
					addCustomEvent(virtualBullet);
					virtualBullets.add(virtualBullet);
				}
			}

			// out.println("Firing with " + bestGun.toString());
			// out.println();

			double theta = thetas.get(bestGun);
			setTurnGunRightRadians(Utils.normalRelativeAngle(theta
					- getGunHeadingRadians()));
			fire(bulletPower);
			justFired = !justFired;
		} else {
			doNothing();
		}
	}

	private void goTo(Point2D point) {
		translateInsideField(point, Util.ROBOT_WIDTH + 10);

		double distance = Math.sqrt(Math.pow(getX() - point.x, 2)
				+ Math.pow(getY() - point.y, 2));
		double angle = Utils.normalRelativeAngle(absoluteBearing(new Point2D(
				getX(), getY()), point)
				- getHeadingRadians());
		if (Math.abs(angle) > Math.PI / 2) {
			distance *= -1.0;
			if (angle > 0.0) {
				angle -= Math.PI;
			} else {
				angle += Math.PI;
			}
		}
		setTurnRightRadians(angle);
		// setMaxVelocity((Math.random() * 20) + 3);
		setAhead(distance);
	}

	private void translateInsideField(Point2D point, double margin) {
		point.x = Math.max(margin, Math.min(getBattleFieldWidth() - margin,
				point.x));
		point.y = Math.max(margin, Math.min(getBattleFieldHeight() - margin,
				point.y));
	}

	private double absoluteBearing(Point2D source, Point2D target) {
		return Math.atan2(target.x - source.x, target.y - source.y);
	}

	public void onPaint(Graphics2D g) {
		for (VirtualBullet virtualBullet : virtualBullets) {
			g.setColor(virtualBullet.gun.getColor());
			g.fillOval((int) virtualBullet.location.x,
					(int) virtualBullet.location.y, 4, 4);
		}

		int i = 0;
		for (Gun gun : guns.keySet()) {
			g.setColor(gun.getColor());
			g.drawString(gun.toString() + ": " + guns.get(gun), 20, 5 + i * 15);
			g.fillOval(15, (i * 15) + 5, 4, 4);
			i++;
		}
	}

	public void onWin(WinEvent e) {
		outputGunStats();
	}

	public void onDeath(DeathEvent e) {
		outputGunStats();
	}

	private void outputGunStats() {
		if (!printedMessage) {
			printedMessage = true;
			Gun bestGun = null;
			double bestGunScore = 0;

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(2).get(0) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(2).get(0);
				}
			}
			out.println("When in the open, " + enemyName
					+ " is most vaunerable to a " + bestGun.toString());

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(1).get(0) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(1).get(0);
				}
			}
			out.println("When nearby, " + enemyName
					+ " is most vaunerable to a " + bestGun.toString());

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(0).get(0) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(0).get(0);
				}
			}
			out.println("When very close, " + enemyName
					+ " is most vaunerable to a " + bestGun.toString());

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(2).get(1) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(2).get(1);
				}
			}
			out.println("When far away and near a wall, " + enemyName
					+ " is best targeted with a " + bestGun.toString());

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(1).get(1) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(1).get(1);
				}
			}
			out.println("When close up and near a wall, " + enemyName
					+ " is best targeted with a " + bestGun.toString());

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(0).get(1) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(0).get(1);
				}
			}
			out.println("When pinned against a wall, " + enemyName
					+ " is best targeted with a " + bestGun.toString());

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(2).get(2) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(2).get(2);
				}
			}
			out.println("When far away and in a corner, " + enemyName
					+ " can be targeted with a " + bestGun.toString());

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(1).get(2) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(1).get(2);
				}
			}
			out.println("When close up and in a corner, " + enemyName
					+ " can be targeted with a " + bestGun.toString());

			bestGun = null;
			bestGunScore = 0;
			for (Map.Entry<Gun, List<List<Double>>> thisGun : guns.entrySet()) {
				if (bestGun == null
						|| thisGun.getValue().get(0).get(2) > bestGunScore) {
					bestGun = thisGun.getKey();
					bestGunScore = thisGun.getValue().get(0).get(2);
				}
			}
			out.println("When trapped in a corner, " + enemyName
					+ " can be targeted with a " + bestGun.toString());
		}
	}
}
