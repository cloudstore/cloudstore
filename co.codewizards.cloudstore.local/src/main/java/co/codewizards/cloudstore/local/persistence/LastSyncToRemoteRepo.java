package co.codewizards.cloudstore.local.persistence;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

@PersistenceCapable
@Unique(name="LastSyncToRemoteRepo_remoteRepository", members="remoteRepository")
@Query(name="getLastSyncToRemoteRepo_remoteRepository", value="SELECT UNIQUE WHERE this.remoteRepository == :remoteRepository")
public class LastSyncToRemoteRepo extends Entity {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private RemoteRepository remoteRepository;
	private long localRepositoryRevisionSynced = -1;
	private long localRepositoryRevisionInProgress = -1;

	public RemoteRepository getRemoteRepository() {
		return remoteRepository;
	}
	public void setRemoteRepository(RemoteRepository remoteRepository) {
		this.remoteRepository = remoteRepository;
	}

	/**
	 * Gets the {@link LocalRepository#getRevision() LocalRepository.revision} that
	 * was synced to the remote repository.
	 * <p>
	 * This means all local changes with a {@link AutoTrackLocalRevision#getLocalRevision() localRevision}
	 * greater than (&gt;) this revision are not yet sent to the remote repo.
	 * @return the {@link LocalRepository#getRevision() LocalRepository.revision} that
	 * was synced to the remote repository.
	 */
	public long getLocalRepositoryRevisionSynced() {
		return localRepositoryRevisionSynced;
	}
	public void setLocalRepositoryRevisionSynced(long localRepositoryRevision) {
		this.localRepositoryRevisionSynced = localRepositoryRevision;
	}

	public long getLocalRepositoryRevisionInProgress() {
		return localRepositoryRevisionInProgress;
	}
	public void setLocalRepositoryRevisionInProgress(long localRepositoryRevisionInProgress) {
		this.localRepositoryRevisionInProgress = localRepositoryRevisionInProgress;
	}
}
