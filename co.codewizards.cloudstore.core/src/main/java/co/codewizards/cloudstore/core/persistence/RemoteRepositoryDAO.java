package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.HashUtil.sha1;
import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.net.URL;
import java.util.UUID;

import javax.jdo.Query;

import co.codewizards.cloudstore.core.util.UrlUtil;

public class RemoteRepositoryDAO extends DAO<RemoteRepository, RemoteRepositoryDAO> {
	public RemoteRepository getRemoteRepository(UUID repositoryId) {
		assertNotNull("repositoryId", repositoryId);
		Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepository_repositoryId");
		try {
			RemoteRepository remoteRepository = (RemoteRepository) query.execute(repositoryId.toString());
			return remoteRepository;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepository getRemoteRepository(URL remoteRoot) {
		assertNotNull("remoteRoot", remoteRoot);
		remoteRoot = UrlUtil.canonicalizeURL(remoteRoot);
		Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepository_remoteRootSha1");
		try {
			String remoteRootSha1 = sha1(remoteRoot.toExternalForm());
			RemoteRepository remoteRepository = (RemoteRepository) query.execute(remoteRootSha1);
			return remoteRepository;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepository getRemoteRepositoryOrFail(UUID repositoryId) {
		RemoteRepository remoteRepository = getRemoteRepository(repositoryId);
		if (remoteRepository == null)
			throw new IllegalArgumentException(String.format(
					"There is no RemoteRepository with repositoryId='%s'!",
					repositoryId));

		return remoteRepository;
	}

	public RemoteRepository getRemoteRepositoryOrFail(URL remoteRoot) {
		RemoteRepository remoteRepository = getRemoteRepository(remoteRoot);
		if (remoteRepository == null)
			throw new IllegalArgumentException(String.format(
					"There is no RemoteRepository with remoteRoot='%s'!",
					UrlUtil.canonicalizeURL(remoteRoot)));

		return remoteRepository;
	}

}
