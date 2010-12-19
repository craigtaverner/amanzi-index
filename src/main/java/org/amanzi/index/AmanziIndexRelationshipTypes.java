package org.amanzi.index;

import org.neo4j.graphdb.RelationshipType;

public enum AmanziIndexRelationshipTypes implements RelationshipType {
	AMANZI_INDEX, INDEX_CHILD, INDEX_NEXT, INDEX_CONFIG, INDEX_ROOT, INDEX_LEAF;
}
