package robots;

import java.io.IOException;

import org.jgap.Chromosome;

import robocode.AdvancedRobot;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import com.anji.persistence.Persistence;
import com.anji.util.Configurable;
import com.anji.util.DummyConfiguration;
import com.anji.util.Properties;

public class RobotController extends AdvancedRobot implements Configurable {
	private static ActivatorTranscriber activatorFactory;
	private static Persistence db;
	private RobotActor actor;
	private RobotSensor sensor;
	private Activator activator;
	
	{
	}
	
	public RobotController(RobotActor actor, RobotSensor sensor, Long chromosomeId) {
		super();
		this.actor = actor;
		this.sensor = sensor;
//		try {
//			activator = loadActivator(chromosomeId);
//		} catch(TranscriberException e) {
//			//TODO: Handle error
//			e.printStackTrace();
//		}
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
	
	public static void main(String[] args) throws Exception {
		Properties props = new Properties( "robocode-controller.properties" );
		RobotController robot = new RobotController(null, null, 7L);
		robot.init(props);
	}

	@Override
	public void init(Properties props) throws Exception {
		db = (Persistence) props.singletonObjectProperty( Persistence.PERSISTENCE_CLASS_KEY );
		activatorFactory = (ActivatorTranscriber) props.singletonObjectProperty(ActivatorTranscriber.class);
		
	}
}