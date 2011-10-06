package org.amanzi.index.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class CollectionUtilities {

	/**
	 * A static HashMap creator from a formatted string, 
	 * the HashMap is specially used for the index mapper,
	 * with the string(data of the node), and the integer
	 * (index key of the node). 
	 * @param str {k1=v1, k2=v2, ...}
	 * @return
	 */
	public static HashMap<Integer, String> StringToHashMap(String str) {
		HashMap<Integer, String> keyMap = new HashMap<Integer, String>();
		String [] entry = removeBracketPair(str).split(", ");
		for (String value : entry) {
			String[] kv = value.split("=");
			keyMap.put(Integer.parseInt(kv[0]), kv[1]);
		}
		return keyMap;
	}
	
	public static HashMap<Integer, ArrayList<String>> StringToHashMapExtra(String str) {
		HashMap<Integer, ArrayList<String>> keyMap = new HashMap<Integer, ArrayList<String>>();
		String [] entry = removeBracketPair(str).split("\\], ");
		for (String value : entry) {
			String[] kv = value.split("=\\[");
			keyMap.put(Integer.parseInt(kv[0]), 
					StringArrayToArrayList(removeRightBracket(kv[1]).split(", ")));
		}
		return keyMap;
	}
	
	public static int getKeyFromValue(Set<Entry<Integer, String>> set, String value) {
		for (Entry<Integer, String> entry : set) {
			if (entry.getValue().equalsIgnoreCase(value)) {
				return entry.getKey();
			}
		}
		return 0;
	}

	public static int [] StringtoIntArray(String str) {
		String [] values = removeBracketPair(str).split(",");
		int [] arr = new int[values.length];
		for(int i = 0; i < values.length; i ++) {
			arr[i] = Integer.parseInt(values[i]);
		}
		return arr;
	}
	
	private static String removeBracketPair(String value) {
		String str = value.trim();
		// Assume existing both brackets
		return str.substring(1, str.length() - 1);
	}
	
	private static String removeLeftBracket(String value) {
		return value.substring(1);
	}
	
	private static String removeRightBracket(String value) {
		String str = value.trim();
		return value.substring(0, str.length() - 1);
	}
	
	private static ArrayList<String> StringArrayToArrayList(String[] arr) {
		ArrayList<String> arrayList = new ArrayList<String>();
		for (String str : arr) {
			arrayList.add(str);
		}
		return arrayList;
	}
}
