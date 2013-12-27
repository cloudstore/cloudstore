package co.codewizards.cloudstore.client.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import co.codewizards.cloudstore.shared.util.IOUtil;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class PathSegment extends RelativePathPart
{
	private String pathSegment;

	public PathSegment() { }

	public PathSegment(String pathSegment) {
		this.pathSegment = pathSegment;
	}

	/**
	 * Create a <code>PathSegment</code> with a <code>String</code>-representable object.
	 * The object must be able to be used as path-segment according to REST rules. This means,
	 * it must have a single-<code>String</code>-constructor or a <code>valueOf(String)</code> method
	 * and its {@link Object#toString() toString()} method must produce a <code>String</code> which is
	 * parseable.
	 * @param pathSegment the object to be converted into a <code>String</code> (via {@link Object#toString()}).
	 */
	public PathSegment(Object pathSegment) {
		this.pathSegment = pathSegment == null ? null : pathSegment.toString();
	}

	public String getPathSegment() {
		return pathSegment;
	}
	public void setPathSegment(String pathSegment) {
		this.pathSegment = pathSegment;
	}

	@Override
	public String toString()
	{
		String ps = pathSegment;
		if (ps == null)
			return "";

		try {
			return URLEncoder.encode(ps, IOUtil.CHARSET_NAME_UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
