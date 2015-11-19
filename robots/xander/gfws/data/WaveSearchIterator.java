package xander.gfws.data;

import java.util.Iterator;

public abstract class WaveSearchIterator<T> implements Iterator<T> {

	/**
	 * Returns the search distance for the last element retrieved by a call to next().
	 * 
	 * @return search distance for the last element retrieved by a call to next()
	 */
	public abstract double getSearchDistance();

	@Override
	public void remove() {
		// wave search iterators are read-only
		throw new UnsupportedOperationException();	
	}
}
