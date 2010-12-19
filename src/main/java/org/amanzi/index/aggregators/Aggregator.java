package org.amanzi.index.aggregators;

/**
 * This interface defines the capabilities of a class to collect numbers into
 * summaries or aggregations. It can be used by an index to build statistics
 * within the index nodes for the data defined in the nodes lower down in the
 * tree. It is important to note that the aggregations can be based on previous
 * aggregations, since the index trees are of multiple depths. This means that
 * we need to keep a counter as well as totals, and only produce averages on
 * demand.
 * 
 * @author craig
 */
public interface Aggregator<T> {
	public T max(T value, T max);
	public T min(T value, T min);
	public T total(T value, T total);
	public T average(T total, int count);
}
