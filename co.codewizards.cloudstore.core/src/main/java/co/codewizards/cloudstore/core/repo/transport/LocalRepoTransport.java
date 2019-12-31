package co.codewizards.cloudstore.core.repo.transport;

import java.util.Set;
import java.util.UUID;

import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;

public interface LocalRepoTransport extends RepoTransport, ContextWithLocalRepoManager {

//	boolean isTransferDone(UUID fromRepositoryId, UUID toRepositoryId, TransferDoneMarkerType transferDoneMarkerType,
//			long fromEntityId, long fromLocalRevision);
//
//	void markTransferDone(UUID fromRepositoryId, UUID toRepositoryId, TransferDoneMarkerType transferDoneMarkerType,
//			long fromEntityId, long fromLocalRevision);

	/**
	 * Before transferring a file, mark it to be 'inProgress' for this specific from-to connection. In case of an
	 * interruption of a sync, the next sync is aware of the situation.
	 * @param inProgress True will set the marker, false will remove it.
	 */
	void markFileInProgress(UUID fromRepository, UUID toRepository, String path, boolean inProgress);

	Set<String> getFileInProgressPaths(UUID fromRepository, UUID toRepository);

}
