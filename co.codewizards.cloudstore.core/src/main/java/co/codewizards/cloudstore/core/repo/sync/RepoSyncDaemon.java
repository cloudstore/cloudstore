package co.codewizards.cloudstore.core.repo.sync;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;

public interface RepoSyncDaemon {

	UUID startSync(File file);

	void shutdown();

	void shutdownNow();

	List<RepoSyncState> getSyncStates(UUID localRepositoryId);

	void removeSyncStates(Collection<RepoSyncState> repoSyncStates);

}