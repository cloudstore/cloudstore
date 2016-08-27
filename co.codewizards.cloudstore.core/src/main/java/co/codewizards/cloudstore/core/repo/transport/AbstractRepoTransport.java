package co.codewizards.cloudstore.core.repo.transport;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.UrlDecoder;
import co.codewizards.cloudstore.core.util.UrlUtil;

public abstract class AbstractRepoTransport implements RepoTransport {
	private static final Logger logger = LoggerFactory.getLogger(AbstractRepoTransport.class);

	private static final String SLASH = "/";

	private RepoTransportFactory repoTransportFactory;
	private URL remoteRoot;
	private URL remoteRootWithoutPathPrefix;
	private String pathPrefix;
	private UUID clientRepositoryId;

	// Don't know, if fillInStackTrace() is necessary, but better do it.
	// I did a small test: 1 million invocations of new Exception() vs. new Exception() with fillInStackTrace(): 3 s vs 2.2 s
	private volatile Throwable repoTransportCreatedStackTraceException = new Exception("repoTransportCreatedStackTraceException").fillInStackTrace();

	@Override
	public RepoTransportFactory getRepoTransportFactory() {
		return repoTransportFactory;
	}

	@Override
	public void setRepoTransportFactory(final RepoTransportFactory repoTransportFactory) {
		this.repoTransportFactory = assertNotNull("repoTransportFactory", repoTransportFactory);
	}

	@Override
	public URL getRemoteRoot() {
		return remoteRoot;
	}
	@Override
	public void setRemoteRoot(URL remoteRoot) {
		remoteRoot = UrlUtil.canonicalizeURL(remoteRoot);
		final URL rr = this.remoteRoot;
		if (rr != null && !rr.equals(remoteRoot))
			throw new IllegalStateException("Cannot re-assign remoteRoot!");

		this.remoteRoot = remoteRoot;
	}

	public UUID getClientRepositoryIdOrFail() {
		final UUID clientRepositoryId = getClientRepositoryId();
		if (clientRepositoryId == null)
			throw new IllegalStateException("clientRepositoryId == null :: You must invoke setClientRepositoryId(...) before!");

		return clientRepositoryId;
	}

	@Override
	public UUID getClientRepositoryId() {
		return clientRepositoryId;
	}
	@Override
	public void setClientRepositoryId(final UUID clientRepositoryId) {
		this.clientRepositoryId = clientRepositoryId;
	}

	@Override
	public URL getRemoteRootWithoutPathPrefix() {
		if (remoteRootWithoutPathPrefix == null) {
			remoteRootWithoutPathPrefix = UrlUtil.canonicalizeURL(determineRemoteRootWithoutPathPrefix());
		}
		return remoteRootWithoutPathPrefix;
	}

	protected abstract URL determineRemoteRootWithoutPathPrefix();

	@Override
	public String getPathPrefix() {
		String pathPrefix = this.pathPrefix;
		if (pathPrefix == null)
			this.pathPrefix = pathPrefix = determinePathPrefix();

		return pathPrefix;
	}

	protected String determinePathPrefix() {
		final URL rr = getRemoteRoot();
		if (rr == null)
			throw new IllegalStateException("remoteRoot not yet assigned!");

		final String remoteRoot = rr.toExternalForm();
		final String remoteRootWithoutPathPrefix = getRemoteRootWithoutPathPrefix().toExternalForm();
		if (!remoteRoot.startsWith(remoteRootWithoutPathPrefix))
			throw new IllegalStateException(String.format(
							"remoteRoot='%s' does not start with remoteRootWithoutPathPrefix='%s'",
							remoteRoot, remoteRootWithoutPathPrefix));

		String urlEncodedPathPrefix;
		if (remoteRoot.equals(remoteRootWithoutPathPrefix))
			urlEncodedPathPrefix = "";
		else {
			urlEncodedPathPrefix = remoteRoot.substring(remoteRootWithoutPathPrefix.length());
			if (!urlEncodedPathPrefix.startsWith(SLASH))
				urlEncodedPathPrefix = SLASH + urlEncodedPathPrefix;

			if (urlEncodedPathPrefix.endsWith(SLASH))
				throw new IllegalStateException("pathPrefix should not end with '" + SLASH + "', but it does!");
		}

		final String pathPrefix = UrlDecoder.decode(urlEncodedPathPrefix);
		return pathPrefix;
	}

	@Override
	public String prefixPath(final String path) {
		assertNotNull("path", path);
		if ("".equals(path) || SLASH.equals(path))
			return getPathPrefix();
		if (path.startsWith(SLASH))
			return getPathPrefix() + path;
		else
			return getPathPrefix() + SLASH + path;
	}

	@Override
	public String unprefixPath(String path) {
		assertNotNull("path", path);
		final String pathPrefix = getPathPrefix();
		if (pathPrefix.isEmpty())
			return path;

		if (!path.startsWith(SLASH))
			path = SLASH + path;

		if (!path.startsWith(pathPrefix))
			throw new IllegalArgumentException(String.format("path='%s' does not start with pathPrefix='%s'!", path, pathPrefix));

		final String result = path.substring(pathPrefix.length());
		if (!result.isEmpty() && !result.startsWith(SLASH))
			throw new IllegalStateException(String.format("pathAfterPathPrefix='%s' is neither empty nor does it start with a '/'! path='%s' pathPrefix='%s'", result, path, pathPrefix));

		return result;
	}

	protected boolean isPathUnderPathPrefix(final String path) {
		assertNotNull("path", path);
		final String pathPrefix = getPathPrefix();
		if (pathPrefix.isEmpty())
			return true;

		return path.startsWith(pathPrefix);
	}

	@Override
	protected void finalize() throws Throwable {
		if (repoTransportCreatedStackTraceException != null) {
			logger.warn("finalize: Detected forgotten close() invocation!", repoTransportCreatedStackTraceException);
		}
		super.finalize();
	}

	@Override
	public void close() {
		repoTransportCreatedStackTraceException = null;
	}

}
