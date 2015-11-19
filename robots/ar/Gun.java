package ar;

import java.awt.Color;

import robocode.*;

public abstract class Gun {
	public abstract double getTheta(AdvancedRobot robot, ScannedRobotEvent e,
			double bulletPower, boolean hasFired);
	
	public abstract Color getColor();
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null || o.getClass() != this.getClass()) {
			return false;
		} else {
			return this.toString().equals(o.toString());
		}
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
}
