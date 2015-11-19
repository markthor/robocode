package xander.gfws.data.red;

import xander.gfws.data.WaveSearchIterator;
import ags.utils.dataStructures.MaxHeap;

public class RedKDTreeWaveSearchIterator<T> extends WaveSearchIterator<T> {

	private MaxHeap<T> maxHeap;
	private int maxIdx;
	private int idx;
	
	public RedKDTreeWaveSearchIterator(MaxHeap<T> maxHeap) {
		this.maxHeap = maxHeap;
		this.maxIdx = maxHeap.size()-1;
	}
	
	@Override
	public boolean hasNext() {
		return idx <= maxIdx;
	}

	@Override
	public T next() {
		return (idx > maxIdx)? null : maxHeap.get(idx++);
	}

	@Override
	public double getSearchDistance() {
		return maxHeap.getKey(idx-1);
	}

}
