package org.amanzi.index.mappers;

/**
 * This Integer mapper can convert from an integer data value to an index key.
 * It should be constructed with information about the kind of data that is to
 * be indexed, normally a range and/or step size. As it is queries for more data
 * it will adjust its internal information about the range, but does not support
 * re-indexing.
 * 
 * @author craig
 */
public class IntegerMapper extends NumberMapper<Integer> {

	private IntegerMapper(int min, int max, int step) {
		super(min, max, step);
	}

	/**
	 * Can be constructed before knowing any data, but will set the origin to
	 * zero, so if all data is far from zero, this is not a good choice.
	 * 
	 * @param step
	 *            gap between index points
	 */
	public static IntegerMapper withStep(int step) {
		return new IntegerMapper(0, 0, step);
	}

	/**
	 * Can be constructed after a reasonable representative amount of data is
	 * known, so that the min and max are reasonably set, but still we enforce a
	 * specific step size.
	 * 
	 * @param min
	 * @param max
	 * @param step
	 */
	public static IntegerMapper withRangeAndStep(int min, int max, int step) {
		return new IntegerMapper(min, max, step);
	}

	/**
	 * Can be constructed after a reasonable representative amount of data is
	 * known, so that the min and max are reasonably set, but still we enforce a
	 * specific step size.
	 * 
	 * @param min
	 * @param max
	 * @param step
	 */
	public static IntegerMapper withRangeAndCategories(int min, int max,
			int categories) {
		return new IntegerMapper(min, max, (max - min) / categories);
	}

	public int toKey(Integer value) {
		min = Math.min(value, min);
		max = Math.max(value, max);
		int offset = value < origin ? -1 : 0;
		return (value - origin - offset) / step + offset;
	}

	public int getCategories() {
		return (max - min) / step;
	}

	public String toString() {
		return "IntegerMapper: min[" + min + "] origin[" + origin + "] max["
				+ origin + "] step[" + step + "] categories[" + getCategories()
				+ "]";
	}

	public Integer getMin(int key) {
		return origin + step * key;
	}

	public Integer getMax(int key) {
		return (origin + step * (key + 1)) - 1;
	}

	protected Integer average(Integer a, Integer b) {
		return (a + b) / 2;
	}
}
