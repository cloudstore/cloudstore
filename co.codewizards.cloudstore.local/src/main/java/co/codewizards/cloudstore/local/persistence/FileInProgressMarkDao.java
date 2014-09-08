package co.codewizards.cloudstore.local.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Query;

import co.codewizards.cloudstore.core.util.AssertUtil;

/**
 *
 * @author Sebastian Schefczyk
 */
public class FileInProgressMarkDao extends Dao<FileInProgressMark, FileInProgressMarkDao> {

	public Collection<FileInProgressMark> getFileInProgressMarks(final UUID fromRepository, final UUID toRepository) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getFileInProgressMarks");
		try {
			@SuppressWarnings("unchecked")
			final Collection<FileInProgressMark> fileInProgressMarks = (Collection<FileInProgressMark>) query.execute(convertToString(fromRepository),
					convertToString(toRepository));
			return load(fileInProgressMarks);
		} finally {
			query.closeAll();
		}
	}

	/**
	 * @return <code>null</code> if none was found.
	 */
	public FileInProgressMark getFileInProgressMark(final UUID fromRepositoryId, final UUID toRepositoryId, final String path) {
		AssertUtil.assertNotNull("fromRepositoryId", fromRepositoryId);
		AssertUtil.assertNotNull("toRepositoryId", toRepositoryId);
		AssertUtil.assertNotNull("path", path);
		final Query query = pm().newNamedQuery(getEntityClass(), "getFileInProgressMark");
		try {
			final Map<String, Object> m = new HashMap<String, Object>(3);
			m.put("fromRepositoryId", fromRepositoryId.toString());
			m.put("toRepositoryId", toRepositoryId.toString());
			m.put("path", path);
			final FileInProgressMark result = (FileInProgressMark) query.executeWithMap(m);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public void deleteFileInProgressMarks(final UUID fromRepositoryId, final UUID toRepositoryId) {
		final Collection<FileInProgressMark> fileInProgressMarks = getFileInProgressMarks(fromRepositoryId, toRepositoryId);
		deletePersistentAll(fileInProgressMarks);
	}

	static UUID convertToUuid(final String repositoryId) {
		return UUID.fromString(repositoryId);
	}

	static String convertToString(final UUID repositoryId) {
		return repositoryId == null ? null : repositoryId.toString();
	}
}
