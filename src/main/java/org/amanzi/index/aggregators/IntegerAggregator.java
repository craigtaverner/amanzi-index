package org.amanzi.index.aggregators;

public class IntegerAggregator implements Aggregator<Integer> {

	public Integer max(Integer value, Integer max) {
		return value > max ? value : max;
	}

	public Integer min(Integer value, Integer min) {
		return value < min ? value : min;
	}

	public Integer total(Integer value, Integer total) {
		return value + total;
	}

	public Integer average(Integer total, int count) {
		return total / count;
	}

}
