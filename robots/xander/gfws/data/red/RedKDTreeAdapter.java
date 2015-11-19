package xander.gfws.data.red;

import xander.gfws.data.DataPoint;
import xander.gfws.data.KDTreeAdapter;
import xander.gfws.data.WaveSearchIterable;
import ags.utils.dataStructures.MaxHeap;
import ags.utils.dataStructures.trees.thirdGenKD.DistanceFunction;
import ags.utils.dataStructures.trees.thirdGenKD.KdTree;
import ags.utils.dataStructures.trees.thirdGenKD.SquareEuclideanDistanceFunction;

public class RedKDTreeAdapter implements KDTreeAdapter<DataPoint> {

	private KdTree<DataPoint> tree;
	private DistanceFunction distanceFunction = new SquareEuclideanDistanceFunction();
	
	public RedKDTreeAdapter(int dimensions) {
		this.tree = new KdTree<DataPoint>(dimensions);
	}
	
	public RedKDTreeAdapter(int dimensions, int bucketSize) {
		this.tree = new KdTree<DataPoint>(dimensions, bucketSize);
	}
	
	@Override
	public int size() {
		return tree.size();
	}

	@Override
	public WaveSearchIterable<DataPoint> searchByKNN(double[] key, int n) {
		MaxHeap<DataPoint> nearestNeighbors = tree.findNearestNeighbors(key, n, distanceFunction);
		return new RedKDTreeWaveSearchIterable<DataPoint>(nearestNeighbors);
	}

	@Override
	public WaveSearchIterable<DataPoint> searchByDistance(double[] key, double distance) {
		throw new UnsupportedOperationException("Rednaxela's tree does not support search by distance.");
	}

	@Override
	public WaveSearchIterable<DataPoint> searchByRange(double[] lowerBounds,
			double[] upperBounds) {
		throw new UnsupportedOperationException("Rednaxela's tree does not support range search.");
	}

	@Override
	public void addPoint(double[] key, DataPoint point) {
		tree.addPoint(key, point);
	}
}
