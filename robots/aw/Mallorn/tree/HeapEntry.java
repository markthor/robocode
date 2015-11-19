package aw.Mallorn.tree;

public class HeapEntry<O> {
		public final double dist;
		public final O entryData;
		
		public HeapEntry(double dist, O entryData) {
			this.dist = dist;
			this.entryData = entryData;
		}
}
