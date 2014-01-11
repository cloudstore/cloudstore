package co.codewizards.cloudstore.core.persistence;

import javax.jdo.Query;

import co.codewizards.cloudstore.core.dto.EntityID;

public class RemoteRepositoryRequestDAO extends DAO<RemoteRepositoryRequest, RemoteRepositoryRequestDAO> {

	public RemoteRepositoryRequest getRemoteRepositoryRequest(EntityID repositoryID) {
		String repositoryIDString = repositoryID == null ? null : repositoryID.toString();
		Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepositoryRequest_repositoryID");
		try {
			RemoteRepositoryRequest remoteRepositoryRequest = (RemoteRepositoryRequest) query.execute(repositoryIDString);
			return remoteRepositoryRequest;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepositoryRequest getRemoteRepositoryRequestOrFail(EntityID repositoryID) {
		RemoteRepositoryRequest remoteRepositoryRequest = getRemoteRepositoryRequest(repositoryID);
		if (remoteRepositoryRequest == null)
			throw new IllegalArgumentException(String.format("There is no RemoteRepositoryRequest with repositoryID='%s'!", repositoryID));

		return remoteRepositoryRequest;
	}
}
