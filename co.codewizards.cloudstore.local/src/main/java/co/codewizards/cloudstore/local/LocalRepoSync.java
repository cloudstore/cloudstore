package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.local.persistence.CopyModification;
import co.codewizards.cloudstore.local.persistence.DeleteModification;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDAO;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.ModificationDAO;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.NormalFileDAO;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDAO;

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

	private final Map<String, Set<String>> sha1AndLength2Paths = new HashMap<String, Set<String>>();

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

			// If the type changed - e.g. from normal file to directory - or if the file was deleted
			// we must delete the old instance.
			if (repoFile != null && !isRepoFileTypeCorrect(repoFile, file)) {
				deleteRepoFile(repoFile, false);
				repoFile = null;
			}

			if (repoFile == null) {
				if (!file.exists())
					return;

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
			transaction.flush();
		} finally {
			monitor.done();
		}
	}

	/**
	 * Determines, if the type of the given {@code repoFile} matches the type
	 * of the file in the file system referenced by the given {@code file}.
	 * @param repoFile the {@link RepoFile} currently representing the given {@code file} in the database.
	 * Must not be <code>null</code>.
	 * @param file the file in the file system. Must not be <code>null</code>.
	 * @return <code>true</code>, if both types correspond to each other; <code>false</code> otherwise. If
	 * the file does not exist (anymore) in the file system, <code>false</code> is returned, too.
	 */
	private boolean isRepoFileTypeCorrect(RepoFile repoFile, File file) {
		assertNotNull("repoFile", repoFile);
		assertNotNull("file", file);

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
				if (file.exists())
					logger.warn("createRepoFile: File exists, but is neither a directory nor a normal file! Skipping: {}", file);
				else
					logger.warn("createRepoFile: File does not exist! Skipping: {}", file);

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

		PersistenceManager pm = ((co.codewizards.cloudstore.local.LocalRepoTransactionImpl)transaction).getPersistenceManager();

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

	private int getMaxCopyModificationCount(NormalFile newNormalFile) {
		final long fileLength = newNormalFile.getLength();
		if (fileLength < 10 * 1024) // 10 KiB
			return 0;

		if (fileLength < 100 * 1024) // 100 KiB
			return 1;

		if (fileLength < 1024 * 1024) // 1 MiB
			return 2;

		if (fileLength < 10 * 1024 * 1024) // 10 MiB
			return 3;

		if (fileLength < 100 * 1024 * 1024) // 100 MiB
			return 5;

		if (fileLength < 1024 * 1024 * 1024) // 1 GiB
			return 7;

		if (fileLength < 10 * 1024 * 1024 * 1024) // 10 GiB
			return 9;

		return 11;
	}

	private void createCopyModificationsIfPossible(NormalFile newNormalFile) {
		// A CopyModification is not necessary for an empty file. And since this method is called
		// during RepoTransport.beginPutFile(...), we easily filter out this unwanted case already.
		// Changed to dynamic limit of CopyModifications depending on file size.
		// The bigger the file, the more it's worth the overhead.
		final int maxCopyModificationCount = getMaxCopyModificationCount(newNormalFile);
		if (maxCopyModificationCount < 1)
			return;

		Set<String> fromPaths = new HashSet<String>();

		Set<String> paths = sha1AndLength2Paths.get(getSha1AndLength(newNormalFile.getSha1(), newNormalFile.getLength()));
		if (paths != null) {
			List<String> pathList = new ArrayList<>(paths);
			Collections.shuffle(pathList);

			for (String path : pathList) {
				createCopyModifications(path, newNormalFile, fromPaths);
				if (fromPaths.size() >= maxCopyModificationCount)
					return;
			}
		}

		List<NormalFile> normalFiles = new ArrayList<>(normalFileDAO.getNormalFilesForSha1(newNormalFile.getSha1(), newNormalFile.getLength()));
		Collections.shuffle(normalFiles);
		for (NormalFile normalFile : normalFiles) {
//			if (normalFile.isInProgress()) // Additional check. Do we really want this? I don't think so!
//				continue;

			if (newNormalFile.equals(normalFile)) // should never happen, because newNormalFile is not yet persisted, but we write robust code that doesn't break easily after refactoring.
				continue;

			createCopyModifications(normalFile, newNormalFile, fromPaths);
			if (fromPaths.size() >= maxCopyModificationCount)
				return;
		}

		List<DeleteModification> deleteModifications = new ArrayList<>(deleteModificationDAO.getDeleteModificationsForSha1(newNormalFile.getSha1(), newNormalFile.getLength()));
		Collections.shuffle(deleteModifications);
		for (DeleteModification deleteModification : deleteModifications) {
			createCopyModifications(deleteModification, newNormalFile, fromPaths);
			if (fromPaths.size() >= maxCopyModificationCount)
				return;
		}
	}

	private void createCopyModifications(DeleteModification deleteModification, NormalFile toNormalFile, Set<String> fromPaths) {
		assertNotNull("deleteModification", deleteModification);
		assertNotNull("toNormalFile", toNormalFile);
		assertNotNull("fromPaths", fromPaths);

		if (deleteModification.getLength() != toNormalFile.getLength())
			throw new IllegalArgumentException("fromNormalFile.length != toNormalFile.length");

		if (!deleteModification.getSha1().equals(toNormalFile.getSha1()))
			throw new IllegalArgumentException("fromNormalFile.sha1 != toNormalFile.sha1");

		createCopyModifications(deleteModification.getPath(), toNormalFile, fromPaths);
	}

	private void createCopyModifications(String fromPath, NormalFile toNormalFile, Set<String> fromPaths) {
		assertNotNull("fromPath", fromPath);
		assertNotNull("toNormalFile", toNormalFile);
		assertNotNull("fromPaths", fromPaths);

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

	private void createCopyModifications(NormalFile fromNormalFile, NormalFile toNormalFile, Set<String> fromPaths) {
		assertNotNull("fromNormalFile", fromNormalFile);
		assertNotNull("toNormalFile", toNormalFile);
		assertNotNull("fromPaths", fromPaths);

		if (fromNormalFile.getLength() != toNormalFile.getLength())
			throw new IllegalArgumentException("fromNormalFile.length != toNormalFile.length");

		if (!fromNormalFile.getSha1().equals(toNormalFile.getSha1()))
			throw new IllegalArgumentException("fromNormalFile.sha1 != toNormalFile.sha1");

		createCopyModifications(fromNormalFile.getPath(), toNormalFile, fromPaths);
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
		putIntoSha1AndLength2PathsIfNormalFile(repoFile);
		repoFileDAO.deletePersistent(repoFile);
	}

	private void putIntoSha1AndLength2PathsIfNormalFile(RepoFile repoFile) {
		if (repoFile instanceof NormalFile) {
			NormalFile normalFile = (NormalFile) repoFile;
			String sha1AndLength = getSha1AndLength(normalFile.getSha1(), normalFile.getLength());
			Set<String> paths = sha1AndLength2Paths.get(sha1AndLength);
			if (paths == null) {
				paths = new HashSet<>(1);
				sha1AndLength2Paths.put(sha1AndLength, paths);
			}
			paths.add(normalFile.getPath());
		}
	}

	private String getSha1AndLength(String sha1, long length) {
		return sha1 + ':' + length;
	}

	private void sha(NormalFile normalFile, File file, ProgressMonitor monitor) {
		monitor.beginTask("Local sync...", (int)Math.min(file.length(), Integer.MAX_VALUE));
		try {
			normalFile.getFileChunks().clear();
			transaction.flush();

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
