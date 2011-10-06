package org.amanzi.index;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.Traverser;

class AmanziIndex implements PreparedIndex<Node> {
	private GraphDatabaseService db;
	private IndexConfig config;
	private Node indexNode;
	private Node indexesNode;
	private String name;
	private ArrayList<IndexLevel> levels = new ArrayList<IndexLevel>();
	public static final int NO_PROPERTY = Integer.MIN_VALUE + 1;
	public static final int ANY_VALUE = Integer.MAX_VALUE - 1;

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
		findIndexNode();
		loadConfig();
		loadLevels();
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

	public abstract static class QueryCondition {
		public abstract boolean evaluate(Node node);

		public boolean evaluateIndex(IndexLevel level) {
			return true;
		}

		public void buildIndexFilter(IndexLevel level, int min[], int max[]) {
			// Do nothing by default, which means do not filter on the index
			// nodes
			// This should be overridden by any query conditions capable of
			// filtering on the index nodes.
			// For example, the equality and inequality conditions should
			// definitely filter
		}

		public String toString() {
			return this.getClass().toString().replace("class org.amanzi.index.AmanziIndex$", "");
		}
	}

	public abstract static class GroupCondition extends QueryCondition {
		public ArrayList<QueryCondition> conditions = new ArrayList<QueryCondition>();

		public GroupCondition(ArrayList<QueryCondition> conditions) {
			this.conditions.addAll(conditions);
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (QueryCondition condition : conditions) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(condition.toString());
			}
			return super.toString() + "(" + sb + ")";
		}
	}

	public static class AndCondition extends GroupCondition {
		public AndCondition(ArrayList<QueryCondition> conditions) {
			super(conditions);
		}

		public boolean evaluate(Node node) {
			for (QueryCondition condition : conditions) {
				if (!condition.evaluate(node))
					return false;
			}
			return true;
		}

		public boolean evaluateIndex(IndexLevel level) {
			for (QueryCondition condition : conditions) {
				if (!condition.evaluateIndex(level))
					return false;
			}
			return true;
		}

		/**
		 * The 'and' filter should create the most restrictive ranges
		 */
		public void buildIndexFilter(IndexLevel level, int min[], int max[]) {
			int[] xmin = new int[min.length];
			int[] xmax = new int[max.length];
			Arrays.fill(xmin, Integer.MAX_VALUE);
			Arrays.fill(xmax, Integer.MIN_VALUE);
			for (QueryCondition condition : conditions) {
				int[] tmin = new int[min.length];
				int[] tmax = new int[max.length];
				Arrays.fill(tmin, Integer.MAX_VALUE);
				Arrays.fill(tmax, Integer.MIN_VALUE);
				condition.buildIndexFilter(level, tmin, tmax);
				maxOf(xmin, tmin);
				minOf(xmax, tmax);
			}
			minOf(min, xmin);
			maxOf(max, xmax);
		}

		private void minOf(int[] max, int[] tmax) {
			for (int i = 0; i < max.length; i++) {
				if (max[i] == Integer.MIN_VALUE) {
					max[i] = tmax[i];
				} else if (tmax[i] != Integer.MIN_VALUE) {
					max[i] = Math.min(max[i], tmax[i]);
				}
			}
		}

		private void maxOf(int[] min, int[] tmin) {
			for (int i = 0; i < min.length; i++) {
				if (min[i] == Integer.MAX_VALUE) {
					min[i] = tmin[i];
				} else if (tmin[i] != Integer.MAX_VALUE) {
					min[i] = Math.max(min[i], tmin[i]);
				}
			}
		}
	}

	public static class OrCondition extends GroupCondition {
		public OrCondition(ArrayList<QueryCondition> conditions) {
			super(conditions);
		}

		public boolean evaluate(Node node) {
			for (QueryCondition condition : conditions) {
				if (condition.evaluate(node))
					return true;
			}
			return false;
		}

		public boolean evaluateIndex(IndexLevel level) {
			for (QueryCondition condition : conditions) {
				if (condition.evaluateIndex(level))
					return true;
			}
			return false;
		}

		/**
		 * The 'or' filter should create the least restrictive ranges
		 */
		public void buildIndexFilter(IndexLevel level, int min[], int max[]) {
			for (QueryCondition condition : conditions) {
				condition.buildIndexFilter(level, min, max);
			}
		}
	}

	/**
	 * This abstract class supports the comparison of one property to one value.
	 * This is the basis for equality and inequality operations, like ==, <, <=,
	 * >, >= operators. Each such comparison condition needs to extend this
	 * class and implement only the compareThem method to provide the correct
	 * comparison.
	 * 
	 * @author craig
	 */
	public abstract static class ComparisonCondition extends QueryCondition {
		String property;
		Object value;
		PropertyConfig<?> config;
		String comparisonDescription;

		public ComparisonCondition(String property, Object value, PropertyConfig<?> config, String comparisonDescription) {
			this.property = property;
			this.value = value;
			this.config = config;
			this.comparisonDescription = comparisonDescription;
		}

		protected abstract boolean compareThem(Mapper<?> mapper, Object thisValue, Object otherValue);

		public boolean evaluate(Node node) {
			Object val = node.getProperty(property, null);
			return val != null && compareThem(config.getMapper(), val, value);
		}

		public void buildIndexFilter(IndexLevel level, int min[], int max[]) {
			if (comparisonDescription.contains("<")) {
				level.buildIndexFilter(min, max, property, null, value);
			} else if (comparisonDescription.contains(">")) {
				level.buildIndexFilter(min, max, property, value, null);
			} else {
				level.buildIndexFilter(min, max, property, value, value);
			}
		}

		public String toString() {
			return super.toString() + "[" + property + " " + comparisonDescription + " " + value + "]";
		}
	}

	/**
	 * Implementation of the '==' operator
	 * 
	 * @author craig
	 */
	public static class EqualsCondition extends ComparisonCondition {

		public EqualsCondition(String property, Object value, PropertyConfig<?> config) {
			super(property, value, config, "==");
		}

		@Override
		protected boolean compareThem(Mapper<?> mapper, Object thisValue, Object otherValue) {
			return mapper.compare(thisValue, otherValue) == 0;
		}

	}

/**
	 * Implementation of the '<' operator
	 * @author craig
	 */
	public static class LessThanCondition extends ComparisonCondition {

		public LessThanCondition(String property, Object value, PropertyConfig<?> config) {
			super(property, value, config, "<");
		}

		protected boolean compareThem(Mapper<?> mapper, Object thisValue, Object otherValue) {
			return mapper.compare(thisValue, otherValue) < 0;
		}
	}

	/**
	 * Implementation of the '<=' operator
	 * 
	 * @author craig
	 */
	public static class LessThanOrEqualsCondition extends ComparisonCondition {

		public LessThanOrEqualsCondition(String property, Object value, PropertyConfig<?> config) {
			super(property, value, config, "<=");
		}

		protected boolean compareThem(Mapper<?> mapper, Object thisValue, Object otherValue) {
			return mapper.compare(thisValue, otherValue) <= 0;
		}
	}

	/**
	 * Implementation of the '>' operator
	 * 
	 * @author craig
	 */
	public static class GreaterThanCondition extends ComparisonCondition {

		public GreaterThanCondition(String property, Object value, PropertyConfig<?> config) {
			super(property, value, config, ">");
		}

		protected boolean compareThem(Mapper<?> mapper, Object thisValue, Object otherValue) {
			return mapper.compare(thisValue, otherValue) > 0;
		}
	}

	/**
	 * Implementation of the '>=' operator
	 * 
	 * @author craig
	 */
	public static class GreaterThanOrEqualsCondition extends ComparisonCondition {

		public GreaterThanOrEqualsCondition(String property, Object value, PropertyConfig<?> config) {
			super(property, value, config, ">=");
		}

		protected boolean compareThem(Mapper<?> mapper, Object thisValue, Object otherValue) {
			return mapper.compare(thisValue, otherValue) >= 0;
		}
	}

	/**
	 * This is the main query API, which can be called to query the index with
	 * complex conditions of nest AND and OR operations containing equality and
	 * inequality operators. This can be implemented either as an instance of
	 * the QueryCondition class (which can be a collection of other
	 * QueryConditions), or it can be a parsable string that in turn can be
	 * parsed into a QueryCondition. And example might be:
	 * 
	 * <pre>
	 * query(&quot;propA &lt; 5 and propA &gt; 2 or propA == 7 and propB &gt;= something&quot;)
	 * </pre<
	 */
	public IndexHits<Node> query(Object queryOrQueryObject) {
		if (queryOrQueryObject instanceof QueryCondition) {
			return queryCondition((QueryCondition) queryOrQueryObject);
		} else {
			return queryCondition(makeQueryCondition(queryOrQueryObject.toString().replaceAll(" and ", " AND ")
					.replaceAll(" or ", " OR ")));
		}
	}

	private QueryCondition makeQueryCondition(String query) {
		String[] subqueries = query.split(" OR ");
		if (subqueries.length > 1) {
			return new OrCondition(makeConditions(subqueries));
		} else {
			subqueries = query.split(" AND ");
			if (subqueries.length > 1) {
				return new AndCondition(makeConditions(subqueries));
			} else {
				subqueries = query.split(" (==|\\<\\=|\\>\\=|\\<|\\>) ");
				if (subqueries.length > 1) {
					String op = query.replaceFirst(subqueries[0], "").replaceFirst(subqueries[1], "");
					String property = subqueries[0];
					PropertyConfig<?> propertyConfig = config.getProperty(property);
					Object value = propertyConfig.getMapper().parse(subqueries[1]);
					if (op.contains("==")) {
						return new EqualsCondition(property, value, propertyConfig);
					} else if (op.contains("<=")) {
						return new LessThanOrEqualsCondition(property, value, propertyConfig);
					} else if (op.contains(">=")) {
						return new GreaterThanOrEqualsCondition(property, value, propertyConfig);
					} else if (op.contains("<")) {
						return new LessThanCondition(property, value, propertyConfig);
					} else if (op.contains(">")) {
						return new GreaterThanCondition(property, value, propertyConfig);
					} else {
						throw new RuntimeException("Unimplemented operation[" + op + "] in query: " + query);
					}
				} else {
					throw new RuntimeException("Unimplemented query: " + query);
				}
			}
		}
	}

	private ArrayList<QueryCondition> makeConditions(String[] subqueries) {
		ArrayList<QueryCondition> conditions = new ArrayList<QueryCondition>();
		for (String subquery : subqueries) {
			conditions.add(makeQueryCondition(subquery));
		}
		return conditions;
	}

	private static class AmanziIndexHits implements IndexHits<Node> {

		private SearchEvaluator searchEvaluator;
		private Traverser searchTraverser; // used for streamed searching
		private Iterator<Node> searchIterator; // used for streamed searching
		private ArrayList<Node> results; // used for either empty results, or
											// cached results

		/**
		 * Construct with the index node, or the index root node, and a
		 * SearchEvaluator pre-configured with the search query. This class will
		 * wrap the traverser for streamed search results, or alternatively it
		 * will cache the results and return an iterator on the cache. The
		 * latter mode will be used if the size() method is called. Do not call
		 * size() for potentially very large result sets.
		 * 
		 * @param indexNode
		 *            the node representing the index to search, normally
		 *            containing a 'name' parameter
		 * @param searchEvaluator
		 *            the SearchEvaluator configured with the search query to
		 *            apply to the index and data nodes
		 */
		public AmanziIndexHits(Node indexNode, SearchEvaluator searchEvaluator) {
			this.searchEvaluator = searchEvaluator;
			Relationship rootRel = indexNode.getSingleRelationship(AmanziIndexRelationshipTypes.INDEX_ROOT, Direction.OUTGOING);
			if (rootRel != null) {
				// We have an index tree, so traverse it
				this.searchTraverser = rootRel.getEndNode().traverse(Order.DEPTH_FIRST, searchEvaluator, searchEvaluator,
						AmanziIndexRelationshipTypes.INDEX_CHILD, Direction.OUTGOING, AmanziIndexRelationshipTypes.INDEX_LEAF,
						Direction.OUTGOING);
			} else {
				// We have no index tree, so create empty result set
				results = new ArrayList<Node>();
			}
		}

		private Iterator<Node> getSearchIterator() {
			if (searchIterator == null) {
				if (results != null) {
					searchIterator = results.iterator();
				} else {
					searchIterator = searchTraverser.iterator();
				}
			}
			return searchIterator;
		}

		@Override
		public boolean hasNext() {
			return getSearchIterator().hasNext();
		}

		@Override
		public Node next() {
			return getSearchIterator().next();
		}

		@Override
		public void remove() {
		}

		@Override
		public Iterator<Node> iterator() {
			return this;
		}

		@Override
		public void close() {
			searchIterator = null;
		}

		@Override
		public float currentScore() {
			return 0;
		}

		@Override
		public Node getSingle() {
			Node single = getSearchIterator().next();
			close();
			return single;
		}

		@Override
		public int size() {
			if (results == null) {
				results = new ArrayList<Node>();
				for (Node node : searchTraverser) {
					results.add(node);
				}
				close();
			}
			return results.size();
		}

		public String toString() {
			return "AmanziIndexHits[" + searchEvaluator + "]";
		}
	}

	private class SearchEvaluator implements StopEvaluator, ReturnableEvaluator {
		private final ArrayList<int[]> levelMin = new ArrayList<int[]>();
		private final ArrayList<int[]> levelMax = new ArrayList<int[]>();
		private Node currentNode = null;
		private boolean nodeInRange = false;
		private boolean nodeIsIndex = false;
		private boolean indexOnEdge = false;
		private int countNodes = 0;
		private int countResults = 0;
		private QueryCondition query;

		public SearchEvaluator(QueryCondition query) {
			this.query = query;
			// First we convert the search range into index ranges for each
			// level of the index
			for (IndexLevel level : levels) {
				int[] min = new int[config.size()];
				int[] max = new int[config.size()];
				Arrays.fill(min, Integer.MAX_VALUE);
				Arrays.fill(max, Integer.MIN_VALUE);
				// Convert the query into a set of min/max ranges
				query.buildIndexFilter(level, min, max);
				// Remove any conditions not specified
				for (int i = 0; i < min.length; i++) {
					if (min[i] == Integer.MAX_VALUE)
						min[i] = ANY_VALUE;
					if (max[i] == Integer.MIN_VALUE)
						max[i] = ANY_VALUE;
				}
				levelMin.add(min);
				levelMax.add(max);
			}
		}

		private void debugNode(Node node) {
			if (node.hasProperty("index")) {
				int[] index = (int[]) node.getProperty("index", null);
				Integer level = (Integer) node.getProperty("level", null);
				System.out.println("Searching index node: level[" + level + "], index[" + Arrays.toString(index) + "]");
			} else {
				StringBuffer sb = new StringBuffer();
				for (String p : node.getPropertyKeys()) {
					if (sb.length() > 0)
						sb.append(", ");
					try {
						sb.append(p).append("='").append(node.getProperty(p).toString()).append("'");
					} catch (Exception e) {
					}
				}
				System.out.println("Searching data node: " + node + " (" + sb + ")");
			}
			System.out.println("\tGot results: nodeIsIndex[" + nodeIsIndex + "], nodeInRange[" + nodeInRange + "]");
		}

		private void setTestNode(Node node) {
			if (node != currentNode) {
				currentNode = node;
				countNodes++;
				int[] index = (int[]) node.getProperty("index", null);
				Integer level = (Integer) node.getProperty("level", null);
				// Do not traverse further if we are outside the index tree
				if (index == null || level == null) {
					nodeIsIndex = false;
					nodeInRange = query.evaluate(node);
					// TODO: Consider optimizing using the results of
					// indexOnEdge
					if (nodeInRange) {
						countResults++;
					}
				} else {
					// Index nodes get tested to see if they are in the range
					nodeIsIndex = true;
					nodeInRange = true;
					int[] minIndex = levelMin.get(level);
					int[] maxIndex = levelMax.get(level);
					for (int i = 0; i < minIndex.length; i++) {
						if (minIndex[i] != ANY_VALUE && index[i] < minIndex[i]) {
							nodeInRange = false;
							break;
						}
						if (maxIndex[i] != ANY_VALUE && index[i] > maxIndex[i]) {
							nodeInRange = false;
							break;
						}
						if (index[i] == minIndex[i] || index[i] == maxIndex[i]) {
							// Index is on the edge of the range, and so can
							// contain data on both sides
							indexOnEdge = true;
						}
					}
				}
				debugNode(node);
			}
		}

		@Override
		public boolean isStopNode(TraversalPosition currentPos) {
			// if (currentPos.isStartNode()) {
			// return false;
			// } else {
			setTestNode(currentPos.currentNode());
			return !nodeIsIndex || !nodeInRange;
			// }
		}

		@Override
		public boolean isReturnableNode(TraversalPosition currentPos) {
			if (currentPos.currentNode().hasProperty("state")) {
				if (currentPos.currentNode().getProperty("state").equals("disabled")) {
					return false;
				}
			}

			// if (currentPos.isStartNode()) {
			// return false;
			// } else {
			setTestNode(currentPos.currentNode());
			return nodeInRange && !nodeIsIndex;
			// }
		}

		public String toString() {
			return query.toString();
		}
	}

	private IndexHits<Node> queryCondition(QueryCondition queryCondition) {
		return new AmanziIndexHits(findIndexNode(), new SearchEvaluator(queryCondition));
	}

	public IndexHits<Node> query(String key, Object queryOrQueryObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public void remove(Node entity, String key, Object value) {
		// TODO Auto-generated method stub

	}

	/**
	 * This is the man indexing method of this class. It adds the specified node
	 * to the index. Firstly it uses the PropertyConfig of this index to find
	 * the required properties of the node to be indexed, and then it converts
	 * these to the internal int[] used as the index key, using the Mappers
	 * configured in the PropertyConfig. Then the node is finally attached to
	 * the appropriate index node based on the index key int[]. It is possible,
	 * based on how the index was configured, for this node to be processed
	 * later, and held temporarily in a cache. If that happens, expect it to be
	 * added to the tree at some later add(Node) call, or at the end in the
	 * finishUp() method (which calls flush() to add the cached nodes to the
	 * tree).
	 * <p>
	 * If a configured property does not exist on the node, the special value if
	 * NO_PROPERTY is used. This is the value Integer.MIN_VALUE, and allows the
	 * node to be indexed without that property. In later searches if the query
	 * includes that property, such nodes can be ignored from the results.
	 * </p>
	 * 
	 * @param node
	 *            Node to index
	 */
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
				keys[i] = NO_PROPERTY;
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
	 * This is the main indexing method of the class, finding the index node to
	 * use for a specific index key int[]. We first search up the cached index
	 * stack until we find an index that covers the required value. Then we step
	 * back down creating sub-index nodes as we go until we hit the lowest
	 * level. With data where nodes coming in are usually near the previous
	 * nodes, the cache stack will usually contain the right nodes and it will
	 * not often be required to search high of the stack. This should give very
	 * good performance for that kind of data.
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

	private void loadLevels() {
		levels.clear();
		Relationship rootRel = indexNode.getSingleRelationship(AmanziIndexRelationshipTypes.INDEX_ROOT, Direction.OUTGOING);
		if (rootRel != null) {
			Node currentIndexNode = rootRel.getEndNode();
			levels.add(new IndexLevel(config, currentIndexNode));
			while (levels.get(0).getLevel() > 0) {
				IndexLevel indexLevel = null;
				for (Relationship rel : currentIndexNode.getRelationships(AmanziIndexRelationshipTypes.INDEX_CHILD,
						Direction.OUTGOING)) {
					Node node = rel.getEndNode();
					indexLevel = new IndexLevel(config, node);
					currentIndexNode = node;
					if (indexLevel.isOrigin())
						break;
				}
				if (indexLevel != null) {
					levels.add(0, indexLevel);
				} else {
					break;
				}
			}
		}
	}

	private void loadConfig() {
		findIndexNode();
		if (indexNode != null) {
			Relationship rel = indexNode.getSingleRelationship(AmanziIndexRelationshipTypes.INDEX_CONFIG, Direction.OUTGOING);
			if (rel == null) {
				throw new RuntimeException("No existing configuration found for index '" + name + "'");
			} else {
				config = new DefaultIndexConfig(rel.getEndNode());
			}
		} else {
			throw new RuntimeException("No existing index found with name '" + name + "'");
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

	private Node findIndexNode() {
		if (indexNode == null) {
			for (Relationship rel : getIndexesNode()
					.getRelationships(AmanziIndexRelationshipTypes.AMANZI_INDEX, Direction.OUTGOING)) {
				Node node = rel.getEndNode();
				if (node.getProperty("name").equals(name)) {
					indexNode = node;
				}
			}
		}
		return indexNode;
	}

	/** This finds or create the top node of the index tree. */
	private Node getIndexNode() {
		if (findIndexNode() == null) {
			indexNode = makeNode(getIndexesNode(), AmanziIndexRelationshipTypes.AMANZI_INDEX, "index", null);
			indexNode.setProperty("name", name);
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

	/**
	 * This finds or creates the reference node for all amanzi-indexes in the
	 * database
	 */
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
	 * This method simply dumps a detailed description of the index, allowing
	 * for debugging.
	 */
	public void debug(PrintStream out) {
		for (IndexLevel level : levels) {
			out.println(level);
		}
		debug(out, getIndexNode(), 0);
	}

	private void debug(PrintStream out, Node node, int depth) {
		if (depth > 7)
			return;
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
			if (key.equals("index")) {
				try {
					property = arrayString((int[]) property);
				} catch (Exception e) {
				}
			}
			sb.append(key).append(": ").append(property);
		}
		out.println(sb.toString());
		for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
			out.println(tab + "--" + rel.getType() + "-->");
			debug(out, rel.getEndNode(), depth + 1);
		}
	}
}
