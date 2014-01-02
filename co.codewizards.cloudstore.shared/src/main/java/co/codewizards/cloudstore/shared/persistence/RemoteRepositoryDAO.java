package co.codewizards.cloudstore.shared.persistence;

import java.net.URL;

import javax.jdo.Query;

public class RemoteRepositoryDAO extends DAO<RemoteRepository, RemoteRepositoryDAO> {
	public RemoteRepository getRemoteRepository(URL remoteRoot) {
		Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepository_remoteRootSha1");
		try {
			String remoteRootSha1 = RemoteRepository.sha1(remoteRoot);
			RemoteRepository remoteRepository = (RemoteRepository) query.execute(remoteRootSha1);
			return remoteRepository;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepository getRemoteRepositoryOrFail(URL remoteRoot) {
		RemoteRepository remoteRepository = getRemoteRepository(remoteRoot);
		if (remoteRepository == null)
			throw new IllegalArgumentException(String.format("There is no RemoteRepository with remoteRoot='%s'!", remoteRoot));

		return remoteRepository;
	}

}
