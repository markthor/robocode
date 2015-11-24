package robots;

import robocode.Robot;

public class SimpleRobotController extends Robot {

	@Override
	public void run() {
		ahead(10.0);
		turnRight(30.0);
	}

}
