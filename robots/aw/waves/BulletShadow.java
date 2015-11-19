package aw.waves;

import java.awt.geom.Point2D;
import robocode.Bullet;

public class BulletShadow {
	private double clockwiseAngle;
	private double counterClockwiseAngle;
	private long collisionStartTime;
	private long collisionEndTime;
	private long bulletFireTime;
	private double bulletHeading;
	private double bulletVelocity;
	private Point2D.Double bulletStartPosition;
	private Bullet bullet;
	
	public BulletShadow(double clockWiseAngle, double counterClockwiseAngle, long collisionStartTime, long collisionEndTime, long bulletFireTime, double bulletHeading, double bulletVelocity, Point2D.Double bulletStartPosition, Bullet bullet) {
		this.clockwiseAngle = clockWiseAngle;
		this.counterClockwiseAngle = counterClockwiseAngle;
		this.collisionStartTime = collisionStartTime;
		this.collisionEndTime = collisionEndTime;
		this.bulletFireTime = bulletFireTime;
		this.bulletHeading = bulletHeading;
		this.bulletVelocity = bulletVelocity;
		this.bulletStartPosition = bulletStartPosition;
		this.bullet = bullet;
	}
	
	public double getClockwiseAngle() {
		return clockwiseAngle;
	}
	
	public double getCounterClockwiseAngle() {
		return counterClockwiseAngle;
	}
	
	public long getCollisionStartTime() {
		return collisionStartTime;
	}
	
	public long getCollisionEndTime() {
		return collisionEndTime;
	}
	
	public long getBulletFireTime() {
		return bulletFireTime;
	}
	
	public double getBulletHeading() {
		return bulletVelocity;
	}
	
	public double getBulletVelocity() {
		return bulletHeading;
	}
	
	public Point2D.Double getBulletStartPosition() {
		return bulletStartPosition;
	}
	
	public Bullet getBullet() {
		return bullet;
	}
}
