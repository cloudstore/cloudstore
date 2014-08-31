package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.AssertUtil;

public class RepoFileDao extends Dao<RepoFile, RepoFileDao> {
	private static final Logger logger = LoggerFactory.getLogger(RepoFileDao.class);

	private Directory localRootDirectory;

	private DirectoryCache directoryCache;

	private static class DirectoryCache {
		private static final int MAX_SIZE = 50;
		private final Map<File, Directory> file2DirectoryCache = new HashMap<File, Directory>();
		private final Map<Directory, File> directory2FileCache = new HashMap<Directory, File>();
		private final LinkedList<Directory> directoryCacheList = new LinkedList<Directory>();

		public Directory get(final File file) {
			return file2DirectoryCache.get(file);
		}

		public void put(final File file, final Directory directory) {
			file2DirectoryCache.put(AssertUtil.assertNotNull("file", file), AssertUtil.assertNotNull("directory", directory));
			directory2FileCache.put(directory, file);
			directoryCacheList.remove(directory);
			directoryCacheList.addLast(directory);
			removeOldEntriesIfNecessary();
		}

		public void remove(final Directory directory) {
			final File file = directory2FileCache.remove(directory);
			file2DirectoryCache.remove(file);
		}

		public void remove(final File file) {
			final Directory directory = file2DirectoryCache.remove(file);
			directory2FileCache.remove(directory);
		}

		private void removeOldEntriesIfNecessary() {
			while (directoryCacheList.size() > MAX_SIZE) {
				final Directory directory = directoryCacheList.removeFirst();
				remove(directory);
			}
		}
	}

	/**
	 * Get the child of the given {@code parent} with the specified {@code name}.
	 * @param parent the {@link RepoFile#getParent() parent} of the queried child.
	 * @param name the {@link RepoFile#getName() name} of the queried child.
	 * @return the child matching the given criteria; <code>null</code>, if there is no such object in the database.
	 */
	public RepoFile getChildRepoFile(final RepoFile parent, final String name) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getChildRepoFile_parent_name");
		final RepoFile repoFile = (RepoFile) query.execute(parent, name);
		return repoFile;
	}

	/**
	 * Get the {@link RepoFile} for the given {@code file} in the file system.
	 * @param localRoot the repository's root directory in the file system. Must not be <code>null</code>.
	 * @param file the file in the file system for which to query the associated {@link RepoFile}. Must not be <code>null</code>.
	 * @return the {@link RepoFile} for the given {@code file} in the file system; <code>null</code>, if no such
	 * object exists in the database.
	 * @throws IllegalArgumentException if one of the parameters is <code>null</code> or if the given {@code file}
	 * is not located inside the repository - i.e. it is not a direct or indirect child of the given {@code localRoot}.
	 */
	public RepoFile getRepoFile(final File localRoot, final File file) throws IllegalArgumentException {
		return _getRepoFile(AssertUtil.assertNotNull("localRoot", localRoot), AssertUtil.assertNotNull("file", file), file);
	}

	private RepoFile _getRepoFile(final File localRoot, final File file, final File originallySearchedFile) {
		if (localRoot.equals(file)) {
			return getLocalRootDirectory();
		}

		final DirectoryCache directoryCache = getDirectoryCache();
		final Directory directory = directoryCache.get(file);
		if (directory != null)
			return directory;

		final File parentFile = file.getParentFile();
		if (parentFile == null)
			throw new IllegalArgumentException(String.format("Repository '%s' does not contain file '%s'!", localRoot, originallySearchedFile));

		final RepoFile parentRepoFile = _getRepoFile(localRoot, parentFile, originallySearchedFile);
		final RepoFile result = getChildRepoFile(parentRepoFile, file.getName());
		if (result instanceof Directory)
			directoryCache.put(file, (Directory)result);

		return result;
	}

	public Directory getLocalRootDirectory() {
		if (localRootDirectory == null)
			localRootDirectory = new LocalRepositoryDao().persistenceManager(pm()).getLocalRepositoryOrFail().getRoot();

		return localRootDirectory;
	}

	/**
	 * Get the children of the given {@code parent}.
	 * <p>
	 * The children are those {@link RepoFile}s whose {@link RepoFile#getParent() parent} equals the given
	 * {@code parent} parameter.
	 * @param parent the parent whose children are to be queried. This may be <code>null</code>, but since
	 * there is only one single instance with {@code RepoFile.parent} being null - the root directory - this
	 * is usually never <code>null</code>.
	 * @return the children of the given {@code parent}. Never <code>null</code>, but maybe empty.
	 */
	public Collection<RepoFile> getChildRepoFiles(final RepoFile parent) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getChildRepoFiles_parent");
		try {
			@SuppressWarnings("unchecked")
			final
			Collection<RepoFile> repoFiles = (Collection<RepoFile>) query.execute(parent);
			return load(repoFiles);
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Get those {@link RepoFile}s whose {@link RepoFile#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * @param localRevision the {@link RepoFile#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 * @param exclLastSyncFromRepositoryId the {@link RepoFile#getLastSyncFromRepositoryId() lastSyncFromRepositoryId}
	 * to exclude from the result set. This is used to prevent changes originating from a repository to be synced back
	 * to its origin (unnecessary and maybe causing a collision there).
	 * See <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a>.
	 * @return those {@link RepoFile}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<RepoFile> getRepoFilesChangedAfterExclLastSyncFromRepositoryId(final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		AssertUtil.assertNotNull("exclLastSyncFromRepositoryId", exclLastSyncFromRepositoryId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<RepoFile> repoFiles = (Collection<RepoFile>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getRepoFilesChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			repoFiles = load(repoFiles);
			logger.debug("getRepoFilesChangedAfter: Loading result-set with {} elements took {} ms.", repoFiles.size(), System.currentTimeMillis() - startTimestamp);

			return repoFiles;
		} finally {
			query.closeAll();
		}
	}

	@Override
	public void deletePersistent(final RepoFile entity) {
		getPersistenceManager().flush();
		if (entity instanceof Directory)
			getDirectoryCache().remove((Directory) entity);

		super.deletePersistent(entity);
		getPersistenceManager().flush(); // We run *sometimes* into foreign key violations if we don't delete immediately :-(
	}

	private DirectoryCache getDirectoryCache() {
		if (directoryCache == null)
			directoryCache = new DirectoryCache();

		return directoryCache;
	}
}
