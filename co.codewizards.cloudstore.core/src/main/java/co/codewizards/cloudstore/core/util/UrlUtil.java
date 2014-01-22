package co.codewizards.cloudstore.core.util;

import java.net.MalformedURLException;
import java.net.URL;

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

}
