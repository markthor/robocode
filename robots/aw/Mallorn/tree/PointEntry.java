package aw.Mallorn.tree;

public class PointEntry<O> {
	public final double[] pointCoordinates;
	public final O dataObject;
	
	public PointEntry(double[] coordinates, O object) {
		this.pointCoordinates = coordinates;
		this.dataObject = object;
	}
}
