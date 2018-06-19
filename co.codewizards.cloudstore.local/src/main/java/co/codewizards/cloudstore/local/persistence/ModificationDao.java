package co.codewizards.cloudstore.local.persistence;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class ModificationDao extends Dao<Modification, ModificationDao> {
	private static final Logger logger = LoggerFactory.getLogger(ModificationDao.class);

	/**
	 * Gets those {@link Modification}s being assigned to the given {@link Modification#getRemoteRepository() remoteRepository}
	 * whose {@link Modification#getLocalRevision() localRevision} is greater than the given {@code localRevision}.
	 * @param remoteRepository the {@link Modification#getRemoteRepository() remoteRepository} the queried modifications are assigned to.
	 * @param localRevision the {@link Modification#getLocalRevision() localRevision}, after which the modifications
	 * to be queried where created.
	 * @return those {@link Modification}s matching the given criteria. Never <code>null</code>, but maybe empty.
	 */
	public Collection<Modification> getModificationsAfter(final RemoteRepository remoteRepository, final long localRevision) {
		AssertUtil.assertNotNull(remoteRepository, "remoteRepository");
		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getModificationsAfter_remoteRepository_localRevision");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Modification> modifications = (Collection<Modification>) query.execute(remoteRepository, localRevision);
			logger.debug("getModificationsAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			fetchPlanBackup.restore(pm);
			startTimestamp = System.currentTimeMillis();
			modifications = load(modifications);
			logger.debug("getModificationsAfter: Loading result-set with {} elements took {} ms.", modifications.size(), System.currentTimeMillis() - startTimestamp);

			return modifications;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	public Collection<Modification> getModificationsBeforeOrEqual(final RemoteRepository remoteRepository, final long localRevision) {
		AssertUtil.assertNotNull(remoteRepository, "remoteRepository");
		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getModificationsBeforeOrEqual_remoteRepository_localRevision");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Modification> modifications = (Collection<Modification>) query.execute(remoteRepository, localRevision);
			logger.debug("getModificationsBeforeOrEqual: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			fetchPlanBackup.restore(pm);
			startTimestamp = System.currentTimeMillis();
			modifications = load(modifications);
			logger.debug("getModificationsBeforeOrEqual: Loading result-set with {} elements took {} ms.", modifications.size(), System.currentTimeMillis() - startTimestamp);

			return modifications;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	/**
	 * Gets all {@link Modification}s being assigned to the given {@link Modification#getRemoteRepository() remoteRepository}.
	 * @param remoteRepository the {@link Modification#getRemoteRepository() remoteRepository} the queried modifications are assigned to.
	 * @return those {@link Modification}s matching the given criteria. Never <code>null</code>, but maybe empty.
	 */
	public Collection<Modification> getModifications(final RemoteRepository remoteRepository) {
		return getModificationsAfter(remoteRepository, -1);
	}
}
