package org.amanzi.index.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.amanzi.index.util.CollectionUtilities;
import org.amanzi.index.util.IndexUtilities;


public class ListStringMapper implements Mapper<String> {
	public static final char DEFAULT_ROOT = ' ';
	public static final int DEFAULT_WIDTH = 94;
	public static final String DEFAULT_ORIGIN = "A";
	
	public static final int DEFAULT_GAP = 20;
	public static final int DEFAULT_SIZE = 37; // A-Z, 0-9, other
	public static final int DEFAULT_OFFSET = 0;
	
	protected String min = "";
	protected String max = "";
	protected String origin = DEFAULT_ORIGIN;
	protected char[] originChars;
	
	protected HashMap<Integer, String> keyList;
	protected HashMap<Integer, ArrayList<String>> extraKeyList;
	protected int[] counter;
	protected int counterSize = DEFAULT_SIZE;
	protected int gap = DEFAULT_GAP;
	protected int offset = DEFAULT_OFFSET;
	protected int width = DEFAULT_WIDTH;
	protected char root = DEFAULT_ROOT;

	/**
	 * Create a string mapper based on the provided gap, offset, root and
	 * width.
	 */
	public static ListStringMapper withConfig(Collection<String> data, int gap, int offset, char root, int width) {
		return new ListStringMapper(data, gap, offset, root, width);
	}

	/**
	 * Create a string mapper based on the sample data provided.
	 */
	public static ListStringMapper withSample(Collection<String> data) {
		return new ListStringMapper(data, DEFAULT_GAP, DEFAULT_OFFSET, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	/**
	 * Create a string mapper based on the restore data provided.
	 */
	public static ListStringMapper withRestore(HashMap<Integer, String> map1, HashMap<Integer, ArrayList<String>> map2, int[] count) {
		return new ListStringMapper(map1, map2, count, DEFAULT_GAP, DEFAULT_OFFSET, DEFAULT_ROOT, DEFAULT_WIDTH);
	}

	
	/**
	 * Create a string mapper based on the sample data provided.
	 */
	public static ListStringMapper withSampleGap(Collection<String> data, int gap) {
		return new ListStringMapper(data, gap, DEFAULT_OFFSET, DEFAULT_ROOT, DEFAULT_WIDTH);
	}
	
	/**
	 * Create a string mapper based on the restore data provided.
	 */
	public static ListStringMapper withRestoreGap(HashMap<Integer, String> map1, HashMap<Integer, ArrayList<String>> map2, int[] count, int gap) {
		return new ListStringMapper(map1, map2, count, gap, DEFAULT_OFFSET, DEFAULT_ROOT, DEFAULT_WIDTH);
	}


	/**
	 * The default string mapper.
	 */
	public static ListStringMapper getDefault() {
		return new ListStringMapper(null, DEFAULT_GAP, DEFAULT_OFFSET, DEFAULT_ROOT, DEFAULT_WIDTH);
	}


	/**
	 * Common constructor used by all factory methods for initializing this
	 * mapper.
	 * 
	 */
	protected ListStringMapper(Collection<String> sample, int gap, int offset, char root, int width) {
		// Initialization
		keyList = new HashMap<Integer, String>();
		counter = new int [counterSize]; // Case insensitive
		Arrays.fill(counter, 0);
		this.gap = gap;
		this.offset = offset;
		this.root = root;
		this.width = width;
		
		if (sample != null) {
			Object[] str = sample.toArray();
			min = (String) str[0];
			max = (String) str[str.length - 1];
			
			for (String string : sample) {
				string = string.trim();
				this.toKey(string);
			}
		}
	}
	
	/**
	 * Common restore constructor used by all factory methods for initializing this
	 * mapper.
	 * 
	 */
	protected ListStringMapper(HashMap<Integer, String> map1, HashMap<Integer, 
			ArrayList<String>> map2, int[] count, int gap, int offset, char root, int width) {
		// Initialization
		keyList = map1;
		extraKeyList = map2;
		counter = count;
		this.gap = gap;
		this.offset = offset;
		this.root = root;
		this.width = width;
		
	}

	private char limitChar(char c) {
		if (c < root)
			c = root;
		if (c > root + width)
			c = (char) (root + width);
		return c;
	}
	
	private String limitString(String str) {
		// FIXME: replace reserved characters '[', ']', ','
		return str.replace(',', ';').replace('[', '(').replace(']', ')');
	}

	/**
	 * A description of the range of strings covered by this index key.
	 * Currently we describe nothing, since we have only resolved the
	 * string->key mapping, not the reverse for this class.
	 */
	public String getRangeText(int key) {
		// If the last key entry
		if (key % gap == (gap - 1)) {
			StringBuffer sb = new StringBuffer();
			sb.append(keyList.get(key));
			if (extraKeyList != null) {
				if (extraKeyList.containsKey(key)) {
					for (String value: extraKeyList.get(key)) {
						sb.append(",");
						sb.append(value);
					}
				}
			}
			return "key[" + key + "] range[" + sb + "]";
		} else
			return "key[" + key + "] value[" + keyList.get(key) + "]";
	}

	public String getMin() {
		return min;
	}

	public String getMax() {
		return max;
	}

	public int getGap() {
		return gap;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int toKey(Object obj) {
		String value = limitString((String) obj);
		min = value.compareTo(min) < 0 ? value : min;
		max = value.compareTo(max) > 0 ? value : max;
		return (int) calcKey(value);
	}

	/** Return the text without any changes */
	public String parse(String text) {
		return text;
	}

	@Override
	public int compare(Object a, Object b) {
		return a.toString().compareTo(b.toString());
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	private int calcKey(String value) {
		// value is the 'key' and key is the 'value' for the HashMap
		int key = 0;
		int pos = limitChar(value.charAt(0)) - '0';
		if (pos > 0 && pos < 9) {
			
		} else if (pos > 16 && pos < 43) {
			pos -= 7;
		} else if (pos > 48 && pos < 74) {
			pos -= 39;
		} else {
			pos = counterSize - 1;
		}
		if (!keyList.containsValue(value)) {
			if (counter[pos] < this.gap) {
				key = pos * gap + counter[pos] + this.offset;
				keyList.put(key, value);
				counter[pos] ++;
			} else {
				key = pos * this.gap + this.gap - 1 + this.offset;
				// Only initialize when needed
				if (extraKeyList == null) {
					extraKeyList = new HashMap<Integer, ArrayList<String>>();
				}
				ArrayList<String> extraValues;
				if (extraKeyList.containsKey(key)) {
					extraValues = extraKeyList.get(key);
					if (!extraValues.contains(value)) {
						extraValues.add(value);
					}
				} else {
					extraValues= new ArrayList<String>();
					extraValues.add(value);
				}
				extraKeyList.put(key, extraValues);
			}
		} else {
			// Get the key from the value (one-to-one mapping ensured)
			key = CollectionUtilities.getKeyFromValue(keyList.entrySet(), value);
		}
		return key;
	}

	/**
	 * Return the number of categories, or possible unique nodes at level 0.
	 */
	public int getCategories() {
		return (int) counterSize * gap;
	}
	
	public String getKeyListString() {
		return keyList.toString();
	}
	
	public String getExtraKeyListString() {
		return (extraKeyList != null) ? 
				extraKeyList.toString() : "";
	}
	
	public String getCounterString() {
		return IndexUtilities.arrayString(counter);
	}

	@Override
	public String getOrigin() {
		// Origin is not really used in this mapper
		return null;
	}

}
