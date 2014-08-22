package co.codewizards.cloudstore.util;

import java.nio.charset.Charset;

public final class IOUtil {
	/**
	 * UTF-8 caracter set name.
	 */
	public static final String CHARSET_NAME_UTF_8 = "UTF-8";

	/**
	 * UTF-8 caracter set.
	 */
	public static final Charset CHARSET_UTF_8 = Charset.forName(CHARSET_NAME_UTF_8);
}