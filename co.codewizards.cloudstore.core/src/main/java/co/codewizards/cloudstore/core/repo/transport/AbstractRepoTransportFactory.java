package co.codewizards.cloudstore.core.repo.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;
import java.util.UUID;

import co.codewizards.cloudstore.core.util.AssertUtil;

public abstract class AbstractRepoTransportFactory implements RepoTransportFactory {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public RepoTransport createRepoTransport(URL remoteRoot, UUID clientRepositoryId) {
		AssertUtil.assertNotNull(remoteRoot, "remoteRoot");
		// clientRepositoryId may be null!
		RepoTransport repoTransport = _createRepoTransport();
		if (repoTransport == null)
			throw new IllegalStateException(String.format("Implementation error in class %s: _createRepoTransport(...) returned null!", this.getClass().getName()));

		repoTransport.setRepoTransportFactory(this);
		repoTransport.setRemoteRoot(remoteRoot);
		repoTransport.setClientRepositoryId(clientRepositoryId);
		return repoTransport;
	}

	protected abstract RepoTransport _createRepoTransport();
}
