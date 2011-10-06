package org.amanzi.index.mappers;

/**
 * This Long mapper can convert from an long data value to an index key.
 * It should be constructed with information about the kind of data that is to
 * be indexed, normally a range and/or step size. As it is queries for more data
 * it will adjust its internal information about the range, but does not support
 * re-indexing.
 * 
 * @author K
 */
public class LongMapper extends NumberMapper<Long> {

	private LongMapper(long min, long max, long step) {
		super(min, max, step > 0 ? step : 1);
	}

	/**
	 * Can be constructed before knowing any data, but will set the origin to
	 * zero, so if all data is far from zero, this is not a good choice.
	 * 
	 * @param step
	 *            gap between index points
	 */
	public static LongMapper withStep(long step) {
		return new LongMapper(0, 0, step);
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
	public static LongMapper withRangeAndStep(long min, long max, long step) {
		return new LongMapper(min, max, step);
	}

	/**
	 * Can be constructed after a reasonable representative amount of data is
	 * known, so that the min and max are reasonably set, but still we enforce a
	 * specific step size.
	 * 
	 * @param min
	 * @param max
	 * @param categories
	 */
	public static LongMapper withRangeAndCategories(long min, long max,
			int categories) {
		return new LongMapper(min, max, (max - min) / categories);
	}

	public int toKey(Object obj) {
		Long value = (Long) obj;
		min = Math.min(value, min);
		max = Math.max(value, max);
		int offset = value < origin ? -1 : 0;
		return (int) ((value - origin - offset) / step + offset);
	}

	public Long parse(String text) {
		return Long.parseLong(text);
	}

	@Override
	public int compare(Object a, Object b) {
		return ((Long)a).compareTo((Long)b);
	}

	public int getCategories() {
		return (int) ((max - min) / step);
	}
	
	public String toString() {
		return "LongMapper: min[" + min + "] origin[" + origin + "] max["
				+ origin + "] step[" + step + "] categories[" + getCategories()
				+ "]";
	}

	public Long getStep() {
		return step;
	}

	public Long getMin(int key) {
		return origin + step * key;
	}

	public Long getMax(int key) {
		return (origin + step * (key + 1)) - 1;
	}

	protected Long average(Long a, Long b) {
		return (a + b) / 2;
	}

}
