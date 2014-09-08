package co.codewizards.cloudstore.core.repo.transport;

import java.util.UUID;

import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;

public interface LocalRepoTransport extends RepoTransport, ContextWithLocalRepoManager {

	boolean isTransferDone(UUID fromRepositoryId, UUID toRepositoryId, TransferDoneMarkerType transferDoneMarkerType,
			long fromEntityId, long fromLocalRevision);

	void markTransferDone(UUID fromRepositoryId, UUID toRepositoryId, TransferDoneMarkerType transferDoneMarkerType,
			long fromEntityId, long fromLocalRevision);

	/**
	 * Before transferring a file, mark it to be 'inProgress' for this specific from-to connection. In case of an
	 * interruption of a sync, the next sync is aware of the situation.
	 */
	void setFileInProgressMark(UUID fromRepository, UUID toRepository, String path);

	/** If one file transfer has finished, remove the file-in-progress-mark for this path and this from-to direction. */
	void removeFileInProgressMark(UUID fromRepository, UUID toRepository, String path);

	boolean hasFileInProgressMark(UUID fromRepository, UUID toRepository, String path);

}
