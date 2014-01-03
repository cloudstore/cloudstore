package co.codewizards.cloudstore.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class Util {
	private Util() { }

	/**
	 * Convert an URL to an URI.
	 * @param url The URL to cenvert
	 * @return The URI
	 */
	public static URI urlToUri(URL url) {
		if (url == null)
			return null;

		try {
			return new URI(url.getProtocol(), url.getAuthority(), url.getPath(), url.getQuery(), url.getRef());
		} catch (URISyntaxException e) {
			// Since every URL is an URI, its transformation should never fail. But if it does, we rethrow.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the user name of the user who is currently authenticated at the operating system.
	 * This method simply calls <code>System.getProperty("user.name");</code>.
	 *
	 * @return the user name of the current operating system user.
	 */
	public static String getUserName()
	{
		return System.getProperty("user.name"); //$NON-NLS-1$
	}

	public static <T> T assertNotNull(String name, T object) {
		if (object == null)
			throw new IllegalArgumentException(String.format("%s == null", name));

		return object;
	}

	public static boolean equals(Object one, Object two) {
		return one == null ? two == null : one.equals(two);
	}
}
