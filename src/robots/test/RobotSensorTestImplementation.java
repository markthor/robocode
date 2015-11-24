package robots.test;

import java.util.Vector;

import robocode.ScannedRobotEvent;
import robots.RobotSensorRelevantInputs;

public class RobotSensorTestImplementation extends RobotSensorRelevantInputs {
	
	private double heading;
	private double energy;
	private double velocity;
	private double y;
	private double x;
	private double radarHeading;
	private double gunHeat;
	private double gunHeading;
	private double battleFieldWidth;
	private double battleFieldHeight;
	
	@Override
	public Vector<ScannedRobotEvent> getScannedRobotEvents() {
		return null;
	}

	@Override
	public double getHeading() {
		return heading;
	}

	public void setHeading(double heading) {
		this.heading = heading;
	}

	@Override
	public double getEnergy() {
		return energy;
	}

	public void setEnergy(double energy) {
		this.energy = energy;
	}
	
	@Override
	public double getVelocity() {
		return velocity;
	}

	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}
	
	@Override
	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	@Override
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	@Override
	public double getRadarHeading() {
		return radarHeading;
	}

	public void setRadarHeading(double radarHeading) {
		this.radarHeading = radarHeading;
	}

	@Override
	public double getGunHeat() {
		return gunHeat;
	}

	public void setGunHeat(double gunHeat) {
		this.gunHeat = gunHeat;
	}

	@Override
	public double getGunHeading() {
		return gunHeading;
	}

	public void setGunHeading(double gunHeading) {
		this.gunHeading = gunHeading;
	}
	
	@Override
	public double getBattleFieldWidth() {
		return battleFieldWidth;
	}

	public void setBattleFieldWidth(double battleFieldWidth) {
		this.battleFieldWidth = battleFieldWidth;
	}
	
	@Override
	public double getBattleFieldHeight() {
		return battleFieldHeight;
	}

	public void setBattleFieldHeight(double battleFieldHeight) {
		this.battleFieldHeight = battleFieldHeight;
	}
	
	
}
