package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.jdo.Query;

public class LastSyncToRemoteRepoDAO extends DAO<LastSyncToRemoteRepo, LastSyncToRemoteRepoDAO> {

	public LastSyncToRemoteRepo getLastSyncToRemoteRepo(RemoteRepository remoteRepository) {
		assertNotNull("remoteRepository", remoteRepository);
		Query query = pm().newNamedQuery(getEntityClass(), "getLastSyncToRemoteRepo_remoteRepository");
		try {
			LastSyncToRemoteRepo lastSyncToRemoteRepo = (LastSyncToRemoteRepo) query.execute(remoteRepository);
			return lastSyncToRemoteRepo;
		} finally {
			query.closeAll();
		}
	}

}
