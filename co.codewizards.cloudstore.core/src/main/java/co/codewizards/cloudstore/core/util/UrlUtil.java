package co.codewizards.cloudstore.core.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

public final class UrlUtil {

	private UrlUtil() { }

	public static URL canonicalizeURL(URL url) {
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
			String file = query == null ? path : path + '?' + query;
			try {
				result = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	public static File getFile(URL url) {
		try {
			return new File(url.toURI());
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static File getFile(File parent, String urlEncodedPath) {
		return new File(appendPath(parent.toURI(), urlEncodedPath, true));
	}

	/**
	 * @param uri The URI to append to.
	 * @param pathToAppend A relative path. Must not start with a '/' character.
	 * @param pathToAppendIsEncoded
	 * @return
	 */
	public static URI appendPath(final URI uri, String pathToAppend, boolean pathToAppendIsEncoded) {
		if (pathToAppend == null || pathToAppend.length() < 1)
			return uri;
		try {
			if (pathToAppendIsEncoded) {
				pathToAppend = pathToAppend.replaceAll("\\+", "%2b"); // URLDecoder.decode would subset the '+' with ' ';
				pathToAppend = URLDecoder.decode(pathToAppend, "UTF-8");
			}
			if (pathToAppend.startsWith("/") && uri.getPath().endsWith("/"))
				pathToAppend = pathToAppend.substring(1);
			if (!pathToAppend.startsWith("/") && !uri.getPath().endsWith("/"))
				pathToAppend = "/" + pathToAppend;
			return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath() + pathToAppend, uri.getFragment());
		} catch (URISyntaxException | UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
