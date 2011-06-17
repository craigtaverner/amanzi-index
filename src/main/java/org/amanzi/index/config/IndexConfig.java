package org.amanzi.index.config;

import java.util.Collection;

import org.neo4j.graphdb.Node;

/**
 * Classes implementing this interface are required to provide complete configuration information for a multi-property, multi-type index.
 * This means that each of the properties included in the index needs to be known about in the form of a PropertyConfig instance. This class then collects those
 * individual property configurations into a repeatable order, so that the index of integer keys can be produced.
 * @author craig
 */
public interface IndexConfig {
	
	/** @return a collection of string names for the properties supported */
	public Collection<String> getPropertyNames();

	/** @return a collection of property configurations */
	public Collection<PropertyConfig<?>> getProperties();

	/** Return the configuration of the named property */
	public PropertyConfig<?> getProperty(String property);

	/** Return the position in the index array for the named property */
	int getPropertyPosition(String property);

	/** The number of properties supported by this index */
	public int size();

	/** Convert level 0 key to key at another level */
	int keyFor(int keys, int level);

	/** Convert level 0 keys to keys at any other level */
	public int[] keysFor(int[] keys, int level);
	
	/** Convert keys at some level to level 0 keys */
	public int[] valuesFor(int[] keys, int level);
	
	public void save(Node indexNode);
}
