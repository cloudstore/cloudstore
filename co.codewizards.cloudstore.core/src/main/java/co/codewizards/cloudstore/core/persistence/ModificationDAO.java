package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.Collection;

import javax.jdo.Query;

public class ModificationDAO extends DAO<Modification, ModificationDAO> {
	/**
	 * Get those {@link Modification}s being assigned to the given {@link Modification#getRemoteRepository() remoteRepository}
	 * whose {@link Modification#getLocalRevision() localRevision} is greater than the given {@code localRevision}.
	 * @param remoteRepository the {@link Modification#getRemoteRepository() remoteRepository} the queried modifications are assigned to.
	 * @param localRevision the {@link Modification#getLocalRevision() localRevision}, after which the modifications
	 * to be queried where created.
	 * @return those {@link Modification}s matching the given criteria. Never <code>null</code>, but maybe empty.
	 */
	public Collection<Modification> getModificationsAfter(RemoteRepository remoteRepository, long localRevision) {
		assertNotNull("remoteRepository", remoteRepository);
		Query query = pm().newNamedQuery(getEntityClass(), "getModificationsAfter_remoteRepository_localRevision");
		try {
			@SuppressWarnings("unchecked")
			Collection<Modification> modifications = (Collection<Modification>) query.execute(remoteRepository, localRevision);
			return new ArrayList<Modification>(modifications);
		} finally {
			query.closeAll();
		}
	}
	
	public Collection<Modification> getModificationsBeforeOrEqual(RemoteRepository remoteRepository, long localRevision) {
		assertNotNull("remoteRepository", remoteRepository);
		Query query = pm().newNamedQuery(getEntityClass(), "getModificationsBeforeOrEqual_remoteRepository_localRevision");
		try {
			@SuppressWarnings("unchecked")
			Collection<Modification> modifications = (Collection<Modification>) query.execute(remoteRepository, localRevision);
			return new ArrayList<Modification>(modifications);
		} finally {
			query.closeAll();
		}
	}
}
