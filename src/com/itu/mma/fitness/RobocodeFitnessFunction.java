package com.itu.mma.fitness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;

import com.anji.persistence.Persistence;
import com.anji.util.Configurable;
import com.anji.util.Properties;
import com.itu.mma.robocode.controller.BattleController;
import com.itu.mma.robocode.controller.BattleListener;
import com.itu.mma.robocode.controller.RobocodeController;

public class RobocodeFitnessFunction implements BulkFitnessFunction, Configurable {
	private static Set<String> enemies;
	private static final long serialVersionUID = 4789291203830613376L;
	private RobocodeController robocodeController;
	private Persistence db = null;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void evaluate(List subjects) {
		List<Chromosome> chromosomes = (List<Chromosome>) subjects;
		for(Chromosome chromosome : chromosomes) {
			List<BattleListener> bls = new ArrayList<BattleListener>();
			
			persist(chromosome);
			
			robocodeController = new BattleController();
			for(String enemy : getEnemies()) {
				bls.add(robocodeController.runGame("robots.RobotController", enemy));
			}
			
			//Calc score
			int fitness = 0;
			for (BattleListener bl : bls) {
				while (!bl.isFinished()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();

					}
				}
				fitness += bl.getFitness();
			}

			nextProperty();

			
			chromosome.setFitnessValue(fitness);

		}
	}
	
	private void nextProperty() {
		try {
			Properties pBot = new Properties("testbot.properties");
			Integer next = (int)pBot.get("next") + 1;
			
			Properties pController = new Properties("robocode-controller.properties");
			if ((int)pController.get("popul.size") <= next) {
				pBot.setProperty("next", "0");
			} else {
				pBot.setProperty("next", next.toString());
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void persist(Chromosome chromosome) {
		try {
			db.store(chromosome);
		} catch (Exception e) {
			System.out.println("COULD NOT PERSIST CHROMOSOME");
			e.printStackTrace();
		}
	}

	@Override
	public int getMaxFitnessValue() {
		return 10_000_000;
	}

	@Override
	public void init(Properties props) throws Exception {
		db = (Persistence) props.singletonObjectProperty( Persistence.PERSISTENCE_CLASS_KEY );
	}
	
	public static Set<String> getEnemies(){
		if (enemies != null) {
			return enemies;
		}
		try {
			Properties p = new Properties("ranks.properties");
			enemies = p.stringPropertyNames();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return enemies;
	}
}