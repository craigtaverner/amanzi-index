package org.neo4j.graphdb.index;

import org.neo4j.graphdb.PropertyContainer;

public interface Index<T extends PropertyContainer> {
	/** Adds a key/value pair for entity to the index. */
	public void add(T entity, String key, Object value);

	/** Clears the index and deletes the configuration associated with it. */
	public void delete();

	/** Returns exact matches from this index, given the key/value pair. */
	public IndexHits<T> get(String key, Object value);

	public Class<T> getEntityType();

	public String getName();

	/**
	 * Returns matches from this index based on the supplied query object, which
	 * can be a query string or an implementation-specific query object.
	 */
	public IndexHits<T> query(Object queryOrQueryObject);

	/**
	 * Returns matches from this index based on the supplied key and query
	 * object, which can be a query string or an implementation-specific query
	 * object.
	 */
	public IndexHits<T> query(String key, Object queryOrQueryObject);

	/** Removes a key/value pair for entity from the index.} */
	public void remove(T entity, String key, Object value);
}