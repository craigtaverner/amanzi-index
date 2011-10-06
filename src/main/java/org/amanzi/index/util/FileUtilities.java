package org.amanzi.index.util;

import java.util.regex.Pattern;

public class FileUtilities {
	
	private final static int DELIMITER_SAMPLE_SIZE = 200;
	
	
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
