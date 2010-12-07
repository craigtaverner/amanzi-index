package org.amanzi.index.mappers;

public abstract class NumberMapper<T> implements Mapper<T> {
	protected T min;
	protected T max;
	protected T origin;
	protected T step;

	protected NumberMapper(T min, T max, T step) {
		this.min = min;
		this.max = max;
		this.step = step;
		this.origin = average(max, min);
	}

	public String getRangeText(int key) {
		return "key[" + key + "] range[" + getMin(key) + "," + getMax(key)
				+ "]";
	}

	public T getMin() {
		return min;
	}

	public T getMax() {
		return max;
	}

	public T getOrigin() {
		return origin;
	}

	public T getStep() {
		return step;
	}

	protected abstract T average(T a, T b);
	
	/** Get the minimum value for the specific index */
	public abstract T getMin(int key);

	/** Get the maximum value for the specific index */
	public abstract T getMax(int key);
}
