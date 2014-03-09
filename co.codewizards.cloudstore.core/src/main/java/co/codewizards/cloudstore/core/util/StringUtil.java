package co.codewizards.cloudstore.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StringUtil {

	private StringUtil() { }

	public static List<Integer> getIndexesOf(String string, char c) {
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		for (int index = 0; index < string.length(); ++index) {
			if (string.charAt(index) == c)
				indexes.add(index);
		}
		indexes.trimToSize();
		return Collections.unmodifiableList(indexes);
	}

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}
}
