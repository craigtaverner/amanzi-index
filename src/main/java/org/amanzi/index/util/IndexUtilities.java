package org.amanzi.index.util;

public class IndexUtilities {

	public static String arrayString(int[] test) {
		StringBuffer sb = new StringBuffer();
		for (int i : test)
			addInteger(sb, i);
		sb.append("]");
		return sb.toString();
	}

	private static void addInteger(StringBuffer sb, int i) {
		if (sb.length() == 0) {
			sb.append("[");
		} else {
			sb.append(",");
		}
		sb.append(i);
	}

	public static String arrayString(Integer[] test) {
		StringBuffer sb = new StringBuffer();
		for (int i : test)
			addInteger(sb, i);
		sb.append("]");
		return sb.toString();
	}

}
