package robots.test;

import java.util.Random;

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
		Random r = new Random();
		RobotActorTestImplementation robotActor = new RobotActorTestImplementation();
		double[] input = new double[11];
		for(int i = 0; i < input.length; i++) {
			input[i] = r.nextDouble();
		}
		// Set fire to true such that the firepower can be asserted.
		input[8] = 1.0;
		
		robotActor.act(input);
		
		Assert.assertTrue(isWithinRange(robotActor.getAhead(), 0, RobotRulesConstants.maxMovementPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getBack(), 0, RobotRulesConstants.maxMovementPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getTurnGunLeft(), 0, RobotRulesConstants.maxGunTurnDegreesPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getTurnGunRight(), 0, RobotRulesConstants.maxGunTurnDegreesPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getTurnRadarRight(), 0, RobotRulesConstants.maxRadarTurnDegreesPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getTurnRadarLeft(), 0, RobotRulesConstants.maxRadarTurnDegreesPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getFireBullet(), RobotRulesConstants.minFirePower, RobotRulesConstants.maxFirePower));
		Assert.assertTrue(isWithinRange(robotActor.getTurnRight(), 0, RobotRulesConstants.maxRobotTurnDegreesPerTurn));
		Assert.assertTrue(isWithinRange(robotActor.getTurnLeft(), 0, RobotRulesConstants.maxRobotTurnDegreesPerTurn));
		
	}

	private boolean isWithinRange(double value, double min, double max) {
		return value <= max && value >= min;
	}

}
