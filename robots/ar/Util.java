package ar;

public final class Util {
	private Util() {
	}
	
	public static double ROBOT_WIDTH = 36;
	public static double ROBOT_CENTER = ROBOT_WIDTH / 2;
	
	public static double normalAbsoluteAngle(double angle) {
		if (angle >= 0 && angle < 360)
			return angle;
		double fixedAngle = angle;
		while (fixedAngle < 0)
			fixedAngle += 360;
		while (fixedAngle >= 360)
			fixedAngle -= 360;
		return fixedAngle;
	}
	
	public static double normalAbsoluteAngleRadians(double angle) {
		if (angle >= 0 && angle < 2 * Math.PI)
			return angle;
		double fixedAngle = angle;
		while (fixedAngle < 0)
			fixedAngle += 2 * Math.PI;
		while (fixedAngle >= 2 * Math.PI)
			fixedAngle -= 2 * Math.PI;
		return fixedAngle;
	}
	
	public static double normalRelativeAngle(double angle) {
		if (angle > -180 && angle <= 180)
			return angle;
		double fixedAngle = angle;
		while (fixedAngle <= -180)
			fixedAngle += 360;
		while (fixedAngle > 180)
			fixedAngle -= 360;
		return fixedAngle;
	}
	
	public static double normalRelativeAngleRadians(double angle) {
		if (angle > -Math.PI && angle <= Math.PI)
			return angle;
		double fixedAngle = angle;
		while (fixedAngle <= -Math.PI)
			fixedAngle += Math.PI * 2;
		while (fixedAngle > Math.PI)
			fixedAngle -= Math.PI * 2;
		return fixedAngle;
	}
	
	public static double distance(Point2D p1, Point2D p2) {
		return distance(p1.x, p1.y, p2.x, p2.y);
	}
	
	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}
}
