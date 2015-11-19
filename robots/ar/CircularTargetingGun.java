package ar;

import java.awt.Color;

import robocode.*;

public class CircularTargetingGun extends Gun {
	private double lastEnemyHeading = 0.0;

	@Override
	public String toString() {
		return "Circular Targeting Gun";
	}

	@Override
	public double getTheta(AdvancedRobot robot, ScannedRobotEvent e,
			double bulletPower, boolean hasFired) {
		double absoluteBearing = robot.getHeadingRadians()
				+ e.getBearingRadians();
		double relativeX = e.getDistance() * Math.sin(absoluteBearing);
		double relativeY = e.getDistance() * Math.cos(absoluteBearing);
		double db = 0;
		double ww = lastEnemyHeading; // enemy's starting heading

		do {
			db += 20 - bulletPower*3;
			double dx = e.getVelocity() * Math.sin(ww);
			double dy = e.getVelocity() * Math.cos(ww);
			ww += e.getHeadingRadians() - lastEnemyHeading;
			relativeX += dx;
			relativeY += dy;
		} while (db < Util.distance(0, 0, relativeX, relativeY));
		lastEnemyHeading = e.getHeadingRadians();

		return Math.asin(Math.sin(Math.atan2(relativeX, relativeY)
				- robot.getGunHeadingRadians()))
				+ robot.getGunHeadingRadians();
	}

	@Override
	public Color getColor() {
		return Color.CYAN;
	}
}
