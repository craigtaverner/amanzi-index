package org.amanzi.index.mappers;

import java.util.Arrays;
import java.util.Collection;

/**
 * <p>
 * This is a very simple mapper for strings. It is based on converting the
 * strings to integer index values by simply converting the string characters to
 * integers. For example, if the origin is placed at "A" then the string "B"
 * will be at 1, for a depth zero index. Then "AA" and "AB" will be in the same
 * index, but "AA" and "BA" will not. For this kind of index, the depth is a
 * critical number, because depths that are large put more strings in the same
 * index, while depths that are too shallow will have all strings in their own
 * index. Both cases perform badly. So this index is only good for data where we
 * have some idea of the kind of diversity we expect to see. Use the various
 * 'withX' methods to create an index that suites your particular case. The
 * withSample(Collection<String>) method will scan the collection for a common
 * prefix and choose a depth for you based on that. If the collection passed is
 * a reasonable representation of the kind of data that will continue to be
 * passed into the index, then this should work well.
 * </p>
 * <p>
 * The key parameters used in creating the mapper are:
 * <dl>
 * <dt>origin</dt>
 * <dd>This is the string set as the min value at index key = 0</dd>
 * <dt>depth</dt>
 * <dd>This integer defines how many characters to compare for deciding on the
 * index key. If depth is 1, only the first character is compared, so "AA" and
 * "AB" will be in the same index key (0), but "BA" will be in the next one (1).
 * Obviously it is advisable to choose a depth that matches the kind of
 * diversity intended to be indexed.</dd>
 * </p>
 * 
 * @author craig
 */
public class CharacterStringMapper implements Mapper<String> {
	public static final char DEFAULT_ROOT = ' ';
	public static final int DEFAULT_WIDTH = 94;
	public static final String DEFAULT_ORIGIN = "A";
	public static final int DEFAULT_DEPTH = 2;
	protected String min;
	protected String max;
	protected String origin = DEFAULT_ORIGIN;
	protected char[] originChars;
	protected int depth = DEFAULT_DEPTH;
	protected int width = DEFAULT_WIDTH;
	protected char root = DEFAULT_ROOT;

	/**
	 * Create a string mapper that will make the index that covers all possible
	 * values values of strings with the length defined by the depth. The origin
	 * is set to "A".
	 */
	public static CharacterStringMapper withDepth(int depth) {
		return new CharacterStringMapper(DEFAULT_ORIGIN, null, null, depth, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	/**
	 * Create a string mapper based on the provided origin and depth.
	 */
	public static CharacterStringMapper withOrigin(String origin, int depth) {
		return new CharacterStringMapper(origin, null, null, depth, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	/**
	 * Create a string mapper based on the provided initial min, max and depth.
	 */
	public static CharacterStringMapper withMinMax(String min, String max, int depth) {
		return new CharacterStringMapper(null, min, max, depth, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	/**
	 * Create a string mapper based on the provided origin, depth, root and
	 * width.
	 */
	public static CharacterStringMapper withConfig(String origin, int depth, char root, int width) {
		return new CharacterStringMapper(origin, null, null, depth, root, width);
	}

	/**
	 * Create a string mapper with the specified origin and a depth of 2.
	 */
	public static CharacterStringMapper withOrigin(String origin) {
		return new CharacterStringMapper(origin, null, null, DEFAULT_DEPTH, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	/**
	 * Create a string mapper with the prefix as origin, and a depth of the
	 * prefix length plus 1.
	 */
	public static CharacterStringMapper withPrefix(String prefix) {
		return new CharacterStringMapper(prefix, null, null, prefix.length() + 1, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	/**
	 * Create a string mapper based on the sample data provided. The collection
	 * is scanned for the longest common prefix, and that is used to create a
	 * prefix based mapper, with the prefix as origin and the prefix length plus
	 * 1 as the depth.
	 */
	public static CharacterStringMapper withSample(Collection<String> data) {
		String prefix = findPrefix(data);
		return new CharacterStringMapper(prefix, null, null, prefix.length() + 1, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	/**
	 * The default string mapper has origin "A" and depth 2.
	 */
	public static CharacterStringMapper getDefault() {
		return new CharacterStringMapper(DEFAULT_ORIGIN, null, null, DEFAULT_DEPTH, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	/**
	 * This utility method finds the longest common substring that all strings
	 * passed in have as a prefix. This is useful for finding a potential origin
	 * if we want to base the origin on a prefix. It is also useful to use the
	 * prefix as a choice for deciding the default depth of the index, since
	 * having a depth longer than the prefix will result in only single strings
	 * in each index node, and having a depth shorter than the prefix will
	 * result in too many strings in each index node.
	 * 
	 * @param names
	 *            Collection containing sample of known data to base index
	 *            choices on
	 * @return the longest common prefix of all data
	 */
	public static String findPrefix(Collection<String> names) {
		String prefix = null;
		for (String name : names) {
			if (prefix == null)
				prefix = name;
			else
				while (!name.startsWith(prefix)) {
					prefix = prefix.substring(0, prefix.length() - 1);
				}
		}
		return prefix;
	}

	/**
	 * Common constructor used by all factory methods for initializing this
	 * mapper.
	 * 
	 * @param origin
	 *            to map to the min value at index key 0
	 * @param depth
	 *            to use for deciding when to increment the key
	 */
	protected CharacterStringMapper(String origin, String min, String max, int depth, char root, int width) {
		if(origin == null) origin = average(min, max);
		this.origin = origin;
		while (this.origin.length() < depth)
			this.origin += origin.substring(origin.length() - 1);
		if (this.origin.length() > depth)
			this.origin = this.origin.substring(0, depth);
		this.min = (min == null) ? this.origin : min;
		this.max = (max == null) ? this.origin : max;
		this.depth = depth;
		this.root = root;
		this.width = width;
		this.originChars = this.origin.toCharArray();
		for (int i = 0; i < depth; i++) {
			originChars[i] = limitChar(originChars[i]);
		}
		if (!String.valueOf(originChars).equals(this.origin)) {
			throw new IllegalArgumentException("Origin '" + this.origin + "' out of range from root[" + root + "] and width="
					+ width + ", try '" + String.valueOf(originChars) + "' instead.");
		}
		this.toKey(this.origin);
	}

	private char limitChar(char c) {
		if (c < root)
			c = root;
		if (c > root + width)
			c = (char) (root + width);
		return c;
	}

	/**
	 * A description of the range of strings covered by this index key.
	 * Currently we describe nothing, since we have only resolved the
	 * string->key mapping, not the reverse for this class.
	 */
	public String getRangeText(int key) {
		return "key[" + key + "] range[" + getMin(key) + "," + getMax(key) + "]";
	}

	public String average(String a, String b) {
		char[] ac = a.toCharArray();
		char[] bc = b.toCharArray();
		char[] ans = new char[Math.max(ac.length, bc.length)];
		for (int i = 0; i < ans.length; i++) {
			int x = (i < ac.length ? ac[i] : root) + (i < bc.length ? bc[i] : root);
			ans[i] = (char) (x / 2);
		}
		return String.valueOf(ans);
	}

	public String getMin() {
		return min;
	}

	public String getMax() {
		return max;
	}

	public String getOrigin() {
		return origin;
	}

	public int getDepth() {
		return depth;
	}

	/** Get the minimum value for the specific index */
	public String getMin(int key) {
		return getBase(key) + String.valueOf(root);
	}

	/** Get the maximum value for the specific index */
	public String getMax(int key) {
		return getBase(key) + String.valueOf((char) (root + width));
	}

	/**
	 * Calculate the prefix for the specific key.
	 * 
	 * <pre>
	 * b[i] = floor( (k - b[i-1]) / width^i )
	 * r[i] = b[i] + o[i]
	 * </pre>
	 * 
	 * Where i is the position in the character string, from 0 to depth, and so
	 * r is the complete string returned, made up of a string of r[i]
	 * components.
	 * 
	 * @param key
	 * @return prefix for this key
	 */
	public String getBase(int key) {
		char[] data = new char[origin.length()];
		Arrays.fill(data, root);
		double base = key - calcKey(String.valueOf(data));
		double b = 0;
		double widthFactor = Math.pow(width, depth - 1);
		for (int i = 0; i < depth; i++) {
			base -= b;
			b = (int) (base / widthFactor);
			if (b < 0) {
				data[i] = (char) (root + width + b);
			} else {
				data[i] = (char) (root + b);
			}
			b *= widthFactor;
			widthFactor /= width;
		}
		return String.valueOf(data);
	}

	/**
	 * This is the heart of the mapper, where the string is converted to a index
	 * key. The algorithm is simple. k = SUM[i=0:depth]( (s[i] - o[i]) * width^i
	 * ). So for i=0, we have simply k = s - o, but for i=1
	 */
	public int toKey(Object obj) {
		String value = (String) obj;
		min = value.compareTo(min) < 0 ? value : min;
		max = value.compareTo(max) > 0 ? value : max;
		return (int) calcKey(value);
	}

	/**
	 * Even thought the point of the index is to allow for a small range of key
	 * values, and int (or even short) should be fine, and the API specifies
	 * int, we find that due to the theoretical ranges possible, and the
	 * subsequently very large values calculated especially with large depths,
	 * and big differences between root and origin, or data point and origin, we
	 * need to do internal calculations as doubles, and map back to int only on
	 * final usage. This method is particularly necessary because it is called
	 * from the getBase method which needs to deal with the difference between
	 * the root and the origin, which can often be large.
	 * 
	 * @param value
	 * @return
	 */
	private double calcKey(String value) {
		double key = 0;
		double widthFactor = 1;
		for (int i = depth - 1; i >= 0; i--) {
			char o = originChars[i];
			char part = o;
			if (i < value.length())
				part = value.charAt(i);
			part = limitChar(part);
			int diff = part - o;
			key += widthFactor * diff;
			widthFactor *= width;
		}
		return key;
	}

	/**
	 * Return the number of categories, or possible unique nodes at level 0. For
	 * this index it is always the depth * width. This means for depth 1
	 * indexes, which differentiate only on the first character, the default
	 * width is 94 (or the gap from ' ' to '~') leading to exactly 94
	 * categories. If we move to depth 2, there are 94*94 (or 94^2 = 8836)
	 * categories, which is the default for this mapper. If this mapper is then
	 * used in a step 10 index tree, there will be up to 4 levels to deal with
	 * all possible combinations.
	 */
	public int getCategories() {
		return (int) Math.pow(width, depth);
	}

}
