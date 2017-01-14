package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.HashUtil.*;

import java.util.ArrayList;
import java.util.Collection;

import javax.jdo.Query;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class DeleteModificationDao extends Dao<DeleteModification, DeleteModificationDao> {

	public Collection<DeleteModification> getDeleteModificationsForPathAfter(final String path, final long localRevision, final RemoteRepository remoteRepository) {
		AssertUtil.assertNotNull(path, "path");
		AssertUtil.assertNotNull(remoteRepository, "remoteRepository");
		final String pathSha1 = sha1(path);
		final Query query = pm().newNamedQuery(getEntityClass(), "getDeleteModificationsForPathAfter_pathSha1_localRevision_remoteRepository");
		try {
			@SuppressWarnings("unchecked")
			final Collection<DeleteModification> deleteModifications = (Collection<DeleteModification>) query.execute(pathSha1, localRevision, remoteRepository);
			return new ArrayList<DeleteModification>(deleteModifications);
		} finally {
			query.closeAll();
		}
	}

	public Collection<DeleteModification> getDeleteModificationsForPathOrParentOfPathAfter(final String path, final long localRevision, final RemoteRepository remoteRepository) {
		AssertUtil.assertNotNull(path, "path");
		AssertUtil.assertNotNull(remoteRepository, "remoteRepository");
		if (!path.startsWith("/"))
			throw new IllegalArgumentException("path does not start with '/'!");

		final ArrayList<DeleteModification> deleteModifications = new ArrayList<DeleteModification>();
		String p = path;
		while (true) {
			final Collection<DeleteModification> c = getDeleteModificationsForPathAfter(p, localRevision, remoteRepository);
			deleteModifications.addAll(c);

			final int lastSlash = p.lastIndexOf('/');
			if (lastSlash <= 0) // The root itself cannot be deleted, hence we can quit as soon as we reached '/'.
				break;

			p = p.substring(0, lastSlash);
		}
		return deleteModifications;
	}

	public Collection<DeleteModification> getDeleteModificationsForSha1(final String sha1, final long length) {
		AssertUtil.assertNotNull(sha1, "sha1");
		final Query query = pm().newNamedQuery(getEntityClass(), "getDeleteModifications_sha1_length");
		try {
			@SuppressWarnings("unchecked")
			final
			Collection<DeleteModification> deleteModifications = (Collection<DeleteModification>) query.execute(sha1, length);
			return new ArrayList<DeleteModification>(deleteModifications);
		} finally {
			query.closeAll();
		}
	}

}
