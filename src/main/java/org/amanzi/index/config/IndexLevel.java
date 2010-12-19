package org.amanzi.index.config;

import java.io.IOException;
import java.util.Arrays;

import org.amanzi.index.AmanziIndexRelationshipTypes;
import org.amanzi.index.legacy.PropertyIndex.ValueConverter;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * The IndexLevel class holds information about a specific level or depth of the
 * index. Each instance of this class is specific to a particular level, but
 * also contains information about the current index, or most recently indexed
 * node for that level. This is a small optimization for cases where the data is
 * loaded in some kind of sorted order.
 * 
 * @author craig
 * @since 1.0.0
 */
public class IndexLevel {
	private int level = 0;
	private int[] indices; // current keys at this level
	private int[] values; // currently evaluated low level keys (level 0)
	private Node indexNode = null; // current node at this level
	private IndexConfig config;

	/**
	 * This constructor is used to build index levels dynamically as the data is
	 * being loaded into the database. This is the first call, when no origin
	 * and no lower Node is known.
	 * 
	 * @param level
	 * @param IndexConfig
	 */
	public IndexLevel(int level, IndexConfig config, GraphDatabaseService neo) throws IOException {
		if (level != 0)
			throw new IOException("Incorrect level " + level + ", must be zero");
		this.level = level;
		this.config = config;
		this.indices = new int[config.size()];
		this.values = new int[config.size()];
		makeIndexNode(neo);
	}

	/**
	 * This constructor is used to build index levels dynamically as the data is
	 * being loaded into the database. This relies on previously existing index
	 * nodes at the lower level.
	 * 
	 * @param level
	 * @param IndexConfig
	 * @param values
	 * @param lowerNode
	 * @throws IOException
	 */
	public IndexLevel(int level, IndexConfig config, IndexLevel lowerLevel) throws IOException {
		this.level = level;
		this.config = config;
		this.values = Arrays.copyOf(lowerLevel.getValues(), lowerLevel.getValues().length);
		this.indices = config.keysFor(values, level);
		makeIndexNode(lowerLevel.getIndexNode().getGraphDatabase());
		linkTo(lowerLevel.getIndexNode());
	}

	/**
	 * This constructor is used to build the index levels based on existing
	 * index nodes in the database. This relies on previously existing index
	 * nodes at this level.
	 * 
	 * @param level
	 * @param IndexConfig
	 * @param indexNode
	 * @throws IOException
	 */
	private IndexLevel(int level, IndexConfig config, Node indexNode) throws IOException {
		this.level = level;
		this.config = config;
		this.indices = (int[]) indexNode.getProperty("index");
		this.indexNode = indexNode;
		Integer indexLevel = (Integer) indexNode.getProperty("level", null);
		if (this.getLevel() != indexLevel) {
			throw new IllegalArgumentException("Invalid index node passed for level: " + this.getLevel() + " != " + indexLevel);
		}
	}

	public IndexLevel setKeys(IndexLevel parentLevel, int[] newVals) throws IOException {
		if (!Arrays.equals(newVals, this.values)) {
			this.values = newVals;
			int[] newIndices = config.keysFor(newVals, level);
			if (!Arrays.equals(newIndices, this.indices)) {
				this.indices = newIndices;
				this.indexNode = null;
				// TODO: Support calculating min/max here
			}
		}
		// First search the node tree for existing child index nodes that
		// match
		if (indexNode == null) {
			searchChildrenOf(parentLevel.indexNode);
		}
		// If no child node was found, create one and link it into the tree
		if (this.indexNode == null) {
			this.makeIndexNode(parentLevel.getIndexNode().getGraphDatabase());
			parentLevel.linkTo(this.indexNode);
		}
		return this;
	}

	public Node makeIndexNode(GraphDatabaseService neo) throws IOException {
		if (indexNode == null) {
			indexNode = neo.createNode();
			indexNode.setProperty("index", indices);
			indexNode.setProperty("type", "multi_index");
			indexNode.setProperty("level", getLevel());
			// TODO: Support storing min/max here
		}
		return indexNode;
	}

	public void linkTo(Node lowerNode) {
		if (indexNode != null && lowerNode != null) {
			indexNode.createRelationshipTo(lowerNode, AmanziIndexRelationshipTypes.INDEX_CHILD);
		}
	}

	public void searchChildrenOf(Node parentIndex) {
		for (Relationship rel : parentIndex.getRelationships(AmanziIndexRelationshipTypes.INDEX_CHILD, Direction.OUTGOING)) {
			Node child = rel.getEndNode();
			int[] testIndex = (int[]) child.getProperty("index");
			if (Arrays.equals(testIndex, indices)) {
				indexNode = child;
				break;
			}
		}
	}

	public int getLevel() {
		return level;
	}

	public boolean includes(int[] newVals) {
		// TODO: Optimize by exiting the loop on first element mismatch
		int[] newIndices = config.keysFor(newVals, level);
		return Arrays.equals(newIndices, this.indices);
	}

	public Node getIndexNode() {
		return indexNode;
	}

	public int[] getValues() {
		return values;
	}

}
