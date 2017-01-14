package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;

class RepoSyncQueueItem {
	public final UUID repositoryId;
	public final File localRoot;
	// TODO later, we should allow for syncing only a certain directory or even file

	public RepoSyncQueueItem(final UUID repositoryId, final File localRoot) {
		this.repositoryId = assertNotNull(repositoryId, "repositoryId");
		this.localRoot = assertNotNull(localRoot, "localRoot");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((repositoryId == null) ? 0 : repositoryId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final RepoSyncQueueItem other = (RepoSyncQueueItem) obj;
		return equal(this.repositoryId, other.repositoryId);
	}
}