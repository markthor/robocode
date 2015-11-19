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

package aw.movement;

import aw.utils.ClassificationWeightingScheme;
import aw.utils.RoboGeom;
import aw.waves.MovementDataWave;
import aw.waves.DataWavePassedRecord;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;

import robocode.util.Utils;

public class VirtualMovementsClassifier implements MovementClassifier {
	private boolean flattenerEnabled = false;

	private ArrayList<MovementClassifier> Classifiers = new ArrayList<MovementClassifier>();
	private ArrayList<Integer> VirtualWavesClassifierIndices = new ArrayList<Integer>();

	// private ArrayList<Double> fastClassifierScores = new ArrayList<Double>();
	// private ArrayList<Double> mediumClassifierScores = new
	// ArrayList<Double>();
	// private ArrayList<Double> slowClassifierScores = new ArrayList<Double>();
	// private HashMap<MovementDataWave, ArrayList<double[][]>>
	// WavesAndClassifierPredictions = new HashMap<MovementDataWave,
	// ArrayList<double[][]>>();

	VirtualMovementsClassifier() {
		ClassificationWeightingScheme CWS = new AntiSimpleTargeterWeightingScheme();
		MovementClassifier MC = new KNNMovementClassifier(CWS, 36, 0, 5, 3, 1.0, 
				Double.MIN_VALUE, 0.0, 0.15);
		Classifiers.add(MC);

		CWS = new AntiSimpleTargeter2WeightingScheme();
		MC = new KNNMovementClassifier(CWS, 36, 0, 5, 3, 1.0, Double.MIN_VALUE, 0.0, 0.15);
		Classifiers.add(MC);

		CWS = new AntiSimpleTargeter3WeightingScheme();
		MC = new KNNMovementClassifier(CWS, 36, 0, 5, 3, 1.0, Double.MIN_VALUE, 0.0, 0.15);
		Classifiers.add(MC);
		

//		MC = new AntiStandardTargeterClassifier();
		CWS = new AntiStandardTargeterWeightingScheme();
		MC = new KNNMovementClassifier(CWS, 36, 0, 5, 10, 50.0, Double.MIN_VALUE, 0.0, 0.08);
		Classifiers.add(MC);

//		MC = new AntiSemiAdvancedTargeterClassifier();
//		Classifiers.add(MC);
//		MC = new AntiSemiAdvancedTargeter2Classifier();
//		Classifiers.add(MC);
//		MC = new AntiSemiAdvancedTargeter3Classifier();
//		Classifiers.add(MC);
//		MC = new AntiSemiAdvancedTargeter4Classifier();
//		Classifiers.add(MC);
		
//		CWS = new AntiSemiAdvancedTargeterWeightingScheme();
//		MC = new KNNMovementClassifier(CWS, 36, 1, 5, 5, 50.0, Double.MIN_VALUE, 1.0, 0.2);
//		Classifiers.add(MC);
//		MC = new KNNMovementClassifier(CWS, 36, 3, 5, 5, 50.0, Double.MIN_VALUE, 1.0, 0.2);
//		Classifiers.add(MC);
//		MC = new KNNMovementClassifier(CWS, 36, 5, 5, 5, 50.0, Double.MIN_VALUE, 1.0, 0.2);
//		Classifiers.add(MC);
//		MC = new KNNMovementClassifier(CWS, 36, 10, 5, 5, 50.0, Double.MIN_VALUE, 1.0, 0.2);
//		Classifiers.add(MC);
//		MC = new KNNMovementClassifier(CWS, 36, 20, 5, 5, 50.0, Double.MIN_VALUE, 1.0, 0.2);
//		Classifiers.add(MC);
//		MC = new KNNMovementClassifier(CWS, 55, 50, 5, 5, 50.0, Double.MIN_VALUE, 1.0, 0.2);
//		Classifiers.add(MC);
//		MC = new KNNMovementClassifier(CWS, 105, 100, 5, 5, 50.0, Double.MIN_VALUE, 1.0, 0.2);
//		Classifiers.add(MC);
//		MC = new KNNMovementClassifier(CWS, 36, 0, 5, 10, 50.0, Double.MIN_VALUE, 1.0, 0.2);
//		Classifiers.add(MC);

		CWS = new AntiSemiAdvancedTargeterWeightingScheme();
		
		MC = new KNNMovementClassifier(CWS, 36, 1, 5, 5, 50.0, Double.MIN_VALUE, 4.0, 0.08);
		Classifiers.add(MC);
		MC = new KNNMovementClassifier(CWS, 36, 3, 5, 5, 50.0, Double.MIN_VALUE, 4.0, 0.08);
		Classifiers.add(MC);
		MC = new KNNMovementClassifier(CWS, 36, 5, 5, 5, 50.0, Double.MIN_VALUE, 4.0, 0.08);
		Classifiers.add(MC);
		MC = new KNNMovementClassifier(CWS, 36, 10, 5, 5, 50.0, Double.MIN_VALUE, 4.0, 0.08);
		Classifiers.add(MC);
		MC = new KNNMovementClassifier(CWS, 36, 20, 5, 5, 50.0, Double.MIN_VALUE, 4.0, 0.08);
		Classifiers.add(MC);
		MC = new KNNMovementClassifier(CWS, 55, 50, 5, 5, 50.0, Double.MIN_VALUE, 4.0, 0.08);
		Classifiers.add(MC);
		MC = new KNNMovementClassifier(CWS, 105, 100, 5, 5, 50.0, Double.MIN_VALUE, 4.0, 0.08);
		Classifiers.add(MC);
		MC = new KNNMovementClassifier(CWS, 36, 0, 5, 10, 50.0, Double.MIN_VALUE, 4.0, 0.08);
		Classifiers.add(MC);
		
		MC = new AntiAdvancedTargeterClassifier();
		Classifiers.add(MC);
		VirtualWavesClassifierIndices.add(12);

		MC = new AntiAdvancedTargeter2Classifier();
		Classifiers.add(MC);
		VirtualWavesClassifierIndices.add(13);
	}

	@Override
	public void train(MovementDataWave wave, DataWavePassedRecord rec) {
		for (MovementClassifier MC : Classifiers) {
			MC.train(wave, rec);
		}

		// if (WavesAndClassifierPredictions.containsKey(wave)) {
		// double angle = 0.5 * RoboGeom.preciseBotWidthAngle(
		// wave.getSourcePosition(),
		// wave.getBulletVelocity() * wave.getBulletTravelTime(),
		// rec.getHitAngle());
		//
		// // NOTE: this will not adjust for recently removed classifiers
		// ArrayList<double[][]> predictions = WavesAndClassifierPredictions
		// .get(wave);
		// for (int i = 0; i < predictions.size()
		// && i < fastClassifierScores.size(); i++) {
		//
		// double[][] AWBArray = predictions.get(i);
		// wave.setAnglesWeightsAndBandwidths(AWBArray);
		// double totalArea = Double.MIN_VALUE;
		// // for (double[] AWB : AWBArray) {
		// // totalArea += AWB[1];
		// // }
		// if (wave.getOrbitDir() == 1) {
		// totalArea = wave
		// .getDangerWithoutBulletShadowsForAngles(
		// wave.getGFOneAngle(),
		// wave.getGFNegOneAngle());
		// } else {
		// totalArea = wave
		// .getDangerWithoutBulletShadowsForAngles(
		// wave.getGFNegOneAngle(),
		// wave.getGFOneAngle());
		// }
		// double additionalScore = wave
		// .getDangerWithoutBulletShadowsForAngles(
		// rec.getHitAngle() + angle, rec.getHitAngle()
		// - angle)
		// / totalArea;
		// double newValue = fastClassifierScores.get(i);
		// newValue *= 0.7;
		// newValue += additionalScore;
		// fastClassifierScores.set(i, newValue);
		// newValue = mediumClassifierScores.get(i);
		// newValue *= 0.9;
		// newValue += additionalScore;
		// mediumClassifierScores.set(i, newValue);
		// newValue = slowClassifierScores.get(i);
		// newValue *= 0.97;
		// newValue += additionalScore;
		// slowClassifierScores.set(i, newValue);
		// }
		// }
	}

	// TODO make sure we don't change waves from the flattener after they have
	// been fired
	public void trainVirtualWave(MovementDataWave wave, DataWavePassedRecord rec) {
		// System.out.println("Start V Adding Points");

		for (Integer i : VirtualWavesClassifierIndices) {
			Classifiers.get(i).trainVirtualWave(wave, rec);
		}
		// System.out.println("Done V Adding Points");

	}

	public void trainSafelyPassedOfBullet(MovementDataWave wave) {
		// if (WavesAndClassifierPredictions.containsKey(wave)) {
		// // NOTE: this will not adjust for recently removed classifiers
		//
		// // TODO use large bulletShadows to train classifiers.
		// ArrayList<double[][]> predictions = WavesAndClassifierPredictions
		// .get(wave);
		// for (int i = 0; i < predictions.size()
		// && i < fastClassifierScores.size(); i++) {
		//
		// double[][] AWBArray = predictions.get(i);
		// wave.setAnglesWeightsAndBandwidths(AWBArray);
		// double totalArea = Double.MIN_VALUE;
		// // for (double[] AWB : AWBArray) {
		// // totalArea += AWB[1];
		// // }
		// if (wave.getOrbitDir() == 1) {
		// totalArea = wave
		// .getDangerWithoutBulletShadowsForAngles(
		// wave.getGFOneAngle(),
		// wave.getGFNegOneAngle());
		// } else {
		// totalArea = wave
		// .getDangerWithoutBulletShadowsForAngles(
		// wave.getGFNegOneAngle(),
		// wave.getGFOneAngle());
		// }
		// double additionalScore = 1.00
		// * wave.getDangerWithoutBulletShadowsForAngles(
		// wave.getMaxClockwiseAngle(),
		// wave.getMaxCounterClockwiseAngle()) / totalArea;
		// double newValue = fastClassifierScores.get(i);
		// newValue -= additionalScore;
		// fastClassifierScores.set(i, newValue);
		// newValue = mediumClassifierScores.get(i);
		// newValue -= additionalScore;
		// mediumClassifierScores.set(i, newValue);
		// newValue = slowClassifierScores.get(i);
		// newValue -= additionalScore;
		// slowClassifierScores.set(i, newValue);
		// }
		// }
	}

	@Override
	public ArrayList<double[]> getRawAnglesWeightsAndBandwidths(MovementDataWave wave, double rawHitRate, double numShotsLogged) {
		// TODO use the AnglesWeightAndBandwidths class ?
		double maxScore = Double.NEGATIVE_INFINITY;
		int indexMaxScore = 0;
		double secondBestScore = Double.NEGATIVE_INFINITY;
		int indexSecondBestScore = 0;

		ArrayList<ArrayList<double[]>> predictions = new ArrayList<ArrayList<double[]>>();

		// for (int i = 0; i < Classifiers.size(); i++) {
		// predictions.add(Classifiers.get(i)
		// .getRawAnglesWeightsAndBandwidths(wave));
		// double score = fastClassifierScores.get(i)
		// + mediumClassifierScores.get(i)
		// + slowClassifierScores.get(i);
		// // double score = slowClassifierScores.get(i);
		//
		// if (score > maxScore) {
		// maxScore = score;
		// indexMaxScore = i;
		// } else if (score > secondBestScore) {
		// secondBestScore = score;
		// indexSecondBestScore = i;
		// }
		// }
		// WavesAndClassifierPredictions.put(wave, predictions);

		ArrayList<double[]> AWB = new ArrayList<double[]>();
		AWB.addAll(predictions.get(indexMaxScore));
		AWB.addAll(predictions.get(indexSecondBestScore));
		return AWB;
	}

	public ArrayList<double[]> getRawAnglesWeightsAndBandwidths(MovementDataWave wave, double enemyHits, double enemyMissPenalties, double enemyRawMissPenalties, 
			double enemyShotsLogged, double numberOfBulletHitEvents) {
		// TODO use the AnglesWeightAndBandwidths class ?
		// double maxScore = Double.NEGATIVE_INFINITY;
		// int indexMaxScore = 0;
		// double secondBestScore = Double.NEGATIVE_INFINITY;
		// int indexSecondBestScore = 0;

		ArrayList<ArrayList<double[]>> predictions = new ArrayList<ArrayList<double[]>>();

		// for (int i = 0; i < Classifiers.size(); i++) {
		// predictions.add(Classifiers.get(i)
		// .getRawAnglesWeightsAndBandwidths(wave));
		// double score = fastClassifierScores.get(i)
		// + mediumClassifierScores.get(i)
		// + slowClassifierScores.get(i);
		// // double score = slowClassifierScores.get(i);
		//
		// if (score > maxScore) {
		// maxScore = score;
		// indexMaxScore = i;
		// } else if (score > secondBestScore) {
		// secondBestScore = score;
		// indexSecondBestScore = i;
		// }
		// }
		// WavesAndClassifierPredictions.put(wave, predictions);

//		TODO optimize threshohlds of remove them somehow
		
		for (int i = 0; i < Classifiers.size(); i++) {
			predictions.add(Classifiers.get(i)
					.getRawAnglesWeightsAndBandwidths(wave, enemyHits/enemyShotsLogged, enemyShotsLogged));
		}
		double enemyHitPercentage = enemyHits / enemyShotsLogged;

		if ((enemyHits - enemyMissPenalties) / enemyShotsLogged < -0.08
				|| enemyShotsLogged * enemyHitPercentage < 3.0) {
			ArrayList<double[]> AWB = new ArrayList<double[]>();
			AWB.addAll(predictions.get(0));
			AWB.addAll(predictions.get(1));
			AWB.addAll(predictions.get(2));
			normalizeWeights(AWB);
			return AWB;
		} else if (!flattenerEnabled(enemyHits, enemyMissPenalties, enemyRawMissPenalties, enemyShotsLogged, numberOfBulletHitEvents)) {
			if ((enemyHits - enemyMissPenalties) / enemyShotsLogged < -0.045) {
				ArrayList<double[]> AWB = new ArrayList<double[]>();
				AWB.addAll(predictions.get(0));
				AWB.addAll(predictions.get(1));
				AWB.addAll(predictions.get(2));
				AWB.addAll(predictions.get(3));
				normalizeWeights(AWB);
				return AWB;
			} else {
				ArrayList<double[]> AWB = new ArrayList<double[]>();
				AWB.addAll(predictions.get(0));
				AWB.addAll(predictions.get(1));
				AWB.addAll(predictions.get(2));
				AWB.addAll(predictions.get(3));
				AWB.addAll(predictions.get(4));
				AWB.addAll(predictions.get(5));
				AWB.addAll(predictions.get(6));
				AWB.addAll(predictions.get(7));
				AWB.addAll(predictions.get(8));
				AWB.addAll(predictions.get(9));
				AWB.addAll(predictions.get(10));
				AWB.addAll(predictions.get(11));
				normalizeWeights(AWB);
				return AWB;
			}

		} else {
			// double score2 = fastClassifierScores.get(2)
			// + mediumClassifierScores.get(2)
			// + slowClassifierScores.get(2);
			// double score3 = fastClassifierScores.get(3)
			// + mediumClassifierScores.get(3)
			// + slowClassifierScores.get(3);
			// if(score2 > score3)
			// return predictions.get(2);
			// else
			// return predictions.get(3);
			// if(score2 > score3) {
			// double[][] AWB = new double[predictions.get(1).length +
			// predictions.get(2).length][3];
			// System.arraycopy(predictions.get(1), 0, AWB, 0,
			// predictions.get(1).length);
			// System.arraycopy(predictions.get(2), 0, AWB,
			// predictions.get(1).length,
			// predictions.get(2).length);
			// return AWB;
			// }
			// else {
			// double[][] AWB = new double[predictions.get(1).length +
			// predictions.get(3).length][3];
			// System.arraycopy(predictions.get(1), 0, AWB, 0,
			// predictions.get(1).length);
			// System.arraycopy(predictions.get(3), 0, AWB,
			// predictions.get(1).length,
			// predictions.get(3).length);
			// return AWB;
			// }

			ArrayList<double[]> AWB = new ArrayList<double[]>();
			AWB.addAll(predictions.get(3));
			AWB.addAll(predictions.get(4));
			AWB.addAll(predictions.get(5));
			AWB.addAll(predictions.get(6));
			AWB.addAll(predictions.get(7));
			AWB.addAll(predictions.get(8));
			AWB.addAll(predictions.get(9));
			AWB.addAll(predictions.get(10));
			AWB.addAll(predictions.get(11));
			AWB.addAll(predictions.get(12));
			AWB.addAll(predictions.get(13));
			normalizeWeights(AWB);
			return AWB;
		}

		// double[][] AWB = new double[predictions.get(indexMaxScore).length
		// + predictions.get(indexSecondBestScore).length][3];
		// System.arraycopy(predictions.get(indexMaxScore), 0, AWB, 0,
		// predictions.get(indexMaxScore).length);
		// System.arraycopy(predictions.get(indexSecondBestScore), 0, AWB,
		// predictions.get(indexMaxScore).length,
		// predictions.get(indexSecondBestScore).length);
		//
		// return AWB;
	}
	
	private void normalizeWeights(ArrayList<double[]> AWB) {
		double sumOfWeights = 0;
		
		for(int i = 0; i < AWB.size(); i++) {
			sumOfWeights += AWB.get(i)[1];
		}
		for(int i = 0; i < AWB.size(); i++) {
			AWB.get(i)[1] = AWB.get(i)[1] / sumOfWeights;
		}
	}

	/**
	 * This adds another MovementClassifier. It does not train this classifier
	 * with the previous situations.
	 * 
	 * @param classifier
	 *            The MovementClassifier to add.
	 */
	public void addClassifier(MovementClassifier classifier) {
		Classifiers.add(classifier);
	}

	public MovementClassifier getClassifier(int index) {
		return Classifiers.get(index);
	}

	public ArrayList<MovementClassifier> getClassifiers() {
		return Classifiers;
	}
	
	

	public boolean flattenerEnabled(double enemyHits, double enemyMissPenalties, double enemyRawMissPenalties, double enemyShotsLogged, double numberOfBulletHitBulletEvents) {
		double enemyHitPercentage = enemyHits / enemyShotsLogged;
//		boolean flattenerStatus = (enemyHits >= 3.0)
//				&& ((enemyHits + numberOfBulletHitBulletEvents * enemyHitPercentage - enemyMissPenalties) / enemyShotsLogged >= 0.5 * Math
//						.sqrt(enemyHitPercentage * (1.0 - enemyHitPercentage)
//								/ enemyShotsLogged));
		
//				boolean flattenerStatus = (enemyHits >= 3.0) && ((enemyHits + numberOfBulletHitBulletEvents * enemyHitPercentage - enemyMissPenalties) / enemyShotsLogged) >= 0.0
//				&& ((enemyHits + numberOfBulletHitBulletEvents * enemyHitPercentage - enemyRawMissPenalties) / enemyShotsLogged >= 1.9 * Math
//						.sqrt(enemyHitPercentage * (1.0 - enemyHitPercentage)
//								/ enemyShotsLogged));
		boolean flattenerStatus = (enemyHits >= 3.0) && ((enemyHits + numberOfBulletHitBulletEvents * enemyHitPercentage - enemyRawMissPenalties) / enemyShotsLogged >= 1.9 * Math
				.sqrt(enemyHitPercentage * (1.0 - enemyHitPercentage)
						/ enemyShotsLogged));
		
		if(flattenerEnabled == false && flattenerStatus == true) {
			flattenerEnabled = true;
			System.out.println("Flattener enabled.");
		}
		else if(flattenerEnabled == true && flattenerStatus == false) {
			flattenerEnabled = false;
			System.out.println("Flattener disabled.");
		}
		
		return flattenerStatus;
		// return true;
	}

	public void listClassifierScores() {
		// for (int i = 0; i < fastClassifierScores.size(); i++) {
		// double score = fastClassifierScores.get(i)
		// + mediumClassifierScores.get(i)
		// + slowClassifierScores.get(i);
		//
		// System.out.println("Movement classifier " + i + "(" + score + ")");
		// System.out.println("Movement classifier " + i + "fast score (" +
		// fastClassifierScores.get(i) + ")");
		// System.out.println("Movement classifier " + i + "medium score ("
		// + mediumClassifierScores.get(i) + ")");
		// System.out.println("Movement classifier " + i + "slow score ("
		// + slowClassifierScores.get(i) + ")\n");

		// }
	}

	// public boolean removeClassifier(MovementClassifier classifier) {
	// return Classifiers.remove(classifier);
	// }
}
