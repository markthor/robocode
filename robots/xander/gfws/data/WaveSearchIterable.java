package xander.gfws.data;

public interface WaveSearchIterable<T> {

	public WaveSearchIterator<T> iterator();
	
	public int size();
	
	public double getMaxDistance();
}
