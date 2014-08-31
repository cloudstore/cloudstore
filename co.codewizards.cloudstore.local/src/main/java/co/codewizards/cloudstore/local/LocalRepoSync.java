package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.util.Util.*;

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

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.local.persistence.CopyModification;
import co.codewizards.cloudstore.local.persistence.DeleteModification;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDao;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.ModificationDao;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.NormalFileDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.local.persistence.Symlink;

public class LocalRepoSync {

	private static final Logger logger = LoggerFactory.getLogger(LocalRepoSync.class);

	private final LocalRepoTransaction transaction;
	private final File localRoot;
	private final RepoFileDao repoFileDao;
	private final NormalFileDao normalFileDao;
	private final RemoteRepositoryDao remoteRepositoryDao;
	private final ModificationDao modificationDao;
	private final DeleteModificationDao deleteModificationDao;
	private Collection<RemoteRepository> remoteRepositories;

	private final Map<String, Set<String>> sha1AndLength2Paths = new HashMap<String, Set<String>>();

	public LocalRepoSync(final LocalRepoTransaction transaction) {
		this.transaction = AssertUtil.assertNotNull("transaction", transaction);
		localRoot = this.transaction.getLocalRepoManager().getLocalRoot();
		repoFileDao = this.transaction.getDao(RepoFileDao.class);
		normalFileDao = this.transaction.getDao(NormalFileDao.class);
		remoteRepositoryDao = this.transaction.getDao(RemoteRepositoryDao.class);
		modificationDao = this.transaction.getDao(ModificationDao.class);
		deleteModificationDao = this.transaction.getDao(DeleteModificationDao.class);
	}

	public void sync(final ProgressMonitor monitor) {
		sync(null, localRoot, monitor);
	}

	public RepoFile sync(final File file, final ProgressMonitor monitor) {
		if (!(AssertUtil.assertNotNull("file", file).isAbsolute()))
			throw new IllegalArgumentException("file is not absolute: " + file);

		if (localRoot.equals(file)) {
			return sync(null, file, monitor);
		}

		monitor.beginTask("Local sync...", 100);
		try {
			final File parentFile = file.getParentFile();
			RepoFile parentRepoFile = repoFileDao.getRepoFile(localRoot, parentFile);
			if (parentRepoFile == null) {
				// If the file does not exist and its RepoFile neither exists, then
				// this is in sync already and we can simply leave. This regularly
				// happens during the deletion of a directory which is the connection-point
				// of a remote-repository. The following re-up-sync then leads us here.
				// To speed up things, we simply quit as this is a valid state.
				if (!file.isSymbolicLink() && !file.exists() && repoFileDao.getRepoFile(localRoot, file) == null)
					return null;

				// In the unlikely event, that this is not a valid state, we simply sync all
				// and return.
				sync(null, localRoot, new SubProgressMonitor(monitor, 99));
				final RepoFile repoFile = repoFileDao.getRepoFile(localRoot, file);
				if (repoFile != null) // if it still does not exist, we run into the re-sync below and this might quickly return null, if that is correct or otherwise sync what's needed.
					return repoFile;

				parentRepoFile = repoFileDao.getRepoFile(localRoot, parentFile);
				if (parentRepoFile == null && parentFile.exists())
					throw new IllegalStateException("RepoFile not found for existing file/dir: " + parentFile.getAbsolutePath());
			}

			monitor.worked(1);

			return sync(parentRepoFile, file, new SubProgressMonitor(monitor, 99));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Sync the single given {@code file}.
	 * <p>
	 * If {@code file} is a directory, it recursively syncs all its children.
	 * @param parentRepoFile the parent. May be <code>null</code>, if the file is the repository's root-directory.
	 * For non-root files, this must not be <code>null</code>!
	 * @param file the file to be synced. Must not be <code>null</code>.
	 * @param monitor the progress-monitor. Must not be <code>null</code>.
	 * @return the {@link RepoFile} corresponding to the given {@code file}. Is <code>null</code>, if the given
	 * {@code file} does not exist; otherwise it is never <code>null</code>.
	 */
	private RepoFile sync(final RepoFile parentRepoFile, final File file, final ProgressMonitor monitor) {
		AssertUtil.assertNotNull("file", file);
		AssertUtil.assertNotNull("monitor", monitor);
		monitor.beginTask("Local sync...", 100);
		try {
			RepoFile repoFile = repoFileDao.getRepoFile(localRoot, file);

			// If the type changed - e.g. from normal file to directory - or if the file was deleted
			// we must delete the old instance.
			if (repoFile != null && !isRepoFileTypeCorrect(repoFile, file)) {
				deleteRepoFile(repoFile, false);
				repoFile = null;
			}

			final boolean fileIsSymlink = file.isSymbolicLink();
			if (repoFile == null) {
				if (!fileIsSymlink && !file.exists())
					return null;

				repoFile = createRepoFile(parentRepoFile, file, new SubProgressMonitor(monitor, 50));
				if (repoFile == null) { // ignoring non-normal files.
					return null;
				}
			} else if (isModified(repoFile, file))
				updateRepoFile(repoFile, file, new SubProgressMonitor(monitor, 50));
			else
				monitor.worked(50);

			final Set<String> childNames = new HashSet<String>();
			if (!fileIsSymlink) {
				final SubProgressMonitor childSubProgressMonitor = new SubProgressMonitor(monitor, 50);
				final File[] children = file.listFiles(new FilenameFilterSkipMetaDir());
				if (children != null && children.length > 0) {
					childSubProgressMonitor.beginTask("Local sync...", children.length);
					for (final File child : children) {
						childNames.add(child.getName());
						sync(repoFile, child, new SubProgressMonitor(childSubProgressMonitor, 1));
					}
				}
				childSubProgressMonitor.done();
			}

			final Collection<RepoFile> childRepoFiles = repoFileDao.getChildRepoFiles(repoFile);
			for (final RepoFile childRepoFile : childRepoFiles) {
				if (!childNames.contains(childRepoFile.getName())) {
					deleteRepoFile(childRepoFile);
				}
			}

			transaction.flush();
			return repoFile;
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
	private boolean isRepoFileTypeCorrect(final RepoFile repoFile, final File file) {
		AssertUtil.assertNotNull("repoFile", repoFile);
		AssertUtil.assertNotNull("file", file);

		if (file.isSymbolicLink())
			return repoFile instanceof Symlink;

		if (file.isFile())
			return repoFile instanceof NormalFile;

		if (file.isDirectory())
			return repoFile instanceof Directory;

		return false;
	}

	public boolean isModified(final RepoFile repoFile, final File file) {
		final long fileLastModified = file.getLastModifiedNoFollow();
		if (repoFile.getLastModified().getTime() != fileLastModified) {
			if (logger.isDebugEnabled()) {
				logger.debug("isModified: repoFile.lastModified != file.lastModified: repoFile.lastModified={} file.lastModified={} file={}",
						repoFile.getLastModified(), new Date(fileLastModified), file);
			}
			return true;
		}

		if (file.isSymbolicLink()) {
			if (!(repoFile instanceof Symlink))
				throw new IllegalArgumentException("repoFile is not an instance of Symlink! file=" + file);

			final Symlink symlink = (Symlink) repoFile;
			String fileSymlinkTarget;
			try {
				fileSymlinkTarget = file.readSymbolicLinkToPathString();
			} catch (final IOException e) {
				//as this is already checked as symbolicLink, this should never happen!
				throw new IllegalArgumentException(e);
			}
			return !fileSymlinkTarget.equals(symlink.getTarget());
		}
		else if (file.isFile()) {
			if (!(repoFile instanceof NormalFile))
				throw new IllegalArgumentException("repoFile is not an instance of NormalFile! file=" + file);

			final NormalFile normalFile = (NormalFile) repoFile;
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

	private RepoFile createRepoFile(final RepoFile parentRepoFile, final File file, final ProgressMonitor monitor) {
		if (parentRepoFile == null)
			throw new IllegalStateException("Creating the root this way is not possible! Why is it not existing, yet?!???");

		monitor.beginTask("Local sync...", 100);
		try {
			RepoFile repoFile;
			if (file.isSymbolicLink()) {
				final Symlink symlink = (Symlink) (repoFile = new Symlink());
				try {
					symlink.setTarget(file.readSymbolicLinkToPathString());
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			} else if (file.isDirectory()) {
				repoFile = new Directory();
			} else if (file.isFile()) {
				final NormalFile normalFile = (NormalFile) (repoFile = new NormalFile());
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
			repoFile.setLastModified(new Date(file.getLastModifiedNoFollow()));

			if (repoFile instanceof NormalFile)
				createCopyModificationsIfPossible((NormalFile)repoFile);

			return repoFileDao.makePersistent(repoFile);
		} finally {
			monitor.done();
		}
	}

	public void updateRepoFile(final RepoFile repoFile, final File file, final ProgressMonitor monitor) {
		logger.debug("updateRepoFile: id={} file={}", repoFile.getId(), file);
		monitor.beginTask("Local sync...", 100);
		try {
			if (file.isSymbolicLink()) {
				if (!(repoFile instanceof Symlink))
					throw new IllegalArgumentException("repoFile is not an instance of Symlink! file=" + file);

				final Symlink symlink = (Symlink) repoFile;
				try {
					symlink.setTarget(file.readSymbolicLinkToPathString());
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
			else if (file.isFile()) {
				if (!(repoFile instanceof NormalFile))
					throw new IllegalArgumentException("repoFile is not an instance of NormalFile!");

				final NormalFile normalFile = (NormalFile) repoFile;
				sha(normalFile, file, new SubProgressMonitor(monitor, 100));
			}
			repoFile.setLastSyncFromRepositoryId(null);
			repoFile.setLastModified(new Date(file.getLastModifiedNoFollow()));
		} finally {
			monitor.done();
		}
	}

	public void deleteRepoFile(final RepoFile repoFile) {
		deleteRepoFile(repoFile, true);
	}

	private void deleteRepoFile(final RepoFile repoFile, final boolean createDeleteModifications) {
		final RepoFile parentRepoFile = AssertUtil.assertNotNull("repoFile", repoFile).getParent();
		if (parentRepoFile == null)
			throw new IllegalStateException("Deleting the root is not possible!");

		final PersistenceManager pm = ((co.codewizards.cloudstore.local.LocalRepoTransactionImpl)transaction).getPersistenceManager();

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

	private int getMaxCopyModificationCount(final NormalFile newNormalFile) {
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

	private void createCopyModificationsIfPossible(final NormalFile newNormalFile) {
		// A CopyModification is not necessary for an empty file. And since this method is called
		// during RepoTransport.beginPutFile(...), we easily filter out this unwanted case already.
		// Changed to dynamic limit of CopyModifications depending on file size.
		// The bigger the file, the more it's worth the overhead.
		final int maxCopyModificationCount = getMaxCopyModificationCount(newNormalFile);
		if (maxCopyModificationCount < 1)
			return;

		final Set<String> fromPaths = new HashSet<String>();

		final Set<String> paths = sha1AndLength2Paths.get(getSha1AndLength(newNormalFile.getSha1(), newNormalFile.getLength()));
		if (paths != null) {
			final List<String> pathList = new ArrayList<>(paths);
			Collections.shuffle(pathList);

			for (final String path : pathList) {
				createCopyModifications(path, newNormalFile, fromPaths);
				if (fromPaths.size() >= maxCopyModificationCount)
					return;
			}
		}

		final List<NormalFile> normalFiles = new ArrayList<>(normalFileDao.getNormalFilesForSha1(newNormalFile.getSha1(), newNormalFile.getLength()));
		Collections.shuffle(normalFiles);
		for (final NormalFile normalFile : normalFiles) {
//			if (normalFile.isInProgress()) // Additional check. Do we really want this? I don't think so!
//				continue;

			if (newNormalFile.equals(normalFile)) // should never happen, because newNormalFile is not yet persisted, but we write robust code that doesn't break easily after refactoring.
				continue;

			createCopyModifications(normalFile, newNormalFile, fromPaths);
			if (fromPaths.size() >= maxCopyModificationCount)
				return;
		}

		final List<DeleteModification> deleteModifications = new ArrayList<>(deleteModificationDao.getDeleteModificationsForSha1(newNormalFile.getSha1(), newNormalFile.getLength()));
		Collections.shuffle(deleteModifications);
		for (final DeleteModification deleteModification : deleteModifications) {
			createCopyModifications(deleteModification, newNormalFile, fromPaths);
			if (fromPaths.size() >= maxCopyModificationCount)
				return;
		}
	}

	private void createCopyModifications(final DeleteModification deleteModification, final NormalFile toNormalFile, final Set<String> fromPaths) {
		AssertUtil.assertNotNull("deleteModification", deleteModification);
		AssertUtil.assertNotNull("toNormalFile", toNormalFile);
		AssertUtil.assertNotNull("fromPaths", fromPaths);

		if (deleteModification.getLength() != toNormalFile.getLength())
			throw new IllegalArgumentException("fromNormalFile.length != toNormalFile.length");

		if (!deleteModification.getSha1().equals(toNormalFile.getSha1()))
			throw new IllegalArgumentException("fromNormalFile.sha1 != toNormalFile.sha1");

		createCopyModifications(deleteModification.getPath(), toNormalFile, fromPaths);
	}

	private void createCopyModifications(final String fromPath, final NormalFile toNormalFile, final Set<String> fromPaths) {
		AssertUtil.assertNotNull("fromPath", fromPath);
		AssertUtil.assertNotNull("toNormalFile", toNormalFile);
		AssertUtil.assertNotNull("fromPaths", fromPaths);

		if (!fromPaths.add(fromPath)) // already done before => prevent duplicates.
			return;

		for (final RemoteRepository remoteRepository : getRemoteRepositories()) {
			final CopyModification modification = new CopyModification();
			modification.setRemoteRepository(remoteRepository);
			modification.setFromPath(fromPath);
			modification.setToPath(toNormalFile.getPath());
			modification.setLength(toNormalFile.getLength());
			modification.setSha1(toNormalFile.getSha1());
			modificationDao.makePersistent(modification);
		}
	}

	private void createCopyModifications(final NormalFile fromNormalFile, final NormalFile toNormalFile, final Set<String> fromPaths) {
		AssertUtil.assertNotNull("fromNormalFile", fromNormalFile);
		AssertUtil.assertNotNull("toNormalFile", toNormalFile);
		AssertUtil.assertNotNull("fromPaths", fromPaths);

		if (fromNormalFile.getLength() != toNormalFile.getLength())
			throw new IllegalArgumentException("fromNormalFile.length != toNormalFile.length");

		if (!fromNormalFile.getSha1().equals(toNormalFile.getSha1()))
			throw new IllegalArgumentException("fromNormalFile.sha1 != toNormalFile.sha1");

		createCopyModifications(fromNormalFile.getPath(), toNormalFile, fromPaths);
	}

	private void createDeleteModifications(final RepoFile repoFile) {
		AssertUtil.assertNotNull("repoFile", repoFile);
		NormalFile normalFile = null;
		if (repoFile instanceof NormalFile)
			normalFile = (NormalFile) repoFile;

		for (final RemoteRepository remoteRepository : getRemoteRepositories()) {
			final DeleteModification modification = new DeleteModification();
			modification.setRemoteRepository(remoteRepository);
			modification.setPath(repoFile.getPath());
			modification.setLength(normalFile == null ? -1 : normalFile.getLength());
			modification.setSha1(normalFile == null ? null : normalFile.getSha1());
			modificationDao.makePersistent(modification);
		}
	}

	private Collection<RemoteRepository> getRemoteRepositories() {
		if (remoteRepositories == null)
			remoteRepositories = Collections.unmodifiableCollection(remoteRepositoryDao.getObjects());

		return remoteRepositories;
	}

	private void deleteRepoFileWithAllChildrenRecursively(final RepoFile repoFile) {
		AssertUtil.assertNotNull("repoFile", repoFile);
		for (final RepoFile childRepoFile : repoFileDao.getChildRepoFiles(repoFile)) {
			deleteRepoFileWithAllChildrenRecursively(childRepoFile);
		}
		putIntoSha1AndLength2PathsIfNormalFile(repoFile);
		repoFileDao.deletePersistent(repoFile);
	}

	private void putIntoSha1AndLength2PathsIfNormalFile(final RepoFile repoFile) {
		if (repoFile instanceof NormalFile) {
			final NormalFile normalFile = (NormalFile) repoFile;
			final String sha1AndLength = getSha1AndLength(normalFile.getSha1(), normalFile.getLength());
			Set<String> paths = sha1AndLength2Paths.get(sha1AndLength);
			if (paths == null) {
				paths = new HashSet<>(1);
				sha1AndLength2Paths.put(sha1AndLength, paths);
			}
			paths.add(normalFile.getPath());
		}
	}

	private String getSha1AndLength(final String sha1, final long length) {
		return sha1 + ':' + length;
	}

	private void sha(final NormalFile normalFile, final File file, final ProgressMonitor monitor) {
		monitor.beginTask("Local sync...", (int)Math.min(file.length(), Integer.MAX_VALUE));
		try {
			normalFile.getFileChunks().clear();
			transaction.flush();

			final MessageDigest mdAll = MessageDigest.getInstance(HashUtil.HASH_ALGORITHM_SHA);
			final MessageDigest mdChunk = MessageDigest.getInstance(HashUtil.HASH_ALGORITHM_SHA);

			final int bufLength = 32 * 1024;
			final int chunkLength = 32 * bufLength; // 1 MiB chunk size

			long offset = 0;
			final InputStream in = file.createFileInputStream();
			try {
				FileChunk fileChunk = null;

				final byte[] buf = new byte[bufLength];
				while (true) {
					if (fileChunk == null) {
						fileChunk = new FileChunk();
						fileChunk.setRepoFile(normalFile);
						fileChunk.setOffset(offset);
						fileChunk.setLength(0);
						mdChunk.reset();
					}

					final int bytesRead = in.read(buf, 0, buf.length);

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

			final long fileLength = file.length(); // Important to check it now at the end.
			if (fileLength != offset) {
				logger.warn("sha: file.length() != bytesReadTotal :: File seems to be written concurrently! file='{}' file.length={} bytesReadTotal={}",
						file, fileLength, offset);
			}
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			monitor.done();
		}
	}
}
