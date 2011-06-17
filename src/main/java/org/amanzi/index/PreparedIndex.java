package org.amanzi.index;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;

public interface PreparedIndex<T extends PropertyContainer> extends Index<T> {
	public void add(T node);
}
