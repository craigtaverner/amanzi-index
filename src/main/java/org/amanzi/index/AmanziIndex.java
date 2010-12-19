package org.amanzi.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.amanzi.index.config.DefaultIndexConfig;
import org.amanzi.index.config.IndexConfig;
import org.amanzi.index.config.IndexLevel;
import org.amanzi.index.config.PropertyConfig;
import org.amanzi.index.mappers.Mapper;
import static org.amanzi.index.util.IndexUtilities.arrayString;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;
import static org.amanzi.index.util.IndexUtilities.arrayString;

class AmanziIndex implements PreparedIndex<Node> {
	private GraphDatabaseService db;
	private IndexConfig config;
	private Node indexNode;
	private Node indexesNode;
	private String name;
	private ArrayList<IndexLevel> levels = new ArrayList<IndexLevel>();

	public AmanziIndex(String name, GraphDatabaseService db, IndexConfig config) {
		this.name = name;
		this.db = db;
		this.config = config;
		getIndexNode();
		saveConfig();
	}

	public AmanziIndex(String name, GraphDatabaseService db) {
		this.name = name;
		this.db = db;
		getIndexNode();
		loadConfig();
	}

	public void add(Node entity, String key, Object value) {
		throw new UnsupportedOperationException("This method is not valid for Prepared Indexes");
	}

	public void delete() {
		// TODO Auto-generated method stub

	}

	public IndexHits<Node> get(String key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Class<Node> getEntityType() {
		return Node.class;
	}

	public String getName() {
		return name;
	}

	public IndexHits<Node> query(Object queryOrQueryObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public IndexHits<Node> query(String key, Object queryOrQueryObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public void remove(Node entity, String key, Object value) {
		// TODO Auto-generated method stub

	}

	// private boolean sameIndex(Integer[] a, Integer[] b) {
	// if (a.length != b.length)
	// return false;
	// for (int i = 0; i < a.length; i++) {
	// if (a[i] != b[i])
	// return false;
	// }
	// return true;
	// }

	public void add(Node node) {
		int[] keys = new int[config.size()];
		int i = 0;
		for (PropertyConfig<?> property : config.getProperties()) {
			String propName = property.getName();
			if (node.hasProperty(property.getName())) {
				Mapper<?> mapper = property.getMapper();
				int key = mapper.toKey(node.getProperty(property.getName()));
				keys[i] = key;
			} else {
				System.err.println("Did not find property '" + propName + "' in node: " + node);
			}
			i++;
		}
		Node indexNode = null;
		try {
			indexNode = getIndexNode(keys);
		} catch (IOException e) {
			System.err.println("Failed to find appropriate index node for value node[" + node + "] with keys" + arrayString(keys)
					+ ": " + e.getMessage());
			e.printStackTrace(System.err);
		}
		if (indexNode != null) {
			indexNode.createRelationshipTo(node, AmanziIndexRelationshipTypes.INDEX_LEAF);
			// TODO: Update statistics in the index
		} else {
			System.err.println("Failed to find appropriate index node for value node[" + node + "] with keys" + arrayString(keys));
		}
	}

	/**
	 * This is the main indexing method of the class. Here we first search up
	 * the cached index stack until we find an index that covers the required
	 * value. Then we step back down creating sub-index nodes as we go until we
	 * hit the lowest level. With data where nodes coming in are usually near
	 * the previous nodes, the cache stack will usually contain the right nodes
	 * and it will not often be required to search high of the stack. This
	 * should give very good performance for that kind of data.
	 * 
	 * @param value
	 *            of the specific type being indexed
	 * @return the level 0 index node for this value (created and linked into
	 *         the index if required)
	 * @throws IOException
	 */
	private Node getIndexNode(int[] keys) throws IOException {
		// search as high as necessary to find a node that covers this value
		IndexLevel indexLevel = getLevelIncluding(keys);
		// now step down building index all the way to the bottom
		while (indexLevel.getLevel() > 0) {
			IndexLevel lowerLevel = levels.get(indexLevel.getLevel() - 1);
			// Set the value in the lower level to the desired value to index,
			// this removes internal
			// node cash, so we much recreate that by finding or creating a new
			// index node
			lowerLevel.setKeys(indexLevel, keys);
			// Finally step down one level and repeat until we're at the bottom
			indexLevel = lowerLevel;
		}
		return indexLevel.getIndexNode();
	}

	/**
	 * Search up the cached index stack for an index that includes the specified
	 * value. The higher we go, the wider the range covered, so at some point
	 * the specified value will match. If the value is similar to the previous
	 * value, the search is likely to exit at level 0 or 1. The more different,
	 * the higher it needs to go. This mean our dynamic index is fastest for
	 * data that comes in a stream of related values. Note that each level is
	 * created on demand, based on the contents of the lower level index, not
	 * the passed in value. This in effect means the index is forced to be
	 * related to the previous data, ensuring no disconnected graphs in the
	 * index tree.
	 * 
	 * @param value
	 *            to index
	 * @return lowest cached index value matching the data.
	 * @throws IOException
	 */
	private IndexLevel getLevelIncluding(int[] keys) throws IOException {
		int level = 0;
		IndexLevel indexLevel = null;
		do {
			indexLevel = getLevel(level++);
		} while (!indexLevel.includes(keys));
		return indexLevel;
	}

	/**
	 * Return or create the specified index level in the index cache. Each level
	 * created is based on the values in the level below, so that we maintain a
	 * connected graph. When a new level is created, its index Node is also
	 * created in the graph and connected to the index node of the level below
	 * it.
	 * 
	 * @param int level to get, or -1 to get the last level (-2 for one below
	 *        that, etc.)
	 * @throws IOException
	 */
	private IndexLevel getLevel(int level) throws IOException {
		while (levels.size() <= level) {
			// Need to create a higher 'parent' level
			int iLev = levels.size();
			if (iLev == 0) {
				// When creating the very first level, use the origin point, and
				// no child index node
				levels.add(new IndexLevel(iLev, config, db));
			} else {
				// All higher levels are build on top of the lower levels (using
				// the same current
				// value as the level below, and linking the index nodes
				// together into the index
				// tree)
				IndexLevel lowerLevel = levels.get(iLev - 1);
				levels.add(new IndexLevel(iLev, config, lowerLevel));
			}
		}
		return levels.get(level < 0 ? levels.size() + level : level);
	}

	public void find(Node node) {
		// TODO Auto-generated method stub

	}

	/**
	 * This method processes the current set of nodes, adding them to the index,
	 * and then clearing the list. It returns the last index node created.
	 * 
	 * @throws IOException
	 */
	public Node flush() throws IOException {
		// TODO: Base this on actual caches
		ArrayList<Node> nodesToIndex = new ArrayList<Node>();
		for (Node node : nodesToIndex) {
			add(node);
		}
		nodesToIndex.clear();
		return levels.size() > 0 ? levels.get(0).getIndexNode() : null;
	}

	public void finishUp() {
		try {
			flush();
		} catch (IOException e) {
		}
		if (indexNode != null) {
			Node highestIndex = null;
			for (IndexLevel level : levels) {
				if (level.getIndexNode() != null) {
					highestIndex = level.getIndexNode();
				}
			}
			if (highestIndex != null) {
				// Deleting any previous starting relationships
				for (Relationship rel : indexNode.getRelationships(AmanziIndexRelationshipTypes.INDEX_ROOT, Direction.OUTGOING)) {
					rel.delete();
				}
				// Make a new one to the top node (might be same as before or
				// higher level node
				indexNode.createRelationshipTo(highestIndex, AmanziIndexRelationshipTypes.INDEX_ROOT);
			}
		}
	}

	private void loadConfig() {
		Relationship rel = getIndexNode().getSingleRelationship(AmanziIndexRelationshipTypes.INDEX_CONFIG, Direction.OUTGOING);
		if (rel == null) {
			throw new RuntimeException("No existing configuration found for index '" + name + "'");
		} else {
			config = new DefaultIndexConfig(rel.getEndNode());
		}
	}

	private void saveConfig() {
		Relationship rel = indexNode.getSingleRelationship(AmanziIndexRelationshipTypes.INDEX_CONFIG, Direction.OUTGOING);
		if (rel != null) {
			throw new RuntimeException("Not allowed to overwrite existing configuration for index '" + name + "'");
		} else {
			config.save(getIndexNode());
		}
	}

	private Node getIndexNode() {
		if (indexNode == null) {
			Relationship rel = getIndexesNode()
					.getSingleRelationship(AmanziIndexRelationshipTypes.AMANZI_INDEX, Direction.OUTGOING);
			if (rel != null) {
				indexNode = rel.getEndNode();
			} else {
				indexNode = makeNode(getIndexesNode(), AmanziIndexRelationshipTypes.AMANZI_INDEX, "index", null);
			}
		}
		return indexNode;
	}

	private Node makeNode(Node parent, RelationshipType relType, String nodeType, Map<String, Object> properties) {
		Transaction tx = db.beginTx();
		try {
			Node node = db.createNode();
			node.setProperty("type", nodeType);
			if (properties != null && properties.size() > 0) {
				for (String key : properties.keySet()) {
					node.setProperty(key, properties.get(key));
				}
			}
			parent.createRelationshipTo(node, relType);
			tx.success();
			return node;
		} finally {
			tx.finish();
		}
	}

	private Node getIndexesNode() {
		if (indexesNode == null) {
			Relationship rel = db.getReferenceNode().getSingleRelationship(AmanziIndexRelationshipTypes.AMANZI_INDEX,
					Direction.OUTGOING);
			if (rel != null) {
				indexesNode = rel.getEndNode();
			} else {
				indexesNode = makeNode(db.getReferenceNode(), AmanziIndexRelationshipTypes.AMANZI_INDEX, "indexes", null);
			}
		}
		return indexesNode;
	}

	/**
	 * THis method simply dumps a detailed description of the index, allowing
	 * for debugging.
	 */
	public void debug() {
		debug(getIndexNode(), 0);
	}

	private void debug(Node node, int depth) {
		if(depth>7) return;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < depth; i++) {
			sb.append("  ");
		}
		String tab = sb.toString();
		sb.append(node.toString());
		int i = 0;
		for (String key : node.getPropertyKeys()) {
			if (i == 0) {
				sb.append(": ");
			} else {
				sb.append(", ");
			}
			Object property = node.getProperty(key);
			if(key.equals("index")){
				try{
					property = arrayString((int[])property);
				}catch(Exception e){
				}
			}
			sb.append(key).append(": ").append(property);
		}
		System.out.println(sb.toString());
		for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
			System.out.println(tab + "--" + rel.getType() + "-->");
			debug(rel.getEndNode(), depth + 1);
		}
	}
}
