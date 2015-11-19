package robots;

public interface RobotSensor {
	
	/***
	 * Reads the data from the robots state, for example gun angle, energy left or distance to nearest object.
	 * @return Data from the state of the robot
	 */
	public double[] sense();
}