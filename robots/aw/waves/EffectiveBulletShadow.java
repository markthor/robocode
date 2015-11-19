package aw.waves;


public class EffectiveBulletShadow {
	private double clockwiseAngle;
	private double counterClockwiseAngle;

	public EffectiveBulletShadow(double clockWiseAngle, double counterClockwiseAngle) {
		this.clockwiseAngle = clockWiseAngle;
		this.counterClockwiseAngle = counterClockwiseAngle;

	}
	
	public double getClockwiseAngle() {
		return clockwiseAngle;
	}
	
	public double getCounterClockwiseAngle() {
		return counterClockwiseAngle;
	}
}
