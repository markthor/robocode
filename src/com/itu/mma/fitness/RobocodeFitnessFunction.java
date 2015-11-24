package com.itu.mma.fitness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;

import robocode.Robot;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import com.anji.persistence.Persistence;
import com.anji.util.Configurable;
import com.anji.util.Properties;
import com.itu.mma.robocode.controller.BattleListener;
import com.itu.mma.robocode.controller.RobocodeController;

public class RobocodeFitnessFunction implements BulkFitnessFunction, Configurable {
	private static Set<String> enemies;
	private static final long serialVersionUID = 4789291203830613376L;
	private RobocodeController robocodeController;
	private ActivatorTranscriber activatorFactory;
	private Persistence db = null;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void evaluate(List subjects) {
		List<Chromosome> chromosomes = (List<Chromosome>) subjects;
		for(Chromosome chromosome : chromosomes) {
			try {
				List<BattleListener> bls = new ArrayList<BattleListener>();
				
				persist(chromosome);
				
				Activator network = activatorFactory.newActivator(chromosome);
				List<Robot> enemies = new ArrayList<Robot>();
				for(String enemy : getEnemies()) {
					bls.add(robocodeController.runGame(chromosome.getId().toString(), enemy));
					//totalScore += robocodeController.runGame(network, enemy);
				}
				
				//Calc score
				int fitness = 0;
				for (BattleListener bl : bls) {
					while (!bl.isFinished()) {
						try {
							wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					fitness += bl.getFitness();
				}
				
				chromosome.setFitnessValue(fitness);
			} catch(TranscriberException e) {
				e.printStackTrace();
			}
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
		activatorFactory = (ActivatorTranscriber) props.singletonObjectProperty(ActivatorTranscriber.class);
		db = (Persistence) props.singletonObjectProperty( Persistence.PERSISTENCE_CLASS_KEY );
	}
	
	public static Set<String> getEnemies(){
		if (enemies != null) {
			return enemies;
		}
		try {
			Properties p = new Properties("ranks.properties");
			return p.stringPropertyNames();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}