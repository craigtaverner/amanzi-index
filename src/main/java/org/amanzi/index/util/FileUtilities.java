package org.amanzi.index.util;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtilities {
	
	private final static int DELIMITER_SAMPLE_SIZE = 200;
	
	public static int [] getDefaultYearMonthDay(String fileName) {
		int [] date = {-1, -1, -1};
		// By default, the file name should be MMDD_No.txt
		String [] tmp = fileName.split("/");
		String day = tmp[tmp.length - 1].split("\\_")[0];
		if (day.length() == 4) {
			date [0] = Calendar.getInstance().get(Calendar.YEAR);
			date [1] = Integer.parseInt(day.substring(0, 2));
			date [2] = Integer.parseInt(day.substring(2, 4));
		} else if (day.length() == 8) {
			date [0] = Integer.parseInt(day.substring(0, 4));
			date [1] = Integer.parseInt(day.substring(4, 6));
			date [2] = Integer.parseInt(day.substring(6, 8));
		} else {
			System.err.println("Unable to extract the date from the file name!");
		}
		return date;
	}
	
	public static String getDelimiter(String str) {
		// Take the sample of the string instead the whole string for better performance
		str = str.substring(0,DELIMITER_SAMPLE_SIZE);
		// Regex
		if (Pattern.compile("[^,.]+,[^,.]+").matcher(str).find()) {
			return ",";
		} else if (Pattern.compile("[^\t.]+\t[^\t.]+").matcher(str).find()) {
			return "\t";
		} else if (Pattern.compile("[^ .]+ [^ .]+").matcher(str).find()) {
			return " ";
		} else
			return null;
	}
}
