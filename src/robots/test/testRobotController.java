package robots.test;

import org.testng.Assert;
import org.testng.annotations.Test;

import robots.RobotRulesConstants;

public class testRobotController {

	@Test
	public void testSimpleInput() {
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
		robotSensor.setHeading(190.0);

		double[] result = robotSensor.sense();
	}

	@Test
	public void testSimpleOutput() {
		RobotActorTestImplementation robotActor = new RobotActorTestImplementation();
		double[] input = new double[11];
		for (double d : input) {
			d = 0.5;
		}
		robotActor.act(input);
		
		Assert.assertTrue(isWithinRange(robotActor.getAhead(), 0, RobotRulesConstants.maxMovementPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getBack(), 0, RobotRulesConstants.maxMovementPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getTurnGunLeft(), 0, RobotRulesConstants.maxGunTurnDegreesPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getTurnGunRight(), 0, RobotRulesConstants.maxGunTurnDegreesPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getTurnGunRight(), 0, RobotRulesConstants.maxGunTurnDegreesPerTurn));
		
	}

	private boolean isWithinRange(double value, double min, double max) {
		return value <= max && value >= min;
	}

}
