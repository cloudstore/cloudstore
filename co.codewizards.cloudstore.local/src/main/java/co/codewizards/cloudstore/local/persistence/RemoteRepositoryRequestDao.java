package co.codewizards.cloudstore.local.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.jdo.Query;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class RemoteRepositoryRequestDao extends Dao<RemoteRepositoryRequest, RemoteRepositoryRequestDao> {

	public RemoteRepositoryRequest getRemoteRepositoryRequest(final UUID repositoryId) {
		final String repositoryIdString = repositoryId == null ? null : repositoryId.toString();
		final Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepositoryRequest_repositoryId");
		try {
			final RemoteRepositoryRequest remoteRepositoryRequest = (RemoteRepositoryRequest) query.execute(repositoryIdString);
			return remoteRepositoryRequest;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepositoryRequest getRemoteRepositoryRequestOrFail(final UUID repositoryId) {
		final RemoteRepositoryRequest remoteRepositoryRequest = getRemoteRepositoryRequest(repositoryId);
		if (remoteRepositoryRequest == null)
			throw new IllegalArgumentException(String.format("There is no RemoteRepositoryRequest with repositoryId='%s'!", repositoryId));

		return remoteRepositoryRequest;
	}

	public Collection<RemoteRepositoryRequest> getRemoteRepositoryRequestsChangedBefore(final Date changed) {
		AssertUtil.assertNotNull("changed", changed);
		final Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepositoryRequestsChangedBefore_changed");
		try {
			@SuppressWarnings("unchecked")
			final
			Collection<RemoteRepositoryRequest> c = (Collection<RemoteRepositoryRequest>) query.execute(changed);
			return new ArrayList<RemoteRepositoryRequest>(c);
		} finally {
			query.closeAll();
		}
	}
}
