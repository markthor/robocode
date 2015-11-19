package cs.utils;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Chase
 */
public class MaximumDeque<E> implements Iterable<E> {
	private int maxSize = 7;
	private LinkedList<E> list = new LinkedList<E>();

	public MaximumDeque(int size) {
		maxSize = size;
	}

	public E addFirst(E e) {
		E value = null;
		if(list.size() == maxSize) {
			value = list.removeLast();
		}
		list.addFirst(e);
		return value;
	}

	public E addLast(E e) {
		E value = null;
		if(list.size() == maxSize) {
			value = list.removeFirst();
		}
		list.addLast(e);
		return value;
	}

	public E removeFirst() {
		return list.removeFirst();
	}

	public E removeLast() {
		return list.removeLast();
	}

	public E peekFirst() {
		return list.peekFirst();
	}

	public E peekLast() {
		return list.peekLast();
	}

	public E remove(int index) {
		return list.remove(index);
	}

	public E get(int index) {
		return list.get(index);
	}

	public int size() {
		return list.size();
	}

	/**
	 * if size < oldSize, starts trim from the first element
	 */
	public void setMaxSize(int size) {
		maxSize = size;
		while(list.size() > maxSize) {
			list.removeFirst();
		}
	}
	public Iterator<E> iterator() {
		return list.iterator();
	}
}
