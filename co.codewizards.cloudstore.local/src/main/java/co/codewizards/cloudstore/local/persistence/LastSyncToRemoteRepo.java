package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

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

//	@Column(defaultValue = "N") // does not work with PostgreSQL! In the long term, we likely need a different way of handling upgrades and downward-compatiblity.
	private boolean resyncMode;

	public RemoteRepository getRemoteRepository() {
		return remoteRepository;
	}
	public void setRemoteRepository(RemoteRepository remoteRepository) {
		if (! equal(this.remoteRepository, remoteRepository))
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
		if (! equal(this.localRepositoryRevisionSynced, localRepositoryRevision))
			this.localRepositoryRevisionSynced = localRepositoryRevision;
	}

	public long getLocalRepositoryRevisionInProgress() {
		return localRepositoryRevisionInProgress;
	}
	public void setLocalRepositoryRevisionInProgress(long localRepositoryRevisionInProgress) {
		if (! equal(this.localRepositoryRevisionInProgress, localRepositoryRevisionInProgress))
			this.localRepositoryRevisionInProgress = localRepositoryRevisionInProgress;
	}

	public boolean isResyncMode() {
		return resyncMode;
	}
	public void setResyncMode(boolean resyncMode) {
		this.resyncMode = resyncMode;
	}
}
