/*
 * Copyright (c) 2011 Andrew Wells. All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or without modification, are
 *	permitted provided that the following conditions are met:
 *
 *		1. Redistributions of source code must retain the above copyright notice, this list of
 *     conditions and the following disclaimer.
 *
 *		2. Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 *	THIS SOFTWARE IS PROVIDED BY Andrew Wells ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *	WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *	FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Andrew Wells OR
 *	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *	CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *	SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *	ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *	ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * 	The views and conclusions contained in the software and documentation are those of the
 *	authors and should not be interpreted as representing official policies, either expressed
 *	or implied, of Andrew Wells.
 */

package aw.gun;

import aw.utils.RoboGeom;
import aw.waves.GunDataWave;
import aw.waves.DataWavePassedRecord;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;

import robocode.util.Utils;

public class VirtualGunsClassifier implements WaveGunClassifier {

	private ArrayList<WaveGunClassifier> Classifiers = new ArrayList<WaveGunClassifier>();
	private ArrayList<Double> ClassifierScores = new ArrayList<Double>();
	private ArrayList<Integer> ClassifierHits = new ArrayList<Integer>();
	private ArrayList<Double> ClassifierMissPenalties = new ArrayList<Double>();
	private double numOfScoresLogged;

	private HashMap<Long, double[]> WavesAndClassifierPredictions = new HashMap<Long, double[]>();

	VirtualGunsClassifier() {
//		GunClassifier GC = new AntiSurferGunClassifier();
//		Classifiers.add(GC);
//		ClassifierScores.add(0.0);
//		ClassifierHits.add(0);
//		ClassifierMissPenalties.add(0.0);
		
		WaveGunClassifier GC = new AntiRandomMovementGunClassifier();
		Classifiers.add(GC);
		ClassifierScores.add(0.0);
		ClassifierHits.add(0);
		ClassifierMissPenalties.add(0.0);
		GC = new AntiSurferGunClassifier();
		Classifiers.add(GC);
		ClassifierScores.add(0.0);
		ClassifierHits.add(0);
		ClassifierMissPenalties.add(0.0);
	}

	@Override
	public void train(GunDataWave wave, DataWavePassedRecord rec) {

		for (WaveGunClassifier GC : Classifiers) {
			GC.train(wave, rec);
		}
		updateScores(wave, rec);
	}
	
	@Override
	public void trainEnemyBulletDetection(GunDataWave wave, DataWavePassedRecord rec) {

		for (WaveGunClassifier GC : Classifiers) {
			GC.trainEnemyBulletDetection(wave, rec);
		}
	}	

	@Override
	public double aim(GunDataWave wave) {
		wave.recalcAttributes();

		double[] predictions = new double[Classifiers.size()];
		predictions[0] = Classifiers.get(0).aim(wave);
		double maxScore = ClassifierScores.get(0);
		int indexMaxScore = 0;

		for (int i = 1; i < Classifiers.size(); i++) {
			predictions[i] = Classifiers.get(i).aim(wave);
			if (ClassifierScores.get(i) >= maxScore) {
				maxScore = ClassifierScores.get(i);
				indexMaxScore = i;
			}
		}
			WavesAndClassifierPredictions.put(wave.getAbsFireTime(), predictions);

		return predictions[indexMaxScore];
	}
	


	@Override
	public double[][] getRawAnglesAndScores(GunDataWave wave) {
		// TODO Auto-generated method stub
		return null;
	}

//	TODO fix this
	private void updateScores(GunDataWave wave, DataWavePassedRecord rec) {
		if (WavesAndClassifierPredictions.containsKey(wave.getAbsFireTime())) {
			// NOTE: this will not adjust for recently removed classifiers
			double[] predictions = WavesAndClassifierPredictions.get(wave.getAbsFireTime());
			numOfScoresLogged++;
			for (int i = 0; i < predictions.length
					&& i < ClassifierScores.size(); i++) {
				double newValue = ClassifierScores.get(i);
				// newValue *= 0.97;
				if (Utils.normalRelativeAngle(predictions[i]
						- wave.getMaxClockwiseAngle()) <= 0.0
						&& Utils.normalRelativeAngle(predictions[i]
								- wave.getMaxCounterClockwiseAngle()) >= 0.0)
					ClassifierHits.set(i, ClassifierHits.get(i) + 1);

				double botAngle = Math.abs(Utils.normalRelativeAngle(wave
						.getMaxClockwiseAngle()
						- wave.getMaxCounterClockwiseAngle()));
				ClassifierMissPenalties
						.set(i,
								ClassifierMissPenalties.get(i)
										+ botAngle
										/ (Math.abs(Utils.normalRelativeAngle(wave
												.getGFOneAngle()
												- wave.getGFNegOneAngle())) + botAngle));

				ClassifierScores
						.set(i,
								(ClassifierHits.get(i) - ClassifierMissPenalties
										.get(i)) / numOfScoresLogged);
			}
			WavesAndClassifierPredictions.remove(wave.getAbsFireTime());
		}
	}

	/**
	 * This adds another GunClassifier. It does not train this classifier with
	 * the previous situations.
	 * 
	 * @param classifier
	 *            The GunClassifier to add.
	 */
	public void addClassifier(WaveGunClassifier classifier) {
		Classifiers.add(classifier);
	}

	public WaveGunClassifier getClassifier(int index) {
		return Classifiers.get(index);
	}

	public ArrayList<WaveGunClassifier> getClassifiers() {
		return Classifiers;
	}

	public void onPaint(Graphics2D graphicsObject, long time) {
		double maxScore = 0.0;
		int indexMaxScore = 0;
		for (int i = 0; i < Classifiers.size(); i++) {
			if (ClassifierScores.get(i) > maxScore) {
				maxScore = ClassifierScores.get(i);
				indexMaxScore = i;
			}
		}
		Classifiers.get(indexMaxScore).onPaint(graphicsObject, time);
	}

	public void listClassifierScores() {
//		System.out.println(numOfScoresLogged);
		for (int i = 0; i < ClassifierScores.size(); i++) {
			System.out.println("Targeting classifier " + i + " ("
					+ ClassifierScores.get(i) + ") Raw = " + 100.0
					* ClassifierHits.get(i) / numOfScoresLogged + "%");
		}
	}

	// public boolean removeClassifier(GunClassifier classifier) {
	// return Classifiers.remove(classifier);
	// }
}
