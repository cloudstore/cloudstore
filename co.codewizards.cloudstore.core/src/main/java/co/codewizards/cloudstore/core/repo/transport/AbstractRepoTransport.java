package co.codewizards.cloudstore.core.repo.transport;

import java.net.URL;

public abstract class AbstractRepoTransport implements RepoTransport {

	private RepoTransportFactory repoTransportFactory;
	private URL remoteRoot;

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
		final URL rr = this.remoteRoot;
		if (rr != null && !rr.equals(remoteRoot))
			throw new IllegalStateException("Cannot re-assign remoteRoot!");

		this.remoteRoot = remoteRoot;
	}
}
