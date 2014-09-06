package co.codewizards.cloudstore.local.persistence;


import java.util.ArrayList;
import java.util.Collection;

import javax.jdo.Query;

import co.codewizards.cloudstore.core.util.AssertUtil;

public class NormalFileDao extends Dao<NormalFile, NormalFileDao> {
	/**
	 * Get those {@link RepoFile}s whose {@link RepoFile#getSha1() sha1} and {@link RepoFile#getLength() length}
	 * match the given parameters.
	 * @param sha1 the {@link RepoFile#getSha1() sha1} for which to query. Must not be <code>null</code>.
	 * @param length the {@link RepoFile#getLength() length} for which to query.
	 * @return those {@link RepoFile}s matching the given criteria. Never <code>null</code>; but maybe empty.
	 */
	public Collection<NormalFile> getNormalFilesForSha1(final String sha1, final long length) {
		AssertUtil.assertNotNull("sha1", sha1);
		final Query query = pm().newNamedQuery(getEntityClass(), "getNormalFiles_sha1_length");
		try {
			@SuppressWarnings("unchecked")
			final
			Collection<NormalFile> repoFiles = (Collection<NormalFile>) query.execute(sha1, length);
			return new ArrayList<NormalFile>(repoFiles);
		} finally {
			query.closeAll();
		}
	}

	public Collection<NormalFile> getNormalFilesInProgress() {
		final Query query = pm().newNamedQuery(getEntityClass(), "getNormalFiles_inProgress");
		try {
			@SuppressWarnings("unchecked")
			final
			Collection<NormalFile> repoFiles = (Collection<NormalFile>) query.execute();
			return new ArrayList<NormalFile>(repoFiles);
		} finally {
			query.closeAll();
		}
	}

	@Override
	public void deletePersistent(final NormalFile entity) {
		throw new UnsupportedOperationException("Use RepoFileDao for this operation!");
	}

	@Override
	public <P extends NormalFile> P makePersistent(final P entity) {
		throw new UnsupportedOperationException("Use RepoFileDao for this operation!");
	}
}
