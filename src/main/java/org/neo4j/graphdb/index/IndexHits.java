package org.neo4j.graphdb.index;

import java.util.Iterator;

public interface IndexHits<T> extends Iterator<T>, Iterable<T> {
	/** Closes the underlying search result. */
	public void close();

	/**
	 * Returns the score of the most recently fetched item from this iterator
	 * (from Iterator.next()).
	 */
	float currentScore();

	/**
	 * Returns the first and only item from the result iterator, or null there
	 * was none.
	 */
	public T getSingle();

	/** Returns the size of this iterable. */
	public int size();
}
