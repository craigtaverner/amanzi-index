package org.amanzi.index.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.amanzi.index.mappers.CharacterStringMapper;
import org.amanzi.index.mappers.FloatMapper;
import org.amanzi.index.mappers.IntegerMapper;
import org.amanzi.index.mappers.ListStringMapper;
import org.amanzi.index.mappers.LongMapper;
import org.amanzi.index.mappers.Mapper;
import org.amanzi.index.util.CollectionUtilities;
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
		int categories = getMapper().getCategories();
		props.put("categories", categories);
		if (categories < 0 && getTypeName().equalsIgnoreCase("long")) {
			props.put("step", ((LongMapper) getMapper()).getStep());
		} else if (getTypeName().equalsIgnoreCase("string")) {
			props.put("depth", ((CharacterStringMapper) getMapper()).getDepth());
		} else if (getTypeName().equals("listString")) {
			props.put("gap", ((ListStringMapper) getMapper()).getGap());
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
	
	public static DefaultPropertyConfig<String> makeListStringConfig(String name, Collection<String> data) {
		return makeListStringConfig(name, data, ListStringMapper.DEFAULT_GAP);
	}

	public static DefaultPropertyConfig<String> makeListStringConfig(String name, final Collection<String> data, final int gap) {
		return new DefaultPropertyConfig<String>(name, "", "") {
			@Override
			public Mapper<String> makeMapper() {
				return ListStringMapper.withSampleGap(data, gap);
			}

			@Override
			public String getTypeName() {
				return "listString";
			}
		};
	}
	
	public static DefaultPropertyConfig<String> makeListStringConfig(String name, final HashMap<Integer, String> map1, 
			final HashMap<Integer, ArrayList<String>> map2, final int[] count, final int gap) {
		return new DefaultPropertyConfig<String>(name, "", "") {
			@Override
			public Mapper<String> makeMapper() {
				return ListStringMapper.withRestoreGap(map1, map2, count, gap);
			}

			@Override
			public String getTypeName() {
				return "listString";
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
	
	public static DefaultPropertyConfig<Long> makeLongConfig(String name, long min, long max) {
		return makeLongConfig(name, min, max, 1000);
	}

	public static DefaultPropertyConfig<Long> makeLongConfig(String name, long min, long max, final long step) {
		return new DefaultPropertyConfig<Long>(name, min, max) {
			@Override
			public Mapper<Long> makeMapper() {
				// For LongMapper, use step instead of categories 
				return LongMapper.withRangeAndStep(min, max, step);
			}

			@Override
			public String getTypeName() {
				return "long";
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
			if ("string".equalsIgnoreCase(type)) {
				Object depth = node.getProperty("depth", CharacterStringMapper.DEFAULT_DEPTH);
				return makeStringConfig(name, (String) min, (String) max, (Integer) depth);
			} else if ("listString".equalsIgnoreCase(type)) {
				Object gap = node.getProperty("gap", ListStringMapper.DEFAULT_GAP);
				HashMap<Integer, String> keyList = CollectionUtilities.StringToHashMap(
						(String) node.getProperty("keys"));
				HashMap<Integer, ArrayList<String>> extraKeyList = CollectionUtilities.StringToHashMapExtra(
						(String) node.getProperty("extraKeys"));
				int[] counter = CollectionUtilities.StringtoIntArray((String) node.getProperty("counter"));
				return makeListStringConfig(name, keyList, extraKeyList, counter, (Integer) gap);
			} else if ("integer".equalsIgnoreCase(type)) {
				return makeIntegerConfig(name, (Integer) min, (Integer) max, (Integer) categories);
			} else if ("float".equalsIgnoreCase(type)) {
				return makeFloatConfig(name, (Float) min, (Float) max, (Integer) categories);
			} else if ("long".equalsIgnoreCase(type)) {
				Object step = node.getProperty("step", 1000);
				return makeLongConfig(name, (Long) min, (Long) max, (Long) step);
			}
		}
		return null;
	}
}
