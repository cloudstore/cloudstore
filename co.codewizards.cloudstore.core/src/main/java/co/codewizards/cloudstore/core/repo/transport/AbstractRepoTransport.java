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
	public void setRemoteRoot(URL remoteRoot) {
		this.remoteRoot = remoteRoot;
	}
}
