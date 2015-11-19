package aw.utils;

import java.awt.geom.Point2D;

public class RobotEPIFState {
	private double practicalHeading;
	private double practicalVelocity;
	private double absTargetRelHeading;
	private Point2D.Double coordinates;
	long time;
	
	public RobotEPIFState(Point2D.Double coordinates, double velocity, double absHeading, double absTargetRelHeading, long time) {
		this.coordinates = coordinates;
		this.practicalHeading = absHeading;
		this.practicalVelocity = velocity;
		this.absTargetRelHeading = absTargetRelHeading;
		this.time = time;
	}
	
	public Point2D.Double getCoordinates() {
		return coordinates;
	}
	
	public double getAbsHeading() {
		return practicalHeading;
	}
	
	public double getVelocity() {
		return practicalVelocity;
	}
	
	public double getAbsTargetRelHeading() {
		return absTargetRelHeading;
	}
	
	public long getTime() {
		return time;
	}
	
	
}
