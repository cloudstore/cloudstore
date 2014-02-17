package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.persistence.CopyModification;
import co.codewizards.cloudstore.core.persistence.DeleteModification;
import co.codewizards.cloudstore.core.persistence.DeleteModificationDAO;
import co.codewizards.cloudstore.core.persistence.Directory;
import co.codewizards.cloudstore.core.persistence.FileChunk;
import co.codewizards.cloudstore.core.persistence.ModificationDAO;
import co.codewizards.cloudstore.core.persistence.NormalFile;
import co.codewizards.cloudstore.core.persistence.NormalFileDAO;
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
	private final NormalFileDAO normalFileDAO;
	private final RemoteRepositoryDAO remoteRepositoryDAO;
	private final ModificationDAO modificationDAO;
	private final DeleteModificationDAO deleteModificationDAO;
	private Collection<RemoteRepository> remoteRepositories;

	public LocalRepoSync(LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
		localRoot = this.transaction.getLocalRepoManager().getLocalRoot();
		repoFileDAO = this.transaction.getDAO(RepoFileDAO.class);
		normalFileDAO = this.transaction.getDAO(NormalFileDAO.class);
		remoteRepositoryDAO = this.transaction.getDAO(RemoteRepositoryDAO.class);
		modificationDAO = this.transaction.getDAO(ModificationDAO.class);
		deleteModificationDAO = this.transaction.getDAO(DeleteModificationDAO.class);
	}

	public void sync(ProgressMonitor monitor) {
		sync(null, localRoot, monitor);
	}

	public void sync(File file, ProgressMonitor monitor) {
		if (!(assertNotNull("file", file).isAbsolute()))
			throw new IllegalArgumentException("file is not absolute: " + file);

		if (localRoot.equals(file)) {
			sync(null, file, monitor);
			return;
		}

		monitor.beginTask("Local sync...", 100);
		try {
			File parentFile = file.getParentFile();
			RepoFile parentRepoFile = repoFileDAO.getRepoFile(localRoot, parentFile);
			if (parentRepoFile == null) {
				// If the file does not exist and its RepoFile neither exists, then
				// this is in sync already and we can simply leave. This regularly
				// happens during the deletion of a directory which is the connection-point
				// of a remote-repository. The following re-up-sync then leads us here.
				// To speed up things, we simply quit as this is a valid state.
				if (!file.exists() && repoFileDAO.getRepoFile(localRoot, file) == null)
					return;

				// In the unlikely event, that this is not a valid state, we simply sync all
				// and return.
				sync(null, localRoot, new SubProgressMonitor(monitor, 99));
				return;
			}

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

			if (normalFile.getFileChunks().isEmpty()) // TODO remove this - only needed for downward compatibility!
				return true;
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
				sha(normalFile, file, new SubProgressMonitor(monitor, 99));
			} else {
				logger.warn("File is neither a directory nor a normal file! Skipping: {}", file);
				return null;
			}

			repoFile.setParent(parentRepoFile);
			repoFile.setName(file.getName());
			repoFile.setLastModified(new Date(file.lastModified()));

			if (repoFile instanceof NormalFile)
				createCopyModificationsIfPossible((NormalFile)repoFile);

			return repoFileDAO.makePersistent(repoFile);
		} finally {
			monitor.done();
		}
	}

	public void updateRepoFile(RepoFile repoFile, File file, ProgressMonitor monitor) {
		logger.debug("updateRepoFile: id={} file={}", repoFile.getId(), file);
		monitor.beginTask("Local sync...", 100);
		try {
			if (file.isFile()) {
				if (!(repoFile instanceof NormalFile))
					throw new IllegalArgumentException("repoFile is not an instance of NormalFile!");

				NormalFile normalFile = (NormalFile) repoFile;
				sha(normalFile, file, new SubProgressMonitor(monitor, 100));
				normalFile.setLastSyncFromRepositoryId(null);
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

	private void createCopyModificationsIfPossible(NormalFile newNormalFile) {
		// A CopyModification is not necessary for an empty file. And since this method is called
		// during RepoTransport.beginPutFile(...), we easily filter out this unwanted case already.
		// Changed to a minimum size of 100 KB - it's not worth the overhead for such small files.
		if (newNormalFile.getLength() < 100 * 1024)
			return;

		Collection<NormalFile> normalFiles = normalFileDAO.getNormalFilesForSha1(newNormalFile.getSha1(), newNormalFile.getLength());
		for (NormalFile normalFile : normalFiles) {
//			if (normalFile.isInProgress()) // Additional check. Do we really want this?
//				continue;

			if (newNormalFile.equals(normalFile)) // should never happen, because newNormalFile is not yet persisted, but we write robust code that doesn't break easily after refactoring.
				continue;

			createCopyModifications(normalFile, newNormalFile);
		}

		Set<String> fromPaths = new HashSet<String>();
		Collection<DeleteModification> deleteModifications = deleteModificationDAO.getDeleteModificationsForSha1(newNormalFile.getSha1(), newNormalFile.getLength());
		for (DeleteModification deleteModification : deleteModifications)
			createCopyModifications(deleteModification, newNormalFile, fromPaths);
	}

	private void createCopyModifications(DeleteModification deleteModification, NormalFile toNormalFile, Set<String> fromPaths) {
		assertNotNull("deleteModification", deleteModification);
		assertNotNull("toNormalFile", toNormalFile);

		if (deleteModification.getLength() != toNormalFile.getLength())
			throw new IllegalArgumentException("fromNormalFile.length != toNormalFile.length");

		if (!deleteModification.getSha1().equals(toNormalFile.getSha1()))
			throw new IllegalArgumentException("fromNormalFile.sha1 != toNormalFile.sha1");

		String fromPath = deleteModification.getPath();
		if (!fromPaths.add(fromPath)) // already done before => prevent duplicates.
			return;

		for (RemoteRepository remoteRepository : getRemoteRepositories()) {
			CopyModification modification = new CopyModification();
			modification.setRemoteRepository(remoteRepository);
			modification.setFromPath(fromPath);
			modification.setToPath(toNormalFile.getPath());
			modification.setLength(toNormalFile.getLength());
			modification.setSha1(toNormalFile.getSha1());
			modificationDAO.makePersistent(modification);
		}
	}

	private void createCopyModifications(NormalFile fromNormalFile, NormalFile toNormalFile) {
		assertNotNull("fromNormalFile", fromNormalFile);
		assertNotNull("toNormalFile", toNormalFile);

		if (fromNormalFile.getLength() != toNormalFile.getLength())
			throw new IllegalArgumentException("fromNormalFile.length != toNormalFile.length");

		if (!fromNormalFile.getSha1().equals(toNormalFile.getSha1()))
			throw new IllegalArgumentException("fromNormalFile.sha1 != toNormalFile.sha1");

		for (RemoteRepository remoteRepository : getRemoteRepositories()) {
			CopyModification modification = new CopyModification();
			modification.setRemoteRepository(remoteRepository);
			modification.setFromPath(fromNormalFile.getPath());
			modification.setToPath(toNormalFile.getPath());
			modification.setLength(toNormalFile.getLength());
			modification.setSha1(toNormalFile.getSha1());
			modificationDAO.makePersistent(modification);
		}
	}

	private void createDeleteModifications(RepoFile repoFile) {
		assertNotNull("repoFile", repoFile);
		NormalFile normalFile = null;
		if (repoFile instanceof NormalFile)
			normalFile = (NormalFile) repoFile;

		for (RemoteRepository remoteRepository : getRemoteRepositories()) {
			DeleteModification modification = new DeleteModification();
			modification.setRemoteRepository(remoteRepository);
			modification.setPath(repoFile.getPath());
			modification.setLength(normalFile == null ? -1 : normalFile.getLength());
			modification.setSha1(normalFile == null ? null : normalFile.getSha1());
			modificationDAO.makePersistent(modification);
		}
	}

	private Collection<RemoteRepository> getRemoteRepositories() {
		if (remoteRepositories == null)
			remoteRepositories = Collections.unmodifiableCollection(remoteRepositoryDAO.getObjects());

		return remoteRepositories;
	}

	private void deleteRepoFileWithAllChildrenRecursively(RepoFile repoFile) {
		assertNotNull("repoFile", repoFile);
		for (RepoFile childRepoFile : repoFileDAO.getChildRepoFiles(repoFile)) {
			deleteRepoFileWithAllChildrenRecursively(childRepoFile);
		}
		repoFileDAO.deletePersistent(repoFile);
		repoFileDAO.getPersistenceManager().flush(); // We run *sometimes* into foreign key violations if we don't delete immediately :-(
	}

	private void sha(NormalFile normalFile, File file, ProgressMonitor monitor) {
		monitor.beginTask("Local sync...", (int)Math.min(file.length(), Integer.MAX_VALUE));
		try {
			normalFile.getFileChunks().clear();
			transaction.getPersistenceManager().flush();

			MessageDigest mdAll = MessageDigest.getInstance(HashUtil.HASH_ALGORITHM_SHA);
			MessageDigest mdChunk = MessageDigest.getInstance(HashUtil.HASH_ALGORITHM_SHA);

			final int bufLength = 32 * 1024;
			final int chunkLength = 32 * bufLength; // 1 MiB chunk size

			long offset = 0;
			InputStream in = new FileInputStream(file);
			try {
				FileChunk fileChunk = null;

				byte[] buf = new byte[bufLength];
				while (true) {
					if (fileChunk == null) {
						fileChunk = new FileChunk();
						fileChunk.setRepoFile(normalFile);
						fileChunk.setOffset(offset);
						fileChunk.setLength(0);
						mdChunk.reset();
					}

					int bytesRead = in.read(buf, 0, buf.length);

					if (bytesRead > 0) {
						mdAll.update(buf, 0, bytesRead);
						mdChunk.update(buf, 0, bytesRead);
						offset += bytesRead;
						fileChunk.setLength(fileChunk.getLength() + bytesRead);
					}

					if (bytesRead < 0 || fileChunk.getLength() >= chunkLength) {
						fileChunk.setSha1(HashUtil.encodeHexStr(mdChunk.digest()));
						fileChunk.makeReadOnly();
						normalFile.getFileChunks().add(fileChunk);
						fileChunk = null;

						if (bytesRead < 0) {
							break;
						}
					}

					if (bytesRead > 0)
						monitor.worked(bytesRead);
				}
			} finally {
				in.close();
			}
			normalFile.setSha1(HashUtil.encodeHexStr(mdAll.digest()));
			normalFile.setLength(offset);

			long fileLength = file.length(); // Important to check it now at the end.
			if (fileLength != offset) {
				logger.warn("sha: file.length() != bytesReadTotal :: File seems to be written concurrently! file='{}' file.length={} bytesReadTotal={}",
						file, fileLength, offset);
			}
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			monitor.done();
		}
	}
}
