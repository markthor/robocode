package com.itu.mma.fitness;

import java.util.ArrayList;
import java.util.List;

import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;

import robocode.Robot;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TranscriberException;
import com.anji.util.Configurable;
import com.anji.util.Properties;
import com.itu.mma.robocode.controller.RobocodeController;

public class RobocodeFitnessFunction implements BulkFitnessFunction, Configurable {

	private static final long serialVersionUID = 4789291203830613376L;
	private RobocodeController robocodeController;
	private ActivatorTranscriber activatorFactory;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void evaluate(List subjects) {
		List<Chromosome> chromosomes = (List<Chromosome>) subjects;
		for(Chromosome chromosome : chromosomes) {
			try {
				Activator network = activatorFactory.newActivator(chromosome);
				List<Robot> enemies = new ArrayList<Robot>();
				int totalScore = 0;
				for(Robot enemy : enemies) {
					totalScore += robocodeController.runGame(network, enemy);
				}
				chromosome.setFitnessValue(totalScore);
			} catch(TranscriberException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int getMaxFitnessValue() {
		return 10_000;
	}

	@Override
	public void init(Properties properties) throws Exception {
		activatorFactory = (ActivatorTranscriber) properties.singletonObjectProperty(ActivatorTranscriber.class);
		
	}
}