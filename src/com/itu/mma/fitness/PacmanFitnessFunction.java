package com.itu.mma.fitness;

import java.util.List;

import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;

import pacman.Executor;
import pacman.controller.PacmanController;
import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Constants.MOVE;

public class PacmanFitnessFunction implements BulkFitnessFunction {

	private static final long serialVersionUID = -1747301290297046236L;
	private static final ActivatorTranscriber activatorFactory = new ActivatorTranscriber();
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void evaluate(List subjects) {
		List<Chromosome> chromosomes = (List<Chromosome>) subjects;
		for (Chromosome chromosome : chromosomes) {
//			try {

				PacmanController pacManController = null;
				try {
					pacManController = new PacmanController(activatorFactory.newActivator(chromosome));
				} catch (TranscriberException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				Controller<MOVE> pacManController = new OurPacManController(activatorFactory.newActivator(chromosome));

				Executor exec = new Executor();
				exec.runGame(pacManController, new StarterGhosts(), false, 0);

				// TODO: Uncomment when implemented
				chromosome.setFitnessValue(pacManController.getScore());
//			} catch (TranscriberException e) {
//				e.printStackTrace();
//			}
		}
	}

	@Override
	public int getMaxFitnessValue() {
		return 1_000_000;
	}
}