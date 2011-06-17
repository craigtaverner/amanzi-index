package org.amanzi.index.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.amanzi.index.AmanziIndexRelationshipTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

public class DefaultIndexConfig implements IndexConfig {
	private IntegerStepper stepper;
	private HashMap<String, PropertyConfig<?>> properties;
	private HashMap<String, Integer> propertyPositions;
	private Node configNode;

	/**
	 * This stepper converts low level index keys to higher level keys and back.
	 */
	public static class IntegerStepper {
		private final int step;

		public IntegerStepper(int step) {
			if (step < 2)
				step = 2;
			this.step = step;
		}

		public int indexOf(int value, int origin, int stepSize) {
			return (value - (origin - stepSize / 2)) / stepSize;
		}

		public int valueOf(int index, int origin, int stepSize) {
			return (origin - stepSize / 2) + index * stepSize;
		}

		public int stepSize(int level) {
			return (int) (Math.pow(step, level));
		}

		public int compare(Integer a, Integer b) {
			return a.compareTo(b);
		}
	}

	public DefaultIndexConfig(int step, Collection<PropertyConfig<?>> properties) {
		// step = step%2==1?step:step+1;
		this.stepper = new IntegerStepper(step);
		this.properties = new LinkedHashMap<String, PropertyConfig<?>>();
		for (PropertyConfig<?> property : properties) {
			this.properties.put(property.getName(), property);
		}
	}

	/**
	 * Create a configuration based on the existing configuration graph in the
	 * index.
	 * 
	 * @param node
	 *            containing configuration information
	 */
	public DefaultIndexConfig(Node node) {
		this.configNode = node;
		this.stepper = new IntegerStepper((Integer) configNode.getProperty("step"));
		HashMap<Integer, PropertyConfig<?>> propMap = new HashMap<Integer, PropertyConfig<?>>();
		for (Relationship rel : configNode.getRelationships(AmanziIndexRelationshipTypes.INDEX_CONFIG, Direction.OUTGOING)) {
			int order = (Integer) rel.getProperty("order");
			PropertyConfig<?> property = DefaultPropertyConfig.makeFrom(rel.getEndNode());
			propMap.put(order, property);
		}
		this.properties = new LinkedHashMap<String, PropertyConfig<?>>();
		ArrayList<Integer> ordered = new ArrayList<Integer>(propMap.keySet());
		Collections.sort(ordered);
		for (int order : ordered) {
			PropertyConfig<?> property = propMap.get(order);
			this.properties.put(property.getName(), property);
		}
	}

	@Override
	public void save(Node indexNode) {
		Transaction tx = indexNode.getGraphDatabase().beginTx();
		try {
			configNode = indexNode.getGraphDatabase().createNode();
			configNode.setProperty("step", this.stepper.step);
			indexNode.createRelationshipTo(configNode, AmanziIndexRelationshipTypes.INDEX_CONFIG);
			int order = 0;
			for (PropertyConfig<?> property : getProperties()) {
				Node propNode = indexNode.getGraphDatabase().createNode();
				Map<String, Object> nodeProps = property.getNodeProperties();
				for (String key : nodeProps.keySet()) {
					propNode.setProperty(key, nodeProps.get(key));
				}
				Relationship rel = configNode.createRelationshipTo(propNode, AmanziIndexRelationshipTypes.INDEX_CONFIG);
				rel.setProperty("order", order);
				order++;
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}

	@Override
	public Collection<PropertyConfig<?>> getProperties() {
		return properties.values();
	}

	@Override
	public PropertyConfig<?> getProperty(String property) {
		return properties.get(property);
	}

	@Override
	public int getPropertyPosition(String property) {
		if (propertyPositions == null) {
			propertyPositions = new HashMap<String, Integer>();
			int position = 0;
			for (PropertyConfig<?> propertyConfig : properties.values()) {
				propertyPositions.put(propertyConfig.getName(), position);
				position++;
			}
		}
		return propertyPositions.get(property);
	}

	@Override
	public int size() {
		return properties.size();
	}

	private int stepSize(int level) {
		// TODO: Optimize with cache
		return stepper.stepSize(level);
	}

	@Override
	public Collection<String> getPropertyNames() {
		return properties.keySet();
	}

	@Override
	public int keyFor(int keys, int level) {
		return stepper.indexOf(keys, 0, stepSize(level));
	}

	@Override
	public int[] keysFor(int[] keys, int level) {
		int[] newKeys = new int[keys.length];
		for (int i = 0; i < keys.length; i++) {
			newKeys[i] = stepper.indexOf(keys[i], 0, stepSize(level));
		}
		return newKeys;
	}

	@Override
	public int[] valuesFor(int[] keys, int level) {
		// TODO Auto-generated method stub
		return null;
	}

}
