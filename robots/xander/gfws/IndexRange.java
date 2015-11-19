package xander.gfws;

public class IndexRange {

	private double preciseMinIndex;
	private double preciseMaxIndex;
	
	public IndexRange(double preciseMinIndex, double preciseMaxIndex) {
		this.preciseMaxIndex = preciseMaxIndex;
		this.preciseMinIndex = preciseMinIndex;
	}
	
	public void setRange(double preciseMinIndex, double preciseMaxIndex) {
		this.preciseMinIndex = preciseMinIndex;
		this.preciseMaxIndex = preciseMaxIndex;
	}
	
	public void shiftRange(double shiftBy) {
		this.preciseMinIndex += shiftBy;
		this.preciseMaxIndex += shiftBy;
	}
	
	public double getPreciseMinIndex() {
		return preciseMinIndex;
	}
	
	public double getPreciseMaxIndex() {
		return preciseMaxIndex;
	}
	
	public double getPreciseCenterIndex() {
		return (preciseMinIndex + preciseMaxIndex) / 2d;
	}
	
	public int getMinIndexRounded() {
		return (int) Math.round(preciseMinIndex);
	}
	
	public int getMaxIndexRounded() {
		return (int) Math.round(preciseMaxIndex);
	}
	
	public int getMinIndexFloor() {
		return (int) Math.round(Math.floor(preciseMinIndex));
	}
	
	public int getMinIndexCeil() {
		return (int) Math.round(Math.ceil(preciseMinIndex));
	}
	
	public int getMaxIndexFloor() {
		return (int) Math.round(Math.floor(preciseMaxIndex));
	}
	
	public int getMaxIndexCeil() {
		return (int) Math.round(Math.ceil(preciseMaxIndex));
	}
}
