package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Sebastian Schefczyk
 */
public class FileInProgressMarkerDao extends Dao<FileInProgressMarker, FileInProgressMarkerDao> {

	private static final Logger logger = LoggerFactory.getLogger(FileInProgressMarkerDao.class);

	public Collection<FileInProgressMarker> getFileInProgressMarkers(final UUID fromRepository, final UUID toRepository) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getFileInProgressMarkers_fromRepositoryId_toRepositoryId");
		try {
			@SuppressWarnings("unchecked")
			final Collection<FileInProgressMarker> fileInProgressMarkers = (Collection<FileInProgressMarker>) query.execute(convertToString(fromRepository),
					convertToString(toRepository));
			return load(fileInProgressMarkers);
		} finally {
			query.closeAll();
		}
	}

	/**
	 * @return <code>null</code> if none was found.
	 */
	public FileInProgressMarker getFileInProgressMarker(final UUID fromRepositoryId, final UUID toRepositoryId, final String path) {
		assertNotNull("fromRepositoryId", fromRepositoryId);
		assertNotNull("toRepositoryId", toRepositoryId);
		assertNotNull("path", path);
		final String pathSha1 = sha1(path);
		final Query query = pm().newNamedQuery(getEntityClass(), "getFileInProgressMarker_fromRepositoryId_toRepositoryId_pathSha1");
		try {
			final Map<String, Object> m = new HashMap<String, Object>(3);
			m.put("fromRepositoryId", fromRepositoryId.toString());
			m.put("toRepositoryId", toRepositoryId.toString());
			m.put("pathSha1", pathSha1);
			final FileInProgressMarker result = (FileInProgressMarker) query.executeWithMap(m);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public void deleteFileInProgressMarkers(final UUID fromRepositoryId, final UUID toRepositoryId) {
		final Collection<FileInProgressMarker> fileInProgressMarkers = getFileInProgressMarkers(fromRepositoryId, toRepositoryId);
		if (fileInProgressMarkers.size() > 0) {
			logger.info("deleteFileInProgressMarkers: deleting {} FileInProgressMarker(s) from={}, to={}", fileInProgressMarkers.size(),
					fromRepositoryId, toRepositoryId);
			deletePersistentAll(fileInProgressMarkers);
		}
	}

	static UUID convertToUuid(final String repositoryId) {
		return UUID.fromString(repositoryId);
	}

	static String convertToString(final UUID repositoryId) {
		return repositoryId == null ? null : repositoryId.toString();
	}
}
