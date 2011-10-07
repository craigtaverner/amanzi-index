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
	
	public static long dateStringLong(String timeline, int [] ymd) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d HH:mm:ss.SSS");
		Date date;
		String time = "";
		if (timeline.length() > 12) {
			// FIXME: Time in the full length
			time = timeline;
		} else {
			if (ymd.length == 3) {
				// By default, the Time value string format is HH:MM:SS.MS
				// Add "0" at the end for time format parsing: 
				// HH:mm:ss.SS -> HH:mm:ss.SSS
				time = ymd[0] + "-" + ymd[1] + "-" + ymd[2]+ " " + 
				((timeline.length() == 12) ? timeline : (timeline + "0"));
			} else {
				System.err.println("Unexpected date format encountered, invalid date!");
			}
		}
		try {
			date = dateFormat.parse(time);
			return date.getTime();
		} catch (ParseException e) {
			System.err.println("Unexpected date format encountered!");
			e.printStackTrace();
		}
		return 0;
		
	}

}
