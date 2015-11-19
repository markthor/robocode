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

import aw.Mallorn.tree.*;
import aw.utils.ClassificationWeightingScheme;
import aw.utils.misc;
import aw.waves.DataWavePassedRecord;
import aw.waves.MovementDataWave;
import java.util.ArrayList;

import robocode.util.Utils;

public class KNNMovementClassifier implements MovementClassifier {

	private double defaultBandwidth;
	private int _numPointsForDensityCalcDivisor;
	private int _maxNumOfPointsForDensityCalc;
	private double _weight;
	private double _baseDenominatorValue;
	private double _timeDecayExponent;

	// double ONE_OVER_HALF_NUM_FIXED_ANGLES = 1.0 / 34;

	private ClassificationWeightingScheme _weightingScheme;
	private KDTree<double[]> tree;

	// TODO don't use time

	public KNNMovementClassifier(ClassificationWeightingScheme weightingScheme,
			int maxBucketSize, int maxTreeSize,
			int numPointsForDensityCalcDivisor, int maxNumPointForDensityCalc,
			double weight, double baseDenominatiorValue,
			double timeDecayExponent, double defaultBandwidth) {
		this._weightingScheme = weightingScheme;

		if (maxTreeSize == 0)
			tree = new KDTree<double[]>(maxBucketSize,
					_weightingScheme.getWeights().length);
		else
			tree = new KDTree<double[]>(maxBucketSize,
					_weightingScheme.getWeights().length, maxTreeSize);

		this._numPointsForDensityCalcDivisor = numPointsForDensityCalcDivisor;
		this._maxNumOfPointsForDensityCalc = maxNumPointForDensityCalc;
		this._weight = weight;
		this._baseDenominatorValue = baseDenominatiorValue;
		this._timeDecayExponent = timeDecayExponent;
		this.defaultBandwidth = defaultBandwidth;
	}

	public KNNMovementClassifier(ClassificationScheme Scheme) {
		this._weightingScheme = Scheme.getWeightingScheme();

		if (Scheme.getMaxTreeSize() == 0)
			tree = new KDTree<double[]>(Scheme.getMaxBucketSize(),
					_weightingScheme.getWeights().length);
		else
			tree = new KDTree<double[]>(Scheme.getMaxBucketSize(),
					_weightingScheme.getWeights().length,
					Scheme.getMaxTreeSize());

		this._numPointsForDensityCalcDivisor = Scheme
				.getNumPointsForDensityCalcDivisor();
		this._maxNumOfPointsForDensityCalc = Scheme
				.getMaxNumPointsForDensityCalc();
		this._baseDenominatorValue = Scheme.getBaseDenominatorValue();
		this._timeDecayExponent = Scheme.getTimeDecayExponent();
		this.defaultBandwidth = Scheme.getBotWidthAngleMultiplier();
	}

	@Override
	public void train(MovementDataWave wave, DataWavePassedRecord rec) {
		double[] point = _weightingScheme.getPointCoordinates(wave);

		double[] entry = new double[2];
		entry[0] = rec.getHitGF();
		entry[1] = wave.getAbsFireTime();
		tree.addPoint(point, entry);
	}

	public void trainVirtualWave(MovementDataWave wave, DataWavePassedRecord rec) {
		double[] point = _weightingScheme.getPointCoordinates(wave);
		for (int i = 0; i < point.length; i++) {
			// if (point[i] < 0 || point[i] > 10) {
			// System.out.println("ERROR" + point[i] + "    i = " + i);
			// }
		}
		double[] entry = new double[2];
		entry[0] = rec.getVisitGF();
		entry[1] = wave.getAbsFireTime();
		tree.addPoint(point, entry);
	}

	@Override
	public ArrayList<double[]> getRawAnglesWeightsAndBandwidths(
			MovementDataWave wave, double rawHitRate, double numShotsLogged) {
		int _variableNumPointsForDanger = Math.min(
				_maxNumOfPointsForDensityCalc, Math.min(tree.size,
						(tree.size / _numPointsForDensityCalcDivisor) + 1));

		ArrayList<double[]> anglesWeightsAndBandwidths = new ArrayList<double[]>(
				_variableNumPointsForDanger);
		if (tree.size == 0) {
			anglesWeightsAndBandwidths = new ArrayList<double[]>(0);
			double[] arr = new double[3];
			arr[0] = wave.convertGFToAngle(0.0);
			arr[1] = 1.0;
			arr[2] = 0.2;
			anglesWeightsAndBandwidths.add(arr);
		} else {
			HeapEntry<PointEntry<double[]>>[] nearestPoints = tree
					.getNNearestPoints(
							_weightingScheme.getPointCoordinates(wave),
							_variableNumPointsForDanger);
			double[][] entries = new double[_variableNumPointsForDanger][2];
			entries[0] = nearestPoints[0].entryData.dataObject;
			if (entries[0] == null) {
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
			double[] timesOfEntries = new double[nearestPoints.length];

			for (int i = 1; i < _variableNumPointsForDanger; i++) {
				try {
					entries[i] = nearestPoints[i].entryData.dataObject;
				} catch (NullPointerException exception) {
					System.out.println("Null Pointer Exception at: "
							+ exception.getStackTrace());
					for (double coordinate : _weightingScheme
							.getPointCoordinates(wave)) {
						System.out.print(coordinate + ", ");
					}
				}
				if (entries[i] == null) {
					System.out.println("NullPointer at 171 " + tree.size + "; "
							+ i);
				}
				if (entries[i][1] > timeLatestEntry)
					timeLatestEntry = entries[i][1];
				else if (entries[i][1] < timeEarliestEntry)
					timeEarliestEntry = entries[i][1];

				timesOfEntries[i] = entries[i][1];
			}

			int[] orderTimeOfEntries = aw.utils.misc
					.getOrderAscendingDangers(timesOfEntries);
			
//			TODO bug hunt
//			for(int i = 0; i < orderTimeOfEntries.length; i++) {
//				System.out.println(timesOfEntries[orderTimeOfEntries[i]]);
//			}
//			
//			System.out.println("dist of earliest point: " + nearestPoints[orderTimeOfEntries[0]].dist);
//			System.out.println("time of earliest point: " + nearestPoints[orderTimeOfEntries[0]].entryData.dataObject[1]);
//			double[] pcoor = nearestPoints[orderTimeOfEntries[0]].entryData.pointCoordinates;
//			for(double d : pcoor) {
//				System.out.println(d);
//			}
			
			
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
				 / (timeLatestEntry - timeEarliestEntry),
				 _timeDecayExponent)
				 / (Math.sqrt(nearestPoints[i].dist) + _baseDenominatorValue);
//				if (_timeDecayExponent == 0)
//					arr[1] = _weight
//							/ (Math.sqrt(nearestPoints[i].dist) + _baseDenominatorValue);
//				else
//					arr[1] = _weight
//							* Math.pow(2, orderTimeOfEntries[i]
//									* _timeDecayExponent)
//							/ (Math.sqrt(nearestPoints[i].dist) + _baseDenominatorValue);

				arr[2] = getBandwidth(rawHitRate, numShotsLogged);

				anglesWeightsAndBandwidths.add(arr);
			}
		}

		return anglesWeightsAndBandwidths;
	}

	private double getBandwidth(double rawHitRate, double numShotsLogged) {
		double retentionFactor = 2;
		return (defaultBandwidth * retentionFactor + Math.sqrt(numShotsLogged) * Math.min(0.2,
				Math.max(rawHitRate, 0.02))) / (retentionFactor + Math.sqrt(numShotsLogged));
//		return 0.08;
	}

}
