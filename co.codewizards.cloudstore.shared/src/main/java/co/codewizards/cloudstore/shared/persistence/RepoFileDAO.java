package co.codewizards.cloudstore.shared.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.jdo.Query;

public class RepoFileDAO extends DAO<RepoFile, RepoFileDAO> {

	public RepoFile getRepoFile(RepoFile parent, String name) {
		Query query = pm().newNamedQuery(getEntityClass(), "getRepoFile_parent_name");
		RepoFile repoFile = (RepoFile) query.execute(parent, name);
		return repoFile;
	}

	public RepoFile getRepoFile(File localRoot, File file) {
		return _getRepoFile(localRoot, file);
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
		RepoFile repoFile = getRepoFile(parentRepoFile, file.getName());
		return repoFile;
	}

	public Collection<RepoFile> getChildRepoFiles(RepoFile repoFile) {
		Query query = pm().newNamedQuery(getEntityClass(), "getChildRepoFiles_parent");
		@SuppressWarnings("unchecked")
		Collection<RepoFile> repoFiles = (Collection<RepoFile>) query.execute(repoFile);
		repoFiles = new ArrayList<RepoFile>(repoFiles);
		query.closeAll();
		return repoFiles;
	}

}
