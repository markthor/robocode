package xander.gfws.data;

public interface KDTreeAdapter<T> {

	public int size();
	
	public WaveSearchIterable<T> searchByKNN(double[] key, int n);
	
	public WaveSearchIterable<T> searchByDistance(double[] key, double distance);
	
	public WaveSearchIterable<T> searchByRange(double[] lowerBounds, double[] upperBounds);
	
	public void addPoint(double[] key, T point);
}
