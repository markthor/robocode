package robots;

import robocode.AdvancedRobot;

import static robots.RobotRulesConstants.*;

public class RobotActorInteractive extends AdvancedRobot implements RobotActor {

	private static final int numberOfInputs = 11;
	private static final double extremumMarginPercentage = 0.1;
	
	public void act(double[] activatorOutput) {
		if(activatorOutput.length != numberOfInputs) {
			throw new IllegalArgumentException("Wrong number of inputs. Expected " + numberOfInputs + ", but got " + activatorOutput.length);
		}
		
		double moveForwards 	= activatorOutput[0];
		double moveBackwards 	= activatorOutput[1];
		double turnGunRight 	= activatorOutput[2];
		double turnGunLeft 		= activatorOutput[3];
		double turnRobotRight 	= activatorOutput[4];
		double turnRobotLeft 	= activatorOutput[5];
		double turnRadarRight 	= activatorOutput[6];
		double turnRadarLeft 	= activatorOutput[7];
		double fire 			= activatorOutput[8];
		double firePower 		= activatorOutput[9];
		double scan 			= activatorOutput[10];
		
		
		setMovement(moveForwards, moveBackwards);
		setGunRotation(turnGunRight, turnGunLeft);
		setRobotRotation(turnRobotRight, turnRobotLeft);
		setRadarRotation(turnRadarRight, turnRadarLeft);
		setFire(fire, firePower);
		setScan(scan);
		
		execute();
	}
	
	private void setMovement(double forward, double backward) {
		double forwardScaled = scale(forward, maxMovementPerTurn);
		double backwardScaled = scale(backward, maxMovementPerTurn);
		
		if(forwardScaled >= backwardScaled) {
			setAhead(forwardScaled-backwardScaled);
		} else {
			setBack(backwardScaled-forwardScaled);
		}
	}
	
	private void setRobotRotation(double right, double left) {
		double rightScaled = scale(right, maxRobotTurnDegreesPerTurn);
		double leftScaled = scale(right, maxRobotTurnDegreesPerTurn);
		
		if(rightScaled >= leftScaled) {
			setTurnRight(rightScaled-leftScaled);
		} else {
			setTurnLeft(leftScaled-rightScaled);
		}
	}
	
	private void setGunRotation(double right, double left) {
		double rightScaled = scale(right, maxGunTurnDegreesPerTurn);
		double leftScaled = scale(right, maxGunTurnDegreesPerTurn);
		
		if(rightScaled >= leftScaled) {
			setTurnGunRight(rightScaled-leftScaled);
		} else {
			setTurnGunLeft(leftScaled-rightScaled);
		}
	}
	
	private void setRadarRotation(double right, double left) {
		double rightScaled = scale(right, maxRadarTurnDegreesPerTurn);
		double leftScaled = scale(right, maxRadarTurnDegreesPerTurn);
		
		if(rightScaled >= leftScaled) {
			setTurnRadarRight(rightScaled-leftScaled);
		} else {
			setTurnRadarLeft(leftScaled-rightScaled);
		}
	}
	
	private void setScan(double scan) {
		if(scan > 0.5) {
			scan();
		}
	}
	
	private void setFire(double fire, double power) {
		if(fire > 0.5) {
			setFireBullet(scaleFirePower(power));
		}
	}
	
	private double scaleFirePower(double power) {
		return scale(power, maxFirePower-minFirePower) + minFirePower;
	}
	
	private double scale(double input, double max) {
		if(isInLowerMargin(input)) return 0;
		if(isInUpperMargin(input)) return max;
		
		return ((input - extremumMarginPercentage) / (1-(2*extremumMarginPercentage))) * max;
	}
	
	private boolean isInLowerMargin(double input) {
		return input < extremumMarginPercentage;
	}
	
	private boolean isInUpperMargin(double input) {
		return input > 1-extremumMarginPercentage;
	}
}
