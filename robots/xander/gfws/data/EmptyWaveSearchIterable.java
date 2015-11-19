package xander.gfws.data;

public class EmptyWaveSearchIterable<T> implements WaveSearchIterable<T> {

	public static class EmptyIterator<T> extends WaveSearchIterator<T> {		
		@Override
		public boolean hasNext() {
			return false;
		}
		@Override
		public T next() {
			return null;
		}
		@Override
		public double getSearchDistance() {
			return 0;
		}
	}	

	@Override
	public WaveSearchIterator<T> iterator() {
		return new EmptyIterator<T>();
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public double getMaxDistance() {
		return 0;
	}
}
