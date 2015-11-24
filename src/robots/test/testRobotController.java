package robots.test;

import org.testng.annotations.Test;

public class testRobotController {
	
  @Test
  public void testSimpleInputOutput() {
	  RobotSensorTestImplementation robotSensor = new RobotSensorTestImplementation();
	  robotSensor.setGunHeading(40.0);
	  robotSensor.setEnergy(90);
	  robotSensor.setVelocity(7.0);
	  robotSensor.setX(10.0);
	  robotSensor.setY(30.0);
	  robotSensor.setRadarHeading(210.0);
	  robotSensor.setGunHeat(1.2);
	  robotSensor.setBattleFieldHeight(100.0);
	  robotSensor.setBattleFieldWidth(100.0);
	  
	  double[] result = robotSensor.sense();
  }
}

