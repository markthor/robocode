package robots;

import static robots.RobotRulesConstants.maxEnergy;
import static robots.RobotRulesConstants.maxFirePower;
import static robots.RobotRulesConstants.maxGunHeat;
import static robots.RobotRulesConstants.maxGunTurnDegreesPerTurn;
import static robots.RobotRulesConstants.maxMovementPerTurn;
import static robots.RobotRulesConstants.maxRadarTurnDegreesPerTurn;
import static robots.RobotRulesConstants.maxRobotTurnDegreesPerTurn;
import static robots.RobotRulesConstants.minFirePower;

import org.jgap.Chromosome;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import com.anji.persistence.Persistence;
import com.anji.util.Configurable;
import com.anji.util.DummyConfiguration;
import com.anji.util.Properties;

public class RobotControllerFlat extends AdvancedRobot implements Configurable {
	private static ActivatorTranscriber activatorFactory;
	private static Persistence db;
	private RobotActor actor = new RobotActorInteractive();
	private RobotSensor sensor = new RobotSensorRelevantInputs();
	private Activator activator;
	private Long chromosomeId = 1L;
	
	public void run() {
		act(activator.next(sense()));
		execute();
	}
	
	private Activator loadActivator(Long chromosomeId) throws TranscriberException {
		Chromosome chromosome = db.loadChromosome(chromosomeId.toString(), new DummyConfiguration());
		return activatorFactory.newActivator(chromosome);
	}

	@Override
	public void init(Properties props) throws Exception {
		db = (Persistence) props.singletonObjectProperty( Persistence.PERSISTENCE_CLASS_KEY );
		activatorFactory = (ActivatorTranscriber) props.singletonObjectProperty(ActivatorTranscriber.class);
		
		try {
			activator = loadActivator(chromosomeId);
		} catch(TranscriberException e) {
			//TODO: Handle error
			e.printStackTrace();
		}
	}
	
	private static final int numberOfBasicInputs = 13;
	private static final int numberOfRangeFinders = 4;
	private static final double rangeCeiling = 1.0;
	private static final double rangeFloor = -1.0;
	private boolean didLastBulletHit = false;
	
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
		result[12] = getRobotGunHeading();
		
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
			results[i] = scaleDistance(getDistanceToWall(i*angle));
		}
		return results;
	}
	
	private double getDistanceToWall(double offSet) {
		double absoluteAngle = (getHeading() + offSet) % 360.0;
		double adjacentOppositeFactor, deltaX, deltaY, deltaXYFactor;
		
		// First quarter (clockwise from 12 o'clock)
		if(absoluteAngle < 90) {
			adjacentOppositeFactor = Math.cos(degreesToRadians(absoluteAngle))/Math.sin(degreesToRadians(absoluteAngle));
			deltaX = getBattleFieldWidth()-getX();
			deltaY = getBattleFieldHeight()-getY();
			deltaXYFactor = deltaX/deltaY;
			
			if(adjacentOppositeFactor < deltaXYFactor) {
				return deltaX/Math.sin(degreesToRadians(absoluteAngle));
			} else {
				return deltaY/Math.cos(degreesToRadians(absoluteAngle));
			}
		// Second quarter
		} else if(absoluteAngle <= 180 ) {
			absoluteAngle -= 90;
			adjacentOppositeFactor = Math.cos(degreesToRadians(absoluteAngle))/Math.sin(degreesToRadians(absoluteAngle));
			deltaX = getBattleFieldWidth()-getX();
			deltaY = getY();
			deltaXYFactor = deltaX/deltaY;
			
			if(adjacentOppositeFactor < deltaXYFactor) {
				return deltaY/Math.sin(degreesToRadians(absoluteAngle));
			} else {
				return deltaX/Math.cos(degreesToRadians(absoluteAngle));
			}
		// Third quarter
		} else if(absoluteAngle <= 270) {
			absoluteAngle -= 180.0;
			adjacentOppositeFactor = Math.cos(degreesToRadians(absoluteAngle))/Math.sin(degreesToRadians(absoluteAngle));
			deltaX = getX();
			deltaY = getY();
			deltaXYFactor = deltaX/deltaY;
			if(adjacentOppositeFactor < deltaXYFactor) {
				return deltaX/Math.sin(degreesToRadians(absoluteAngle));
			} else {
				return deltaY/Math.cos(degreesToRadians(absoluteAngle));
			}
		// Fourth quarter
		} else {
			absoluteAngle -= 270.0;
			adjacentOppositeFactor = Math.cos(degreesToRadians(absoluteAngle))/Math.sin(degreesToRadians(absoluteAngle));
			deltaX = getX();
			deltaY = getBattleFieldHeight()-getY();
			deltaXYFactor = deltaX/deltaY;
			if(adjacentOppositeFactor < deltaXYFactor) {
				return deltaY/Math.sin(degreesToRadians(absoluteAngle));
			} else {
				return deltaX/Math.cos(degreesToRadians(absoluteAngle));
			}
		}
	}
	
	private double getRobotRadarHeading() {
		return scaleDegrees(getRadarHeading());
	}
	
	private double getRobotGunHeading() {
		return scaleDegrees(getGunHeading());
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
		return booleanToDouble(getScannedRobotEventIfItExists() != null);
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
		if(getScannedRobotEvents() == null) return null;
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
		return ((rangeCeiling - rangeFloor) * scaledBetweenOneAndZero) + rangeFloor;
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
	
	private double degreesToRadians(double degrees) {
		return (degrees * Math.PI) / 180;
	}
	
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
	}
	
	protected void setMovement(double forward, double backward) {
		double forwardScaled = scale(forward, maxMovementPerTurn);
		double backwardScaled = scale(backward, maxMovementPerTurn);
		
		if(forwardScaled >= backwardScaled) {
			setAhead(forwardScaled-backwardScaled);
		} else {
			setBack(backwardScaled-forwardScaled);
		}
	}
	
	protected void setRobotRotation(double right, double left) {
		double rightScaled = scale(right, maxRobotTurnDegreesPerTurn);
		double leftScaled = scale(left, maxRobotTurnDegreesPerTurn);
		
		if(rightScaled >= leftScaled) {
			setTurnRight(rightScaled-leftScaled);
		} else {
			setTurnLeft(leftScaled-rightScaled);
		}
	}
	
	protected void setGunRotation(double right, double left) {
		double rightScaled = scale(right, maxGunTurnDegreesPerTurn);
		double leftScaled = scale(left, maxGunTurnDegreesPerTurn);
		
		if(rightScaled >= leftScaled) {
			setTurnGunRight(rightScaled-leftScaled);
		} else {
			setTurnGunLeft(leftScaled-rightScaled);
		}
	}
	
	protected void setRadarRotation(double right, double left) {
		double rightScaled = scale(right, maxRadarTurnDegreesPerTurn);
		double leftScaled = scale(left, maxRadarTurnDegreesPerTurn);
		
		if(rightScaled >= leftScaled) {
			setTurnRadarRight(rightScaled-leftScaled);
		} else {
			setTurnRadarLeft(leftScaled-rightScaled);
		}
	}
	
	protected void setScan(double scan) {
		if(scan > 0.5) {
			scan();
		}
	}
	
	protected void setFire(double fire, double power) {
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