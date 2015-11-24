package robots;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import static robots.RobotRulesConstants.*;

public class RobotSensorRelevantInputs extends AdvancedRobot implements RobotSensor {

	private static final int numberOfBasicInputs = 12;
	private static final int numberOfRangeFinders = 4;
	private static final double rangeCeiling = 1.0;
	private static final double rangeFloor = -1.0;
	private boolean didLastBulletHit = false;
	
	@Override
	public double[] sense() {
		int totalNumberOfInputs = numberOfBasicInputs+numberOfRangeFinders;
		double[] result = new double[totalNumberOfInputs];
		
		result[0] = hasRecentlyScannedEnemy();
		result[1] = getEnemyVelocity();
		result[2] = getEnemyHeading();
		result[3] = getEnemyBearing();
		result[4] = getDistanceToEnemy();
		result[5] =	getEnemyVelocity();
		result[6] = getEnemyEnergy();
		result[7] = getRobotEnergy();
		result[8] = getRobotVelocity();
		result[9] = getRobotHeading();
		result[10] = getRobotGunHeat();
		result[11] = getRobotRadarHeading();
		
		double[] distances = getDistanceToWalls(numberOfRangeFinders);
		for(int i = numberOfBasicInputs; i < totalNumberOfInputs; i++) {
			result[i] = distances[i-numberOfBasicInputs];
		}
		
		validateResult(result);
		return result;
	}
	
	public void validateResult(double[] result) {
		for(double d : result) {
			if(!isWithinRange(d, rangeFloor, rangeCeiling)) throw new IllegalStateException("NN input is outside desired range. This should not happen, revise code.");
		}
	}
	
	private double[] getDistanceToWalls(int numberOfRangeFinders) {
		double[] results = new double[numberOfRangeFinders];
		double angle = 360.0/numberOfRangeFinders;
		for(int i = 0; i < results.length; i++) {
			results[i] = scaleDistance(getDistanceToWall(getHeading()+i*angle));
		}
		return results;
	}
	
	private double getDistanceToWall(double offSet) {
		double absoluteAngle = (getHeading() + offSet) % 360.0;
		double adjacentOppositeFactor, deltaX, deltaY, deltaXYFactor;
		
		// First quarter (clockwise from 12 o'clock)
		if(absoluteAngle < 90) {
			adjacentOppositeFactor = Math.cos(absoluteAngle)/Math.sin(absoluteAngle);
			deltaX = getBattleFieldWidth()-getX();
			deltaY = getBattleFieldHeight()-getY();
			deltaXYFactor = deltaX/deltaY;
			
			if(adjacentOppositeFactor < deltaXYFactor) {
				return deltaY/Math.sin(absoluteAngle);
			} else {
				return deltaX/Math.cos(absoluteAngle);
			}
		// Second quarter
		} else if(absoluteAngle <= 180 ) {
			absoluteAngle -= 90;
			adjacentOppositeFactor = Math.cos(absoluteAngle)/Math.sin(absoluteAngle);
			deltaX = getBattleFieldWidth()-getX();
			deltaY = getY();
			deltaXYFactor = deltaX/deltaY;
			
			if(adjacentOppositeFactor < deltaXYFactor) {
				return deltaY/Math.sin(absoluteAngle);
			} else {
				return deltaX/Math.cos(absoluteAngle);
			}
		// Third quarter
		} else if(absoluteAngle <= 270) {
			absoluteAngle -= 180.0;
			adjacentOppositeFactor = Math.cos(absoluteAngle)/Math.sin(absoluteAngle);
			deltaX = getX();
			deltaY = getY();
			deltaXYFactor = deltaX/deltaY;
			if(adjacentOppositeFactor < deltaXYFactor) {
				return deltaY/Math.sin(absoluteAngle);
			} else {
				return deltaX/Math.cos(absoluteAngle);
			}
		// Fourth quarter
		} else {
			absoluteAngle -= 270.0;
			adjacentOppositeFactor = Math.cos(absoluteAngle)/Math.sin(absoluteAngle);
			deltaX = getX();
			deltaY = getBattleFieldHeight()-getY();
			deltaXYFactor = deltaX/deltaY;
			if(adjacentOppositeFactor < deltaXYFactor) {
				return deltaY/Math.sin(absoluteAngle);
			} else {
				return deltaX/Math.cos(absoluteAngle);
			}
		}
	}
	
	private double getRobotRadarHeading() {
		return scaleDegrees(getRadarHeading());
	}
	
	private double getRobotGunHeat() {
		return scaleGunHeat(getGunHeat());
	}
	
	private double getRobotHeading() {
		return scaleDegrees(getHeading());
	}
	
	private double getRobotVelocity() {
		return scaleRobotVelocity(getVelocity());
	}
	
	private double getRobotEnergy() {
		return scaleEnergy(getEnergy());
	}
	
	private double hasRecentlyScannedEnemy() {
		return booleanToDouble(getScannedRobotEvents().size() > 0);
	}
	
	private double getEnemyVelocity() {
		ScannedRobotEvent event = getScannedRobotEventIfItExists();
		if(event != null) {
			return scaleRobotVelocity(event.getVelocity());
		} else {
			return 1.0;
		}
	}
	
	private double getEnemyHeading() {
		ScannedRobotEvent event = getScannedRobotEventIfItExists();
		if(event != null) {
			return scaleDegrees(event.getHeading());
		} else {
			return 1.0;
		}
	}
	
	private double getEnemyBearing() {
		ScannedRobotEvent event = getScannedRobotEventIfItExists();
		if(event != null) {
			return scaleDegrees(event.getBearing());
		} else {
			return 1.0;
		}
	}
	
	private double getDistanceToEnemy() {
		ScannedRobotEvent event = getScannedRobotEventIfItExists();
		if(event != null) {
			return scaleDistance(event.getDistance());
		} else {
			return 1.0;
		}
	}
	
	private double getEnemyEnergy() {
		ScannedRobotEvent event = getScannedRobotEventIfItExists();
		if(event != null) {
			return scaleEnergy(event.getEnergy());
		} else {
			return 1.0;
		}
	}
	
	private ScannedRobotEvent getScannedRobotEventIfItExists() {
		if(getScannedRobotEvents().size() > 1) {
			throw new IllegalStateException("Are you running a game with more than 2 robots? If not, this should not happen, revise code.");
		}
		
		if(getScannedRobotEvents().size() < 0) {
			return getScannedRobotEvents().get(0);
		} else {
			return null;
		}
	}
	
	private double scaleGunHeat(double gunHeat) {
		return scale(gunHeat, maxGunHeat, 0.0);
	}
	
	private double scaleRobotVelocity(double velocity) {
		return scale(velocity, maxMovementPerTurn, 0.0);
	}
	
	private double scaleDegrees(double degrees) {
		return scale(degrees, 360.0, 0.0);
	}
	
	private double scaleEnergy(double energy) {
		return scale(energy, maxEnergy, 0.0);
	}
	
	private double scaleDistance(double distance) {
		double maxDistance = getMaxDistance();
		if(maxDistance < distance) throw new IllegalStateException("Distance cannot be greater than the maximum distance. Should not happen, revise code.");
		return scale(distance, maxDistance, 0.0);
	}
	
	private double scale(double value, double max, double min) {
		if(!isWithinRange(value, min, max)) throw new IllegalStateException("Min max values does not match input. Should not happen, revise code.");
		double scaledBetweenOneAndZero = (value/(max - min));
		return ((rangeCeiling - rangeFloor) * scaledBetweenOneAndZero) - rangeFloor;
	}
	
	private double getMaxDistance() {
		return Math.sqrt(Math.pow(getBattleFieldHeight(), 2) + Math.pow(getBattleFieldWidth(), 2));
	}
	
	
	private boolean isWithinRange(double value, double min, double max) {
		return value <= max && value >= min;
	}
	
	private double booleanToDouble(boolean bool) {
		return bool ? 1.0 : 0.0;
	}
}
