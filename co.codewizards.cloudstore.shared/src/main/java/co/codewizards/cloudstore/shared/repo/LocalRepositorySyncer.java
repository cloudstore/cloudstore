package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.persistence.FileType;
import co.codewizards.cloudstore.shared.persistence.RepoFile;
import co.codewizards.cloudstore.shared.persistence.RepoFileDAO;
import co.codewizards.cloudstore.shared.progress.ProgressMonitor;
import co.codewizards.cloudstore.shared.util.HashUtil;

class LocalRepositorySyncer {

	private static final Logger logger = LoggerFactory.getLogger(LocalRepositorySyncer.class);

	private final RepositoryTransaction transaction;
	private final RepoFileDAO repoFileDAO;
	private final File localRoot;

	public LocalRepositorySyncer(RepositoryTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
		repoFileDAO = new RepoFileDAO().persistenceManager(transaction.getPersistenceManager());
		localRoot = transaction.getRepositoryManager().getLocalRoot();
	}

	public void sync(ProgressMonitor monitor) { // TODO use this monitor!!!
		sync(null, localRoot);
	}

	private void sync(RepoFile parentRepoFile, File file) {
		RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);
		if (repoFile == null) {
			repoFile = createRepoFile(parentRepoFile, file);
			if (repoFile == null) { // ignoring non-normal files.
				return;
			}
		} else if (isModified(repoFile, file)) {
			updateRepoFile(repoFile, file);
		}

		Set<String> childNames = new HashSet<String>();
		File[] children = file.listFiles(new FilenameFilterSkipMetaDir());
		if (children != null) {
			for (File child : children) {
				childNames.add(child.getName());
				sync(repoFile, child);
			}
		}

		Collection<RepoFile> childRepoFiles = repoFileDAO.getChildRepoFiles(repoFile);
		for (RepoFile childRepoFile : childRepoFiles) {
			if (!childNames.contains(childRepoFile.getName())) {
				deleteRepoFile(childRepoFile);
			}
		}
	}

	private boolean isModified(RepoFile repoFile, File file) {
		if (file.isFile()) {
			long difference = Math.abs(repoFile.getLastModified().getTime() - file.lastModified());
			// TODO make time difference threshold configurable
			if (difference > 5000)
				return true;

			return repoFile.getSize() != file.length();
		} else {
			return false;
		}
	}

	private RepoFile createRepoFile(RepoFile parentRepoFile, File file) {
		RepoFile repoFile = new RepoFile();
		repoFile.setParent(parentRepoFile);
		repoFile.setName(file.getName());

		if (file.isDirectory()) {
			repoFile.setFileType(FileType.DIRECTORY);
		} else if (file.isFile()) {
			repoFile.setFileType(FileType.FILE);
			repoFile.setLastModified(new Date(file.lastModified()));
			repoFile.setSize(file.length());
			repoFile.setSha(sha(file));
		} else {
			logger.warn("File is neither a directory nor a normal file! Skipping: {}", file);
			return null;
		}

		repoFile.setRevision(transaction.getRevision());
		return repoFileDAO.makePersistent(repoFile);
	}

	private void updateRepoFile(RepoFile repoFile, File file) {
		repoFile.setLastModified(new Date(file.lastModified()));
		repoFile.setSize(file.length());
		repoFile.setSha(sha(file));
		repoFile.setRevision(transaction.getRevision());
	}

	private void deleteRepoFile(RepoFile repoFile) {
		repoFileDAO.deletePersistent(repoFile);
		// TODO create history entries for synchronisation with remote repositories... somehow... think this through ;-)
		// Or maybe mark the parent (=> increment its revision)? that's maybe even better.
	}

	private String sha(File file) {
		assertNotNull("file", file);
		if (!file.isFile()) {
			return null;
		}
		try {
			FileInputStream in = new FileInputStream(file);
			byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, in);
			in.close();
			return HashUtil.encodeHexStr(hash, 0, hash.length);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
