package co.codewizards.cloudstore.local.persistence;

import static java.util.Objects.*;

import javax.jdo.Query;

public class LastSyncToRemoteRepoDao extends Dao<LastSyncToRemoteRepo, LastSyncToRemoteRepoDao> {

	public LastSyncToRemoteRepo getLastSyncToRemoteRepo(final RemoteRepository remoteRepository) {
		requireNonNull(remoteRepository, "remoteRepository");
		final Query query = pm().newNamedQuery(getEntityClass(), "getLastSyncToRemoteRepo_remoteRepository");
		try {
			final LastSyncToRemoteRepo lastSyncToRemoteRepo = (LastSyncToRemoteRepo) query.execute(remoteRepository);
			return lastSyncToRemoteRepo;
		} finally {
			query.closeAll();
		}
	}

	public LastSyncToRemoteRepo getLastSyncToRemoteRepoOrFail(final RemoteRepository remoteRepository) {
		final LastSyncToRemoteRepo lastSyncToRemoteRepo = getLastSyncToRemoteRepo(remoteRepository);
		if (lastSyncToRemoteRepo == null)
			throw new IllegalStateException("There is no LastSyncToRemoteRepo for the RemoteRepository with repositoryId=" + remoteRepository.getRepositoryId());

		return lastSyncToRemoteRepo;
	}
}
