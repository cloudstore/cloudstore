package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;
import java.util.Collection;
import java.util.UUID;

import javax.jdo.Query;

import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.UrlUtil;

public class RemoteRepositoryDao extends Dao<RemoteRepository, RemoteRepositoryDao> {
	public RemoteRepository getRemoteRepository(UUID repositoryId) {
		AssertUtil.assertNotNull("repositoryId", repositoryId);
		Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepository_repositoryId");
		try {
			RemoteRepository remoteRepository = (RemoteRepository) query.execute(repositoryId.toString());
			return remoteRepository;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepository getRemoteRepository(URL remoteRoot) {
		AssertUtil.assertNotNull("remoteRoot", remoteRoot);
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

	@Override
	public void deletePersistent(RemoteRepository entity) {
		AssertUtil.assertNotNull("entity", entity);
		deleteDependentObjects(entity);
		pm().flush();
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(Collection<? extends RemoteRepository> entities) {
		AssertUtil.assertNotNull("entities", entities);
		for (RemoteRepository remoteRepository : entities) {
			deleteDependentObjects(remoteRepository);
		}
		pm().flush();
		super.deletePersistentAll(entities);
	}

	protected void deleteDependentObjects(RemoteRepository remoteRepository) {
		AssertUtil.assertNotNull("remoteRepository", remoteRepository);

		final ModificationDao modificationDao = getDao(ModificationDao.class);
		modificationDao.deletePersistentAll(modificationDao.getModifications(remoteRepository));

		final LastSyncToRemoteRepoDao lastSyncToRemoteRepoDao = getDao(LastSyncToRemoteRepoDao.class);
		LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDao.getLastSyncToRemoteRepo(remoteRepository);
		if (lastSyncToRemoteRepo != null)
			lastSyncToRemoteRepoDao.deletePersistent(lastSyncToRemoteRepo);
	}

}
