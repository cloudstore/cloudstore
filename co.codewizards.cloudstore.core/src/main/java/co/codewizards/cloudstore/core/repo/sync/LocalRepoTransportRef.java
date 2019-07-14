package co.codewizards.cloudstore.core.repo.sync;

import java.util.Set;
import java.util.UUID;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.transport.LocalRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.TransferDoneMarkerType;

public class LocalRepoTransportRef extends RepoTransportRef implements LocalRepoTransport {

	@Override
	public LocalRepoTransport getDelegate() {
		return (LocalRepoTransport) super.getDelegate();
	}

	@Override
	public LocalRepoTransport getDelegateOrFail() {
		return (LocalRepoTransport) super.getDelegateOrFail();
	}

	@Override
	public void setDelegate(RepoTransport delegate) {
		if (delegate != null && ! (delegate instanceof LocalRepoTransport))
			throw new IllegalArgumentException("! (delegate instanceof LocalRepoTransport)");

		super.setDelegate(delegate);
	}

	@Override
	public LocalRepoManager getLocalRepoManager() {
		return getDelegateOrFail().getLocalRepoManager();
	}

	@Override
	public boolean isTransferDone(UUID fromRepositoryId, UUID toRepositoryId, TransferDoneMarkerType transferDoneMarkerType, long fromEntityId, long fromLocalRevision) {
		return getDelegateOrFail().isTransferDone(fromRepositoryId, toRepositoryId, transferDoneMarkerType, fromEntityId, fromLocalRevision);
	}

	@Override
	public void markTransferDone(UUID fromRepositoryId, UUID toRepositoryId, TransferDoneMarkerType transferDoneMarkerType, long fromEntityId, long fromLocalRevision) {
		getDelegateOrFail().markTransferDone(fromRepositoryId, toRepositoryId, transferDoneMarkerType, fromEntityId, fromLocalRevision);
	}

	@Override
	public void markFileInProgress(UUID fromRepository, UUID toRepository, String path, boolean inProgress) {
		getDelegateOrFail().markFileInProgress(fromRepository, toRepository, path, inProgress);
	}

	@Override
	public Set<String> getFileInProgressPaths(UUID fromRepository, UUID toRepository) {
		return getDelegateOrFail().getFileInProgressPaths(fromRepository, toRepository);
	}
}
