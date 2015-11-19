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

import java.util.ArrayList;
import java.util.HashMap;

import robocode.util.Utils;

import aw.Mallorn.tree.*;
import aw.utils.ClassificationWeightingScheme;
import aw.waves.DataWavePassedRecord;
import aw.waves.MovementDataWave;

public class AntiAdvancedTargeterClassifier implements MovementClassifier {

	private final static double _botWidthAngleMultiplier = 0.8;
	private final static int _numPointsForDensityCalcDivisor = 20;
	private final static int _maxNumOfPointsForDensityCalc = 10;
	private final static double _baseDenominatorValue = Double.MIN_VALUE;
	private final static double _weight = 500;
	private final static double _timeDecayExponent = 4.0;

	
//	double ONE_OVER_HALF_NUM_FIXED_ANGLES = 1.0 / 34;
	
	private static ClassificationWeightingScheme _weightingScheme = new AntiAdvancedTargeterWeightingScheme();
	private static KDTree<double[]> tree = new KDTree<double[]>(2002,
			_weightingScheme.getWeights().length, 2000);
	
//	TODO don't use time

	
	public AntiAdvancedTargeterClassifier() {
		
	}

	@Override
	public void train(MovementDataWave wave, DataWavePassedRecord rec) {
//		TODO bug hunt!
		double[] point = _weightingScheme.getPointCoordinates(wave);
//		for(int i = 0; i < point.length; i++) {
//			System.out.println(i + ": " + point[i]);
//		}
		double[] entry = new double[2];
		entry[0] = rec.getHitGF();
		entry[1] = wave.getAbsFireTime();
		tree.addPoint(point, entry);
	}
	
	public void trainVirtualWave(MovementDataWave wave, DataWavePassedRecord rec) {
//		TODO bug hunt!
		double[] point = _weightingScheme.getPointCoordinates(wave);
		for (int i = 0; i < point.length; i++) {
//			if (point[i] < 0 || point[i] > 10) {
//				System.out.println("ERROR" + point[i] + "    i = " + i);
//			}
		}
		double[] entry = new double[2];
		entry[0] = rec.getVisitGF();
		entry[1] = wave.getAbsFireTime();
		tree.addPoint(point, entry);
	}

	@Override
	public ArrayList<double[]> getRawAnglesWeightsAndBandwidths(MovementDataWave wave, double rawHitRate, double numShotsLogged) {
		int _variableNumPointsForDanger = Math.min(
				_maxNumOfPointsForDensityCalc, Math.min(tree.size,
						(tree.size / _numPointsForDensityCalcDivisor) + 1));

		ArrayList<double[]> anglesWeightsAndBandwidths = new ArrayList<double[]>(_variableNumPointsForDanger);
		if (tree.size == 0) {
			anglesWeightsAndBandwidths = new ArrayList<double[]>(0);
			double[] arr = new double[3];
			arr[0] = wave.convertGFToAngle(0.0);
			arr[1] = 1.0;
			arr[2] = 0.2;
		} else {
			HeapEntry<PointEntry<double[]>>[] nearestPoints = tree
					.getNNearestPoints(_weightingScheme.getPointCoordinates(wave),
							_variableNumPointsForDanger);
			double[][] entries = new double[_variableNumPointsForDanger][2];
			entries[0] = nearestPoints[0].entryData.dataObject;
			if (nearestPoints[0] == null) {
				System.out.println("Null entry");
				if (nearestPoints[0] == null)
					System.out.println("search returned null point");
				if (_weightingScheme.getPointCoordinates(wave) == null)
					System.out.println("wave datapoint = null");
				for (double num : _weightingScheme.getPointCoordinates(wave)) {
					System.out.println(num);
				}

			}
			double timeLatestEntry = entries[0][1];
			double timeEarliestEntry = entries[0][1] - 1;
			for (int i = 1; i < _variableNumPointsForDanger; i++) {
				try {
					entries[i] = nearestPoints[i].entryData.dataObject;
				} catch (NullPointerException exception) {
					System.out.println("Null Pointer Exception at: "
							+ exception.getStackTrace());
					for (double coordinate : _weightingScheme.getPointCoordinates(wave)) {
						System.out.print(coordinate + ", ");
					}
				}
				if (entries[i][1] > timeLatestEntry)
					timeLatestEntry = entries[i][1];
				else if (entries[i][1] < timeEarliestEntry)
					timeEarliestEntry = entries[i][1];
			}
			for (int i = 0; i < _variableNumPointsForDanger; i++) {
				double[] arr = new double[3];
				arr[0] = wave.convertGFToAngle(entries[i][0]);
				// TODO tweak
				// double baseValue = 40 - _variableNumPointsForDanger;
				// anglesAndWeights[i][1] = baseValue
				// * Math.sqrt((entries[i][1] - timeEarliestEntry)
				// / (timeLatestEntry - timeEarliestEntry))
				// / (Bucket.squareDistBetweenPoints(nearestPoints[i],
				// wave.dataPointCoordinates) + baseValue);
				arr[1] = _weight * Math.pow(
						(entries[i][1] - timeEarliestEntry)
								/ (timeLatestEntry - timeEarliestEntry), _timeDecayExponent)
						/ (Math.sqrt(nearestPoints[i].dist) + _baseDenominatorValue);
				arr[2]= getBandwidth();
				anglesWeightsAndBandwidths.add(arr);
			}
		}

		return anglesWeightsAndBandwidths;
	}
	
	private double getBandwidth() {
		return 0.16;
	}

}
