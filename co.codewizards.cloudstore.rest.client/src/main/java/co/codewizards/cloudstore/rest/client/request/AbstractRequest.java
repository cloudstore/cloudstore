package co.codewizards.cloudstore.rest.client.request;

import static java.util.Objects.*;

import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RemoteException;
import co.codewizards.cloudstore.core.dto.RemoteExceptionUtil;
import co.codewizards.cloudstore.core.util.UrlEncoder;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;

/**
 * Abstract base class for REST requests.
 * <p>
 * Implementors are encouraged to sub-class {@code AbstractRequest} or {@link VoidRequest} instead of
 * directly implementing {@link Request}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <R> the response type, i.e. the type of the object sent from the server back to the client.
 */
public abstract class AbstractRequest<R> implements Request<R> {
	private static final Logger logger = LoggerFactory.getLogger(AbstractRequest.class);

	private CloudStoreRestClient cloudStoreRestClient;

	@Override
	public CloudStoreRestClient getCloudStoreRestClient() {
		return cloudStoreRestClient;
	}

	@Override
	public void setCloudStoreRestClient(final CloudStoreRestClient cloudStoreRestClient) {
		this.cloudStoreRestClient = cloudStoreRestClient;
	}

	/**
	 * Gets the {@link CloudStoreRestClient} or throws an exception, if it was not assigned.
	 * <p>
	 * Implementors are encouraged to use this method instead of {@link #getCloudStoreRestClient()} in their
	 * {@link #execute()} method.
	 * @return the {@link CloudStoreRestClient}. Never <code>null</code>.
	 */
	protected CloudStoreRestClient getCloudStoreRestClientOrFail() {
		final CloudStoreRestClient cloudStoreRestClient = getCloudStoreRestClient();
		requireNonNull(cloudStoreRestClient, "cloudStoreRestClient");
		return cloudStoreRestClient;
	}

//	protected void handleException(final RuntimeException x) {
//		getCloudStoreRestClientOrFail().handleAndRethrowException(x);
//	}

	protected Invocation.Builder assignCredentials(final Invocation.Builder builder) {
		return getCloudStoreRestClientOrFail().assignCredentials(builder);
	}

	protected String getPath(final Class<?> dtoClass) {
		return "_" + dtoClass.getSimpleName();
	}

	/**
	 * Encodes the given {@code string}.
	 * <p>
	 * This method does <i>not</i> use {@link java.net.URLEncoder URLEncoder}, because of
	 * <a href="https://java.net/jira/browse/JERSEY-417">JERSEY-417</a>.
	 * <p>
	 * The result of this method can be used in both URL-paths and URL-query-parameters.
	 * @param string the {@code String} to be encoded. Must not be <code>null</code>.
	 * @return the encoded {@code String}.
	 */
	protected static String urlEncode(final String string) {
		requireNonNull(string, "string");
		// This UriComponent method is safe. It does not try to handle the '{' and '}'
		// specially and with type PATH_SEGMENT, it encodes spaces using '%20' instead of '+'.
		// It can therefore be used for *both* path segments *and* query parameters.
//		return org.glassfish.jersey.uri.UriComponent.encode(string, UriComponent.Type.PATH_SEGMENT);
		return UrlEncoder.encode(string);
	}

	/**
	 * Create a {@link WebTarget} from the given path segments.
	 * <p>
	 * This method prefixes the path with the {@link #getBaseURL() base-URL} and appends
	 * all path segments separated via slashes ('/').
	 * <p>
	 * We do not use <code>client.target(getBaseURL()).path("...")</code>, because the
	 * {@link WebTarget#path(String) path(...)} method does not encode curly braces
	 * (which might be part of a file name!).
	 * Instead it resolves them using {@linkplain WebTarget#matrixParam(String, Object...) matrix-parameters}.
	 * The matrix-parameters need to be encoded manually, too (at least I tried it and it failed, if I didn't).
	 * Because of these reasons and in order to make the calls more compact, we assemble the path
	 * ourselves here.
	 * @param pathSegments the parts of the path. May be <code>null</code>. The path segments are
	 * appended to the path as they are. They are not encoded at all! If you require encoding,
	 * use {@link #encodePath(String)} or {@link #urlEncode(String)} before! Furthermore, all path segments
	 * are separated with a slash inbetween them, but <i>not</i> at the end. If a single path segment
	 * already contains a slash, duplicate slashes might occur.
	 * @return the target. Never <code>null</code>.
	 */
	protected WebTarget createWebTarget(final String ... pathSegments) {
		final Client client = getClientOrFail();

		final StringBuilder sb = new StringBuilder();
		sb.append(getBaseURL());

		boolean first = true;
		if (pathSegments != null && pathSegments.length != 0) {
			for (final String pathSegment : pathSegments) {
				if (!first) // the base-URL already ends with a slash!
					sb.append('/');
				first = false;
				sb.append(pathSegment);
			}
		}

		final WebTarget webTarget = client.target(URI.create(sb.toString()));
		return webTarget;
	}

	/**
	 * Get the server's base-URL.
	 * <p>
	 * This base-URL is the base of the <code>CloudStoreREST</code> application. Hence all URLs
	 * beneath this base-URL are processed by the <code>CloudStoreREST</code> application.
	 * <p>
	 * In other words: All repository-names are located directly beneath this base-URL. The special services, too,
	 * are located directly beneath this base-URL.
	 * <p>
	 * For example, if the server's base-URL is "https://host.domain:8443/", then the test-service is
	 * available via "https://host.domain:8443/_test" and the repository with the alias "myrepo" is
	 * "https://host.domain:8443/myrepo".
	 * @return the base-URL. This URL always ends with "/".
	 */
	protected String getBaseURL() {
		return getCloudStoreRestClientOrFail().getBaseUrl();
	}

	protected Client getClientOrFail() {
		return getCloudStoreRestClientOrFail().getClientOrFail();
	}

	/**
	 * Encodes the given {@code path} (using {@link #urlEncode(String)}) and removes leading &amp; trailing slashes.
	 * <p>
	 * Slashes are not encoded, but retained as they are; only the path segments (the strings between the slashes) are
	 * encoded.
	 * <p>
	 * Duplicate slashes are removed.
	 * <p>
	 * The result of this method can be used in both URL-paths and URL-query-parameters.
	 * <p>
	 * For example the input "/some//ex ample///path/" becomes "some/ex%20ample/path".
	 * @param path the path to be encoded. Must not be <code>null</code>.
	 * @return the encoded path. Never <code>null</code>.
	 */
	protected String encodePath(final String path) {
		requireNonNull(path, "path");

		final StringBuilder sb = new StringBuilder();
		final String[] segments = path.split("/");
		for (final String segment : segments) {
			if (segment.isEmpty())
				continue;

			if (sb.length() != 0)
				sb.append('/');

			sb.append(urlEncode(segment));
		}

		return sb.toString();
	}

	protected void assertResponseIndicatesSuccess(final Response response) {
		if (400 <= response.getStatus() && response.getStatus() <= 599) {
			response.bufferEntity();
			if (response.hasEntity()) {
				Error error = null;
				try {
					error = response.readEntity(Error.class);
				} catch (final Exception y) {
					logger.error("handleException: " + y, y);
				}
				if (error != null) {
					throwOriginalExceptionIfPossible(error);
					throw new RemoteException(error);
				}
			}
			throw new WebApplicationException(response);
		}
	}

	protected void throwOriginalExceptionIfPossible(final Error error) {
		RemoteExceptionUtil.throwOriginalExceptionIfPossible(error);
	}
}
