package xander.gfws.drive;

import xander.core.drive.DriveState;

public class DirectSurfSelection implements Comparable<DirectSurfSelection> {
	
	private boolean set;
	private double factorAngle;
	private double heading;
	private double danger;
	private DriveState finalDriveState;
	
	public void setSelection(DriveState finalDriveState, 
			double heading, double factorAngle, double danger) {
		this.set = true;
		this.heading = heading;
		this.factorAngle = factorAngle;
		this.danger = danger;
		this.finalDriveState = finalDriveState;
	}
	
	public boolean isSet() {
		return set;
	}

	public double getFactorAngle() {
		return factorAngle;
	}

	public double getHeading() {
		return heading;
	}

	public double getDanger() {
		return danger;
	}

	public DriveState getFinalDriveState() {
		return finalDriveState;
	}

	public void clear() {
		this.set = false;
		this.danger = Double.MAX_VALUE;
	}
	
	@Override
	public int compareTo(DirectSurfSelection dss) {
		double diff = danger - dss.danger;
		if (diff < 0) {
			return -1;
		} else if (diff > 0) {
			return 1;
		} else {
			return 0;
		}
	}
}
