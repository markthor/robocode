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

package aw.Mallorn.tree;

import java.util.ArrayList;
import java.util.LinkedList;

public class KDTree<O> {
	KDTreeBucket<O> trunk;
	final static int defaultMaxBucketCapacity = 24;
	int dimensions;
	LinkedList<PointEntry<O>> listOfPointsInOrderAdded;
	int maxSize;
	public int size = 0;

	public KDTree(int dimensions) {
		trunk = new KDTreeBucket<O>(defaultMaxBucketCapacity, dimensions);
		this.dimensions = dimensions;
		listOfPointsInOrderAdded = null;
	}

	public KDTree(int maxBucketCapacity, int dimensions) {
		trunk = new KDTreeBucket<O>(maxBucketCapacity, dimensions);
		this.dimensions = dimensions;
		listOfPointsInOrderAdded = null;
	}

	public KDTree(int maxBucketCapacity, int dimensions, int maxSize) {
		trunk = new KDTreeBucket<O>(maxBucketCapacity, dimensions);
		this.dimensions = dimensions;
		listOfPointsInOrderAdded = new LinkedList<PointEntry<O>>();
		this.maxSize = maxSize;
	}

	/**
	 * @param point
	 *            The point to add.
	 * @return In a size limited tree, this returns the removed point.
	 */
	public PointEntry<O> addPoint(double[] point, O object) {
		PointEntry<O> entry = new PointEntry<O>(point, object);
		PointEntry<O> returnPoint = null;
		if (listOfPointsInOrderAdded != null) {
			listOfPointsInOrderAdded.addLast(entry);
			if (listOfPointsInOrderAdded.size() > maxSize) {
				returnPoint = listOfPointsInOrderAdded.removeFirst();
				removePoint(returnPoint);
			}
		}

		KDTreeBucket<O> currentBucket = trunk;

		while (!currentBucket.isTerminal) {
			extendBounds(entry, currentBucket);
			if (point[currentBucket.dividingDimension] < currentBucket.divider)
				currentBucket = currentBucket.LeftBucket;
			else
				currentBucket = currentBucket.RightBucket;
		}
		extendBounds(entry, currentBucket);
		currentBucket.points[currentBucket.currentLastPoint++] = entry;
		if (currentBucket.currentLastPoint == currentBucket.maxBucketSize) {
			setDivider(currentBucket);
			// setting Left and Right buckets, isTerminal and calling
			// redistribute points will be done in setDivider() so that they are
			// not done if no divider is set. This happens when the points
			// in the bucket are all identical.
			// redistribute points also sets the points list to null.
		}
		size++;
		return returnPoint;
	}

	private void removePoint(PointEntry<O> point) {
		KDTreeBucket<O> currentBucket = trunk;

		while (!currentBucket.isTerminal) {
			if (point.pointCoordinates[currentBucket.dividingDimension] < currentBucket.divider)
				currentBucket = currentBucket.LeftBucket;
			else
				currentBucket = currentBucket.RightBucket;
		}
		int indexOfPoint = -1;
		for (int i = 0; i < currentBucket.points.length; i++) {
			if (currentBucket.points[i] == (point)) {
				indexOfPoint = i;
				break;
			}
		}
		if (currentBucket.currentLastPoint > 1) {
			currentBucket.points[indexOfPoint] = currentBucket.points[--currentBucket.currentLastPoint];

			ArrayList<Integer> effectsDimensionMBR = new ArrayList<Integer>();
			double[][] newMBR = new double[dimensions][2];
			for (int i = 0; i < dimensions; i++) {
				double max = currentBucket.points[0].pointCoordinates[i];
				double min = currentBucket.points[0].pointCoordinates[i];

				for (int j = 1; j < currentBucket.currentLastPoint; j++) {
					if (currentBucket.points[j].pointCoordinates[i] > max) {
						max = currentBucket.points[j].pointCoordinates[i];
					} else if (currentBucket.points[j].pointCoordinates[i] < min) {
						min = currentBucket.points[j].pointCoordinates[i];
					}
				}
				newMBR[i][0] = min;
				newMBR[i][1] = max;
				if (newMBR[i][0] != currentBucket.boundingBoxOfPoints[i][0]
						|| newMBR[i][1] != currentBucket.boundingBoxOfPoints[i][1])
					effectsDimensionMBR.add(i);
			}

			currentBucket = currentBucket.Parent;
			while (currentBucket != null && effectsDimensionMBR.size() > 0) {
				ArrayList<Integer> newEffectsDimensionMBR = new ArrayList<Integer>();
				for (Integer dimension : effectsDimensionMBR) {
					double min = newMBR[dimension][0];
					double max = newMBR[dimension][1];
					if (currentBucket.RightBucket.boundingBoxOfPoints[dimension][0] < min) {
						min = currentBucket.RightBucket.boundingBoxOfPoints[dimension][0];
						newEffectsDimensionMBR.add(dimension);
					} else if (currentBucket.RightBucket.boundingBoxOfPoints[dimension][1] > max) {
						max = currentBucket.RightBucket.boundingBoxOfPoints[dimension][1];
						newEffectsDimensionMBR.add(dimension);
					}
					if (currentBucket.LeftBucket.boundingBoxOfPoints[dimension][0] < min) {
						min = currentBucket.LeftBucket.boundingBoxOfPoints[dimension][0];
						newEffectsDimensionMBR.add(dimension);
					} else if (currentBucket.LeftBucket.boundingBoxOfPoints[dimension][1] > max) {
						max = currentBucket.LeftBucket.boundingBoxOfPoints[dimension][1];
						newEffectsDimensionMBR.add(dimension);
					}
					currentBucket.boundingBoxOfPoints[dimension][0] = min;
					currentBucket.boundingBoxOfPoints[dimension][1] = max;
				}
				effectsDimensionMBR = newEffectsDimensionMBR;
				currentBucket = currentBucket.Parent;
			}

		} else {
			while (currentBucket.Parent != null) {
				if (currentBucket.Parent.RightBucket == currentBucket) {
					if (currentBucket.Parent.LeftBucket.isTerminal
							&& currentBucket.Parent.LeftBucket.currentLastPoint == 0) {
						currentBucket = currentBucket.Parent;
					} else {
						currentBucket = new KDTreeBucket<O>(currentBucket.Parent);
						currentBucket.Parent.RightBucket = currentBucket;
						currentBucket.Parent.boundingBoxOfPoints = currentBucket.Parent.LeftBucket.boundingBoxOfPoints;
						break;
					}
				} else {
					if (currentBucket.Parent.RightBucket.isTerminal
							&& currentBucket.Parent.RightBucket.currentLastPoint == 0) {
						currentBucket = currentBucket.Parent;
					} else {
						currentBucket = new KDTreeBucket<O>(currentBucket.Parent);
						currentBucket.Parent.LeftBucket = currentBucket;
						currentBucket.Parent.boundingBoxOfPoints = currentBucket.Parent.RightBucket.boundingBoxOfPoints;
						break;
					}
				}
			}
			if (currentBucket.Parent == null) {
				currentBucket = new KDTreeBucket<O>(currentBucket.maxBucketSize, currentBucket.dimensions);
			}
		}
		size--;
	}

	// TODO multiple implementations (in C++ version?)
	private void setDivider(KDTreeBucket<O> bucket) {
		double widestVariance = 0;
		double max, min;
		max = 0;
		min = 0;
		double bestDimensionMin = 0;
		int dimensionWithWidestVariance = 0;

		for (int j = 0; j < dimensions; j++) {
			double[] currentPoint = bucket.points[0].pointCoordinates;
			max = currentPoint[j];
			min = currentPoint[j];
			for (int i = 1; i < bucket.maxBucketSize; i++) {
				currentPoint = bucket.points[i].pointCoordinates;
				if (currentPoint[j] < min)
					min = currentPoint[j];
				else if (currentPoint[j] > max)
					max = currentPoint[j];
			}
			if (max - min > widestVariance) {
				widestVariance = max - min;
				dimensionWithWidestVariance = j;
				bestDimensionMin = min;
			}
		}

		// TODO Use median of widest dimension instead?
		// if there is no variance double the bucket size
		// this will and should carry over to child nodes since they will
		// have these points in them.
		if (widestVariance == 0) {
			bucket.maxBucketSize *= 2;
			PointEntry<O>[] tempPoints = bucket.points;
			bucket.points = new PointEntry[bucket.maxBucketSize];
			System.arraycopy(tempPoints, 0, bucket.points, 0, tempPoints.length);
			tempPoints = null;
		} else {
			bucket.LeftBucket = new KDTreeBucket<O>(bucket);
			bucket.RightBucket = new KDTreeBucket<O>(bucket);
			bucket.dividingDimension = dimensionWithWidestVariance;
			bucket.divider = bestDimensionMin + (widestVariance) * 0.5;
			redistributePoints(bucket);
			bucket.points = null;
			bucket.isTerminal = false;
		}
	}

	private void redistributePoints(KDTreeBucket<O> bucket) {
		for (int i = 0; i < bucket.maxBucketSize; i++) {
			PointEntry<O> currentPoint = bucket.points[i];
			if (currentPoint.pointCoordinates[bucket.dividingDimension] < bucket.divider) {
				bucket.LeftBucket.points[bucket.LeftBucket.currentLastPoint++] = (currentPoint);
				extendBounds(currentPoint, bucket.LeftBucket);
			} else {
				bucket.RightBucket.points[bucket.RightBucket.currentLastPoint++] = (currentPoint);
				extendBounds(currentPoint, bucket.RightBucket);
			}
		}
	}

	private void extendBounds(PointEntry<O> point, KDTreeBucket<O> bucket) {
		for (int i = 0; i < dimensions; i++) {
			if (point.pointCoordinates[i] < bucket.boundingBoxOfPoints[i][0])
				bucket.boundingBoxOfPoints[i][0] = point.pointCoordinates[i];
			if (point.pointCoordinates[i] > bucket.boundingBoxOfPoints[i][1])
				bucket.boundingBoxOfPoints[i][1] = point.pointCoordinates[i];
		}
	}

	/**
	 * 
	 * @param point
	 *            Query point for N Nearest Neighbors search.
	 * @param n
	 *            Number of neighbors to return. This should never be 0.
	 * @return The N nearest neighbors of the point.
	 */
	public HeapEntry<PointEntry<O>>[] getNNearestPoints(double[] point, int n) {
		// System.out.println("get points started");

		MaxHeap<PointEntry<O>> heapOfPoints = new MaxHeap<PointEntry<O>>(n);
		KDTreeBucket<O> currentBucket = trunk;
		double distToWorstNearPoint = Double.POSITIVE_INFINITY; // just using a
																// large
//		HeapEntry<PointEntry<O>>[] results = new HeapEntry[n];
//		int currentNearestPointsIndex = 0;

		// number

		// TODO optimize more this version may be worse for robocode

		// use square of actual distances:
		do {
			if (currentBucket.isTerminal) {
				for (int i = 0; i < currentBucket.currentLastPoint; i++) {
					double currentDist = squareDistBetweenPoints(
							currentBucket.points[i].pointCoordinates, point);
					if (currentDist <= distToWorstNearPoint) {
						HeapEntry<PointEntry<O>> entry = new HeapEntry<PointEntry<O>>(currentDist,
								currentBucket.points[i]);
						heapOfPoints.add(entry);

						
						 // TODO there should be a better way to do this.
						 if (heapOfPoints.nextEntryIndex == heapOfPoints.size)
						 distToWorstNearPoint = heapOfPoints.heap[0].dist;

//						results[currentNearestPointsIndex] = entry;
//						for (int j = currentNearestPointsIndex - 1; j > -1
//								&& currentDist <= results[j].dist; j--) {
//							HeapEntry<PointEntry<O>> temp;
//							temp = results[j];
//							results[j] = results[j + 1];
//							results[j + 1] = temp;
//						}
//						if (currentNearestPointsIndex < n - 1) {
//							currentNearestPointsIndex++;
//						} else {
//							distToWorstNearPoint = results[currentNearestPointsIndex].dist;
//						}

					}
				}
				// if we can guarantee that this is not null, we can save time:
				currentBucket = currentBucket.Parent;
			}

			else if (currentBucket.nodesVisited == NodesVisited.None) {
				// TODO use MBR
				if (point[currentBucket.dividingDimension] < currentBucket.divider) {
					currentBucket.nodesVisited = NodesVisited.Left;
					if (distToWorstNearPoint >= squareDistFromPointToHyperRactangle(
							point, currentBucket.LeftBucket))
						currentBucket = currentBucket.LeftBucket;
				} else {
					currentBucket.nodesVisited = NodesVisited.Right;
					if (distToWorstNearPoint >= squareDistFromPointToHyperRactangle(
							point, currentBucket.RightBucket))
						currentBucket = currentBucket.RightBucket;
				}
			} else if (currentBucket.nodesVisited == NodesVisited.Right
					&& distToWorstNearPoint >= squareDistFromPointToHyperRactangle(
							point, currentBucket.LeftBucket)) {
				currentBucket.nodesVisited = NodesVisited.All;
				currentBucket = currentBucket.LeftBucket;
			} else if (currentBucket.nodesVisited == NodesVisited.Left
					&& distToWorstNearPoint >= squareDistFromPointToHyperRactangle(
							point, currentBucket.RightBucket)) {
				currentBucket.nodesVisited = NodesVisited.All;
				currentBucket = currentBucket.RightBucket;
			} else {
				currentBucket.nodesVisited = NodesVisited.None;
				currentBucket = currentBucket.Parent;
			}

		} while (currentBucket != null);

		return heapOfPoints.heap;
//		return results;
	}

	public static double squareDistBetweenPoints(double[] pointOne,
			double[] pointTwo) {
		double squareDist = 0.0;
		for (int i = 0; i < pointOne.length; i++) {
			double factor = pointOne[i] - pointTwo[i];
			// if the points will lie at exactly the same spot
			// on some axes frequently enough, this could be:

			// if(factor != 0)
			squareDist += factor * factor;
		}
		return squareDist;
	}

	private double squareDistFromPointToHyperRactangle(double[] point,
			KDTreeBucket<O> bucket) {
		double factor;
		double squareDist = 0.0;
		for (int i = 0; i < dimensions; i++) {
			if (point[i] < bucket.boundingBoxOfPoints[i][0]) {
				factor = bucket.boundingBoxOfPoints[i][0] - point[i];
				squareDist += factor * factor;
			} else if (point[i] > bucket.boundingBoxOfPoints[i][1]) {
				factor = point[i] - bucket.boundingBoxOfPoints[i][1];
				squareDist += factor * factor;
			}
		}
		return squareDist;
	}

	/**
	 * 
	 * @param point
	 *            The point to perform the N Nearest Neighbors search for.
	 * @param n
	 *            The number of points to return, this should never be 0;
	 * @param weights
	 *            The weights for each dimension.
	 * @return The N Nearest Neighbors for the query point.
	 */
	public double[][] getNNearestPoints(double[] point, int n, double[] weights) {
		// System.out.println("get points started");

		// TODO use a heap

		double[][] tempNearestPoints = new double[n][];
		// double[][] nearestPoints = new double[n][]; // this will be a binary
		// max heap
		double[] nearestPointsDistances = new double[n];
		for (int i = 0; i < n; i++) {
			nearestPointsDistances[i] = Double.POSITIVE_INFINITY;
		}
		int currentNearestPointsIndex = 0;
		KDTreeBucket<O> currentBucket = trunk;
		double distToWorstNearPoint = Double.POSITIVE_INFINITY;

		// TODO optimize more this version may be worse for robocode

		// use square of actual distances:
		do {
			if (currentBucket.isTerminal) {
				for (int i = 0; i < currentBucket.currentLastPoint; i++) {
					double currentDist = squareDistBetweenPoints(
							currentBucket.points[i].pointCoordinates, point,
							weights);
					if (currentDist <= distToWorstNearPoint) {

						// // if the heap is full, remove the furthestPoint and
						// // set heap
						// if(currentNearestPointIndex == n) {
						// nearestPoints[0] = (nearestPointsDistances[1] -
						// nearestPointsDistances[2] > 0) ? nearestPoints[1] :
						// nearestPoints[2];
						// }
						// // insert new point
						// nearestPoints[currentNearestPointIndex++] =
						// currentBucket.points[i];
						// // check that new heap is valid
						//
						// // set distToWorstNearPoint

						tempNearestPoints[currentNearestPointsIndex] = currentBucket.points[i].pointCoordinates;
						nearestPointsDistances[currentNearestPointsIndex] = currentDist;
						for (int j = currentNearestPointsIndex - 1; j > -1
								&& currentDist <= nearestPointsDistances[j]; j--) {
							double[] temp;
							double tempDist;
							temp = tempNearestPoints[j];
							tempDist = nearestPointsDistances[j];
							tempNearestPoints[j] = tempNearestPoints[j + 1];
							nearestPointsDistances[j] = nearestPointsDistances[j + 1];
							tempNearestPoints[j + 1] = temp;
							nearestPointsDistances[j + 1] = tempDist;
						}
						if (currentNearestPointsIndex < n - 1)
							currentNearestPointsIndex++;
						distToWorstNearPoint = nearestPointsDistances[currentNearestPointsIndex];

					}
				}
				// if we can guarantee that this is not null, we can save time:
				currentBucket = currentBucket.Parent;
			}

			else if (currentBucket.nodesVisited == NodesVisited.None) {
				if (point[currentBucket.dividingDimension] < currentBucket.divider) {
					currentBucket.nodesVisited = NodesVisited.Left;
					if (distToWorstNearPoint >= squareDistFromPointToHyperRactangle(
							point, currentBucket.LeftBucket, weights))
						currentBucket = currentBucket.LeftBucket;
				} else {
					currentBucket.nodesVisited = NodesVisited.Right;
					if (distToWorstNearPoint >= squareDistFromPointToHyperRactangle(
							point, currentBucket.RightBucket, weights))
						currentBucket = currentBucket.RightBucket;
				}
			} else if (currentBucket.nodesVisited == NodesVisited.Right
					&& distToWorstNearPoint >= squareDistFromPointToHyperRactangle(
							point, currentBucket.LeftBucket, weights)) {
				currentBucket.nodesVisited = NodesVisited.All;
				currentBucket = currentBucket.LeftBucket;
			} else if (currentBucket.nodesVisited == NodesVisited.Left
					&& distToWorstNearPoint >= squareDistFromPointToHyperRactangle(
							point, currentBucket.RightBucket, weights)) {
				currentBucket.nodesVisited = NodesVisited.All;
				currentBucket = currentBucket.RightBucket;
			} else {
				currentBucket.nodesVisited = NodesVisited.None;
				currentBucket = currentBucket.Parent;
			}

		} while (currentBucket != null);

		return tempNearestPoints;
	}

	public static double squareDistBetweenPoints(double[] pointOne,
			double[] pointTwo, double[] weights) {
		double squareDist = 0.0;
		for (int i = 0; i < pointOne.length; i++) {
			double factor = (pointOne[i] - pointTwo[i]) * weights[i];
			// if the points will lie at exactly the same spot
			// on some axes frequently enough, this could be:

			// if(factor != 0)
			squareDist += factor * factor;
		}
		return squareDist;
	}

	private double squareDistFromPointToHyperRactangle(
			double[] pointCoordinates, KDTreeBucket<O> bucket, double[] weights) {
		double factor;
		double squareDist = 0.0;
		for (int i = 0; i < dimensions; i++) {
			if (pointCoordinates[i] < bucket.boundingBoxOfPoints[i][0]) {
				factor = (bucket.boundingBoxOfPoints[i][0] - pointCoordinates[i])
						* weights[i];
				squareDist += factor * factor;
			} else if (pointCoordinates[i] > bucket.boundingBoxOfPoints[i][1]) {
				factor = (pointCoordinates[i] - bucket.boundingBoxOfPoints[i][1])
						* weights[i];
				squareDist += factor * factor;
			}
		}
		return squareDist;
	}

}
