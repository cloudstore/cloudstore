package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static java.util.Objects.*;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Query;

import co.codewizards.cloudstore.core.util.UrlUtil;

public class RemoteRepositoryDao extends Dao<RemoteRepository, RemoteRepositoryDao> {
	public RemoteRepository getRemoteRepository(final UUID repositoryId) {
		requireNonNull(repositoryId, "repositoryId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepository_repositoryId");
		try {
			final RemoteRepository remoteRepository = (RemoteRepository) query.execute(repositoryId.toString());
			return remoteRepository;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepository getRemoteRepository(URL remoteRoot) {
		requireNonNull(remoteRoot, "remoteRoot");
		remoteRoot = UrlUtil.canonicalizeURL(remoteRoot);
		final Query query = pm().newNamedQuery(getEntityClass(), "getRemoteRepository_remoteRootSha1");
		try {
			final String remoteRootSha1 = sha1(remoteRoot.toExternalForm());
			final RemoteRepository remoteRepository = (RemoteRepository) query.execute(remoteRootSha1);
			return remoteRepository;
		} finally {
			query.closeAll();
		}
	}

	public RemoteRepository getRemoteRepositoryOrFail(final UUID repositoryId) {
		final RemoteRepository remoteRepository = getRemoteRepository(repositoryId);
		if (remoteRepository == null)
			throw new IllegalArgumentException(String.format(
					"There is no RemoteRepository with repositoryId='%s'!",
					repositoryId));

		return remoteRepository;
	}

	public RemoteRepository getRemoteRepositoryOrFail(final URL remoteRoot) {
		final RemoteRepository remoteRepository = getRemoteRepository(remoteRoot);
		if (remoteRepository == null)
			throw new IllegalArgumentException(String.format(
					"There is no RemoteRepository with remoteRoot='%s'!",
					UrlUtil.canonicalizeURL(remoteRoot)));

		return remoteRepository;
	}

	public Map<UUID, URL> getRemoteRepositoryId2RemoteRootMap() {
		final Map<UUID, URL> result = new HashMap<UUID, URL>();
		final Collection<RemoteRepository> remoteRepositories = getObjects();
		for (final RemoteRepository remoteRepository : remoteRepositories) {
			if (remoteRepository.getRemoteRoot() == null)
				continue;

			result.put(remoteRepository.getRepositoryId(), remoteRepository.getRemoteRoot());
		}
		return result;
	}

	@Override
	public void deletePersistent(final RemoteRepository entity) {
		requireNonNull(entity, "entity");
		deleteDependentObjects(entity);
		pm().flush();
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(final Collection<? extends RemoteRepository> entities) {
		requireNonNull(entities, "entities");
		for (final RemoteRepository remoteRepository : entities) {
			deleteDependentObjects(remoteRepository);
		}
		pm().flush();
		super.deletePersistentAll(entities);
	}

	protected void deleteDependentObjects(final RemoteRepository remoteRepository) {
		requireNonNull(remoteRepository, "remoteRepository");

		final ModificationDao modificationDao = getDao(ModificationDao.class);
		modificationDao.deletePersistentAll(modificationDao.getModifications(remoteRepository));

		final LastSyncToRemoteRepoDao lastSyncToRemoteRepoDao = getDao(LastSyncToRemoteRepoDao.class);
		final LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDao.getLastSyncToRemoteRepo(remoteRepository);
		if (lastSyncToRemoteRepo != null)
			lastSyncToRemoteRepoDao.deletePersistent(lastSyncToRemoteRepo);
	}

}
