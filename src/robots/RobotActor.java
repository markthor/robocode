package robots;

public interface RobotActor {
	
	/***
	 * Makes the robot do stuff, such as turning the gun or moving ahead
	 * @param input The output of the neural network that decides the actions of the robot
	 */
	public void act(double[] activatorOutput);
}
