package ar;

import java.awt.Color;

import robocode.*;

public class LinearTargetingGun extends Gun {
	@Override
	public String toString() {
		return "Linear Targeting Gun";
	}

	@Override
	public double getTheta(AdvancedRobot robot, ScannedRobotEvent e,
			double bulletPower, boolean hasFired) {
		double myX = robot.getX();
		double myY = robot.getY();
		double distance = e.getDistance();
		double absoluteBearing = robot.getHeadingRadians()
				+ e.getBearingRadians();
		double enemyX = robot.getX() + distance * Math.sin(absoluteBearing);
		double enemyY = robot.getY() + distance * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyVelocity = e.getVelocity();

		double deltaTime = 0;
		double battleFieldHeight = robot.getBattleFieldHeight(), battleFieldWidth = robot.getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while ((++deltaTime) * (20.0 - bulletPower*3) < Util.distance(
				myX, myY, predictedX, predictedY)) {
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			if (predictedX < Util.ROBOT_CENTER
					|| predictedY < Util.ROBOT_CENTER
					|| predictedX > battleFieldWidth - Util.ROBOT_CENTER
					|| predictedY > battleFieldHeight - Util.ROBOT_CENTER) {
				predictedX = Math.min(Math.max(Util.ROBOT_CENTER, predictedX),
						battleFieldWidth - Util.ROBOT_CENTER);
				predictedY = Math.min(Math.max(Util.ROBOT_CENTER, predictedY),
						battleFieldHeight - Util.ROBOT_CENTER);
				break;
			}
		}
		return Util.normalAbsoluteAngleRadians(Math.atan2(predictedX
				- robot.getX(), predictedY - robot.getY()));
	}
	
	@Override
	public Color getColor() {
		return Color.YELLOW;
	}
}
