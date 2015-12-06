package com.itu.mma.fitness;

import java.io.IOException;
import java.util.List;

import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;

import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import com.anji.util.Properties;

import pacman.Executor;
import pacman.controller.PacmanController;
import pacman.controllers.examples.StarterGhosts;

public class PacmanFitnessFunction implements BulkFitnessFunction {

	private static final long serialVersionUID = -1747301290297046236L;
	private ActivatorTranscriber activatorFactory;
	
	public PacmanFitnessFunction() {
			Properties props;
			try {
				props = new Properties("robocode-controller.properties");
				activatorFactory =  (ActivatorTranscriber) props.singletonObjectProperty( ActivatorTranscriber.class );
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(2);
			}
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void evaluate(List subjects) {
		List<Chromosome> chromosomes = (List<Chromosome>) subjects;
		int runs = 30;
		
		int high = 0;
		Chromosome best = null;
		
		for (Chromosome chromosome : chromosomes) {
			PacmanController pacManController = null;
			try {
				pacManController = new PacmanController(activatorFactory.newActivator(chromosome));
			} catch (TranscriberException e) {
				e.printStackTrace();
			}
			
			Executor exec = new Executor();
			
			int fitness = 0;
			for (int i = 0; i < runs; i++) {
				exec.runGame(pacManController, new StarterGhosts(), false, 0);
				fitness += pacManController.getScore();
				pacManController.reset();
			}
			
			chromosome.setFitnessValue(fitness / runs);
			
			if (fitness / runs > high) {
				best = chromosome;
			}
		}
		
		
		if (high > 6000) {
			try {
				System.in.read();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			PacmanController pacManController = null;
			try {
				pacManController = new PacmanController(activatorFactory.newActivator(best));
			} catch (TranscriberException e) {
				e.printStackTrace();
			}
			Executor exec = new Executor();
			exec.runGame(pacManController, new StarterGhosts(), true, 40);
		}
	}

	@Override
	public int getMaxFitnessValue() {
		return 1_000_000;
	}
}