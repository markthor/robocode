package xander.gfws.data.red;

import xander.gfws.data.WaveSearchIterable;
import xander.gfws.data.WaveSearchIterator;
import ags.utils.dataStructures.MaxHeap;

public class RedKDTreeWaveSearchIterable<T> implements WaveSearchIterable<T> {

	private MaxHeap<T> maxHeap;
	
	public RedKDTreeWaveSearchIterable(MaxHeap<T> maxHeap) {
		this.maxHeap = maxHeap;
	}
	
	@Override
	public WaveSearchIterator<T> iterator() {
		return new RedKDTreeWaveSearchIterator<T>(maxHeap);
	}

	@Override
	public int size() {
		return maxHeap.size();
	}

	@Override
	public double getMaxDistance() {
		return maxHeap.getMaxKey();
	}

	
}
