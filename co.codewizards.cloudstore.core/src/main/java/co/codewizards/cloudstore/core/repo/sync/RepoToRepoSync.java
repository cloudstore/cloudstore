package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.oio.file.FileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.CopyModificationDto;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.DirectoryDto;
import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.dto.SymlinkDto;
import co.codewizards.cloudstore.core.oio.file.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.repo.transport.LocalRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.repo.transport.TransferDoneMarkerType;
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
	private final LocalRepoTransport localRepoTransport;
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
		final File localRootWithoutPathPrefix = LocalRepoHelper.getLocalRootContainingFile(assertNotNull("localRoot", localRoot));
		this.remoteRoot = UrlUtil.canonicalizeURL(assertNotNull("remoteRoot", remoteRoot));
		localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRootWithoutPathPrefix);
		this.localRoot = localRoot = newFile(localRootWithoutPathPrefix, localRepoManager.getLocalPathPrefixOrFail(remoteRoot));

		localRepositoryId = localRepoManager.getRepositoryId();
		if (localRepositoryId == null)
			throw new IllegalStateException("localRepoManager.getRepositoryId() returned null!");

		remoteRepositoryId = localRepoManager.getRemoteRepositoryIdOrFail(remoteRoot);

		remoteRepoTransport = createRepoTransport(remoteRoot, localRepositoryId);
		localRepoTransport = (LocalRepoTransport) createRepoTransport(localRoot, remoteRepositoryId);
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
				syncDown(true, new SubProgressMonitor(monitor, 50));

				if (localSyncExecutor != null)
					throw new IllegalStateException("localSyncExecutor != null");

				if (localSyncFuture != null)
					throw new IllegalStateException("localSyncFuture != null");

				syncUp(new SubProgressMonitor(monitor, 50));
				// Immediately sync back to make sure the changes we caused don't cause problems later
				// (right now there's very likely no collision and this should be very fast).
				syncDown(false, new SubProgressMonitor(monitor, 50));
			}
			else { // THIS IS FOR TESTING ONLY!
				logger.info("sync: locally syncing on *remote* side {} ('{}')", localRepositoryId, localRoot);
				remoteRepoTransport.getChangeSetDto(true); // trigger the local sync on the remote side (we don't need the change set)

				waitForAndCheckLocalSyncFuture();

				syncUp(new SubProgressMonitor(monitor, 50));
				syncDown(false, new SubProgressMonitor(monitor, 50));
				syncUp(new SubProgressMonitor(monitor, 50));
			}
		} finally {
			monitor.done();
		}
	}

	protected void syncUp(final ProgressMonitor monitor) {
		logger.info("sync: up: fromID={} from='{}' toID={} to='{}'",
				localRepositoryId, localRoot, remoteRepositoryId, remoteRoot);
		sync(localRepoTransport, false, remoteRepoTransport, monitor);
	}

	protected void syncDown(final boolean fromRepoLocalSync, final ProgressMonitor monitor) {
		logger.info("sync: down: fromID={} from='{}' toID={} to='{}', fromRepoLocalSync={}",
				remoteRepositoryId, remoteRoot, localRepositoryId, localRoot, fromRepoLocalSync);
		sync(remoteRepoTransport, fromRepoLocalSync, localRepoTransport, monitor);
	}

	private void waitForAndCheckLocalSyncFutureIfExists() {
		if (localSyncFuture != null)
			waitForAndCheckLocalSyncFuture();
	}

	private void waitForAndCheckLocalSyncFuture() {
		try {
			assertNotNull("localSyncFuture", localSyncFuture).get();
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		assertNotNull("localSyncExecutor", localSyncExecutor).shutdown();
		localSyncFuture = null;
		localSyncExecutor = null;
	}

	private void readRemoteRepositoryIdFromRepoTransport() {
		final UUID repositoryId = remoteRepoTransport.getRepositoryId();
		if (repositoryId == null)
			throw new IllegalStateException("remoteRepoTransport.getRepositoryId() returned null!");

		if (!repositoryId.equals(remoteRepositoryId))
			throw new IllegalStateException(
					String.format("remoteRepoTransport.getRepositoryId() does not match repositoryId in local DB! %s != %s", repositoryId, remoteRepositoryId));
	}

	private RepoTransport createRepoTransport(final File rootFile, final UUID clientRepositoryId) {
		URL rootURL;
		try {
			rootURL = rootFile.toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return createRepoTransport(rootURL, clientRepositoryId);
	}

	private RepoTransport createRepoTransport(final URL remoteRoot, final UUID clientRepositoryId) {
		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot);
		return repoTransportFactory.createRepoTransport(remoteRoot, clientRepositoryId);
	}

	private void sync(final RepoTransport fromRepoTransport, final boolean fromRepoLocalSync, final RepoTransport toRepoTransport, final ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			final ChangeSetDto changeSetDto = fromRepoTransport.getChangeSetDto(fromRepoLocalSync);
			monitor.worked(8);

			waitForAndCheckLocalSyncFutureIfExists();

			sync(fromRepoTransport, toRepoTransport, changeSetDto, new SubProgressMonitor(monitor, 90));

			fromRepoTransport.endSyncFromRepository();
			toRepoTransport.endSyncToRepository(changeSetDto.getRepositoryDto().getRevision());
			monitor.worked(2);
		} finally {
			monitor.done();
		}
	}

	private void sync(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport, final ChangeSetDto changeSetDto, final ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", changeSetDto.getModificationDtos().size() + 2 * changeSetDto.getRepoFileDtos().size());
		try {
			final RepoFileDtoTreeNode repoFileDtoTree = RepoFileDtoTreeNode.createTree(changeSetDto.getRepoFileDtos());
			if (repoFileDtoTree != null) {
				sync(fromRepoTransport, toRepoTransport, repoFileDtoTree,
						new Class<?>[] { DirectoryDto.class }, new Class<?>[0],
						new SubProgressMonitor(monitor, repoFileDtoTree.size()));
			}

			syncModifications(fromRepoTransport, toRepoTransport, changeSetDto.getModificationDtos(),
					new SubProgressMonitor(monitor, changeSetDto.getModificationDtos().size()));

			if (repoFileDtoTree != null) {
				sync(fromRepoTransport, toRepoTransport, repoFileDtoTree,
						new Class<?>[] { RepoFileDto.class }, new Class<?>[] { DirectoryDto.class },
						new SubProgressMonitor(monitor, repoFileDtoTree.size()));
			}
		} finally {
			monitor.done();
		}
	}

	private void sync(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDtoTreeNode repoFileDtoTree,
			final Class<?>[] repoFileDtoClassesIncl, final Class<?>[] repoFileDtoClassesExcl,
			final ProgressMonitor monitor) {
		assertNotNull("fromRepoTransport", fromRepoTransport);
		assertNotNull("toRepoTransport", toRepoTransport);
		assertNotNull("repoFileDtoTree", repoFileDtoTree);
		assertNotNull("repoFileDtoClassesIncl", repoFileDtoClassesIncl);
		assertNotNull("repoFileDtoClassesExcl", repoFileDtoClassesExcl);
		assertNotNull("monitor", monitor);

		final Map<Class<?>, Boolean> repoFileDtoClass2Included = new HashMap<Class<?>, Boolean>();
		final Map<Class<?>, Boolean> repoFileDtoClass2Excluded = new HashMap<Class<?>, Boolean>();

		monitor.beginTask("Synchronising...", repoFileDtoTree.size());
		try {
			for (final RepoFileDtoTreeNode repoFileDtoTreeNode : repoFileDtoTree) {
				final RepoFileDto repoFileDto = repoFileDtoTreeNode.getRepoFileDto();
				final Class<? extends RepoFileDto> repoFileDtoClass = repoFileDto.getClass();

				Boolean included = repoFileDtoClass2Included.get(repoFileDtoClass);
				if (included == null) {
					included = false;
					for (final Class<?> clazz : repoFileDtoClassesIncl) {
						if (clazz.isAssignableFrom(repoFileDtoClass)) {
							included = true;
							break;
						}
					}
					repoFileDtoClass2Included.put(repoFileDtoClass, included);
				}

				Boolean excluded = repoFileDtoClass2Excluded.get(repoFileDtoClass);
				if (excluded == null) {
					excluded = false;
					for (final Class<?> clazz : repoFileDtoClassesExcl) {
						if (clazz.isAssignableFrom(repoFileDtoClass)) {
							excluded = true;
							break;
						}
					}
					repoFileDtoClass2Excluded.put(repoFileDtoClass, excluded);
				}

				if (!included || excluded) {
					monitor.worked(1);
					continue;
				}

				if (isDone(fromRepoTransport, toRepoTransport, repoFileDto)) {
					logger.debug("sync: Skipping file already done in an interrupted transfer before: {}", repoFileDtoTreeNode.getPath());
					monitor.worked(1);
					continue;
				}

				if (repoFileDto instanceof DirectoryDto)
					syncDirectory(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, (DirectoryDto) repoFileDto, new SubProgressMonitor(monitor, 1));
				else if (repoFileDto instanceof NormalFileDto) {
					syncFile(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, repoFileDto, monitor);
				}
				else if (repoFileDto instanceof SymlinkDto)
					syncSymlink(fromRepoTransport, toRepoTransport, repoFileDtoTreeNode, (SymlinkDto) repoFileDto, new SubProgressMonitor(monitor, 1));
				else
					throw new IllegalStateException("Unsupported RepoFileDto type: " + repoFileDto);

				markDone(fromRepoTransport, toRepoTransport, repoFileDto);
			}
		} finally {
			monitor.done();
		}
	}

	private boolean isDone(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport, final RepoFileDto repoFileDto) {
		return localRepoTransport.isTransferDone(
				fromRepoTransport.getRepositoryId(), toRepoTransport.getRepositoryId(),
				TransferDoneMarkerType.REPO_FILE, repoFileDto.getId(), repoFileDto.getLocalRevision());
	}

	private void markDone(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport, final RepoFileDto repoFileDto) {
		localRepoTransport.markTransferDone(
				fromRepoTransport.getRepositoryId(), toRepoTransport.getRepositoryId(),
				TransferDoneMarkerType.REPO_FILE, repoFileDto.getId(), repoFileDto.getLocalRevision());
	}

	private boolean isDone(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport, final ModificationDto modificationDto) {
		return localRepoTransport.isTransferDone(
				fromRepoTransport.getRepositoryId(), toRepoTransport.getRepositoryId(),
				TransferDoneMarkerType.MODIFICATION, modificationDto.getId(), modificationDto.getLocalRevision());
	}

	private void markDone(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport, final ModificationDto modificationDto) {
		localRepoTransport.markTransferDone(
				fromRepoTransport.getRepositoryId(), toRepoTransport.getRepositoryId(),
				TransferDoneMarkerType.MODIFICATION, modificationDto.getId(), modificationDto.getLocalRevision());
	}

	private SortedMap<Long, Collection<ModificationDto>> getLocalRevision2ModificationDtos(final Collection<ModificationDto> modificationDtos) {
		final SortedMap<Long, Collection<ModificationDto>> map = new TreeMap<Long, Collection<ModificationDto>>();
		for (final ModificationDto modificationDto : modificationDtos) {
			final long localRevision = modificationDto.getLocalRevision();
			Collection<ModificationDto> collection = map.get(localRevision);
			if (collection == null) {
				collection = new ArrayList<ModificationDto>();
				map.put(localRevision, collection);
			}
			collection.add(modificationDto);
		}
		return map;
	}

	private void syncModifications(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport, final Collection<ModificationDto> modificationDtos, final ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", modificationDtos.size());
		try {
			final SortedMap<Long,Collection<ModificationDto>> localRevision2ModificationDtos = getLocalRevision2ModificationDtos(modificationDtos);
			for (final Map.Entry<Long,Collection<ModificationDto>> me : localRevision2ModificationDtos.entrySet()) {
				final ModificationDtoSet modificationDtoSet = new ModificationDtoSet(me.getValue());

				for (final List<CopyModificationDto> copyModificationDtos : modificationDtoSet.getFromPath2CopyModificationDtos().values()) {
					for (final Iterator<CopyModificationDto> itCopyMod = copyModificationDtos.iterator(); itCopyMod.hasNext(); ) {
						final CopyModificationDto copyModificationDto = itCopyMod.next();

						if (isDone(fromRepoTransport, toRepoTransport, copyModificationDto)) {
							logger.debug("sync: Skipping CopyModificaton already done in an interrupted transfer before: {} => {}", copyModificationDto.getFromPath(), copyModificationDto.getToPath());
							monitor.worked(1);
							continue;
						}

						final List<DeleteModificationDto> deleteModificationDtos = modificationDtoSet.getPath2DeleteModificationDtos().get(copyModificationDto.getFromPath());
						boolean moveInstead = false;
						if (!itCopyMod.hasNext() && deleteModificationDtos != null && !deleteModificationDtos.isEmpty())
							moveInstead = true;

						if (moveInstead) {
							logger.info("syncModifications: Moving from '{}' to '{}'", copyModificationDto.getFromPath(), copyModificationDto.getToPath());
							toRepoTransport.move(copyModificationDto.getFromPath(), copyModificationDto.getToPath());
						}
						else {
							logger.info("syncModifications: Copying from '{}' to '{}'", copyModificationDto.getFromPath(), copyModificationDto.getToPath());
							toRepoTransport.copy(copyModificationDto.getFromPath(), copyModificationDto.getToPath());
						}

						if (!moveInstead && deleteModificationDtos != null) {
							for (final DeleteModificationDto deleteModificationDto : deleteModificationDtos) {
								logger.info("syncModifications: Deleting '{}'", deleteModificationDto.getPath());
								toRepoTransport.delete(deleteModificationDto.getPath());
							}
						}

						markDone(fromRepoTransport, toRepoTransport, copyModificationDto);
					}
				}

				for (final List<DeleteModificationDto> deleteModificationDtos : modificationDtoSet.getPath2DeleteModificationDtos().values()) {
					for (final DeleteModificationDto deleteModificationDto : deleteModificationDtos) {
						if (isDone(fromRepoTransport, toRepoTransport, deleteModificationDto)) {
							logger.debug("sync: Skipping DeleteModificaton already done in an interrupted transfer before: {}", deleteModificationDto.getPath());
							monitor.worked(1);
							continue;
						}

						logger.info("syncModifications: Deleting '{}'", deleteModificationDto.getPath());
						toRepoTransport.delete(deleteModificationDto.getPath());

						markDone(fromRepoTransport, toRepoTransport, deleteModificationDto);
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	private void syncDirectory(
			final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport,
			final RepoFileDtoTreeNode repoFileDtoTreeNode, final DirectoryDto directoryDto, final ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			final String path = repoFileDtoTreeNode.getPath();
			logger.info("syncDirectory: path='{}'", path);
			try {
				toRepoTransport.makeDirectory(path, directoryDto.getLastModified());
			} catch (final DeleteModificationCollisionException x) {
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
			final RepoFileDtoTreeNode repoFileDtoTreeNode, final SymlinkDto symlinkDto, final SubProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			final String path = repoFileDtoTreeNode.getPath();
			try {
				toRepoTransport.makeSymlink(path, symlinkDto.getTarget(), symlinkDto.getLastModified());
			} catch (final DeleteModificationCollisionException x) {
				logger.info("DeleteModificationCollisionException during makeSymlink: {}", path);
				if (logger.isDebugEnabled())
					logger.debug(x.toString(), x);

				return;
			}
		} finally {
			monitor.done();
		}
	}

	private void syncFile(final RepoTransport fromRepoTransport, final RepoTransport toRepoTransport, final RepoFileDtoTreeNode repoFileDtoTreeNode, final RepoFileDto normalFileDto, final ProgressMonitor monitor) {
		monitor.beginTask("Synchronising...", 100);
		try {
			final String path = repoFileDtoTreeNode.getPath();
			logger.info("syncFile: path='{}'", path);

			final RepoFileDto fromRepoFileDto = fromRepoTransport.getRepoFileDto(path);
			if (fromRepoFileDto == null) {
				logger.warn("File was deleted during sync on source side: {}", path);
				return;
			}
			if (!(fromRepoFileDto instanceof NormalFileDto)) {
				logger.warn("Normal file was replaced by a directory (or another type) during sync on source side: {}", path);
				return;
			}
			monitor.worked(10);

			final NormalFileDto fromNormalFileDto = (NormalFileDto) fromRepoFileDto;

			final RepoFileDto toRepoFileDto = toRepoTransport.getRepoFileDto(path);
			if (areFilesExistingAndEqual(fromRepoFileDto, toRepoFileDto)) {
				logger.info("File is already equal on destination side (sha1='{}'): {}", fromNormalFileDto.getSha1(), path);
				return;
			}
			monitor.worked(10);

			logger.info("Beginning to copy file (from.sha1='{}' to.sha1='{}'): {}", fromNormalFileDto.getSha1(),
					toRepoFileDto instanceof NormalFileDto ? ((NormalFileDto)toRepoFileDto).getSha1() : "<NoInstanceOf_NormalFileDto>",
							path);

			final NormalFileDto toNormalFileDto;
			if (toRepoFileDto instanceof NormalFileDto)
				toNormalFileDto = (NormalFileDto) toRepoFileDto;
			else
				toNormalFileDto = new NormalFileDto(); // dummy (null-object pattern)

			try {
				toRepoTransport.beginPutFile(path);
			} catch (final DeleteModificationCollisionException x) {
				logger.info("DeleteModificationCollisionException during beginPutFile: {}", path);
				if (logger.isDebugEnabled())
					logger.debug(x.toString(), x);

				return;
			}
			monitor.worked(1);

			final Map<Long, FileChunkDto> offset2ToTempFileChunkDto = new HashMap<>(toNormalFileDto.getTempFileChunkDtos().size());
			for (final FileChunkDto toTempFileChunkDto : toNormalFileDto.getTempFileChunkDtos())
				offset2ToTempFileChunkDto.put(toTempFileChunkDto.getOffset(), toTempFileChunkDto);

			logger.debug("Comparing {} FileChunkDtos. path='{}'", fromNormalFileDto.getFileChunkDtos().size(), path);
			final List<FileChunkDto> fromFileChunkDtosDirty = new ArrayList<FileChunkDto>();
			final Iterator<FileChunkDto> toFileChunkDtoIterator = toNormalFileDto.getFileChunkDtos().iterator();
			int fileChunkIndex = -1;
			for (final FileChunkDto fromFileChunkDto : fromNormalFileDto.getFileChunkDtos()) {
				final FileChunkDto toFileChunkDto = toFileChunkDtoIterator.hasNext() ? toFileChunkDtoIterator.next() : null;
				++fileChunkIndex;
				final FileChunkDto toTempFileChunkDto = offset2ToTempFileChunkDto.get(fromFileChunkDto.getOffset());
				if (toTempFileChunkDto == null) {
					if (toFileChunkDto != null
							&& equal(fromFileChunkDto.getOffset(), toFileChunkDto.getOffset())
							&& equal(fromFileChunkDto.getLength(), toFileChunkDto.getLength())
							&& equal(fromFileChunkDto.getSha1(), toFileChunkDto.getSha1())) {
						if (logger.isTraceEnabled()) {
							logger.trace("Skipping clean FileChunkDto. index={} offset={} sha1='{}'",
									fileChunkIndex, fromFileChunkDto.getOffset(), fromFileChunkDto.getSha1());
						}
						continue;
					}
				}
				else {
					if (equal(fromFileChunkDto.getOffset(), toTempFileChunkDto.getOffset())
							&& equal(fromFileChunkDto.getLength(), toTempFileChunkDto.getLength())
							&& equal(fromFileChunkDto.getSha1(), toTempFileChunkDto.getSha1())) {
						if (logger.isTraceEnabled()) {
							logger.trace("Skipping clean temporary FileChunkDto. index={} offset={} sha1='{}'",
									fileChunkIndex, fromFileChunkDto.getOffset(), fromFileChunkDto.getSha1());
						}
						continue;
					}
				}

				if (logger.isTraceEnabled()) {
					logger.trace("Enlisting dirty FileChunkDto. index={} fromOffset={} toOffset={} fromSha1='{}' toSha1='{}'",
							fileChunkIndex, fromFileChunkDto.getOffset(),
							(toFileChunkDto == null ? "null" : toFileChunkDto.getOffset()),
							fromFileChunkDto.getSha1(),
							(toFileChunkDto == null ? "null" : toFileChunkDto.getSha1()));
				}
				fromFileChunkDtosDirty.add(fromFileChunkDto);
			}

			logger.info("Need to copy {} dirty file-chunks (of {} total). path='{}'",
					fromFileChunkDtosDirty.size(), fromNormalFileDto.getFileChunkDtos().size(), path);

			final ProgressMonitor subMonitor = new SubProgressMonitor(monitor, 73);
			subMonitor.beginTask("Synchronising...", fromFileChunkDtosDirty.size());
			fileChunkIndex = -1;
			long bytesCopied = 0;
			final long copyChunksBeginTimestamp = System.currentTimeMillis();
			for (final FileChunkDto fileChunkDto : fromFileChunkDtosDirty) {
				++fileChunkIndex;
				if (logger.isTraceEnabled()) {
					logger.trace("Reading data for dirty FileChunkDto (index {} of {}). path='{}' offset={}",
							fileChunkIndex, fromFileChunkDtosDirty.size(), path, fileChunkDto.getOffset());
				}
				final byte[] fileData = fromRepoTransport.getFileData(path, fileChunkDto.getOffset(), fileChunkDto.getLength());

				if (fileData == null || fileData.length != fileChunkDto.getLength() || !sha1(fileData).equals(fileChunkDto.getSha1())) {
					logger.warn("Source file was modified or deleted during sync: {}", path);
					// The file is left in state 'inProgress'. Thus it should definitely not be synced back in the opposite
					// direction. The file should be synced again in the correct direction in the next run (after the source
					// repo did a local sync, too).
					return;
				}

				if (logger.isTraceEnabled()) {
					logger.trace("Writing data for dirty FileChunkDto ({} of {}). path='{}' offset={}",
							fileChunkIndex + 1, fromFileChunkDtosDirty.size(), path, fileChunkDto.getOffset());
				}
				toRepoTransport.putFileData(path, fileChunkDto.getOffset(), fileData);
				bytesCopied += fileData.length;
				subMonitor.worked(1);
			}
			subMonitor.done();

			logger.info("Copied {} dirty file-chunks with together {} bytes in {} ms. path='{}'",
					fromFileChunkDtosDirty.size(), bytesCopied, System.currentTimeMillis() - copyChunksBeginTimestamp, path);

			toRepoTransport.endPutFile(
					path, fromNormalFileDto.getLastModified(),
					fromNormalFileDto.getLength(), fromNormalFileDto.getSha1());

			monitor.worked(6);
		} finally {
			monitor.done();
		}
	}

	private String sha1(final byte[] data) {
		assertNotNull("data", data);
		try {
			final byte[] hash = HashUtil.hash(HashUtil.HASH_ALGORITHM_SHA, new ByteArrayInputStream(data));
			return HashUtil.encodeHexStr(hash);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean areFilesExistingAndEqual(final RepoFileDto fromRepoFileDto, final RepoFileDto toRepoFileDto) {
		if (!(fromRepoFileDto instanceof NormalFileDto))
			return false;

		if (!(toRepoFileDto instanceof NormalFileDto))
			return false;

		final NormalFileDto fromNormalFileDto = (NormalFileDto) fromRepoFileDto;
		final NormalFileDto toNormalFileDto = (NormalFileDto) toRepoFileDto;

		return equal(fromNormalFileDto.getLength(), toNormalFileDto.getLength())
				&& equal(fromNormalFileDto.getLastModified(), toNormalFileDto.getLastModified())
				&& equal(fromNormalFileDto.getSha1(), toNormalFileDto.getSha1());
	}

	public void close() {
		localRepoManager.close();
		localRepoTransport.close();
		remoteRepoTransport.close();
	}
}
