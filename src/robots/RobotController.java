package robots;

import org.jgap.Chromosome;

import robocode.AdvancedRobot;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import com.anji.persistence.FilePersistence;
import com.anji.persistence.Persistence;
import com.anji.util.DummyConfiguration;

public class RobotController extends AdvancedRobot {
	private static ActivatorTranscriber activatorFactory = new ActivatorTranscriber();
	private static Persistence db = new FilePersistence();
	private RobotActor actor;
	private RobotSensor sensor;
	private Activator activator;
	
	public RobotController(RobotActor actor, RobotSensor sensor, Long chromosomeId) {
		super();
		this.actor = actor;
		this.sensor = sensor;
		try {
			activator = loadActivator(chromosomeId);
		} catch(TranscriberException e) {
			//TODO: Handle error
			e.printStackTrace();
		}
	}
	
	public void run() {
		while(true) {
			actor.act(activator.next(sensor.sense()));
		}
	}
	
	private Activator loadActivator(Long chromosomeId) throws TranscriberException {
		Chromosome chromosome = db.loadChromosome(chromosomeId.toString(), new DummyConfiguration());
		return activatorFactory.newActivator(chromosome);
	}
}