package org.amanzi.index.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	
	public static String arrayString(String[] test) {
		StringBuffer sb = new StringBuffer();
		for (String i : test)
			addString(sb, i);
		sb.append("]");
		return sb.toString();
	}
	
	private static void addString(StringBuffer sb, String i) {
		if (sb.length() == 0) {
			sb.append("[");
		} else {
			sb.append(",");
		}
		sb.append(i);
	}
	
	public static long dateStringLong(String time, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		try {
			Date date = dateFormat.parse(time);
			return date.getTime();
		} catch (ParseException e) {
			System.err.println("Unexpected date format encountered!");
			e.printStackTrace();
		}
		return 0;
		
	}

}
