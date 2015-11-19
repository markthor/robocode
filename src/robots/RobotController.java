package robots;

import robocode.AdvancedRobot;

import com.anji.integration.Activator;

public class RobotController extends AdvancedRobot {
	private RobotActor actor;
	private RobotSensor sensor;
	private Activator activator;
	
	public RobotController(RobotActor actor, RobotSensor sensor, String activatorFilePath) {
		super();
		this.actor = actor;
		this.sensor = sensor;
		activator = loadActivator(activatorFilePath);
	}
	
	public void run() {
		while(true) {
			actor.act(activator.next(sensor.sense()));
		}
	}
	
	private Activator loadActivator(String activatorFilePath) {
		return null;
	}
}
