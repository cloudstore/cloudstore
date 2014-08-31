package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.codewizards.cloudstore.core.oio.File;

public final class UrlUtil {

	private UrlUtil() { }

	public static URL canonicalizeURL(final URL url) {
		if (url == null)
			return null;

		URL result = url;

		String query = url.getQuery();
		if (query != null && query.isEmpty()) {
			query = null;
			result = null;
		}

		String path = url.getPath();
		while (path.endsWith("/")) {
			 path = path.substring(0, path.length() - 1);
			 result = null;
		}

		if (result == null) {
			final String file = query == null ? path : path + '?' + query;
			try {
				result = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
			} catch (final MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	public static File getFile(final URL url) {
		try {
			return createFile(url.toURI());
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Appends the {@linkplain URLEncoder URL-encoded} {@code path} to the given
	 * base {@code url}.
	 * @param url the URL to be appended. Must not be <code>null</code>.
	 * @param path the path to append. May be <code>null</code>. It is assumed that this
	 * path is already encoded. It is therefore <b>not</b> modified at all and appended
	 * as-is.
	 * @return the URL composed of the prefix {@code url} and the suffix {@code path}.
	 * @see #appendNonEncodedPath(URL, String)
	 */
	public static URL appendEncodedPath(final URL url, final String path) {
		AssertUtil.assertNotNull("url", url);
		if (path == null || path.isEmpty())
			return url;

		return appendEncodedPath(url, Collections.singletonList(path));
	}

	/**
	 * Appends the plain {@code path} to the given base {@code url}.
	 * <p>
	 * Each path segment (the text between '/') is separately {@linkplain URLEncoder URL-encoded}. A
	 * '/' itself is therefore conserved and not encoded.
	 * @param url the URL to be appended. Must not be <code>null</code>.
	 * @param path the path to append. May be <code>null</code>.
	 * @return the URL composed of the prefix {@code url} and the suffix {@code path}.
	 * @see #appendEncodedPath(URL, String)
	 */
	public static URL appendNonEncodedPath(final URL url, final String path) {
		AssertUtil.assertNotNull("url", url);
		if (path == null || path.isEmpty())
			return url;

		final String[] pathSegments = path.split("/");
		final List<String> encodedPathSegments = new ArrayList<String>(pathSegments.length);
		for (final String pathSegment : pathSegments) {
			try {
				encodedPathSegments.add(URLEncoder.encode(pathSegment, CHARSET_NAME_UTF_8));
			} catch (final UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return appendEncodedPath(url, encodedPathSegments);
	}

	private static URL appendEncodedPath(final URL url, final List<String> pathSegments) {
		AssertUtil.assertNotNull("url", url);

		if (pathSegments == null || pathSegments.isEmpty())
			return url;

		try {
			final StringBuilder urlString = new StringBuilder(url.toExternalForm());

			for (final String ps : pathSegments) {
				if (ps == null || ps.isEmpty())
					continue;

				if (ps.startsWith("/") && getLastChar(urlString) == '/')
					urlString.append(ps.substring(1));
				else if (!ps.startsWith("/") && getLastChar(urlString) != '/')
					urlString.append('/').append(ps);
				else
					urlString.append(ps);
			}

			return new URL(urlString.toString());
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static char getLastChar(final StringBuilder stringBuilder) {
		AssertUtil.assertNotNull("stringBuilder", stringBuilder);

		final int index = stringBuilder.length() - 1;
		if (index < 0)
			return 0;

		return stringBuilder.charAt(index);
	}

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
}
