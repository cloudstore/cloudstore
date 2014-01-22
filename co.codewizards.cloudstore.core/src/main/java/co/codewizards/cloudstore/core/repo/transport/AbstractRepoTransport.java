package co.codewizards.cloudstore.core.repo.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;

import co.codewizards.cloudstore.core.util.UrlUtil;

public abstract class AbstractRepoTransport implements RepoTransport {

	private RepoTransportFactory repoTransportFactory;
	private URL remoteRoot;
	private URL remoteRootWithoutPathPrefix;
	private String pathPrefix;

	@Override
	public RepoTransportFactory getRepoTransportFactory() {
		return repoTransportFactory;
	}

	@Override
	public void setRepoTransportFactory(RepoTransportFactory repoTransportFactory) {
		this.repoTransportFactory = repoTransportFactory;
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
		if (pathPrefix == null) {
			URL rr = getRemoteRoot();
			if (rr == null)
				throw new IllegalStateException("remoteRoot not yet assigned!");

			String remoteRoot = rr.toExternalForm();
			String remoteRootWithoutPathPrefix = getRemoteRootWithoutPathPrefix().toExternalForm();
			if (!remoteRoot.startsWith(remoteRootWithoutPathPrefix))
				throw new IllegalStateException(String.format(
								"remoteRoot='%s' does not start with remoteRootWithoutPathPrefix='%s'",
								remoteRoot, remoteRootWithoutPathPrefix));

			if (remoteRoot.equals(remoteRootWithoutPathPrefix))
				pathPrefix = "";
			else {
				pathPrefix = remoteRoot.substring(remoteRootWithoutPathPrefix.length());
				if (!pathPrefix.startsWith("/"))
					pathPrefix = '/' + pathPrefix;

				if (pathPrefix.endsWith("/"))
					throw new IllegalStateException("pathPrefix should not end with '/', but it does!");
			}

			this.pathPrefix = pathPrefix;
		}
		return pathPrefix;
	}

	protected String prefixPath(String path) {
		assertNotNull("path", path);
		if ("".equals(path) || "/".equals(path))
			return getPathPrefix();
		if (path.startsWith("/"))
			return getPathPrefix() + path;
		else
			return getPathPrefix() + '/' + path;
	}

	protected String unprefixPath(String path) {
		assertNotNull("path", path);
		String pathPrefix = getPathPrefix();
		if (pathPrefix.isEmpty())
			return path;

		if (!path.startsWith(pathPrefix))
			throw new IllegalArgumentException(String.format("path='%s' does not start with pathPrefix='%s'!", path, pathPrefix));

		String result = path.substring(pathPrefix.length());
		if (!result.isEmpty() && !result.startsWith("/"))
			throw new IllegalStateException(String.format("pathAfterPathPrefix='%s' is neither empty nor does it start with a '/'! path='%s' pathPrefix='%s'", result, path, pathPrefix));

		return result;
	}

	protected boolean isPathUnderPathPrefix(String path) {
		assertNotNull("path", path);
		String pathPrefix = getPathPrefix();
		if (pathPrefix.isEmpty())
			return true;

		return path.startsWith(pathPrefix);
	}
}
