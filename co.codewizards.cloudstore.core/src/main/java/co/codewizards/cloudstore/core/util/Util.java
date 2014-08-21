package co.codewizards.cloudstore.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

public final class Util {
	private Util() { }

	/**
	 * Convert an URL to an URI.
	 * @param url The URL to cenvert
	 * @return The URI
	 */
	public static final URI urlToUri(final URL url) {
		if (url == null)
			return null;

		try {
			return new URI(url.getProtocol(), url.getAuthority(), url.getPath(), url.getQuery(), url.getRef());
		} catch (final URISyntaxException e) {
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
	public static final String getUserName()
	{
		return System.getProperty("user.name"); //$NON-NLS-1$
	}

	public static final <T> T assertNotNull(final String name, final T object) {
		if (object == null)
			throw new IllegalArgumentException(String.format("%s == null", name));

		return object;
	}

	public static final <T> T[] assertNotNullAndNoNullElement(final String name, final T[] array) {
		assertNotNull(name, array);
		for (int i = 0; i < array.length; i++) {
			if (array[i] == null)
				throw new IllegalArgumentException(String.format("%s[%s] == null", name, i));
		}
		return array;
	}

	public static final <E, T extends Collection<E>> T assertNotNullAndNoNullElement(final String name, final T collection) {
		assertNotNull(name, collection);
		int i = -1;
		for (final E element : collection) {
			++i;
			if (element == null)
				throw new IllegalArgumentException(String.format("%s[%s] == null", name, i));
		}
		return collection;
	}

	public static final boolean equal(final Object one, final Object two) {
		return one == null ? two == null : one.equals(two);
	}

	public static final boolean equal(final boolean one, final boolean two) {
		return one == two;
	}

	public static final boolean equal(final byte one, final byte two) {
		return one == two;
	}

	public static final boolean equal(final short one, final short two) {
		return one == two;
	}

	public static final boolean equal(final int one, final int two) {
		return one == two;
	}

	public static final boolean equal(final long one, final long two) {
		return one == two;
	}

	/**
	 * Does really nothing.
	 * <p>
	 * This method should be used in the catch of a try-catch-block, if there's really no action needed.
	 */
	public static final void doNothing() { }
}
