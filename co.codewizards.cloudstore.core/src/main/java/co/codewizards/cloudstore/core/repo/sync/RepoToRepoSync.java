package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.concurrent.CallerBlocksPolicy;
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.CopyModificationDTO;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.FileChunkDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTOTreeNode;
import co.codewizards.cloudstore.core.dto.SymlinkDTO;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.UrlUtil;

/**
 * Logic for synchronising a local with a remote repository.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class RepoToRepoSync {
	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSync.class);

	/**
	 * Sync in the inverse direction. This is only for testing whether the RepoTransport implementations
	 * are truly symmetric. It is less efficient! Therefore, this must NEVER be true in production!!!
	 */
	private static final boolean TEST_INVERSE = false;

	private final File localRoot;
	private final URL remoteRoot;
	private final LocalRepoManager localRepoManager;
	private final RepoTransport localRepoTransport;
	private final RepoTransport remoteRepoTransport;
	private final UUID localRepositoryId;
	private final UUID remoteRepositoryId;

	private ExecutorService localSyncExecutor;
	private Future<Void> localSyncFuture;

	/**
	 * Create an instance.
	 * @param localRoot the root of the local repository or any file/directory inside it. This is
	 * automatically adjusted to fit the connection-point to the remote repository (the remote
	 * repository might be connected to a sub-directory).
	 * @param remoteRoot the root of the remote repository. This must exactly match the connection point.
	 * If a sub-directory of the remote repository is connected to the local repository, this sub-directory
	 * must be referenced here.
	 */
	public RepoToRepoSync(File localRoot, final URL remoteRoot) {
		File localRootWithoutPathPrefix = LocalRepoHelper.getLocalRootContainingFile(assertNotNull("localRoot", localRoot));
		this.remoteRoot = UrlUtil.canonicalizeURL(assertNotNull("remoteRoot", remoteRoot));
		localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRootWithoutPathPrefix);
		this.localRoot = localRoot = new File(localRootWithoutPathPrefix, localRepoManager.getLocalPathPrefixOrFail(remoteRoot));

		localRepositoryId = localRepoManager.getRepositoryId();
		if (localRepositoryId == null)
			throw new IllegalStateException("localRepoManager.getRepositoryId() returned null!");

		remoteRepositoryId = localRepoManager.getRemoteRepositoryIdOrFail(remoteRoot);

		remoteRepoTransport = createRepoTransport(remoteRoot, localRepositoryId);
		localRepoTransport = createRepoTransport(localRoot, remoteRepositoryId);
	}

	public void sync(final ProgressMonitor monitor) {
		assertNotNull("monitor", monitor);
		monitor.beginTask("Synchronising...", 201);
		try {
			readRemoteRepositoryIdFromRepoTransport();
			monitor.worked(1);

			if (localSyncExecutor != null)
				throw new IllegalStateException("localSyncExecutor != null");
			if (localSyncFuture != null)
				throw new IllegalStateException("localSyncFuture != null");

			localSyncExecutor = Executors.newFixedThreadPool(1);
			localSyncFuture = localSyncExecutor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					logger.info("sync: locally syncing {} ('{}')", localRepositoryId, localRoot);
					localRepoManager.localSync(new SubProgressMonitor(monitor, 50));
					return null;
				}
			});

			if (!TEST_INVERSE) { // This is the normal sync (NOT test).
				logger.info("sync: down: fromID={} from='{}' toID={} to='{}'", remoteRepositoryId, remoteRoot, localRepositoryId, localRoot);
				sync(remoteRepoTransport, true, localRepoTransport, new SubProgressMonitor(monitor, 50));

				if (localSyncExecutor != null)
					throw new IllegalStateException("localSyncExecutor != null");
				if (localSyncFuture != null)
					throw new IllegalStateException("localSyncFuture != null");

				logger.info("sync: up: fromID={} from='{}' toID={} to='{}'", localRepositoryId, localRoot, remoteRepositoryId, remoteRoot);
				sync(localRepoTransport, false, remoteRepoTransport, new SubProgressMonitor(monitor, 50));

				// Immediately sync back to make sure the changes we caused don't cause problems later
				// (right now there's very likely no collision and this should be very fast).
				logger.info("sync: down again: fromID={} from='{}' toID={} to='{}'", remoteRepositoryId, remoteRoot, localRepositoryId, localRoot);
				sync(remoteRepoTransport, false, localRepoTransport, new SubProgressMonitor(monitor, 50));
			}
			else { // THIS IS FOR TESTING ONLY!
				logger.info("sync: locally syncing on *remote* side {} ('{}')", localRepositoryId, localRoot);
				remoteRepoTransport.getChangeSetDTO(true); // trigger the local sync on the remote side (we don't need the change set)

				waitForAndCheckLocalSyncFuture();

				logger.info("sync: up: fromID={} from='{}' toID={} to='{}'", localRepositoryId, localRoot, remoteRepositoryId, remoteRoot);
				sync(localRepoTransport, false, remoteRepoTransport, new SubProgressMonitor(monitor, 50));

				logger.info("sync: down: fromID={} from='{}' toID={} to='{}'", remoteRepositoryId, remoteRoot, localRepositoryId, localRoot);
				sync(remoteRepoTransport, false, localRepoTransport, new SubProgressMonitor(monitor, 50));

				logger.info("sync: up again: fromID={} from='{}' toID={} to='{}'", localRepositoryId, localRoot, remoteRepositoryId, remoteRoot);
				sync(localRepoTransport, false, remoteRepoTransport, new SubProgressMonitor(monitor, 50));
			}
		} finally {
			monitor.done();
		}
	}

	private void waitForAndCheckLocalSyncFutureIfExists() {
		if (localSyncFuture != null)
			waitForAndCheckLocalSyncFuture();
	}

	private void waitForAndCheckLocalSyncFuture() {
		try {
			assertNotNull("localSyncFuture", localSyncFuture).get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		assertNotNull("localSyncExecutor", localSyncExecutor).shutdown();
		localSyncFuture = null;
		localSyncExecutor = null;
	}

	private void readRemoteRepositoryIdFromRepoTransport() {
		UUID repositoryId = remoteRepoTransport.getRepositoryId();
		if (repositoryId == null)
			throw new IllegalStateException("remoteRepoTransport.getRepositoryId() returned null!");

		if (!repositoryId.equals(remoteRepositoryId))
			throw new IllegalStateException(
					String.format("remoteRepoTransport.getRepositoryId() does not match repositoryId in local DB! %s != %s", repositoryId, remoteRepositoryId));
	}

	private RepoTransport createRepoTransport(File rootFile, UUID clientRepositoryId) {
		URL rootURL;
		try {
			rootURL = rootFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return createRepoTransport(rootURL, clientRepositoryId);
	}

	private RepoTransport createRepoTransport(URL remoteRoot, UUID clientRepositoryId) {
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot);
		return repoTransportFactory.createRepoTransport(remoteRoot, clientRepositoryId);
	}

	private void sync(RepoTransport fromRepoTransport, boolean fromRepoLocalSync, RepoTransport toRepoTransport, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			ChangeSetDTO changeSetDTO = fromRepoTransport.getChangeSetDTO(fromRepoLocalSync);
			monitor.worked(8);

			waitForAndCheckLocalSyncFutureIfExists();

			sync(fromRepoTransport, toRepoTransport, changeSetDTO, new SubProgressMonitor(monitor, 90));

			fromRepoTransport.endSyncFromRepository();
			toRepoTransport.endSyncToRepository(changeSetDTO.getRepositoryDTO().getRevision());
			monitor.worked(2);
		} finally {
			monitor.done();
		}
	}

	private void sync(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, ChangeSetDTO changeSetDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", changeSetDTO.getModificationDTOs().size() + 2 * changeSetDTO.getRepoFileDTOs().size());
		try {
			RepoFileDTOTreeNode repoFileDTOTree = RepoFileDTOTreeNode.createTree(changeSetDTO.getRepoFileDTOs());
			if (repoFileDTOTree != null) {
				sync(fromRepoTransport, toRepoTransport, repoFileDTOTree,
						new Class<?>[] { DirectoryDTO.class }, new Class<?>[0],
						new SubProgressMonitor(monitor, repoFileDTOTree.size()));
			}

			syncModifications(fromRepoTransport, toRepoTransport, changeSetDTO.getModificationDTOs(),
					new SubProgressMonitor(monitor, changeSetDTO.getModificationDTOs().size()));

			if (repoFileDTOTree != null) {
				sync(fromRepoTransport, toRepoTransport, repoFileDTOTree,
						new Class<?>[] { RepoFileDTO.class }, new Class<?>[] { DirectoryDTO.class },
						new SubProgressMonitor(monitor, repoFileDTOTree.size()));
			}
		} finally {
			monitor.done();
		}
	}

	private void sync(RepoTransport fromRepoTransport, RepoTransport toRepoTransport,
			RepoFileDTOTreeNode repoFileDTOTree,
			Class<?>[] repoFileDTOClassesIncl, Class<?>[] repoFileDTOClassesExcl,
			ProgressMonitor monitor) {
		assertNotNull("fromRepoTransport", fromRepoTransport);
		assertNotNull("toRepoTransport", toRepoTransport);
		assertNotNull("repoFileDTOTree", repoFileDTOTree);
		assertNotNull("repoFileDTOClassesIncl", repoFileDTOClassesIncl);
		assertNotNull("repoFileDTOClassesExcl", repoFileDTOClassesExcl);
		assertNotNull("monitor", monitor);

		Map<Class<?>, Boolean> repoFileDTOClass2Included = new HashMap<Class<?>, Boolean>();
		Map<Class<?>, Boolean> repoFileDTOClass2Excluded = new HashMap<Class<?>, Boolean>();

		monitor.beginTask("Synchronising...", repoFileDTOTree.size());
		try {
			LinkedList<Future<Void>> syncFileAsynchronouslyFutures = new LinkedList<Future<Void>>();
			ThreadPoolExecutor syncFileAsynchronouslyExecutor = createSyncFileAsynchronouslyExecutor();
			try {
				for (RepoFileDTOTreeNode repoFileDTOTreeNode : repoFileDTOTree) {
					RepoFileDTO repoFileDTO = repoFileDTOTreeNode.getRepoFileDTO();
					Class<? extends RepoFileDTO> repoFileDTOClass = repoFileDTO.getClass();

					Boolean included = repoFileDTOClass2Included.get(repoFileDTOClass);
					if (included == null) {
						included = false;
						for (Class<?> clazz : repoFileDTOClassesIncl) {
							if (clazz.isAssignableFrom(repoFileDTOClass)) {
								included = true;
								break;
							}
						}
						repoFileDTOClass2Included.put(repoFileDTOClass, included);
					}

					Boolean excluded = repoFileDTOClass2Excluded.get(repoFileDTOClass);
					if (excluded == null) {
						excluded = false;
						for (Class<?> clazz : repoFileDTOClassesExcl) {
							if (clazz.isAssignableFrom(repoFileDTOClass)) {
								excluded = true;
								break;
							}
						}
						repoFileDTOClass2Excluded.put(repoFileDTOClass, excluded);
					}

					if (!included || excluded) {
						monitor.worked(1);
						continue;
					}

					if (repoFileDTO instanceof DirectoryDTO)
						syncDirectory(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, (DirectoryDTO) repoFileDTO, new SubProgressMonitor(monitor, 1));
					else if (repoFileDTO instanceof NormalFileDTO) {
						Future<Void> syncFileAsynchronouslyFuture = syncFileAsynchronously(syncFileAsynchronouslyExecutor,
								fromRepoTransport, toRepoTransport,
								repoFileDTOTreeNode, repoFileDTO, new SubProgressMonitor(monitor, 1));
						syncFileAsynchronouslyFutures.add(syncFileAsynchronouslyFuture);
					}
					else if (repoFileDTO instanceof SymlinkDTO)
						syncSymlink(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, (SymlinkDTO) repoFileDTO, new SubProgressMonitor(monitor, 1));
					else
						throw new IllegalStateException("Unsupported RepoFileDTO type: " + repoFileDTO);

					checkAndEvictDoneSyncFileAsynchronouslyFutures(syncFileAsynchronouslyFutures);
				}

				checkAndEvictAllSyncFileAsynchronouslyFutures(syncFileAsynchronouslyFutures);
			} finally {
				syncFileAsynchronouslyExecutor.shutdown();
			}
		} finally {
			monitor.done();
		}
	}

	private SortedMap<Long, Collection<ModificationDTO>> getLocalRevision2ModificationDTOs(Collection<ModificationDTO> modificationDTOs) {
		SortedMap<Long, Collection<ModificationDTO>> map = new TreeMap<Long, Collection<ModificationDTO>>();
		for (ModificationDTO modificationDTO : modificationDTOs) {
			long localRevision = modificationDTO.getLocalRevision();
			Collection<ModificationDTO> collection = map.get(localRevision);
			if (collection == null) {
				collection = new ArrayList<ModificationDTO>();
				map.put(localRevision, collection);
			}
			collection.add(modificationDTO);
		}
		return map;
	}

	private void syncModifications(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, Collection<ModificationDTO> modificationDTOs, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", modificationDTOs.size());
		try {
			SortedMap<Long,Collection<ModificationDTO>> localRevision2ModificationDTOs = getLocalRevision2ModificationDTOs(modificationDTOs);
			for (Map.Entry<Long,Collection<ModificationDTO>> me : localRevision2ModificationDTOs.entrySet()) {
				ModificationDTOSet modificationDTOSet = new ModificationDTOSet(me.getValue());

				for (List<CopyModificationDTO> copyModificationDTOs : modificationDTOSet.getFromPath2CopyModificationDTOs().values()) {

					for (Iterator<CopyModificationDTO> itCopyMod = copyModificationDTOs.iterator(); itCopyMod.hasNext(); ) {
						CopyModificationDTO copyModificationDTO = itCopyMod.next();
						List<DeleteModificationDTO> deleteModificationDTOs = modificationDTOSet.getPath2DeleteModificationDTOs().get(copyModificationDTO.getFromPath());
						boolean moveInstead = false;
						if (!itCopyMod.hasNext() && deleteModificationDTOs != null && !deleteModificationDTOs.isEmpty())
							moveInstead = true;

						if (moveInstead) {
							logger.info("syncModifications: Moving from '{}' to '{}'", copyModificationDTO.getFromPath(), copyModificationDTO.getToPath());
							toRepoTransport.move(copyModificationDTO.getFromPath(), copyModificationDTO.getToPath());
						}
						else {
							logger.info("syncModifications: Copying from '{}' to '{}'", copyModificationDTO.getFromPath(), copyModificationDTO.getToPath());
							toRepoTransport.copy(copyModificationDTO.getFromPath(), copyModificationDTO.getToPath());
						}

						if (!moveInstead && deleteModificationDTOs != null) {
							for (DeleteModificationDTO deleteModificationDTO : deleteModificationDTOs) {
								logger.info("syncModifications: Deleting '{}'", deleteModificationDTO.getPath());
								toRepoTransport.delete(deleteModificationDTO.getPath());
							}
						}
					}
				}

				for (List<DeleteModificationDTO> deleteModificationDTOs : modificationDTOSet.getPath2DeleteModificationDTOs().values()) {
					for (DeleteModificationDTO deleteModificationDTO : deleteModificationDTOs) {
						logger.info("syncModifications: Deleting '{}'", deleteModificationDTO.getPath());
						toRepoTransport.delete(deleteModificationDTO.getPath());
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	private void syncDirectory(
			final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDTOTreeNode repoFileDTOTreeNode, final DirectoryDTO directoryDTO, final ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			final String path = repoFileDTOTreeNode.getPath();
			logger.info("syncDirectory: path='{}'", path);
			try {
				toRepoTransport.makeDirectory(path, directoryDTO.getLastModified());
			} catch (DeleteModificationCollisionException x) {
				logger.info("DeleteModificationCollisionException during makeDirectory: {}", path);
				if (logger.isDebugEnabled())
					logger.debug(x.toString(), x);

				return;
			}
		} finally {
			monitor.done();
		}
	}

	private void syncSymlink(
			final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDTOTreeNode repoFileDTOTreeNode, final SymlinkDTO symlinkDTO, final SubProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			final String path = repoFileDTOTreeNode.getPath();
			try {
				toRepoTransport.makeSymlink(path, symlinkDTO.getTarget(), symlinkDTO.getLastModified());
			} catch (DeleteModificationCollisionException x) {
				logger.info("DeleteModificationCollisionException during makeSymlink: {}", path);
				if (logger.isDebugEnabled())
					logger.debug(x.toString(), x);

				return;
			}
		} finally {
			monitor.done();
		}
	}

	private Future<Void> syncFileAsynchronously(
			final ThreadPoolExecutor syncFileAsynchronouslyExecutor,
			final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDTOTreeNode repoFileDTOTreeNode, final RepoFileDTO normalFileDTO, final ProgressMonitor monitor) {

		Callable<Void> callable = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				syncFile(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, normalFileDTO, monitor);
				return null;
			}
		};
		Future<Void> future = syncFileAsynchronouslyExecutor.submit(callable);
		return future;
	}

	private void syncFile(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, RepoFileDTOTreeNode repoFileDTOTreeNode, RepoFileDTO normalFileDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			final String path = repoFileDTOTreeNode.getPath();
			logger.info("syncFile: path='{}'", path);

			final RepoFileDTO fromRepoFileDTO = fromRepoTransport.getRepoFileDTO(path);
			if (fromRepoFileDTO == null) {
				logger.warn("File was deleted during sync on source side: {}", path);
				return;
			}
			if (!(fromRepoFileDTO instanceof NormalFileDTO)) {
				logger.warn("Normal file was replaced by a directory (or another type) during sync on source side: {}", path);
				return;
			}
			monitor.worked(10);

			NormalFileDTO fromNormalFileDTO = (NormalFileDTO) fromRepoFileDTO;

			final RepoFileDTO toRepoFileDTO = toRepoTransport.getRepoFileDTO(path);
			if (areFilesExistingAndEqual(fromRepoFileDTO, toRepoFileDTO)) {
				logger.info("File is already equal on destination side (sha1='{}'): {}", fromNormalFileDTO.getSha1(), path);
				return;
			}
			monitor.worked(10);

			logger.info("Beginning to copy file (from.sha1='{}' to.sha1='{}'): {}", fromNormalFileDTO.getSha1(),
					toRepoFileDTO instanceof NormalFileDTO ? ((NormalFileDTO)toRepoFileDTO).getSha1() : "<NoInstanceOf_NormalFileDTO>",
							path);

			final NormalFileDTO toNormalFileDTO;
			if (toRepoFileDTO instanceof NormalFileDTO)
				toNormalFileDTO = (NormalFileDTO) toRepoFileDTO;
			else
				toNormalFileDTO = new NormalFileDTO(); // dummy (null-object pattern)

			try {
				toRepoTransport.beginPutFile(path);
			} catch (DeleteModificationCollisionException x) {
				logger.info("DeleteModificationCollisionException during beginPutFile: {}", path);
				if (logger.isDebugEnabled())
					logger.debug(x.toString(), x);

				return;
			}
			monitor.worked(1);

			final Map<Long, FileChunkDTO> offset2ToTempFileChunkDTO = new HashMap<>(toNormalFileDTO.getTempFileChunkDTOs().size());
			for (FileChunkDTO toTempFileChunkDTO : toNormalFileDTO.getTempFileChunkDTOs())
				offset2ToTempFileChunkDTO.put(toTempFileChunkDTO.getOffset(), toTempFileChunkDTO);

			logger.debug("Comparing {} FileChunkDTOs. path='{}'", fromNormalFileDTO.getFileChunkDTOs().size(), path);
			final List<FileChunkDTO> fromFileChunkDTOsDirty = new ArrayList<FileChunkDTO>();
			final Iterator<FileChunkDTO> toFileChunkDTOIterator = toNormalFileDTO.getFileChunkDTOs().iterator();
			int fileChunkIndex = -1;
			for (final FileChunkDTO fromFileChunkDTO : fromNormalFileDTO.getFileChunkDTOs()) {
				final FileChunkDTO toFileChunkDTO = toFileChunkDTOIterator.hasNext() ? toFileChunkDTOIterator.next() : null;
				++fileChunkIndex;
				final FileChunkDTO toTempFileChunkDTO = offset2ToTempFileChunkDTO.get(fromFileChunkDTO.getOffset());
				if (toTempFileChunkDTO == null) {
					if (toFileChunkDTO != null
							&& equal(fromFileChunkDTO.getOffset(), toFileChunkDTO.getOffset())
							&& equal(fromFileChunkDTO.getLength(), toFileChunkDTO.getLength())
							&& equal(fromFileChunkDTO.getSha1(), toFileChunkDTO.getSha1())) {
						if (logger.isTraceEnabled()) {
							logger.trace("Skipping clean FileChunkDTO. index={} offset={} sha1='{}'",
									fileChunkIndex, fromFileChunkDTO.getOffset(), fromFileChunkDTO.getSha1());
						}
						continue;
					}
				}
				else {
					if (equal(fromFileChunkDTO.getOffset(), toTempFileChunkDTO.getOffset())
							&& equal(fromFileChunkDTO.getLength(), toTempFileChunkDTO.getLength())
							&& equal(fromFileChunkDTO.getSha1(), toTempFileChunkDTO.getSha1())) {
						if (logger.isTraceEnabled()) {
							logger.trace("Skipping clean temporary FileChunkDTO. index={} offset={} sha1='{}'",
									fileChunkIndex, fromFileChunkDTO.getOffset(), fromFileChunkDTO.getSha1());
						}
						continue;
					}
				}

				if (logger.isTraceEnabled()) {
					logger.trace("Enlisting dirty FileChunkDTO. index={} fromOffset={} toOffset={} fromSha1='{}' toSha1='{}'",
							fileChunkIndex, fromFileChunkDTO.getOffset(),
							(toFileChunkDTO == null ? "null" : toFileChunkDTO.getOffset()),
							fromFileChunkDTO.getSha1(),
							(toFileChunkDTO == null ? "null" : toFileChunkDTO.getSha1()));
				}
				fromFileChunkDTOsDirty.add(fromFileChunkDTO);
			}

			logger.info("Need to copy {} dirty file-chunks (of {} total). path='{}'",
					fromFileChunkDTOsDirty.size(), fromNormalFileDTO.getFileChunkDTOs().size(), path);

			ProgressMonitor subMonitor = new SubProgressMonitor(monitor, 73);
			subMonitor.beginTask("Synchronising...", fromFileChunkDTOsDirty.size());
			fileChunkIndex = -1;
			long bytesCopied = 0;
			long copyChunksBeginTimestamp = System.currentTimeMillis();
			for (FileChunkDTO fileChunkDTO : fromFileChunkDTOsDirty) {
				++fileChunkIndex;
				if (logger.isTraceEnabled()) {
					logger.trace("Reading data for dirty FileChunkDTO (index {} of {}). path='{}' offset={}",
							fileChunkIndex, fromFileChunkDTOsDirty.size(), path, fileChunkDTO.getOffset());
				}
				byte[] fileData = fromRepoTransport.getFileData(path, fileChunkDTO.getOffset(), fileChunkDTO.getLength());

				if (fileData == null || fileData.length != fileChunkDTO.getLength() || !sha1(fileData).equals(fileChunkDTO.getSha1())) {
					logger.warn("Source file was modified or deleted during sync: {}", path);
					// The file is left in state 'inProgress'. Thus it should definitely not be synced back in the opposite
					// direction. The file should be synced again in the correct direction in the next run (after the source
					// repo did a local sync, too).
					return;
				}

				if (logger.isTraceEnabled()) {
					logger.trace("Writing data for dirty FileChunkDTO ({} of {}). path='{}' offset={}",
							fileChunkIndex + 1, fromFileChunkDTOsDirty.size(), path, fileChunkDTO.getOffset());
				}
				toRepoTransport.putFileData(path, fileChunkDTO.getOffset(), fileData);
				bytesCopied += fileData.length;
				subMonitor.worked(1);
			}
			subMonitor.done();

			logger.info("Copied {} dirty file-chunks with together {} bytes in {} ms. path='{}'",
					fromFileChunkDTOsDirty.size(), bytesCopied, System.currentTimeMillis() - copyChunksBeginTimestamp, path);

			toRepoTransport.endPutFile(
					path, fromNormalFileDTO.getLastModified(),
					fromNormalFileDTO.getLength(), fromNormalFileDTO.getSha1());

			monitor.worked(6);
		} finally {
			monitor.done();
		}
	}

	private String sha1(byte[] data) {
		assertNotNull("data", data);
		try {
			byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(data));
			return HashUtil.encodeHexStr(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean areFilesExistingAndEqual(RepoFileDTO fromRepoFileDTO, RepoFileDTO toRepoFileDTO) {
		if (!(fromRepoFileDTO instanceof NormalFileDTO))
			return false;

		if (!(toRepoFileDTO instanceof NormalFileDTO))
			return false;

		NormalFileDTO fromNormalFileDTO = (NormalFileDTO) fromRepoFileDTO;
		NormalFileDTO toNormalFileDTO = (NormalFileDTO) toRepoFileDTO;

		return equal(fromNormalFileDTO.getLength(), toNormalFileDTO.getLength())
				&& equal(fromNormalFileDTO.getLastModified(), toNormalFileDTO.getLastModified())
				&& equal(fromNormalFileDTO.getSha1(), toNormalFileDTO.getSha1());
	}

	public void close() {
		localRepoManager.close();
		localRepoTransport.close();
		remoteRepoTransport.close();
	}

	private ThreadPoolExecutor createSyncFileAsynchronouslyExecutor() {
		// TODO make configurable
		ThreadPoolExecutor syncFileAsynchronouslyExecutor = new ThreadPoolExecutor(3, 3,
				60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(2));
		syncFileAsynchronouslyExecutor.setRejectedExecutionHandler(new CallerBlocksPolicy());
		return syncFileAsynchronouslyExecutor;
	}

	private void checkAndEvictDoneSyncFileAsynchronouslyFutures(LinkedList<Future<Void>> syncFileAsynchronouslyFutures) {
		for (Iterator<Future<Void>> it = syncFileAsynchronouslyFutures.iterator(); it.hasNext();) {
			Future<Void> future = it.next();
			if (future.isDone()) {
				it.remove();
				try {
					future.get(); // We definitely don't need a timeout here, because we invoke it only, if it's done already. It should never wait.
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void checkAndEvictAllSyncFileAsynchronouslyFutures(LinkedList<Future<Void>> syncFileAsynchronouslyFutures) {
		for (Iterator<Future<Void>> it = syncFileAsynchronouslyFutures.iterator(); it.hasNext();) {
			Future<Void> future = it.next();
			it.remove();
			try {
				future.get(); // I don't think we need a timeout, because the operations done in the callable have timeouts already.
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
