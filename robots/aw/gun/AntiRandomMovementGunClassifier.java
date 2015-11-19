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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import aw.waves.DataWavePassedRecord;

import robocode.util.Utils;

import aw.Mallorn.tree.*;
import aw.utils.ClassificationWeightingScheme;
import aw.utils.RoboGeom;
import aw.waves.GunDataWave;

public class AntiRandomMovementGunClassifier implements WaveGunClassifier {

	private GunDataWave LatestWave = null;
	private long lastAimTime = 0;

	private final static double _botWidthAngleMultiplier = 0.8;
	private final static int _numPointsForDensityCalcDivisor = 15;
	private final static int _maxNumOfPointsForDensityCalc = 340;
	private final static int _numOfFixedAnglesForDensityCalc = 61;
	private final static double _baseDenominatorValue = Double.MIN_VALUE;

	private final static double ONE_OVER_HALF_NUM_FIXED_ANGLES = 1.0 / 30;
	private final static int halfNumOfAngles = _numOfFixedAnglesForDensityCalc / 2;

	private static ClassificationWeightingScheme _weightingScheme = new AntiRandomMovementWeightingScheme();
	private static KDTree<Double> _tree = new KDTree<Double>(36,
			_weightingScheme.getWeights().length);

	public AntiRandomMovementGunClassifier() {
	}

	@Override
	public void train(GunDataWave wave, DataWavePassedRecord rec) {
		// TODO bug hunt!
		double[] point = _weightingScheme.getPointCoordinates(wave);
		// for(int i = 0; i < point.length; i++) {
		// if(point[i] < 0 || point[i] > 10)
		// System.out.println(i + ": " + point[i]);
		// }
		_tree.addPoint(point, rec.getVisitGF());
	}
	
	@Override
	public void trainEnemyBulletDetection(GunDataWave wave, DataWavePassedRecord rec) {
		return;
	}	

	@Override
	public double aim(GunDataWave wave) {
		LatestWave = wave;
		lastAimTime = wave.getFireTime() - 1;

		int _variableNumOfPointsForDensityCalc = Math
				.min(_maxNumOfPointsForDensityCalc,
						Math.min(
								_tree.size,
								(_tree.size / _numPointsForDensityCalcDivisor) + 1));

		// TODO adaptive learning
		if (_variableNumOfPointsForDensityCalc > _numOfFixedAnglesForDensityCalc) {
			double GFZeroBearing = wave.getGFZeroAngle();

			double angleOfPositiveGFs = Utils.normalRelativeAngle(wave
					.getGFOneAngle() - GFZeroBearing);
			double angleOfNegativeGFs = Utils.normalRelativeAngle(wave
					.getGFNegOneAngle() - GFZeroBearing);

			double[] currentDataPoint = _weightingScheme
					.getPointCoordinates(wave);

			HeapEntry<PointEntry<Double>>[] heapOfPoints = _tree.getNNearestPoints(
					currentDataPoint, _variableNumOfPointsForDensityCalc);
			double[][] fixedAngles = new double[_numOfFixedAnglesForDensityCalc][2];

			int i;
			for (i = _numOfFixedAnglesForDensityCalc / 2; i > 0; i--) {
				fixedAngles[i][0] = GFZeroBearing + angleOfPositiveGFs * i
						* ONE_OVER_HALF_NUM_FIXED_ANGLES;
				fixedAngles[i][1] = 0;
				fixedAngles[i + halfNumOfAngles][0] = GFZeroBearing
						+ angleOfNegativeGFs * i
						* ONE_OVER_HALF_NUM_FIXED_ANGLES;
				fixedAngles[i + halfNumOfAngles][1] = 0;
			}
			fixedAngles[i][0] = GFZeroBearing;
			fixedAngles[i][1] = 0;

			double[][] anglesOfNearestPoints = new double[_variableNumOfPointsForDensityCalc][2];
			for (i = 0; i < _variableNumOfPointsForDensityCalc; i++) {
				double GF = 0.0;
				try {
					GF = heapOfPoints[i].entryData.dataObject;
				} catch (NullPointerException exception) {
					System.out.println("Null Pointer Exception at: "
							+ exception.getStackTrace());
					for (double coordinate : currentDataPoint) {
						System.out.print(coordinate + ", ");
					}
				}
				// if (GF > 1 || GF < -1)
				// robot.out.println(GF);

				if (GF > 0)
					anglesOfNearestPoints[i][0] = GFZeroBearing + GF
							* angleOfPositiveGFs;
				else
					anglesOfNearestPoints[i][0] = GFZeroBearing - GF
							* angleOfNegativeGFs;

				anglesOfNearestPoints[i][1] = 1.0 / (Math.sqrt(heapOfPoints[i].dist) + _baseDenominatorValue);

			}

			double bestAngleDensity = 0;
			int indexBestAngle = 0;

			for (i = 0; i < _numOfFixedAnglesForDensityCalc; i++) {
				double distForBotWidthAngle = 0;
				// if (anglesOfNearestPoints[i][1] > 0)
				distForBotWidthAngle = wave.getSourceToTargetDistance();
				// + anglesOfNearestPoints[i][1]
				// * (Point2D.distance(robotCoordinates.x,
				// robotCoordinates.y, enemyGFOnePoint.x,
				// enemyGFOnePoint.y) - preciseDistanceToEnemy);
				// else
				// distForBotWidthAngle = preciseDistanceToEnemy
				// - anglesOfNearestPoints[i][1]
				// * (Point2D.distance(robotCoordinates.x,
				// robotCoordinates.y, enemyGFNegOnePoint.x,
				// enemyGFNegOnePoint.y) - preciseDistanceToEnemy);

				// use 3/5 instead of 1/2 because the robot will probably be
				// moving:
				double botWidthAngle = RoboGeom.preciseBotWidthAngle(
						wave.getSourcePosition(), distForBotWidthAngle,
						fixedAngles[i][0]) * _botWidthAngleMultiplier;

				for (double angle[] : anglesOfNearestPoints) {

					double ux = Utils.normalRelativeAngle(fixedAngles[i][0]
							- angle[0])
							/ botWidthAngle;
					if (Math.abs(ux) < 1)
						fixedAngles[i][1] += ((1) - ux * ux * (1.0 - 0.34 * ux * ux))
								* (angle[1]);
				}
				if (fixedAngles[i][1] > bestAngleDensity) {
					bestAngleDensity = fixedAngles[i][1];
					indexBestAngle = i;
				}
			}
			return fixedAngles[indexBestAngle][0];
		} else {
			if(_variableNumOfPointsForDensityCalc == 0) {
				return wave.getGFZeroAngle();
			}
			double GFZeroBearing = wave.getGFZeroAngle();

			double angleOfPositiveGFs = Utils.normalRelativeAngle(wave
					.getGFOneAngle() - GFZeroBearing);
			double angleOfNegativeGFs = Utils.normalRelativeAngle(wave
					.getGFNegOneAngle() - GFZeroBearing);

			double[] currentDataPoint = _weightingScheme
					.getPointCoordinates(wave);

			HeapEntry<PointEntry<Double>>[] heapOfPoints = _tree.getNNearestPoints(
					currentDataPoint, _variableNumOfPointsForDensityCalc);
			double[][]anglesAndScores = new double[_variableNumOfPointsForDensityCalc][2];

			double[][] anglesOfNearestPoints = new double[_variableNumOfPointsForDensityCalc][2];
			for (int i = 0; i < _variableNumOfPointsForDensityCalc; i++) {
				double GF = 0.0;
				try {
					GF = heapOfPoints[i].entryData.dataObject;
				} catch (NullPointerException exception) {
					System.out.println("Null Pointer Exception at: "
							+ exception.getStackTrace());
					for (double coordinate : currentDataPoint) {
						System.out.print(coordinate + ", ");
					}
				}
				// if (GF > 1 || GF < -1)
				// robot.out.println(GF);

				if (GF > 0)
					anglesOfNearestPoints[i][0] = GFZeroBearing + GF
							* angleOfPositiveGFs;
				else
					anglesOfNearestPoints[i][0] = GFZeroBearing - GF
							* angleOfNegativeGFs;

				anglesOfNearestPoints[i][1] = 1.0 / (Math.sqrt(heapOfPoints[i].dist) + _baseDenominatorValue);
				anglesAndScores[i][0] = anglesOfNearestPoints[i][0];
			}

			double bestAngleDensity = 0;
			int indexBestAngle = 0;

			for (int i = 0; i < _variableNumOfPointsForDensityCalc; i++) {
				double distForBotWidthAngle = 0;
				// if (anglesOfNearestPoints[i][1] > 0)
				distForBotWidthAngle = wave.getSourceToTargetDistance();
				// + anglesOfNearestPoints[i][1]
				// * (Point2D.distance(robotCoordinates.x,
				// robotCoordinates.y, enemyGFOnePoint.x,
				// enemyGFOnePoint.y) - preciseDistanceToEnemy);
				// else
				// distForBotWidthAngle = preciseDistanceToEnemy
				// - anglesOfNearestPoints[i][1]
				// * (Point2D.distance(robotCoordinates.x,
				// robotCoordinates.y, enemyGFNegOnePoint.x,
				// enemyGFNegOnePoint.y) - preciseDistanceToEnemy);

				// use 3/5 instead of 1/2 because the robot will probably be
				// moving:
				double botWidthAngle = RoboGeom.preciseBotWidthAngle(
						wave.getSourcePosition(), distForBotWidthAngle,
						anglesAndScores[i][0]) * _botWidthAngleMultiplier;

				for (double angle[] : anglesOfNearestPoints) {

					double ux = Utils.normalRelativeAngle(anglesAndScores[i][0]
							- angle[0])
							/ botWidthAngle;
					anglesAndScores[i][1] += Math.exp(-ux * ux)
								* (angle[1]);
				}
				if (anglesAndScores[i][1] > bestAngleDensity) {
					bestAngleDensity = anglesAndScores[i][1];
					indexBestAngle = i;
				}
			}
			return anglesAndScores[indexBestAngle][0];
		}
	}

	@Override
	public double[][] getRawAnglesAndScores(GunDataWave wave) {
		int _variableNumOfPointsForDensityCalc = Math
				.min(_maxNumOfPointsForDensityCalc,
						Math.min(
								_tree.size,
								(_tree.size / _numPointsForDensityCalcDivisor) + 1));

		// TODO adaptive learning

		double GFZeroBearing = wave.getGFZeroAngle();

		double angleOfPositiveGFs = Utils.normalRelativeAngle(wave
				.getGFOneAngle() - GFZeroBearing);
		double angleOfNegativeGFs = Utils.normalRelativeAngle(wave
				.getGFNegOneAngle() - GFZeroBearing);

		double[] currentDataPoint = _weightingScheme.getPointCoordinates(wave);

		HeapEntry<PointEntry<Double>>[] heapOfPoints = _tree.getNNearestPoints(
				currentDataPoint, _variableNumOfPointsForDensityCalc);
		double[][] fixedAngles = new double[_numOfFixedAnglesForDensityCalc][2];

		int i;
		for (i = _numOfFixedAnglesForDensityCalc / 2; i > 0; i--) {
			fixedAngles[i][0] = GFZeroBearing + angleOfPositiveGFs * i
					* ONE_OVER_HALF_NUM_FIXED_ANGLES;
			fixedAngles[i][1] = 0;
			fixedAngles[i + halfNumOfAngles][0] = GFZeroBearing
					+ angleOfNegativeGFs * i * ONE_OVER_HALF_NUM_FIXED_ANGLES;
			fixedAngles[i + halfNumOfAngles][1] = 0;
		}
		fixedAngles[i][0] = GFZeroBearing;
		fixedAngles[i][1] = 0;

		double[][] anglesOfNearestPoints = new double[_variableNumOfPointsForDensityCalc][2];
		for (i = 0; i < _variableNumOfPointsForDensityCalc; i++) {
			double GF = 0.0;
			try {
				GF = heapOfPoints[i].entryData.dataObject;
			} catch (NullPointerException exception) {
				System.out.println("Null Pointer Exception at: "
						+ exception.getStackTrace());
				for (double coordinate : currentDataPoint) {
					System.out.print(coordinate + ", ");
				}
			}
			// if (GF > 1 || GF < -1)
			// robot.out.println(GF);

			if (GF > 0)
				anglesOfNearestPoints[i][0] = GFZeroBearing + GF
						* angleOfPositiveGFs;
			else
				anglesOfNearestPoints[i][0] = GFZeroBearing - GF
						* angleOfNegativeGFs;

			anglesOfNearestPoints[i][1] = 1.0 / (Math
					.sqrt(heapOfPoints[i].dist) + _baseDenominatorValue);

		}
		return anglesOfNearestPoints;
	}

	private void paintVector(Graphics2D g, Point2D.Double origin,
			double length, double bearing, float red, float green, float blue,
			float brightness) {
		Point2D.Double endPoint = RoboGeom.project(origin, length, bearing);
		Point2D.Double arrowPoint1 = RoboGeom.project(endPoint, length / 20,
				bearing + 3.44159);
		Point2D.Double arrowPoint2 = RoboGeom.project(endPoint, length / 20,
				bearing + 2.84159);
		;
		Color color = new Color(red, green, blue, brightness);
		g.setColor(color);
		g.drawLine((int) origin.x, (int) origin.y, (int) endPoint.x,
				(int) endPoint.y);
		g.drawLine((int) endPoint.x, (int) endPoint.y, (int) arrowPoint1.x,
				(int) arrowPoint1.y);
		g.drawLine((int) endPoint.x, (int) endPoint.y, (int) arrowPoint2.x,
				(int) arrowPoint2.y);
	}

	private void paintBotWidthLine(Graphics2D g, Point2D.Double origin,
			double length, double bearing, double angle, double distance) {
		Point2D.Double midPoint = RoboGeom
				.project(origin, length - 20, bearing);
		double lengthOfBotWidthLine = (length - 20) * Math.tan(angle);
		Point2D.Double point1 = RoboGeom.project(midPoint,
				lengthOfBotWidthLine, bearing + 1.57079632675);
		Point2D.Double point2 = RoboGeom.project(midPoint,
				lengthOfBotWidthLine, bearing - 1.57079632675);
		g.drawLine((int) point1.x, (int) point1.y, (int) point2.x,
				(int) point2.y);
	}

	public void onPaint(Graphics2D graphicsObject, long time) {
		if (lastAimTime == time) {
			// TODO tweak.
			int _variableNumOfPointsForDensityCalc = Math
					.min(_maxNumOfPointsForDensityCalc,
							Math.min(
									_tree.size,
									(_tree.size / _numPointsForDensityCalcDivisor) + 1));

			// TODO adaptive learning

			double GFZeroBearing = LatestWave.getGFZeroAngle();

			double angleOfPositiveGFs = Utils.normalRelativeAngle(LatestWave
					.getGFOneAngle() - GFZeroBearing);
			double angleOfNegativeGFs = Utils.normalRelativeAngle(LatestWave
					.getGFNegOneAngle() - GFZeroBearing);

			double[] currentDataPoint = _weightingScheme
					.getPointCoordinates(LatestWave);
			if (currentDataPoint == null) {
				System.out.println("currentDataPoint is null");
			}

			HeapEntry<PointEntry<Double>>[] heapOfPoints = _tree.getNNearestPoints(
					currentDataPoint, _variableNumOfPointsForDensityCalc);
			double[][] fixedAngles = new double[_numOfFixedAnglesForDensityCalc][2];

			int i;
			for (i = _numOfFixedAnglesForDensityCalc / 2; i > 0; i--) {
				fixedAngles[i][0] = GFZeroBearing + angleOfPositiveGFs * i
						* ONE_OVER_HALF_NUM_FIXED_ANGLES;
				fixedAngles[i][1] = 0;
				fixedAngles[i + halfNumOfAngles][0] = GFZeroBearing
						+ angleOfNegativeGFs * i
						* ONE_OVER_HALF_NUM_FIXED_ANGLES;
				fixedAngles[i + halfNumOfAngles][1] = 0;
			}
			fixedAngles[i][0] = GFZeroBearing;
			fixedAngles[i][1] = 0;

			double[][] anglesOfNearestPoints = new double[_variableNumOfPointsForDensityCalc][2];
			double bestWeight = 0;
			for (i = 0; i < _variableNumOfPointsForDensityCalc; i++) {
				double GF = 0.0;
				try {
					GF = heapOfPoints[i].entryData.dataObject;
				} catch (NullPointerException exception) {
					System.out.println("Null Pointer Exception at: "
							+ exception.getStackTrace());
					for (double coordinate : currentDataPoint) {
						System.out.print(coordinate + ", ");
					}
				}
				// if (GF > 1 || GF < -1)
				// robot.out.println(GF);

				if (GF > 0)
					anglesOfNearestPoints[i][0] = GFZeroBearing + GF
							* angleOfPositiveGFs;
				else
					anglesOfNearestPoints[i][0] = GFZeroBearing - GF
							* angleOfNegativeGFs;
				// TODO tweak
				// how important is point distance?
				// 18.44 is the square root of 340
				// max square distance ~ 305
				// double baseValue = 19.0 - Math
				// .sqrt(_variableNumOfPointsForDensityCalc);
				// anglesOfNearestPoints[i][1] = baseValue
				// / (Math.sqrt(Bucket.squareDistBetweenPoints(currentDataPoint,
				// nearestPoints[i])) + baseValue);
				anglesOfNearestPoints[i][1] = 1.0 / (Math.sqrt(heapOfPoints[i].dist) + _baseDenominatorValue);
				if (anglesOfNearestPoints[i][1] > bestWeight) {
					bestWeight = anglesOfNearestPoints[i][1];
				}

			}

			double bestAngleDensity = 0;
			int indexBestAngle = 0;
			double botWidthAngleBestFiringAngle = 0;
			double botWidthDistanceBestFiringAngle = 0;

			for (i = 0; i < _numOfFixedAnglesForDensityCalc; i++) {
				double distForBotWidthAngle = 0;
				// if (anglesOfNearestPoints[i][1] > 0)
				distForBotWidthAngle = LatestWave.getSourceToTargetDistance();
				// + anglesOfNearestPoints[i][1]
				// * (Point2D.distance(robotCoordinates.x,
				// robotCoordinates.y, enemyGFOnePoint.x,
				// enemyGFOnePoint.y) - preciseDistanceToEnemy);
				// else
				// distForBotWidthAngle = preciseDistanceToEnemy
				// - anglesOfNearestPoints[i][1]
				// * (Point2D.distance(robotCoordinates.x,
				// robotCoordinates.y, enemyGFNegOnePoint.x,
				// enemyGFNegOnePoint.y) - preciseDistanceToEnemy);

				// use 3/5 instead of 1/2 because the robot will probably be
				// moving:
				double botWidthAngle = RoboGeom.preciseBotWidthAngle(
						LatestWave.getSourcePosition(), distForBotWidthAngle,
						fixedAngles[i][0]) * _botWidthAngleMultiplier;

				for (double angle[] : anglesOfNearestPoints) {

					double ux = Utils.normalRelativeAngle(fixedAngles[i][0]
							- angle[0])
							/ botWidthAngle;
					if (Math.abs(ux) < 1)
						fixedAngles[i][1] += ((1) - ux * ux * (2 - ux * ux))
								* (angle[1]);
				}
				if (fixedAngles[i][1] > bestAngleDensity) {
					bestAngleDensity = fixedAngles[i][1];
					indexBestAngle = i;
					botWidthAngleBestFiringAngle = botWidthAngle;
					botWidthDistanceBestFiringAngle = distForBotWidthAngle;
				}
			}

			double lengthOfRaysToPaint = LatestWave.getSourceToTargetDistance() * 0.5;
			paintVector(graphicsObject, LatestWave.getSourcePosition(),
					lengthOfRaysToPaint * 1.2, fixedAngles[indexBestAngle][0],
					1.0f, 1.0f, 1.0f, 1.0f);
			paintBotWidthLine(graphicsObject, LatestWave.getSourcePosition(),
					lengthOfRaysToPaint * 1.2, fixedAngles[indexBestAngle][0],
					botWidthAngleBestFiringAngle,
					botWidthDistanceBestFiringAngle);

			for (i = 0; i < _numOfFixedAnglesForDensityCalc; i++) {
//				paintVector(
//						graphicsObject,
//						LatestWave.getSourcePosition(),
//						0.2
//								* (lengthOfRaysToPaint * (fixedAngles[i][1] / bestAngleDensity))
//								+ lengthOfRaysToPaint * 0.8, fixedAngles[i][0],
//						0.9f, 0.9f, 0.9f, 0.8f);
				
				paintVector(
						graphicsObject,
						LatestWave.getSourcePosition(),lengthOfRaysToPaint, fixedAngles[i][0],
						0.9f, 0.9f, 0.9f, 0.2f + 0.79f * (float) (fixedAngles[i][1] / bestAngleDensity));
			}
			for (i = 0; i < anglesOfNearestPoints.length; i++) {
				paintVector(
						graphicsObject,
						LatestWave.getSourcePosition(),
						0.6
								* (lengthOfRaysToPaint
										* anglesOfNearestPoints[i][1] / bestWeight)
								+ lengthOfRaysToPaint * 0.1,
						anglesOfNearestPoints[i][0], 0.1f, 0.1f, 0.9f, 0.8f);
			}
		}
	}
}
