package co.codewizards.cloudstore.core.repo.transport;

import java.util.UUID;

import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;

public interface LocalRepoTransport extends RepoTransport, ContextWithLocalRepoManager {

	boolean isTransferDone(UUID fromRepositoryId, UUID toRepositoryId, TransferDoneMarkerType transferDoneMarkerType, long fromEntityId, long fromLocalRevision);

	void markTransferDone(UUID fromRepositoryId, UUID toRepositoryId, TransferDoneMarkerType transferDoneMarkerType, long fromEntityId, long fromLocalRevision);

}
