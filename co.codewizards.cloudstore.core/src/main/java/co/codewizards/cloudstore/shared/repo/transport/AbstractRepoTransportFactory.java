package co.codewizards.cloudstore.shared.repo.transport;

import java.net.URL;


public abstract class AbstractRepoTransportFactory implements RepoTransportFactory {

	@Override
	public int getPriority() {
		return 500;
	}

	@Override
	public RepoTransport createRepoTransport(URL remoteRoot) {
		RepoTransport repoTransport = _createRepoTransport(remoteRoot);
		if (repoTransport == null)
			throw new IllegalStateException(String.format("Implementation error in class %s: _createRepoTransport(...) returned null!", this.getClass().getName()));

		repoTransport.setRepoTransportFactory(this);
		repoTransport.setRemoteRoot(remoteRoot);
		return repoTransport;
	}

	protected abstract RepoTransport _createRepoTransport(URL remoteRoot);
}
