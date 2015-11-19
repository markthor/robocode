package aw.Mallorn.tree;

/*  
 * Copyright 2011, Andrew Wells.  All rights reserved.
 * 
 * This software is provided without any warranty whatsoever.  Without even the
 * implied warranties of merchantabillity or fitness for a particular purpose.
 * All use of this software is at your own risk.
 * 
 * Notes:
 * 		This tree allows weighting by allowing a bounding box that is not a hyper-cube,
 * but it does not allow weighting to be changed dynamically.
 * 
 *  	This tree does not cycle the dividing dimensions, instead, it uses the
 *  dimension in which the points have the widest variance.  This is good for 
 *  data that is not distributed evenly, but poor for data that is.
 *  
 *  	This tree is not intended to be constructed from previously known points.
 * 
 */

public class KDTreeBucket<O> {
	public boolean isTerminal = true;
	public int dimensions;
	public int maxBucketSize;
	int currentLastPoint = 0;
	PointEntry<O>[] points;
	public KDTreeBucket<O> LeftBucket, RightBucket;
	public KDTreeBucket<O> Parent;
	public int dividingDimension;
	public double divider;
	public double boundingBoxOfPoints[][];

	// it could be faster to not have this and check if we are in the correct
	// node instead.
	public NodesVisited nodesVisited = NodesVisited.None;

	public KDTreeBucket(int maxSize, int dimensions) {
		this.dimensions = dimensions;
		this.maxBucketSize = maxSize;
		this.Parent = null;
		points = new PointEntry[maxSize];
		this.boundingBoxOfPoints = new double[dimensions][2];
		for (int i = 0; i < dimensions; i++) {
			boundingBoxOfPoints[i][0] = 100000000000.0;// just use a large
														// number
			boundingBoxOfPoints[i][1] = -1.0;
		}
	}

	public KDTreeBucket(KDTreeBucket<O> Parent) {
		this.Parent = Parent;
		this.dimensions = Parent.dimensions;
		this.maxBucketSize = Parent.maxBucketSize;
		this.points = new PointEntry[Parent.maxBucketSize];
		this.boundingBoxOfPoints = new double[dimensions][2];
		for (int i = 0; i < dimensions; i++) {
			boundingBoxOfPoints[i][0] = 100000000000.0;// just use a large
														// number
			boundingBoxOfPoints[i][1] = -1.0;
		}
	}
}
