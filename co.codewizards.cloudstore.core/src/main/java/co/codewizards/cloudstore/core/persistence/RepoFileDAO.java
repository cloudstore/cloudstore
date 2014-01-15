package co.codewizards.cloudstore.core.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.jdo.Query;

public class RepoFileDAO extends DAO<RepoFile, RepoFileDAO> {
	/**
	 * Get the child of the given {@code parent} with the specified {@code name}.
	 * @param parent the {@link RepoFile#getParent() parent} of the queried child.
	 * @param name the {@link RepoFile#getName() name} of the queried child.
	 * @return the child matching the given criteria; <code>null</code>, if there is no such object in the database.
	 */
	public RepoFile getChildRepoFile(RepoFile parent, String name) {
		Query query = pm().newNamedQuery(getEntityClass(), "getChildRepoFile_parent_name");
		RepoFile repoFile = (RepoFile) query.execute(parent, name);
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
	public RepoFile getRepoFile(File localRoot, File file) throws IllegalArgumentException {
		return _getRepoFile(assertNotNull("localRoot", localRoot), assertNotNull("file", file));
	}

	private RepoFile _getRepoFile(File localRoot, File file) {
		if (localRoot.equals(file)) {
			return new LocalRepositoryDAO().persistenceManager(pm()).getLocalRepositoryOrFail().getRoot();
		}

		File parentFile = file.getParentFile();
		if (parentFile == null) {
			throw new IllegalArgumentException(String.format("Repository '%s' does not contain file '%s'!", localRoot, file));
		}

		RepoFile parentRepoFile = _getRepoFile(localRoot, parentFile);
		return getChildRepoFile(parentRepoFile, file.getName());
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
	public Collection<RepoFile> getChildRepoFiles(RepoFile parent) {
		Query query = pm().newNamedQuery(getEntityClass(), "getChildRepoFiles_parent");
		try {
			@SuppressWarnings("unchecked")
			Collection<RepoFile> repoFiles = (Collection<RepoFile>) query.execute(parent);
			return new ArrayList<RepoFile>(repoFiles);
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Get those {@link RepoFile}s whose {@link RepoFile#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * @param localRevision the {@link RepoFile#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 * @return those {@link RepoFile}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<RepoFile> getRepoFilesChangedAfter(long localRevision) {
		Query query = pm().newNamedQuery(getEntityClass(), "getRepoFilesChangedAfter_localRevision");
		try {
			@SuppressWarnings("unchecked")
			Collection<RepoFile> repoFiles = (Collection<RepoFile>) query.execute(localRevision);
			return new ArrayList<RepoFile>(repoFiles);
		} finally {
			query.closeAll();
		}
	}
}
