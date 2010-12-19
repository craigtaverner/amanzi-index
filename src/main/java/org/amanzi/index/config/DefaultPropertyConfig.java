package org.amanzi.index.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.amanzi.index.mappers.CharacterStringMapper;
import org.amanzi.index.mappers.FloatMapper;
import org.amanzi.index.mappers.IntegerMapper;
import org.amanzi.index.mappers.Mapper;
import org.neo4j.graphdb.Node;

/**
 * This generic implementation of the PropertyConfig class provides for
 * supporting String, Integer and Float configurations where they are all based
 * on an initial set of min/max values and in the case of Integer and Float, a
 * number of categories to break that range into.
 * 
 * @author craig
 */
public abstract class DefaultPropertyConfig<T extends Object> implements PropertyConfig<T> {
	protected T min;
	protected T max;
	protected String name;
	protected Mapper<T> mapper;

	public DefaultPropertyConfig(String name, T min, T max) {
		this.name = name;
		this.min = min;
		this.max = max;
		this.mapper = null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public T getMin() {
		return min;
	}

	@Override
	public T getMax() {
		return max;
	}

	@Override
	public Mapper<T> getMapper() {
		if (mapper == null)
			mapper = makeMapper();
		return mapper;
	}

	protected abstract Mapper<T> makeMapper();

	public Map<String, Object> getNodeProperties() {
		HashMap<String, Object> props = new LinkedHashMap<String, Object>();
		props.put("property_type", getTypeName());
		props.put("name", getName());
		props.put("min", getMin());
		props.put("max", getMax());
		props.put("categories", getMapper().getCategories());
		if (getTypeName().equalsIgnoreCase("string")) {
			props.put("depth", ((CharacterStringMapper) getMapper()).getDepth());
		}
		return props;
	}

	public static DefaultPropertyConfig<String> makeStringConfig(String name, String min, String max) {
		return makeStringConfig(name, min, max, CharacterStringMapper.DEFAULT_DEPTH);
	}

	public static DefaultPropertyConfig<String> makeStringConfig(String name, String min, String max, final int depth) {
		return new DefaultPropertyConfig<String>(name, min, max) {
			@Override
			public Mapper<String> makeMapper() {
				return CharacterStringMapper.withMinMax(min, max, depth);
			}

			@Override
			public String getTypeName() {
				return "string";
			}
		};
	}

	public static DefaultPropertyConfig<Integer> makeIntegerConfig(String name, int min, int max) {
		return makeIntegerConfig(name, min, max, 100);
	}

	public static DefaultPropertyConfig<Integer> makeIntegerConfig(String name, int min, int max, final int categories) {
		return new DefaultPropertyConfig<Integer>(name, min, max) {
			@Override
			public Mapper<Integer> makeMapper() {
				return IntegerMapper.withRangeAndCategories(min, max, categories);
			}

			@Override
			public String getTypeName() {
				return "integer";
			}
		};
	}

	public static DefaultPropertyConfig<Float> makeFloatConfig(String name, float min, float max) {
		return makeFloatConfig(name, min, max, 100);
	}

	public static DefaultPropertyConfig<Float> makeFloatConfig(String name, float min, float max, final int categories) {
		return new DefaultPropertyConfig<Float>(name, min, max) {
			@Override
			public Mapper<Float> makeMapper() {
				return FloatMapper.withRangeAndCategories(min, max, categories);
			}

			@Override
			public String getTypeName() {
				return "float";
			}
		};
	}

	public static PropertyConfig<?> makeFrom(Node node) {
		String type = (String) node.getProperty("property_type");
		if (type != null) {
			String name = (String) node.getProperty("name");
			Object min = node.getProperty("min");
			Object max = node.getProperty("max");
			Object categories = node.getProperty("categories", 100);
			Object depth = node.getProperty("depth", CharacterStringMapper.DEFAULT_DEPTH);
			if ("string".equalsIgnoreCase(type)) {
				return makeStringConfig(name, (String) min, (String) max, (Integer) depth);
			} else if ("integer".equalsIgnoreCase(type)) {
				return makeIntegerConfig(name, (Integer) min, (Integer) max, (Integer) categories);
			} else if ("float".equalsIgnoreCase(type)) {
				return makeFloatConfig(name, (Float) min, (Float) max, (Integer) categories);
			}
		}
		return null;
	}
}
