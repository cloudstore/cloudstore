package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.jdo.Query;

public class RemoteRepositoryRequestDao extends Dao<RemoteRepositoryRequest, RemoteRepositoryRequestDao> {

	public RemoteRepositoryRequest getRemoteRepositoryRequest(UUID repositoryId) {
		String repositoryIdString = repositoryId == null ? null : repositoryId.toString();
		Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepositoryRequest_repositoryId");
		try {
			RemoteRepositoryRequest remoteRepositoryRequest = (RemoteRepositoryRequest) query.execute(repositoryIdString);
			return remoteRepositoryRequest;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepositoryRequest getRemoteRepositoryRequestOrFail(UUID repositoryId) {
		RemoteRepositoryRequest remoteRepositoryRequest = getRemoteRepositoryRequest(repositoryId);
		if (remoteRepositoryRequest == null)
			throw new IllegalArgumentException(String.format("There is no RemoteRepositoryRequest with repositoryId='%s'!", repositoryId));

		return remoteRepositoryRequest;
	}

	public Collection<RemoteRepositoryRequest> getRemoteRepositoryRequestsChangedBefore(Date changed) {
		assertNotNull("changed", changed);
		Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepositoryRequestsChangedBefore_changed");
		try {
			@SuppressWarnings("unchecked")
			Collection<RemoteRepositoryRequest> c = (Collection<RemoteRepositoryRequest>) query.execute(changed);
			return new ArrayList<RemoteRepositoryRequest>(c);
		} finally {
			query.closeAll();
		}
	}
}
