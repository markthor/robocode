package robots.test;

import robocode.Bullet;
import robots.RobotActorInteractive;

public class RobotActorTestImplementation extends RobotActorInteractive {
	private double turnRight;
	private double turnLeft;
	private double ahead;
	private double back;
	private double turnGunLeft;
	private double turnGunRight;
	private double turnRadarLeft;
	private double turnRadarRight;
	private double fireBullet;
	private double energy;
	
	@Override
	protected void setMovement(double forward, double backward) {
		super.setMovement(forward, backward);
	}
	
	@Override 
	protected void setRobotRotation(double right, double left) {
		super.setRobotRotation(right, left);
	}
	
	@Override
	protected void setGunRotation(double right, double left) {
		super.setGunRotation(right, left);
	}
	
	@Override
	protected void setRadarRotation(double right, double left) {
		super.setRadarRotation(right, left);
	}
	
	@Override 
	protected void setScan(double scan) {
		super.setScan(scan);
	}
	
	@Override
	protected void setFire(double fire, double power) {
		super.setFire(fire, power);
	}
	
	@Override
	public double getEnergy() {
		return energy;
	}

	@Override
	public void scan() {
		
	}
	
	public double getTurnRight() {
		return turnRight;
	}

	@Override
	public void setTurnRight(double turnRight) {
		this.turnRight = turnRight;
	}

	public double getTurnLeft() {
		return turnLeft;
	}

	@Override
	public void setTurnLeft(double turnLeft) {
		this.turnLeft = turnLeft;
	}

	public double getAhead() {
		return ahead;
	}

	@Override
	public void setAhead(double ahead) {
		this.ahead = ahead;
	}

	public double getBack() {
		return back;
	}

	@Override
	public void setBack(double back) {
		this.back = back;
	}

	public double getTurnGunLeft() {
		return turnGunLeft;
	}

	@Override
	public void setTurnGunLeft(double turnGunLeft) {
		this.turnGunLeft = turnGunLeft;
	}

	public double getTurnGunRight() {
		return turnGunRight;
	}

	@Override
	public void setTurnGunRight(double turnGunRight) {
		this.turnGunRight = turnGunRight;
	}

	public double getTurnRadarLeft() {
		return turnRadarLeft;
	}

	@Override
	public void setTurnRadarLeft(double turnRadarLeft) {
		this.turnRadarLeft = turnRadarLeft;
	}

	public double getTurnRadarRight() {
		return turnRadarRight;
	}

	@Override
	public void setTurnRadarRight(double turnRadarRight) {
		this.turnRadarRight = turnRadarRight;
	}

	public double getFireBullet() {
		return fireBullet;
	}

	@Override
	public Bullet setFireBullet(double fireBullet) {
		this.fireBullet = fireBullet;
		return null;
	}
	
	
}
