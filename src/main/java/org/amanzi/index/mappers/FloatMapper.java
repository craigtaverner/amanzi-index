package org.amanzi.index.mappers;

/**
 * This Float mapper can convert from a float data value to an index key.
 * It should be constructed with information about the kind of data that is to
 * be indexed, normally a range and/or step size. As it is queries for more data
 * it will adjust its internal information about the range, but does not support
 * re-indexing.
 * 
 * @author craig
 */
public class FloatMapper extends NumberMapper<Float> {

	private FloatMapper(Float min, Float max, Float step) {
		super(min, max, step);
	}

	/**
	 * Can be constructed before knowing any data, but will set the origin to
	 * zero, so if all data is far from zero, this is not a good choice.
	 * 
	 * @param step
	 *            gap between index points
	 */
	public static FloatMapper withStep(float step) {
		return new FloatMapper(0f, 0f, step);
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
	public static FloatMapper withRangeAndStep(float min, float max, float step) {
		return new FloatMapper(min, max, step);
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
	public static FloatMapper withRangeAndCategories(float min, float max,
			int categories) {
		return new FloatMapper(min, max, (max - min) / (float)categories);
	}

	public int toKey(Float value) {
		min = Math.min(value, min);
		max = Math.max(value, max);
		int offset = value < origin ? -1 : 0;
		return (int)((value - origin - offset) / step + offset);
	}

	public int getCategories() {
		return (int)((max - min) / step);
	}

	public String toString() {
		return "FloatMapper: min[" + min + "] origin[" + origin + "] max["
				+ origin + "] step[" + step + "] categories[" + getCategories()
				+ "]";
	}

	public Float getMin(int key) {
		return origin + step * key;
	}

	public Float getMax(int key) {
		return (origin + step * (key + 1)) - 1;
	}

	protected Float average(Float a, Float b) {
		return (a + b) / 2;
	}
}
