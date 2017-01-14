package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.Serializable;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;

@SuppressWarnings("serial")
public class RepoSyncActivity implements Serializable {

	private final UUID localRepositoryId;
	private final File localRoot;
	private final RepoSyncActivityType activityType;

	public RepoSyncActivity(final UUID localRepositoryId, final File localRoot, final RepoSyncActivityType activityType) {
		this.localRepositoryId = assertNotNull(localRepositoryId, "localRepositoryId");
		this.localRoot = assertNotNull(localRoot, "localRoot");
		this.activityType = assertNotNull(activityType, "activityType");
	}

	public UUID getLocalRepositoryId() {
		return localRepositoryId;
	}
	public File getLocalRoot() {
		return localRoot;
	}
	public RepoSyncActivityType getActivityType() {
		return activityType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((localRepositoryId == null) ? 0 : localRepositoryId.hashCode());
		result = prime * result + ((activityType == null) ? 0 : activityType.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final RepoSyncActivity other = (RepoSyncActivity) obj;
		return equal(this.localRepositoryId, other.localRepositoryId) && equal(this.activityType, other.activityType);
	}

	@Override
	public String toString() {
		return String.format("%s[localRepositoryId=%s, localRoot='%s', activityType=%s]",
				getClass().getName(), localRepositoryId, localRoot, activityType);
	}
}
