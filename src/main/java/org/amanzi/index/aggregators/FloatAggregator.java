package org.amanzi.index.aggregators;

public class FloatAggregator implements Aggregator<Float> {

	public Float max(Float value, Float max) {
		return value > max ? value : max;
	}

	public Float min(Float value, Float min) {
		return value < min ? value : min;
	}

	public Float total(Float value, Float total) {
		return value + total;
	}

	public Float average(Float total, int count) {
		return total / count;
	}

}
