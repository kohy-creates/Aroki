package xyz.kohara.util;

import java.util.HashMap;
import java.util.Map;

public class Utils {

	public static String ordinal(int i) {
		String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
		return switch (i % 100) {
			case 11, 12, 13 -> i + "th";
			default -> i + suffixes[i % 10];
		};
	}

	public static String smallUnicode(String s) {
		Map<Character, Character> map = new HashMap<>();
		String[] mappings = {"aᴀ", "bʙ", "cᴄ", "dᴅ", "eᴇ", "fꜰ", "gɢ", "hʜ", "iɪ", "jᴊ", "kᴋ", "lʟ", "mᴍ", "nɴ", "oᴏ", "pᴘ", "rʀ", "sѕ", "tᴛ", "uᴜ", "wᴡ", "xх", "yʏ", "zᴢ"};
		for (String pair : mappings) {
			map.put(pair.charAt(0), pair.charAt(1));
		}
		StringBuilder result = new StringBuilder();
		for (char c : s.toCharArray()) {
			result.append(map.getOrDefault(c, c));
		}
		return result.toString();
	}
}
