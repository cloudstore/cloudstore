package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.persistence.DeleteModification;
import co.codewizards.cloudstore.core.persistence.Directory;
import co.codewizards.cloudstore.core.persistence.ModificationDAO;
import co.codewizards.cloudstore.core.persistence.NormalFile;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.RepoFile;
import co.codewizards.cloudstore.core.persistence.RepoFileDAO;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
import co.codewizards.cloudstore.core.util.HashUtil;

public class LocalRepoSync {

	private static final Logger logger = LoggerFactory.getLogger(LocalRepoSync.class);

	private final LocalRepoTransaction transaction;
	private final File localRoot;
	private final RepoFileDAO repoFileDAO;
	private final RemoteRepositoryDAO remoteRepositoryDAO;
	private final ModificationDAO modificationDAO;
	private final Map<File, String> file2Sha1 = new HashMap<File, String>();

	public LocalRepoSync(LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
		localRoot = this.transaction.getLocalRepoManager().getLocalRoot();
		repoFileDAO = this.transaction.getDAO(RepoFileDAO.class);
		remoteRepositoryDAO = this.transaction.getDAO(RemoteRepositoryDAO.class);
		modificationDAO = this.transaction.getDAO(ModificationDAO.class);
	}

	public void sync(ProgressMonitor monitor) {
		sync(null, localRoot, monitor);
	}

	public void sync(File file, ProgressMonitor monitor) {
		if (!(assertNotNull("file", file).isAbsolute()))
			throw new IllegalArgumentException("file is not absolute: " + file);

		monitor.beginTask("Local sync...", 100);
		try {
			RepoFile parentRepoFile = repoFileDAO.getRepoFile(localRoot, file.getParentFile());
			if (parentRepoFile == null)
				throw new IllegalStateException("There is no parentRepoFile for the parent of this file: " + file);

			monitor.worked(1);

			sync(parentRepoFile, file, new SubProgressMonitor(monitor, 99));
		} finally {
			monitor.done();
		}
	}

	private void sync(RepoFile parentRepoFile, File file, ProgressMonitor monitor) {
		monitor.beginTask("Local sync...", 100);
		try {
			RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);

			// If the type changed - e.g. from normal file to directory - we must delete
			// the old instance.
			if (repoFile != null && !isRepoFileTypeCorrect(repoFile, file)) {
				deleteRepoFile(repoFile, false);
				repoFile = null;
			}

			if (repoFile == null) {
				repoFile = createRepoFile(parentRepoFile, file, new SubProgressMonitor(monitor, 50));
				if (repoFile == null) { // ignoring non-normal files.
					return;
				}
			} else if (isModified(repoFile, file))
				updateRepoFile(repoFile, file, new SubProgressMonitor(monitor, 50));
			else
				monitor.worked(50);

			SubProgressMonitor childSubProgressMonitor = new SubProgressMonitor(monitor, 50);
			Set<String> childNames = new HashSet<String>();
			File[] children = file.listFiles(new FilenameFilterSkipMetaDir());
			if (children != null && children.length > 0) {
				childSubProgressMonitor.beginTask("Local sync...", children.length);
				for (File child : children) {
					childNames.add(child.getName());
					sync(repoFile, child, new SubProgressMonitor(childSubProgressMonitor, 1));
				}
			}
			childSubProgressMonitor.done();

			Collection<RepoFile> childRepoFiles = repoFileDAO.getChildRepoFiles(repoFile);
			for (RepoFile childRepoFile : childRepoFiles) {
				if (!childNames.contains(childRepoFile.getName())) {
					deleteRepoFile(childRepoFile);
				}
			}
		} finally {
			monitor.done();
		}
	}

	private boolean isRepoFileTypeCorrect(RepoFile repoFile, File file) {
		// TODO support symlinks!
		if (file.isFile())
			return repoFile instanceof NormalFile;

		if (file.isDirectory())
			return repoFile instanceof Directory;

		return false;
	}

	public boolean isModified(RepoFile repoFile, File file) {
		if (repoFile.getLastModified().getTime() != file.lastModified()) {
			if (logger.isDebugEnabled()) {
				logger.debug("isModified: repoFile.lastModified != file.lastModified: repoFile.lastModified={} file.lastModified={} file={}",
						repoFile.getLastModified(), new Date(file.lastModified()), file);
			}
			return true;
		}

		if (file.isFile()) {
			if (!(repoFile instanceof NormalFile))
				throw new IllegalArgumentException("repoFile is not an instance of NormalFile! file=" + file);

			NormalFile normalFile = (NormalFile) repoFile;
			if (normalFile.getLength() != file.length()) {
				if (logger.isDebugEnabled()) {
					logger.debug("isModified: normalFile.length != file.length: repoFile.length={} file.length={} file={}",
							normalFile.getLength(), file.length(), file);
				}
				return true;
			}
		}

		return false;
	}

	private RepoFile createRepoFile(RepoFile parentRepoFile, File file, ProgressMonitor monitor) {
		if (parentRepoFile == null)
			throw new IllegalStateException("Creating the root this way is not possible! Why is it not existing, yet?!???");

		monitor.beginTask("Local sync...", 100);
		try {
			// TODO support symlinks!

			RepoFile repoFile;

			if (file.isDirectory()) {
				repoFile = new Directory();
			} else if (file.isFile()) {
				NormalFile normalFile = (NormalFile) (repoFile = new NormalFile());
				normalFile.setLength(file.length());
				normalFile.setSha1(sha(file, new SubProgressMonitor(monitor, 99)));
			} else {
				logger.warn("File is neither a directory nor a normal file! Skipping: {}", file);
				return null;
			}

			repoFile.setParent(parentRepoFile);
			repoFile.setName(file.getName());
			repoFile.setLastModified(new Date(file.lastModified()));

			return repoFileDAO.makePersistent(repoFile);
		} finally {
			monitor.done();
		}
	}

	public void updateRepoFile(RepoFile repoFile, File file, ProgressMonitor monitor) {
		logger.debug("updateRepoFile: entityID={} idHigh={} idLow={} file={}", repoFile.getEntityID(), repoFile.getIdHigh(), repoFile.getIdLow(), file);
		monitor.beginTask("Local sync...", 100);
		try {
			if (file.isFile()) {
				if (!(repoFile instanceof NormalFile))
					throw new IllegalArgumentException("repoFile is not an instance of NormalFile!");

				NormalFile normalFile = (NormalFile) repoFile;
				normalFile.setLength(file.length());
				normalFile.setSha1(sha(file, new SubProgressMonitor(monitor, 100)));
			}
			repoFile.setLastModified(new Date(file.lastModified()));
		} finally {
			monitor.done();
		}
	}

	public void deleteRepoFile(RepoFile repoFile) {
		deleteRepoFile(repoFile, true);
	}

	private void deleteRepoFile(RepoFile repoFile, boolean createDeleteModifications) {
		RepoFile parentRepoFile = assertNotNull("repoFile", repoFile).getParent();
		if (parentRepoFile == null)
			throw new IllegalStateException("Deleting the root is not possible!");

		PersistenceManager pm = transaction.getPersistenceManager();

		// We make sure, nothing interferes with our deletions (see comment below).
		pm.flush();

		if (createDeleteModifications)
			createDeleteModifications(repoFile);

		deleteRepoFileWithAllChildrenRecursively(repoFile);

		// DN batches UPDATE and DELETE statements. This sometimes causes foreign key violations and other errors in
		// certain situations. Additionally, the deleted objects still linger in the 1st-level-cache and re-using them
		// causes "javax.jdo.JDOUserException: Cannot read fields from a deleted object". This happens when switching
		// from a directory to a file (or vice versa).
		// We therefore must flush to be on the safe side. And to be extra-safe, we flush before and after deletion.
		pm.flush();
	}

	private void createDeleteModifications(RepoFile repoFile) {
		assertNotNull("repoFile", repoFile);
		NormalFile normalFile = null;
		if (repoFile instanceof NormalFile)
			normalFile = (NormalFile) repoFile;

		// TODO check, if the deleted file exists somewhere else. If so, move it (instead of deleting it).
		// Maybe we need a CopyModification instead of a MoveModification, because it can occur in multiple
		// locations and whether to move or to copy should better be decided by the client.
		// TODO Note that there are two possible scenarios: Either the new file is first created and then the old
		// file deleted or the old file is first deleted and then the new file is created. We must handle both
		// cases (here is just one of them).
		// And we should test both cases - somehow.

		for (RemoteRepository remoteRepository : remoteRepositoryDAO.getObjects()) {
			DeleteModification deleteModification = new DeleteModification();
			deleteModification.setRemoteRepository(remoteRepository);
			deleteModification.setPath(repoFile.getPath());
			deleteModification.setLength(normalFile == null ? -1 : normalFile.getLength());
			deleteModification.setSha1(normalFile == null ? null : normalFile.getSha1());
			modificationDAO.makePersistent(deleteModification);
		}
	}

	private void deleteRepoFileWithAllChildrenRecursively(RepoFile repoFile) {
		assertNotNull("repoFile", repoFile);
		for (RepoFile childRepoFile : repoFileDAO.getChildRepoFiles(repoFile)) {
			deleteRepoFileWithAllChildrenRecursively(childRepoFile);
		}
		repoFileDAO.deletePersistent(repoFile);
		repoFileDAO.getPersistenceManager().flush(); // We run *sometimes* into foreign key violations if we don't delete immediately :-(
	}

	private String sha(File file, ProgressMonitor monitor) {
		if (!(assertNotNull("file", file).isAbsolute()))
			throw new IllegalArgumentException("file is not absolute: " + file);

		if (!file.isFile()) {
			return null;
		}

		monitor.beginTask("Local sync...", 100);
		try {
			String sha1 = file2Sha1.get(file);
			if (sha1 != null)
				return sha1;

			FileInputStream in = new FileInputStream(file);
			byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, in, new SubProgressMonitor(monitor, 100));
			in.close();
			return HashUtil.encodeHexStr(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			monitor.done();
		}
	}

	public void putSha1(File file, String sha1) {
		if (!(assertNotNull("file", file).isAbsolute()))
			throw new IllegalArgumentException("file is not absolute: " + file);

		file2Sha1.put(file, sha1);
	}
}
