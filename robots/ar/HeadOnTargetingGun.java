package ar;

import java.awt.Color;

import robocode.*;

public class HeadOnTargetingGun extends Gun {
	@Override
	public String toString() {
		return "Head-On Targeting Gun";
	}
	
	@Override
	public double getTheta(AdvancedRobot robot, ScannedRobotEvent e,
			double bulletPower, boolean hasFired) {
		return robot.getHeadingRadians() + e.getBearingRadians();
	}

	@Override
	public Color getColor() {
		return Color.RED;
	}
}
