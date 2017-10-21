package co.codewizards.cloudstore.core.util;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;

public final class UrlUtil {

	private static final Logger logger = LoggerFactory.getLogger(UrlUtil.class);

	public static final String PROTOCOL_FILE = "file";
	public static final String PROTOCOL_JAR = "jar";

	private UrlUtil() { }

	/**
	 * Turns the given {@code url} into a canonical form.
	 * <p>
	 *
	 * @param url the URL to be canonicalized. May be <code>null</code>.
	 * @return the canonicalized URL. Never <code>null</code>, unless the given {@code url}
	 * is <code>null</code>.
	 */
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

		int duplicateSlashIndex = path.indexOf("//");
		while (duplicateSlashIndex >= 0) {
			path = path.substring(0, duplicateSlashIndex) + path.substring(duplicateSlashIndex + 1);

			duplicateSlashIndex = path.indexOf("//");
			result = null;
		}

		if (result == null) {
			String file = query == null ? path : path + '?' + query;
			if (isEmpty(url.getHost()) && isEmpty(file))
				file = "/";

			try {
				result = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
			} catch (final MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	public static File getFile(final URL url) {
		assertNotNull(url, "url");
		if (!url.getProtocol().equalsIgnoreCase(PROTOCOL_FILE))
			throw new IllegalStateException("url does not reference a local file, i.e. it does not start with 'file:': " + url);

		try {
			return createFile(url.toURI());
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Appends the URL-encoded {@code path} to the given base {@code url}.
	 * <p>
	 * This method does <i>not</i> use {@link java.net.URLEncoder URLEncoder}, because of
	 * <a href="https://java.net/jira/browse/JERSEY-417">JERSEY-417</a>.
	 * @param url the URL to be appended. Must not be <code>null</code>.
	 * @param path the path to append. May be <code>null</code>. It is assumed that this
	 * path is already encoded. It is therefore <b>not</b> modified at all and appended
	 * as-is.
	 * @return the URL composed of the prefix {@code url} and the suffix {@code path}.
	 * @see #appendNonEncodedPath(URL, String)
	 */
	public static URL appendEncodedPath(final URL url, final String path) {
		assertNotNull(url, "url");
		if (path == null || path.isEmpty())
			return url;

		return appendEncodedPath(url, Collections.singletonList(path));
	}

	/**
	 * Appends the plain {@code path} to the given base {@code url}.
	 * <p>
	 * Each path segment (the text between '/') is separately {@linkplain UrlEncoder URL-encoded}. A
	 * '/' itself is therefore conserved and not encoded.
	 * @param url the URL to be appended. Must not be <code>null</code>.
	 * @param path the path to append. May be <code>null</code>.
	 * @return the URL composed of the prefix {@code url} and the suffix {@code path}.
	 * @see #appendEncodedPath(URL, String)
	 */
	public static URL appendNonEncodedPath(final URL url, final String path) {
		assertNotNull(url, "url");
		if (path == null || path.isEmpty())
			return url;

		final String[] pathSegments = path.split("/");
		final List<String> encodedPathSegments = new ArrayList<String>(pathSegments.length);
		for (final String pathSegment : pathSegments) {
			encodedPathSegments.add(UrlEncoder.encode(pathSegment));
		}
		return appendEncodedPath(url, encodedPathSegments);
	}

	private static URL appendEncodedPath(final URL url, final List<String> pathSegments) {
		assertNotNull(url, "url");

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
		assertNotNull(stringBuilder, "stringBuilder");

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

	/**
	 * Gets the File referencing the JAR.
	 *
	 * @param url the url to be unwrapped. Must not be <code>null</code>. Must be a JAR-URL (i.e. protocol must be {@link #PROTOCOL_JAR})!
	 * @return the unwrapped URL, i.e. usually the 'file:'-URL pointing to the JAR-URL.
	 */
	public static File getFileFromJarUrl(final URL url) {
		URL fileUrl = getFileUrlFromJarUrl(url);
		return getFile(fileUrl);
	}

	/**
	 * Removes the 'jar:'-prefix and the '!...'-suffix in order to unwrap the 'file:'-URL pointing to the JAR.
	 *
	 * @param url the url to be unwrapped. Must not be <code>null</code>. Must be a JAR-URL (i.e. protocol must be {@link #PROTOCOL_JAR})!
	 * @return the unwrapped URL, i.e. usually the 'file:'-URL pointing to the JAR-URL.
	 */
	public static URL getFileUrlFromJarUrl(final URL url) { // TODO nested JARs not yet supported!
		assertNotNull(url, "url");
		logger.debug("getFileUrlFromJarUrl: url={}", url);
		if (!url.getProtocol().equalsIgnoreCase(PROTOCOL_JAR))
			throw new IllegalArgumentException("url is not starting with 'jar:': " + url);

		String urlStrWithoutJarPrefix = url.getFile();
		final int exclamationMarkIndex = urlStrWithoutJarPrefix.indexOf('!');
		if (exclamationMarkIndex >= 0) {
			urlStrWithoutJarPrefix = urlStrWithoutJarPrefix.substring(0, exclamationMarkIndex);
		}
		try {
			final URL urlWithoutJarPrefixAndSuffix = new URL(urlStrWithoutJarPrefix);
			logger.debug("getFileUrlFromJarUrl: urlWithoutJarPrefixAndSuffix={}", urlWithoutJarPrefixAndSuffix);
			return urlWithoutJarPrefixAndSuffix;
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
