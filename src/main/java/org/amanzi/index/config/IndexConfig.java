package org.amanzi.index.config;

import java.util.Collection;
import java.util.Map;

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

	//public Collection<String> getProperties(Class<?> type);
	public PropertyConfig<?> getProperty(String property);
	
	/** The number of properties supported by this index */
	public int size();

	/** Convert level 0 keys to keys at any other level */
	public int[] keysFor(int[] keys, int level);
	
	public void save(Node indexNode);
}
