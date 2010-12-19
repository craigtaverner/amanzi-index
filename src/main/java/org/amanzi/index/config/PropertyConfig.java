package org.amanzi.index.config;

import java.util.Map;

import org.amanzi.index.mappers.Mapper;

public interface PropertyConfig<T extends Object> {
	public String getName();
	public T getMin();
	public T getMax();
	public Mapper<T> getMapper();
	public String getTypeName();
	public Map<String, Object> getNodeProperties();
}
