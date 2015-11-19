package aw.Mallorn.tree;

public class MaxHeap<O> {
	public HeapEntry<O>[] heap;
	public final int size;
	public int nextEntryIndex = 0;

	public MaxHeap(int size) {
		heap = new HeapEntry[size];
		this.size = size;
	}

	public void add(HeapEntry<O> entry) {
		if (nextEntryIndex < size) {
			upheap(nextEntryIndex++, entry);
		} else if (heap[0].dist > entry.dist) {
			downheap(entry);
		}
	}
	
	public HeapEntry<O> removeTop() {
		if(nextEntryIndex == 0)
			return null;
		else{
			HeapEntry<O> returnEntry = heap[0];
			downheap(heap[--nextEntryIndex]);
			return returnEntry;	
		}
	}

	private void upheap(int index, HeapEntry<O> entry) {
		while (index != 0) {
			int newI = (index - 1) / 2;
			if (heap[newI].dist < entry.dist) {
				heap[index] = heap[newI];
				index = newI;
			} else
				break;
		}
		heap[index] = entry;
	}

	private void downheap(HeapEntry<O> entry) {
		int index = 0;
		while (index < nextEntryIndex) {
			int newILeft = (index + 1) * 2 - 1;
			if(newILeft >= nextEntryIndex)
				break;
			int newIRight = newILeft + 1;
				if(newIRight >= nextEntryIndex) {
					if(entry.dist < heap[newILeft].dist) {
						heap[index] = heap[newILeft];
						heap[newILeft] = entry;
						return;
					}
					break;
				}
			if (heap[newILeft].dist < entry.dist
					&& heap[newIRight].dist < entry.dist)
				break;
			else if (heap[newILeft].dist > heap[newIRight].dist) {
				heap[index] = heap[newILeft];
				index = newILeft;
			} else {
				heap[index] = heap[newIRight];
				index = newIRight;
			}
		}
		heap[index] = entry;
	}
	
	public HeapEntry<O> getMaxDistEntry() {
		return heap[0];
	}
}
