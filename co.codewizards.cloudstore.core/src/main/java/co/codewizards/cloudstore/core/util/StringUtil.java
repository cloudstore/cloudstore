package co.codewizards.cloudstore.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StringUtil {

	private StringUtil() { }

	public static final List<Integer> getIndexesOf(final String string, final char c) {
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		for (int index = 0; index < string.length(); ++index) {
			if (string.charAt(index) == c)
				indexes.add(index);
		}
		indexes.trimToSize();
		return Collections.unmodifiableList(indexes);
	}

	public static final boolean isEmpty(final String string) {
		return string == null || string.isEmpty();
	}

	public static final String nullToEmpty(final String string) {
		return string == null ? "" : string;
	}

	public static final String emptyToNull(final String string) {
		return isEmpty(string) ? null : string;
	}

	public static final String trim(final String string) {
		return string == null ? null : string.trim();
	}

	public static final String trimLeft(final String string) {
        final int len = string.length();
        int st = 0;

        while ((st < len) && (string.charAt(st) <= ' '))
            st++;

        return st > 0 ? string.substring(st) : string;
    }

	public static final String trimRight(final String string) {
		final int length = string.length();
		int len = length;

        while ((0 < len) && (string.charAt(len - 1) <= ' '))
            len--;

        return len < length ? string.substring(0, len) : string;
    }
}
