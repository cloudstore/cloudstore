package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.Collection;

import javax.jdo.Query;

public class DeleteModificationDAO extends DAO<DeleteModification, DeleteModificationDAO> {

	public Collection<DeleteModification> getDeleteModificationsForPathAfter(String path, long localRevision, RemoteRepository remoteRepository) {
		assertNotNull("path", path);
		assertNotNull("remoteRepository", remoteRepository);
		String pathSha1 = sha1(path);
		Query query = pm().newNamedQuery(getEntityClass(), "getDeleteModificationsForPathAfter_pathSha1_localRevision_remoteRepository");
		try {
			@SuppressWarnings("unchecked")
			Collection<DeleteModification> deleteModifications = (Collection<DeleteModification>) query.execute(pathSha1, localRevision, remoteRepository);
			return new ArrayList<DeleteModification>(deleteModifications);
		} finally {
			query.closeAll();
		}
	}

	public Collection<DeleteModification> getDeleteModificationsForPathOrParentOfPathAfter(String path, long localRevision, RemoteRepository remoteRepository) {
		assertNotNull("path", path);
		assertNotNull("remoteRepository", remoteRepository);
		if (!path.startsWith("/"))
			throw new IllegalArgumentException("path does not start with '/'!");

		ArrayList<DeleteModification> deleteModifications = new ArrayList<DeleteModification>();
		String p = path;
		while (true) {
			Collection<DeleteModification> c = getDeleteModificationsForPathAfter(p, localRevision, remoteRepository);
			deleteModifications.addAll(c);

			int lastSlash = p.lastIndexOf('/');
			if (lastSlash <= 0) // The root itself cannot be deleted, hence we can quit as soon as we reached '/'.
				break;

			p = p.substring(0, lastSlash);
		}
		return deleteModifications;
	}

}
