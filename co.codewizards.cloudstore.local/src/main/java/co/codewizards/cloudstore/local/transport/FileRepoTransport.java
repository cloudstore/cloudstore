package co.codewizards.cloudstore.local.transport;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.CopyModificationDTO;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.FileChunkDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.dto.SymlinkDTO;
import co.codewizards.cloudstore.core.dto.TempChunkFileDTO;
import co.codewizards.cloudstore.core.dto.jaxb.TempChunkFileDTOIO;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.repo.transport.FileWriteStrategy;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.FilenameFilterSkipMetaDir;
import co.codewizards.cloudstore.local.LocalRepoSync;
import co.codewizards.cloudstore.local.LocalRepoTransactionImpl;
import co.codewizards.cloudstore.local.persistence.CopyModification;
import co.codewizards.cloudstore.local.persistence.DeleteModification;
import co.codewizards.cloudstore.local.persistence.DeleteModificationDAO;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepoDAO;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.local.persistence.Modification;
import co.codewizards.cloudstore.local.persistence.ModificationDAO;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequestDAO;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDAO;
import co.codewizards.cloudstore.local.persistence.Symlink;

public class FileRepoTransport extends AbstractRepoTransport {
	private static final Logger logger = LoggerFactory.getLogger(FileRepoTransport.class);

	private static final long MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY = 100; // TODO make configurable!

	private static final String TEMP_CHUNK_FILE_PREFIX = "chunk_";
	private static final String TEMP_CHUNK_FILE_DTO_FILE_SUFFIX = ".xml";

	private LocalRepoManager localRepoManager;

	@Override
	public void close() {
		if (localRepoManager != null) {
			logger.debug("close: Closing localRepoManager.");
			localRepoManager.close();
		} else
			logger.debug("close: There is no localRepoManager.");

		super.close();
	}

	@Override
	public UUID getRepositoryId() {
		return getLocalRepoManager().getRepositoryId();
	}

	@Override
	public byte[] getPublicKey() {
		return getLocalRepoManager().getPublicKey();
	}

	@Override
	public void requestRepoConnection(byte[] publicKey) {
		assertNotNull("publicKey", publicKey);
		UUID clientRepositoryId = getClientRepositoryIdOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			RemoteRepository remoteRepository = remoteRepositoryDAO.getRemoteRepository(clientRepositoryId);
			if (remoteRepository != null)
				throw new IllegalArgumentException("RemoteRepository already connected! repositoryId=" + clientRepositoryId);

			String localPathPrefix = getPathPrefix();
			RemoteRepositoryRequestDAO remoteRepositoryRequestDAO = transaction.getDAO(RemoteRepositoryRequestDAO.class);
			RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDAO.getRemoteRepositoryRequest(clientRepositoryId);
			if (remoteRepositoryRequest != null) {
				logger.info("RemoteRepository already requested to be connected. repositoryId={}", clientRepositoryId);

				// For security reasons, we do not allow to modify the public key! If we did,
				// an attacker might replace the public key while the user is verifying the public key's
				// fingerprint. The user would see & confirm the old public key, but the new public key
				// would be written to the RemoteRepository. This requires really lucky timing, but
				// if the attacker surveils the user, this might be feasable.
				if (!Arrays.equals(remoteRepositoryRequest.getPublicKey(), publicKey))
					throw new IllegalStateException("Cannot modify the public key! Use 'dropRepoConnection' to drop the old request or wait until it expired.");

				// For the same reasons stated above, we do not allow changing the local path-prefix, too.
				if (!remoteRepositoryRequest.getLocalPathPrefix().equals(localPathPrefix))
					throw new IllegalStateException("Cannot modify the local path-prefix! Use 'dropRepoConnection' to drop the old request or wait until it expired.");

				remoteRepositoryRequest.setChanged(new Date()); // make sure it is not deleted soon (the request expires after a while)
			}
			else {
				long remoteRepositoryRequestsCount = remoteRepositoryRequestDAO.getObjectsCount();
				if (remoteRepositoryRequestsCount >= MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY)
					throw new IllegalStateException(String.format(
							"The maximum number of connection requests (%s) is reached or exceeded! Please retry later, when old requests were accepted or expired.", MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY));

				remoteRepositoryRequest = new RemoteRepositoryRequest();
				remoteRepositoryRequest.setRepositoryId(clientRepositoryId);
				remoteRepositoryRequest.setPublicKey(publicKey);
				remoteRepositoryRequest.setLocalPathPrefix(localPathPrefix);
				remoteRepositoryRequestDAO.makePersistent(remoteRepositoryRequest);
			}

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public RepositoryDTO getRepositoryDTO() {
		LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction();
		try {
			LocalRepositoryDAO localRepositoryDAO = transaction.getDAO(LocalRepositoryDAO.class);
			LocalRepository localRepository = localRepositoryDAO.getLocalRepositoryOrFail();
			RepositoryDTO repositoryDTO = toRepositoryDTO(localRepository);
			transaction.commit();
			return repositoryDTO;
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public ChangeSetDTO getChangeSetDTO(boolean localSync) {
		if (localSync)
			getLocalRepoManager().localSync(new LoggerProgressMonitor(logger));

		UUID clientRepositoryId = getClientRepositoryIdOrFail();
		ChangeSetDTO changeSetDTO = new ChangeSetDTO();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); // It writes the LastSyncToRemoteRepo!
		try {
			LocalRepositoryDAO localRepositoryDAO = transaction.getDAO(LocalRepositoryDAO.class);
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);
			ModificationDAO modificationDAO = transaction.getDAO(ModificationDAO.class);
			RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);

			// We must *first* read the LocalRepository and afterwards all changes, because this way, we don't need to lock it in the DB.
			// If we *then* read RepoFiles with a newer localRevision, it doesn't do any harm - we'll simply read them again, in the
			// next run.
			LocalRepository localRepository = localRepositoryDAO.getLocalRepositoryOrFail();
			changeSetDTO.setRepositoryDTO(toRepositoryDTO(localRepository));

			RemoteRepository toRemoteRepository = remoteRepositoryDAO.getRemoteRepositoryOrFail(clientRepositoryId);

			LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepo(toRemoteRepository);
			if (lastSyncToRemoteRepo == null) {
				lastSyncToRemoteRepo = new LastSyncToRemoteRepo();
				lastSyncToRemoteRepo.setRemoteRepository(toRemoteRepository);
				lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(-1);
			}
			lastSyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());
			lastSyncToRemoteRepoDAO.makePersistent(lastSyncToRemoteRepo);

			((LocalRepoTransactionImpl)transaction).getPersistenceManager().getFetchPlan().setGroup(FetchPlan.ALL);
			Collection<Modification> modifications = modificationDAO.getModificationsAfter(toRemoteRepository, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
			changeSetDTO.setModificationDTOs(toModificationDTOs(modifications));

			if (!getPathPrefix().isEmpty()) {
				Collection<DeleteModification> deleteModifications = transaction.getDAO(DeleteModificationDAO.class).getDeleteModificationsForPathOrParentOfPathAfter(
						getPathPrefix(), lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced(), toRemoteRepository);
				if (!deleteModifications.isEmpty()) { // our virtual root was deleted => create synthetic DeleteModificationDTO for virtual root
					DeleteModificationDTO deleteModificationDTO = new DeleteModificationDTO();
					deleteModificationDTO.setId(0);
					deleteModificationDTO.setLocalRevision(localRepository.getRevision());
					deleteModificationDTO.setPath("");
					changeSetDTO.getModificationDTOs().add(deleteModificationDTO);
				}
			}

			Collection<RepoFile> repoFiles = repoFileDAO.getRepoFilesChangedAfter(lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
			RepoFile pathPrefixRepoFile = null; // the virtual root for the client
			if (!getPathPrefix().isEmpty()) {
				pathPrefixRepoFile = repoFileDAO.getRepoFile(getLocalRepoManager().getLocalRoot(), getPathPrefixFile());
			}
			Map<Long, RepoFileDTO> id2RepoFileDTO = getId2RepoFileDTOWithParents(pathPrefixRepoFile, repoFiles, repoFileDAO);
			changeSetDTO.setRepoFileDTOs(new ArrayList<RepoFileDTO>(id2RepoFileDTO.values()));

			transaction.commit();
			return changeSetDTO;
		} finally {
			transaction.rollbackIfActive();
		}
	}

	protected File getPathPrefixFile() {
		String pathPrefix = getPathPrefix();
		if (pathPrefix.isEmpty())
			return getLocalRepoManager().getLocalRoot();
		else
			return new File(getLocalRepoManager().getLocalRoot(), pathPrefix);
	}

	@Override
	public void makeDirectory(String path, Date lastModified) {
		path = prefixPath(path);
		File file = getFile(path);
		UUID clientRepositoryId = getClientRepositoryIdOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			assertNoDeleteModificationCollision(transaction, clientRepositoryId, path);
			mkDir(transaction, clientRepositoryId, file, lastModified);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void makeSymlink(String path, String target, Date lastModified) {
		path = prefixPath(path);
		assertNotNull("target", target);
		final File file = getFile(path);
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			final RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);

			final File parentFile = file.getParentFile();
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				assertNoDeleteModificationCollision(transaction, clientRepositoryId, path);

				if (file.exists() && !isSymlink(file))
					file.renameTo(IOUtil.createCollisionFile(file));

				if (file.exists() && !isSymlink(file))
					throw new IllegalStateException("Could not rename file! It is still in the way: " + file);

				final File localRoot = getLocalRepoManager().getLocalRoot();

				try {
					final boolean currentTargetEqualsNewTarget;
					final Path symlinkPath = file.toPath();
					if (Files.isSymbolicLink(file.toPath()) || file.exists()) {
						final Path currentTargetPath = Files.readSymbolicLink(symlinkPath);
						final String currentTarget = IOUtil.toPathString(currentTargetPath);
						currentTargetEqualsNewTarget = currentTarget.equals(target);
						if (!currentTargetEqualsNewTarget) {
							final RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);
							if (repoFile == null) {
								File collisionFile = IOUtil.createCollisionFile(file);
								file.renameTo(collisionFile);
								if (file.exists())
									throw new IllegalStateException("Could not rename file to resolve collision: " + file);
							}
							else
								detectAndHandleFileCollision(transaction, clientRepositoryId, parentFile, repoFile);

							file.delete();
						}
					}
					else
						currentTargetEqualsNewTarget = false;

					if (!currentTargetEqualsNewTarget)
						Files.createSymbolicLink(symlinkPath, Paths.get(target));

					if (lastModified != null)
						IOUtil.setLastModifiedNoFollow(file, lastModified.getTime());

				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				new LocalRepoSync(transaction).sync(file, new NullProgressMonitor());

				Collection<TempChunkFileWithDTOFile> tempChunkFileWithDTOFiles = getOffset2TempChunkFileWithDTOFile(file).values();
				for (TempChunkFileWithDTOFile tempChunkFileWithDTOFile : tempChunkFileWithDTOFiles) {
					if (tempChunkFileWithDTOFile.getTempChunkFileDTOFile() != null)
						deleteOrFail(tempChunkFileWithDTOFile.getTempChunkFileDTOFile());

					if (tempChunkFileWithDTOFile.getTempChunkFile() != null)
						deleteOrFail(tempChunkFileWithDTOFile.getTempChunkFile());
				}

				final RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);
				if (repoFile == null)
					throw new IllegalStateException("LocalRepoSync.sync(...) did not create the RepoFile for file: " + file);

				if (!(repoFile instanceof Symlink))
					throw new IllegalStateException("LocalRepoSync.sync(...) created an instance of " + repoFile.getClass().getName() + " instead  of a Symlink for file: " + file);

				repoFile.setLastSyncFromRepositoryId(clientRepositoryId);

			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void assertNoDeleteModificationCollision(LocalRepoTransaction transaction, UUID fromRepositoryId, String path) throws DeleteModificationCollisionException {
		RemoteRepository fromRemoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getRemoteRepositoryOrFail(fromRepositoryId);
		long lastSyncFromRemoteRepositoryLocalRevision = fromRemoteRepository.getLocalRevision();

		if (!path.startsWith("/"))
			path = '/' + path;

		DeleteModificationDAO deleteModificationDAO = transaction.getDAO(DeleteModificationDAO.class);
		Collection<DeleteModification> deleteModifications = deleteModificationDAO.getDeleteModificationsForPathOrParentOfPathAfter(
				path, lastSyncFromRemoteRepositoryLocalRevision, fromRemoteRepository);

		if (!deleteModifications.isEmpty())
			throw new DeleteModificationCollisionException(
					String.format("There is at least one DeleteModification for repositoryId=%s path='%s'", fromRepositoryId, path));
	}

	@Override
	public void copy(String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);

		final File fromFile = getFile(fromPath);
		final File toFile = getFile(toPath);

		if (!fromFile.exists()) // TODO throw an exception and catch in RepoToRepoSync!
			return;

		if (toFile.exists()) // TODO either simply throw an exception or implement proper collision check.
			return;

		final File toParentFile = toFile.getParentFile();
		final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(toParentFile);
			try {
				try {
					if (!toParentFile.isDirectory())
						toParentFile.mkdirs();

					Files.copy(fromFile.toPath(), toFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				final LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
				final RepoFile toRepoFile = localRepoSync.sync(toFile, new NullProgressMonitor());
				assertNotNull("toRepoFile", toRepoFile);
				toRepoFile.setLastSyncFromRepositoryId(getClientRepositoryIdOrFail());
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(toParentFile);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void move(String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);

		final File fromFile = getFile(fromPath);
		final File toFile = getFile(toPath);

		if (!fromFile.exists()) // TODO throw an exception and catch in RepoToRepoSync!
			return;

		if (toFile.exists()) // TODO either simply throw an exception or implement proper collision check.
			return;

		final File fromParentFile = fromFile.getParentFile();
		final File toParentFile = toFile.getParentFile();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(fromParentFile);
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(toParentFile);
			try {
				try {
					if (!toParentFile.isDirectory())
						toParentFile.mkdirs();

					Files.move(fromFile.toPath(), toFile.toPath());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				final LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
				final RepoFile toRepoFile = localRepoSync.sync(toFile, new NullProgressMonitor());
				final RepoFile fromRepoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), fromFile);
				if (fromRepoFile != null)
					localRepoSync.deleteRepoFile(fromRepoFile);

				assertNotNull("toRepoFile", toRepoFile);
				toRepoFile.setLastSyncFromRepositoryId(getClientRepositoryIdOrFail());
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(fromParentFile);
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(toParentFile);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void delete(String path) {
		path = prefixPath(path);
		File file = getFile(path);
		UUID clientRepositoryId = getClientRepositoryIdOrFail();
		boolean fileIsLocalRoot = localRepoManager.getLocalRoot().equals(file);
		File parentFile = file.getParentFile();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
				localRepoSync.sync(file, new NullProgressMonitor());

				if (fileIsLocalRoot) {
					// Cannot delete the repository's root! Deleting all its contents instead.
					long fileLastModified = file.lastModified();
					try {
						File[] children = file.listFiles(new FilenameFilterSkipMetaDir());
						if (children == null)
							throw new IllegalStateException("File-listing localRoot returned null: " + file);

						for (File child : children)
							delete(transaction, localRepoSync, clientRepositoryId, child);
					} finally {
						file.setLastModified(fileLastModified);
					}
				}
				else
					delete(transaction, localRepoSync, clientRepositoryId, file);

			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void delete(LocalRepoTransaction transaction, LocalRepoSync localRepoSync, UUID fromRepositoryId, File file) {
		if (detectFileCollisionRecursively(transaction, fromRepositoryId, file)) {
			file.renameTo(IOUtil.createCollisionFile(file));

			if (file.exists())
				throw new IllegalStateException("Renaming file failed: " + file);
		}

		if (!IOUtil.deleteDirectoryRecursively(file)) {
			throw new IllegalStateException("Deleting file or directory failed: " + file);
		}

		RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
		if (repoFile != null)
			localRepoSync.deleteRepoFile(repoFile);
	}

	@Override
	public RepoFileDTO getRepoFileDTO(String path) {
		RepoFileDTO repoFileDTO = null;
		path = prefixPath(path);
		File file = getFile(path);
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction(); // it performs a local sync!
		try {
			LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
			localRepoSync.sync(file, new NullProgressMonitor());

			RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
			RepoFile repoFile = repoFileDAO.getRepoFile(getLocalRepoManager().getLocalRoot(), file);
			if (repoFile != null)
				repoFileDTO = toRepoFileDTO(repoFile, repoFileDAO, Integer.MAX_VALUE); // TODO pass depth as argument - or maybe leave it this way?

			transaction.commit();
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		} finally {
			transaction.rollbackIfActive();
		}
		return repoFileDTO;
	}

	private FileChunkDTO toFileChunkDTO(FileChunk fileChunk) {
		final FileChunkDTO fileChunkDTO = new FileChunkDTO();
		fileChunkDTO.setOffset(fileChunk.getOffset());
		fileChunkDTO.setLength(fileChunk.getLength());
		fileChunkDTO.setSha1(fileChunk.getSha1());
		return fileChunkDTO;
	}

	/**
	 * @param offset the offset in the (real) destination file (<i>not</i> in {@code tempChunkFile}! there the offset is always 0).
	 * @param tempChunkFile the tempChunkFile containing the chunk's data. Must not be <code>null</code>.
	 * @param sha1 the sha1 of the single chunk (in {@code tempChunkFile}). Must not be <code>null</code>.
	 * @return the DTO. Never <code>null</code>.
	 */
	private TempChunkFileDTO createTempChunkFileDTO(final long offset, final File tempChunkFile, final String sha1) {
		assertNotNull("tempChunkFile", tempChunkFile);
		assertNotNull("sha1", sha1);

		if (!tempChunkFile.exists())
			throw new IllegalArgumentException("The tempChunkFile does not exist: " + tempChunkFile.getAbsolutePath());

		final FileChunkDTO fileChunkDTO = new FileChunkDTO();
		fileChunkDTO.setOffset(offset);

		final long tempChunkFileLength = tempChunkFile.length();
		if (tempChunkFileLength > Integer.MAX_VALUE)
			throw new IllegalStateException("tempChunkFile.length > Integer.MAX_VALUE");

		fileChunkDTO.setLength((int) tempChunkFileLength);
		fileChunkDTO.setSha1(sha1);

		final TempChunkFileDTO tempChunkFileDTO = new TempChunkFileDTO();
		tempChunkFileDTO.setFileChunkDTO(fileChunkDTO);
		return tempChunkFileDTO;
	}

	protected LocalRepoManager getLocalRepoManager() {
		if (localRepoManager == null) {
			logger.debug("getLocalRepoManager: Creating a new LocalRepoManager.");
			File remoteRootFile;
			try {
				remoteRootFile = new File(getRemoteRootWithoutPathPrefix().toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(remoteRootFile);
		}
		return localRepoManager;
	}

	@Override
	protected URL determineRemoteRootWithoutPathPrefix() {
		File remoteRootFile;
		try {
			remoteRootFile = new File(getRemoteRoot().toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		File localRootFile = LocalRepoHelper.getLocalRootContainingFile(remoteRootFile);
		if (localRootFile == null)
			throw new IllegalStateException(String.format(
					"remoteRoot='%s' does not point to a file or directory within an existing repository (nor its root directory)!",
					getRemoteRoot()));

		try {
			return localRootFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private List<ModificationDTO> toModificationDTOs(Collection<Modification> modifications) {
		long startTimestamp = System.currentTimeMillis();
		List<ModificationDTO> result = new ArrayList<ModificationDTO>(assertNotNull("modifications", modifications).size());
		for (Modification modification : modifications) {
			ModificationDTO modificationDTO = toModificationDTO(modification);
			if (modificationDTO != null)
				result.add(modificationDTO);
		}
		logger.debug("toModificationDTOs: Creating {} ModificationDTOs took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);
		return result;
	}

	private ModificationDTO toModificationDTO(Modification modification) {
		ModificationDTO modificationDTO;
		if (modification instanceof CopyModification) {
			CopyModification copyModification = (CopyModification) modification;

			String fromPath = copyModification.getFromPath();
			String toPath = copyModification.getToPath();
			if (!isPathUnderPathPrefix(fromPath) || !isPathUnderPathPrefix(toPath))
				return null;

			fromPath = unprefixPath(fromPath);
			toPath = unprefixPath(toPath);

			CopyModificationDTO copyModificationDTO = new CopyModificationDTO();
			modificationDTO = copyModificationDTO;
			copyModificationDTO.setFromPath(fromPath);
			copyModificationDTO.setToPath(toPath);
		}
		else if (modification instanceof DeleteModification) {
			DeleteModification deleteModification = (DeleteModification) modification;

			String path = deleteModification.getPath();
			if (!isPathUnderPathPrefix(path))
				return null;

			path = unprefixPath(path);

			DeleteModificationDTO deleteModificationDTO;
			modificationDTO = deleteModificationDTO = new DeleteModificationDTO();
			deleteModificationDTO.setPath(path);
		}
		else
			throw new IllegalArgumentException("Unknown modification type: " + modification);

		modificationDTO.setId(modification.getId());
		modificationDTO.setLocalRevision(modification.getLocalRevision());

		return modificationDTO;
	}

	private RepositoryDTO toRepositoryDTO(LocalRepository localRepository) {
		RepositoryDTO repositoryDTO = new RepositoryDTO();
		repositoryDTO.setRepositoryId(localRepository.getRepositoryId());
		repositoryDTO.setRevision(localRepository.getRevision());
		repositoryDTO.setPublicKey(localRepository.getPublicKey());
		return repositoryDTO;
	}

	private Map<Long, RepoFileDTO> getId2RepoFileDTOWithParents(RepoFile pathPrefixRepoFile, Collection<RepoFile> repoFiles, RepoFileDAO repoFileDAO) {
		assertNotNull("repoFileDAO", repoFileDAO);
		assertNotNull("repoFiles", repoFiles);
		Map<Long, RepoFileDTO> entityID2RepoFileDTO = new HashMap<Long, RepoFileDTO>();
		for (RepoFile repoFile : repoFiles) {
			RepoFile rf = repoFile;
			if (rf instanceof NormalFile) {
				NormalFile nf = (NormalFile) rf;
				if (nf.isInProgress()) {
					continue;
				}
			}

			if (pathPrefixRepoFile != null && !isDirectOrIndirectParent(pathPrefixRepoFile, rf))
				continue;

			while (rf != null) {
				if (!entityID2RepoFileDTO.containsKey(rf.getId())) {
					RepoFileDTO repoFileDTO = toRepoFileDTO(rf, repoFileDAO, 0);
					if (pathPrefixRepoFile != null && pathPrefixRepoFile.equals(rf)) {
						repoFileDTO.setParentId(null); // virtual root has no parent!
						repoFileDTO.setName(""); // virtual root has no name!
					}

					entityID2RepoFileDTO.put(rf.getId(), repoFileDTO);
				}

				if (pathPrefixRepoFile != null && pathPrefixRepoFile.equals(rf))
					break;

				rf = rf.getParent();
			}
		}
		return entityID2RepoFileDTO;
	}

	private boolean isDirectOrIndirectParent(RepoFile parentRepoFile, RepoFile repoFile) {
		assertNotNull("parentRepoFile", parentRepoFile);
		assertNotNull("repoFile", repoFile);
		RepoFile rf = repoFile;
		while (rf != null) {
			if (parentRepoFile.equals(rf))
				return true;

			rf = rf.getParent();
		}
		return false;
	}

	private RepoFileDTO toRepoFileDTO(RepoFile repoFile, RepoFileDAO repoFileDAO, int depth) {
		assertNotNull("repoFileDAO", repoFileDAO);
		assertNotNull("repoFile", repoFile);
		final RepoFileDTO repoFileDTO;
		if (repoFile instanceof NormalFile) {
			final NormalFile normalFile = (NormalFile) repoFile;
			final NormalFileDTO normalFileDTO;
			repoFileDTO = normalFileDTO = new NormalFileDTO();
			normalFileDTO.setLength(normalFile.getLength());
			normalFileDTO.setSha1(normalFile.getSha1());
			if (depth > 0) {
				// TODO this should actually be a SortedSet, but for whatever reason, I started
				// getting ClassCastExceptions and had to switch to a normal Set :-(
				final List<FileChunk> fileChunks = new ArrayList<>(normalFile.getFileChunks());
				Collections.sort(fileChunks);
				for (final FileChunk fileChunk : fileChunks) {
					normalFileDTO.getFileChunkDTOs().add(toFileChunkDTO(fileChunk));
				}
			}
			if (depth > 1) {
				final TempChunkFileDTOIO tempChunkFileDTOIO = new TempChunkFileDTOIO();
				final File file = repoFile.getFile(getLocalRepoManager().getLocalRoot());
				for (TempChunkFileWithDTOFile tempChunkFileWithDTOFile : getOffset2TempChunkFileWithDTOFile(file).values()) {
					final File tempChunkFileDTOFile = tempChunkFileWithDTOFile.getTempChunkFileDTOFile();
					if (tempChunkFileDTOFile == null)
						continue; // incomplete: meta-data not yet written => ignore

					final TempChunkFileDTO tempChunkFileDTO;
					try {
						tempChunkFileDTO = tempChunkFileDTOIO.deserialize(tempChunkFileDTOFile);
					} catch (Exception x) {
						logger.warn("toRepoFileDTO: Ignoring corrupt tempChunkFileDTOFile '" + tempChunkFileDTOFile.getAbsolutePath() + "': " + x, x);
						continue;
					}
					normalFileDTO.getTempFileChunkDTOs().add(assertNotNull("tempChunkFileDTO.fileChunkDTO", tempChunkFileDTO.getFileChunkDTO()));
				}
			}
		}
		else if (repoFile instanceof Directory) {
			repoFileDTO = new DirectoryDTO();
		}
		else if (repoFile instanceof Symlink) {
			final Symlink symlink = (Symlink) repoFile;
			final SymlinkDTO symlinkDTO;
			repoFileDTO = symlinkDTO = new SymlinkDTO();
			symlinkDTO.setTarget(symlink.getTarget());
		}
		else
			throw new UnsupportedOperationException("RepoFile type not yet supported: " + repoFile);

		repoFileDTO.setId(repoFile.getId());
		repoFileDTO.setLocalRevision(repoFile.getLocalRevision());
		repoFileDTO.setName(repoFile.getName());
		repoFileDTO.setParentId(repoFile.getParent() == null ? null : repoFile.getParent().getId());
		repoFileDTO.setLastModified(repoFile.getLastModified());

		return repoFileDTO;
	}

	private void mkDir(LocalRepoTransaction transaction, UUID clientRepositoryId, File file, Date lastModified) {
		assertNotNull("transaction", transaction);
		assertNotNull("file", file);

		final File localRoot = getLocalRepoManager().getLocalRoot();
		if (localRoot.equals(file)) {
			return;
		}

		final File parentFile = file.getParentFile();
		ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
		try {
			RepoFile parentRepoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, parentFile);

			if (!localRoot.equals(parentFile) && (!parentFile.isDirectory() || parentRepoFile == null))
				mkDir(transaction, clientRepositoryId, parentFile, null);

			if (parentRepoFile == null)
				parentRepoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, parentFile);

			if (parentRepoFile == null) // now, it should definitely not be null anymore!
				throw new IllegalStateException("parentRepoFile == null");

			if (file.exists() && !file.isDirectory())
				file.renameTo(IOUtil.createCollisionFile(file));

			if (file.exists() && !file.isDirectory())
				throw new IllegalStateException("Could not rename file! It is still in the way: " + file);

			if (!file.isDirectory())
				file.mkdir();

			if (!file.isDirectory())
				throw new IllegalStateException("Could not create directory (permissions?!): " + file);

			RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, file);
			if (repoFile != null && !(repoFile instanceof Directory)) {
				transaction.getDAO(RepoFileDAO.class).deletePersistent(repoFile);
				repoFile = null;
			}

			if (lastModified != null)
				file.setLastModified(lastModified.getTime());

			if (repoFile == null) {
				Directory directory;
				repoFile = directory = new Directory();
				directory.setName(file.getName());
				directory.setParent(parentRepoFile);
				directory.setLastModified(new Date(file.lastModified()));
				repoFile = directory = transaction.getDAO(RepoFileDAO.class).makePersistent(directory);
			}
			else if (repoFile.getLastModified().getTime() != file.lastModified())
				repoFile.setLastModified(new Date(file.lastModified()));

			repoFile.setLastSyncFromRepositoryId(clientRepositoryId);
		} finally {
			ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
		}
	}

	/**
	 * @param path the prefixed path (relative to the real root).
	 * @return the file in the local repository. Never <code>null</code>.
	 */
	protected File getFile(String path) {
		path = assertNotNull("path", path).replace('/', File.separatorChar);
		File file = new File(getLocalRepoManager().getLocalRoot(), path);
		return file;
	}

	@Override
	public byte[] getFileData(String path, long offset, int length) {
		path = prefixPath(path);
		File file = getFile(path);
		try {
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			try {
				raf.seek(offset);
				if (length < 0) {
					long l = raf.length() - offset;
					if (l > Integer.MAX_VALUE)
						throw new IllegalArgumentException(
								String.format("The data to be read from file '%s' is too large (offset=%s length=%s limit=%s). You must specify a length (and optionally an offset) to read it partially.",
										path, offset, length, Integer.MAX_VALUE));

					length = (int) l;
				}

				byte[] bytes = new byte[length];
				int off = 0;
				int numRead = 0;
				while (off < bytes.length && (numRead = raf.read(bytes, off, bytes.length-off)) >= 0) {
					off += numRead;
				}

				if (off < bytes.length) // Read INCOMPLETELY => discarding
					return null;

				return bytes;
			} finally {
				raf.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void beginPutFile(String path) {
		path = prefixPath(path);
		final File file = getFile(path); // null-check already inside getFile(...) - no need for another check here
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		final File parentFile = file.getParentFile();
		final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				if (isSymlink(file) || (file.exists() && !file.isFile())) { // exists() and isFile() both resolve symlinks! Their result depends on where the symlink points to.
					logger.info("beginPutFile: Collision: Destination file already exists and is a symlink or a directory! file='{}'", file.getAbsolutePath());
					final File collisionFile = IOUtil.createCollisionFile(file);
					file.renameTo(collisionFile);
					new LocalRepoSync(transaction).sync(collisionFile, new NullProgressMonitor());
				}

				if (isSymlink(file) || (file.exists() && !file.isFile()))
					throw new IllegalStateException("Could not rename file! It is still in the way: " + file);

				final File localRoot = getLocalRepoManager().getLocalRoot();
				assertNoDeleteModificationCollision(transaction, clientRepositoryId, path);

				boolean newFile = false;
				if (!file.isFile()) {
					newFile = true;
					try {
						file.createNewFile();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				if (!file.isFile())
					throw new IllegalStateException("Could not create file (permissions?!): " + file);

				// A complete sync run might take very long. Therefore, we better update our local meta-data
				// *immediately* before beginning the sync of this file and before detecting a collision.
				// Furthermore, maybe the file is new and there's no meta-data, yet, hence we must do this anyway.
				final RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
				new LocalRepoSync(transaction).sync(file, new NullProgressMonitor());

				deleteTempChunkFilesWithoutDTOFile(getOffset2TempChunkFileWithDTOFile(file).values());

				final RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);
				if (repoFile == null)
					throw new IllegalStateException("LocalRepoSync.sync(...) did not create the RepoFile for file: " + file);

				if (!(repoFile instanceof NormalFile))
					throw new IllegalStateException("LocalRepoSync.sync(...) created an instance of " + repoFile.getClass().getName() + " instead  of a NormalFile for file: " + file);

				final NormalFile normalFile = (NormalFile) repoFile;

				if (!newFile && !normalFile.isInProgress())
					detectAndHandleFileCollision(transaction, clientRepositoryId, file, normalFile);

				normalFile.setLastSyncFromRepositoryId(clientRepositoryId);
				normalFile.setInProgress(true);
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private boolean isSymlink(final File file) {
		return Files.isSymbolicLink(file.toPath());
	}

	/**
	 * Detect if the file to be copied has been modified locally (or copied from another repository) after the last
	 * sync from the repository identified by {@code fromRepositoryId}.
	 * <p>
	 * If there is a collision - i.e. the destination file has been modified, too - then the destination file is moved
	 * away by renaming it. The name to which it is renamed is created by {@link IOUtil#createCollisionFile(File)}.
	 * Afterwards the file is copied back to its original name.
	 * <p>
	 * The reason for renaming it first (instead of directly copying it) is that there might be open file handles.
	 * In GNU/Linux, the open file handles stay open and thus are then connected to the renamed file, thus continuing
	 * to modify the file which was moved away. In Windows, the renaming likely fails and we abort with an exception.
	 * In both cases, we do our best to avoid both processes from writing to the same file simultaneously without locking
	 * it.
	 * <p>
	 * In the future (this is NOT YET IMPLEMENTED), we might lock it in {@link #beginPutFile(String)} and
	 * keep the lock until {@link #endPutFile(String, Date, long, String)} or a timeout occurs - and refresh the lock
	 * (i.e. postpone the timeout) with every {@link #putFileData(String, long, byte[])}. The reason for this
	 * quite complicated strategy is that we cannot guarantee that the {@link #endPutFile(String, Date, long, String)}
	 * is ever invoked (the client might crash inbetween). We don't want a locked file to linger forever.
	 *
	 * @param transaction the DB transaction. Must not be <code>null</code>.
	 * @param fromRepositoryId the ID of the source repository from which the file is about to be copied. Must not be <code>null</code>.
	 * @param file the file that is to be copied (i.e. overwritten). Must not be <code>null</code>.
	 * @param normalFileOrSymlink the DB entity corresponding to {@code file}. Must not be <code>null</code>.
	 */
	private void detectAndHandleFileCollision(LocalRepoTransaction transaction, UUID fromRepositoryId, File file, RepoFile normalFileOrSymlink) {
		if (detectFileCollision(transaction, fromRepositoryId, file, normalFileOrSymlink)) {
			final File collisionFile = IOUtil.createCollisionFile(file);
			file.renameTo(collisionFile);
			if (file.exists())
				throw new IllegalStateException("Could not rename file to resolve collision: " + file);

			try {
				Files.copy(collisionFile.toPath(), file.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			new LocalRepoSync(transaction).sync(collisionFile, new NullProgressMonitor()); // TODO sub-progress-monitor!
		}
	}

	private boolean detectFileCollisionRecursively(LocalRepoTransaction transaction, UUID fromRepositoryId, File fileOrDirectory) {
		assertNotNull("transaction", transaction);
		assertNotNull("fromRepositoryId", fromRepositoryId);
		assertNotNull("fileOrDirectory", fileOrDirectory);

		// we handle symlinks before invoking exists() below, because this method and most other File methods resolve symlinks!
		if (Files.isSymbolicLink(fileOrDirectory.toPath())) {
			RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), fileOrDirectory);
			if (!(repoFile instanceof Symlink))
				return true; // We had a change after the last local sync (symlink => directory or normal file)!

			return detectFileCollision(transaction, fromRepositoryId, fileOrDirectory, repoFile);
		}

		if (!fileOrDirectory.exists()) { // Is this correct? If it does not exist, then there is no collision? TODO what if it has been deleted locally and modified remotely and local is destination and that's our collision?!
			return false;
		}

		if (fileOrDirectory.isFile()) {
			RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), fileOrDirectory);
			if (!(repoFile instanceof NormalFile))
				return true; // We had a change after the last local sync (normal file => directory or symlink)!

			return detectFileCollision(transaction, fromRepositoryId, fileOrDirectory, repoFile);
		}

		File[] children = fileOrDirectory.listFiles();
		if (children == null)
			throw new IllegalStateException("listFiles() of directory returned null: " + fileOrDirectory);

		for (File child : children) {
			if (detectFileCollisionRecursively(transaction, fromRepositoryId, child))
				return true;
		}

		return false;
	}

	/**
	 * Detect if the file to be copied or deleted has been modified locally (or copied from another repository) after the last
	 * sync from the repository identified by {@code fromRepositoryId}.
	 * @param transaction
	 * @param fromRepositoryId
	 * @param file
	 * @param normalFileOrSymlink
	 * @return <code>true</code>, if there is a collision; <code>false</code>, if there is none.
	 */
	private boolean detectFileCollision(LocalRepoTransaction transaction, UUID fromRepositoryId, File file, RepoFile normalFileOrSymlink) {
		assertNotNull("transaction", transaction);
		assertNotNull("fromRepositoryId", fromRepositoryId);
		assertNotNull("file", file);
		assertNotNull("normalFileOrSymlink", normalFileOrSymlink);

		if (!file.exists()) {
			logger.debug("detectFileCollision: path='{}': return false, because destination file does not exist.", normalFileOrSymlink.getPath());
			return false;
		}

		RemoteRepository fromRemoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getRemoteRepositoryOrFail(fromRepositoryId);
		long lastSyncFromRemoteRepositoryLocalRevision = fromRemoteRepository.getLocalRevision();
		if (normalFileOrSymlink.getLocalRevision() <= lastSyncFromRemoteRepositoryLocalRevision) {
			logger.debug("detectFileCollision: path='{}': return false, because: normalFileOrSymlink.localRevision <= lastSyncFromRemoteRepositoryLocalRevision :: {} <= {}", normalFileOrSymlink.getPath(), normalFileOrSymlink.getLocalRevision(), lastSyncFromRemoteRepositoryLocalRevision);
			return false;
		}

		// The file was transferred from the same repository before and was thus not changed locally nor in another repo.
		// This can only happen, if the sync was interrupted (otherwise the check for the localRevision above
		// would have already caused this method to abort).
		if (fromRepositoryId.equals(normalFileOrSymlink.getLastSyncFromRepositoryId())) {
			logger.debug("detectFileCollision: path='{}': return false, because: fromRepositoryId == normalFileOrSymlink.lastSyncFromRepositoryId :: fromRepositoryId='{}'", normalFileOrSymlink.getPath(), fromRemoteRepository);
			return false;
		}

		logger.debug("detectFileCollision: path='{}': return true! fromRepositoryId='{}' normalFileOrSymlink.localRevision={} lastSyncFromRemoteRepositoryLocalRevision={} normalFileOrSymlink.lastSyncFromRepositoryId='{}'",
				normalFileOrSymlink.getPath(), fromRemoteRepository, normalFileOrSymlink.getLocalRevision(), lastSyncFromRemoteRepositoryLocalRevision, normalFileOrSymlink.getLastSyncFromRepositoryId());
		return true;
	}

	@Override
	public void putFileData(String path, final long offset, final byte[] fileData) {
		path = prefixPath(path);
		final File file = getFile(path);
		final File parentFile = file.getParentFile();
		final File localRoot = getLocalRepoManager().getLocalRoot();
		final LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction(); // It writes into the file system, but it only reads from the DB.
		try {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, file);
				if (repoFile == null)
					throw new IllegalStateException("No RepoFile found for file: " + file);

				if (!(repoFile instanceof NormalFile))
					throw new IllegalStateException("RepoFile is not an instance of NormalFile for file: " + file);

				NormalFile normalFile = (NormalFile) repoFile;
				if (!normalFile.isInProgress())
					throw new IllegalStateException(String.format("NormalFile.inProgress == false! beginFile(...) not called?! repoFile=%s file=%s",
							repoFile, file));

				final FileWriteStrategy fileWriteStrategy = getFileWriteStrategy(file);
				logger.debug("putFileData: fileWriteStrategy={}", fileWriteStrategy);
				switch (fileWriteStrategy) {
					case directDuringTransfer:
						writeFileDataToDestFile(file, offset, fileData);
						break;
					case directAfterTransfer:
					case replaceAfterTransfer:
						writeFileDataToTempChunkFile(file, offset, fileData);
						break;
					default:
						throw new IllegalStateException("Unknown fileWriteStrategy: " + fileWriteStrategy);
				}
			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void writeTempChunkFileToDestFile(final File destFile, final File tempChunkFile, final TempChunkFileDTO tempChunkFileDTO) {
		assertNotNull("destFile", destFile);
		assertNotNull("tempChunkFile", tempChunkFile);
		assertNotNull("tempChunkFileDTO", tempChunkFileDTO);
		final long offset = assertNotNull("tempChunkFileDTO.fileChunkDTO", tempChunkFileDTO.getFileChunkDTO()).getOffset();
		final byte[] fileData = new byte[(int) tempChunkFile.length()];
		try {
			final InputStream in = new FileInputStream(tempChunkFile);
			try {
				int off = 0;
				while (off < fileData.length) {
					final int bytesRead = in.read(fileData, off, fileData.length - off);
					if (bytesRead > 0) {
						off += bytesRead;
					}
					else if (bytesRead < 0) {
						throw new IllegalStateException("InputStream ended before expected file length!");
					}
				}
				if (off > fileData.length || in.read() != -1)
					throw new IllegalStateException("InputStream contained more data than expected file length!");
			} finally {
				in.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		final String sha1FromDTOFile = tempChunkFileDTO.getFileChunkDTO().getSha1();
		final String sha1FromFileData = sha1(fileData);

		logger.trace("writeTempChunkFileToDestFile: Read {} bytes with SHA1 '{}' from '{}'.", fileData.length, sha1FromFileData, tempChunkFile.getAbsolutePath());

		if (!sha1FromFileData.equals(sha1FromDTOFile))
			throw new IllegalStateException("SHA1 mismatch! Corrupt temporary chunk file or corresponding DTO file: " + tempChunkFile.getAbsolutePath());

		writeFileDataToDestFile(destFile, offset, fileData);
	}

	private void writeFileDataToDestFile(final File destFile, final long offset, final byte[] fileData) {
		assertNotNull("destFile", destFile);
		assertNotNull("fileData", fileData);
		try {
			final RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
			try {
				raf.seek(offset);
				raf.write(fileData);
			} finally {
				raf.close();
			}
			logger.trace("writeFileDataToDestFile: Wrote {} bytes at offset {} to '{}'.", fileData.length, offset, destFile.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeFileDataToTempChunkFile(final File destFile, final long offset, final byte[] fileData) {
		assertNotNull("destFile", destFile);
		assertNotNull("fileData", fileData);
		try {
			final File tempChunkFile = createTempChunkFile(destFile, offset);
			final File tempChunkFileDTOFile = getTempChunkFileDTOFile(tempChunkFile);

			// Delete the meta-data-file, in case we overwrite an older temp-chunk-file. This way it
			// is guaranteed, that if the meta-data-file exists, it is consistent with either
			// the temp-chunk-file or the chunk was already written into the final destination.
			deleteOrFail(tempChunkFileDTOFile);

			final FileOutputStream out = new FileOutputStream(tempChunkFile);
			try {
				out.write(fileData);
			} finally {
				out.close();
			}
			final String sha1 = sha1(fileData);
			logger.trace("writeFileDataToTempChunkFile: Wrote {} bytes with SHA1 '{}' to '{}'.", fileData.length, sha1, tempChunkFile.getAbsolutePath());
			final TempChunkFileDTO tempChunkFileDTO = createTempChunkFileDTO(offset, tempChunkFile, sha1);
			new TempChunkFileDTOIO().serialize(tempChunkFileDTO, tempChunkFileDTOFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void deleteTempChunkFilesWithoutDTOFile(Collection<TempChunkFileWithDTOFile> tempChunkFileWithDTOFiles) {
		for (final TempChunkFileWithDTOFile tempChunkFileWithDTOFile : tempChunkFileWithDTOFiles) {
			final File tempChunkFileDTOFile = tempChunkFileWithDTOFile.getTempChunkFileDTOFile();
			if (tempChunkFileDTOFile == null || !tempChunkFileDTOFile.exists()) {
				File tempChunkFile = tempChunkFileWithDTOFile.getTempChunkFile();
				logger.warn("deleteTempChunkFilesWithoutDTOFile: No DTO-file for temporary chunk-file '{}'! DELETING this temporary file!", tempChunkFile.getAbsolutePath());
				deleteOrFail(tempChunkFile);
				continue;
			}
		}
	}

	private Map<Long, TempChunkFileWithDTOFile> getOffset2TempChunkFileWithDTOFile(final File destFile) {
		final File[] tempFiles = getTempDir(destFile).listFiles();
		if (tempFiles == null)
			return Collections.emptyMap();

		final String destFileName = destFile.getName();
		final Map<Long, TempChunkFileWithDTOFile> result = new TreeMap<Long, TempChunkFileWithDTOFile>();
		for (final File tempFile : tempFiles) {
			String tempFileName = tempFile.getName();
			if (!tempFileName.startsWith(TEMP_CHUNK_FILE_PREFIX))
				continue;

			final boolean dtoFile;
			if (tempFileName.endsWith(TEMP_CHUNK_FILE_DTO_FILE_SUFFIX)) {
				dtoFile = true;
				tempFileName = tempFileName.substring(0, tempFileName.length() - TEMP_CHUNK_FILE_DTO_FILE_SUFFIX.length());
			}
			else
				dtoFile = false;

			final int lastUnderscoreIndex = tempFileName.lastIndexOf('_');
			if (lastUnderscoreIndex < 0)
				throw new IllegalStateException("lastUnderscoreIndex < 0 :: tempFileName='" + tempFileName + '\'');

			final String tempFileDestFileName = tempFileName.substring(TEMP_CHUNK_FILE_PREFIX.length(), lastUnderscoreIndex);
			if (!destFileName.equals(tempFileDestFileName))
				continue;

			final String offsetStr = tempFileName.substring(lastUnderscoreIndex + 1);
			final Long offset = Long.valueOf(offsetStr, 36);
			TempChunkFileWithDTOFile tempChunkFileWithDTOFile = result.get(offset);
			if (tempChunkFileWithDTOFile == null) {
				tempChunkFileWithDTOFile = new TempChunkFileWithDTOFile();
				result.put(offset, tempChunkFileWithDTOFile);
			}
			if (dtoFile)
				tempChunkFileWithDTOFile.setTempChunkFileDTOFile(tempFile);
			else
				tempChunkFileWithDTOFile.setTempChunkFile(tempFile);
		}
		return Collections.unmodifiableMap(result);
	}

	private File getTempChunkFileDTOFile(File file) {
		return new File(file.getParentFile(), file.getName() + TEMP_CHUNK_FILE_DTO_FILE_SUFFIX);
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

	/**
	 * Create the temporary file for the given {@code destFile} and {@code offset}.
	 * <p>
	 * The returned file is created, if it does not yet exist; but it is <i>not</i> overwritten,
	 * if it already exists.
	 * <p>
	 * The {@linkplain #getTempDir(File) temporary directory} in which the temporary file is located
	 * is created, if necessary. In order to prevent collisions with code trying to delete the empty
	 * temporary directory, this method and the corresponding {@link #deleteTempDirIfEmpty(File)} are
	 * both synchronized.
	 * @param destFile the destination file for which to resolve and create the temporary file.
	 * Must not be <code>null</code>.
	 * @param offset the offset (inside the final destination file and the source file) of the block to
	 * be temporarily stored in the temporary file created by this method. The temporary file will hold
	 * solely this block (thus the offset in the temporary file is 0).
	 * @return the temporary file. Never <code>null</code>. The file is already created in the file system
	 * (empty), if it did not yet exist.
	 */
	private synchronized File createTempChunkFile(final File destFile, final long offset) {
		final File tempDir = getTempDir(destFile);
		tempDir.mkdir();
		if (!tempDir.isDirectory())
			throw new IllegalStateException("Creating the directory failed (it does not exist after mkdir): " + tempDir.getAbsolutePath());

		final File tempFile = new File(tempDir, String.format("%s%s_%s",
				TEMP_CHUNK_FILE_PREFIX, destFile.getName(), Long.toString(offset, 36)));
		try {
			tempFile.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return tempFile;
	}

	/**
	 * Deletes the {@linkplain #getTempDir(File) temporary directory} for the given {@code destFile},
	 * if this directory is empty.
	 * <p>
	 * This method is synchronized to prevent it from colliding with {@link #createTempChunkFile(File, long)}
	 * which first creates the temporary directory and then the file in it. Without synchronisation, the
	 * newly created directory might be deleted by this method, before the temporary file in it is created.
	 * @param destFile the destination file for which to resolve and delete the temporary directory.
	 * Must not be <code>null</code>.
	 */
	private synchronized void deleteTempDirIfEmpty(final File destFile) {
		final File tempDir = getTempDir(destFile);
		tempDir.delete(); // deletes only empty directories ;-)
	}

	private File getTempDir(final File destFile) {
		assertNotNull("destFile", destFile);
		final File parentDir = destFile.getParentFile();
		return new File(parentDir, LocalRepoManager.TEMP_DIR_NAME);
	}

	private final Map<File, FileWriteStrategy> file2FileWriteStrategy = new WeakHashMap<>();

	private FileWriteStrategy getFileWriteStrategy(File file) {
		assertNotNull("file", file);
		synchronized (file2FileWriteStrategy) {
			FileWriteStrategy fileWriteStrategy = file2FileWriteStrategy.get(file);
			if (fileWriteStrategy == null) {
				fileWriteStrategy = Config.getInstanceForFile(file).getPropertyAsEnum(FileWriteStrategy.CONFIG_KEY, FileWriteStrategy.CONFIG_DEFAULT_VALUE);
				file2FileWriteStrategy.put(file, fileWriteStrategy);
			}
			return fileWriteStrategy;
		}
	}

	@Override
	public void endPutFile(String path, final Date lastModified, final long length, String sha1) {
		path = prefixPath(path);
		assertNotNull("lastModified", lastModified);
		assertNotNull("sha1", sha1);
		final File file = getFile(path);
		final File parentFile = file.getParentFile();
		final UUID clientRepositoryId = getClientRepositoryIdOrFail();
		final LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			ParentFileLastModifiedManager.getInstance().backupParentFileLastModified(parentFile);
			try {
				final RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
				if (!(repoFile instanceof NormalFile)) {
					throw new IllegalStateException(String.format("RepoFile is not an instance of NormalFile! repoFile=%s file=%s",
							repoFile, file));
				}

				final NormalFile normalFile = (NormalFile) repoFile;
				if (!normalFile.isInProgress())
					throw new IllegalStateException(String.format("NormalFile.inProgress == false! beginFile(...) not called?! repoFile=%s file=%s",
							repoFile, file));

				final FileWriteStrategy fileWriteStrategy = getFileWriteStrategy(file);
				logger.debug("endPutFile: fileWriteStrategy={}", fileWriteStrategy);

				final File destFile = (fileWriteStrategy == FileWriteStrategy.replaceAfterTransfer
						? new File(file.getParentFile(), LocalRepoManager.TEMP_NEW_FILE_PREFIX + file.getName()) : file);

				final InputStream fileIn;
				if (destFile != file) {
					try {
						fileIn = new FileInputStream(file);
						destFile.createNewFile();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				else
					fileIn = null;

				final TempChunkFileDTOIO tempChunkFileDTOIO = new TempChunkFileDTOIO();
				long destFileWriteOffset = 0;
				// tempChunkFileWithDTOFiles are sorted by offset (ascending)
				final Collection<TempChunkFileWithDTOFile> tempChunkFileWithDTOFiles = getOffset2TempChunkFileWithDTOFile(file).values();
				for (final TempChunkFileWithDTOFile tempChunkFileWithDTOFile : tempChunkFileWithDTOFiles) {
					final File tempChunkFile = tempChunkFileWithDTOFile.getTempChunkFile(); // tempChunkFile may be null!!!
					final File tempChunkFileDTOFile = tempChunkFileWithDTOFile.getTempChunkFileDTOFile();
					if (tempChunkFileDTOFile == null)
						throw new IllegalStateException("No meta-data (tempChunkFileDTOFile) for file: " + (tempChunkFile == null ? null : tempChunkFile.getAbsolutePath()));

					final TempChunkFileDTO tempChunkFileDTO = tempChunkFileDTOIO.deserialize(tempChunkFileDTOFile);
					final long offset = assertNotNull("tempChunkFileDTO.fileChunkDTO", tempChunkFileDTO.getFileChunkDTO()).getOffset();

					if (fileIn != null) {
						// The following might fail, if *file* was truncated during the transfer. In this case,
						// throwing an exception now is probably the best choice as the next sync run will
						// continue cleanly.
						writeFileDataToDestFile(destFile, destFileWriteOffset, fileIn, offset - destFileWriteOffset);
						final long tempChunkFileLength = tempChunkFileDTO.getFileChunkDTO().getLength();
						skipOrFail(fileIn, tempChunkFileLength); // skipping beyond the EOF is supported by the FileInputStream according to Javadoc.
						destFileWriteOffset = offset + tempChunkFileLength;
					}

					if (tempChunkFile != null && tempChunkFile.exists()) {
						writeTempChunkFileToDestFile(destFile, tempChunkFile, tempChunkFileDTO);
						deleteOrFail(tempChunkFile);
					}
				}

				if (fileIn != null && destFileWriteOffset < length)
					writeFileDataToDestFile(destFile, destFileWriteOffset, fileIn, length - destFileWriteOffset);

				try {
					final RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
					try {
						raf.setLength(length);
					} finally {
						raf.close();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				if (destFile != file) {
					deleteOrFail(file);
					destFile.renameTo(file);
					if (!file.exists())
						throw new IllegalStateException(String.format("Renaming the file from '%s' to '%s' failed: The destination file does not exist.", destFile.getAbsolutePath(), file.getAbsolutePath()));

					if (destFile.exists())
						throw new IllegalStateException(String.format("Renaming the file from '%s' to '%s' failed: The source file still exists.", destFile.getAbsolutePath(), file.getAbsolutePath()));
				}

				deleteTempChunkFiles(tempChunkFileWithDTOFiles);
				deleteTempDirIfEmpty(file);

				LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
				file.setLastModified(lastModified.getTime());
				localRepoSync.updateRepoFile(normalFile, file, new NullProgressMonitor());
				normalFile.setLastSyncFromRepositoryId(clientRepositoryId);
				normalFile.setInProgress(false);

				logger.trace("endPutFile: Committing: sha1='{}' file='{}'", normalFile.getSha1(), file);
				if (!sha1.equals(normalFile.getSha1())) {
					logger.warn("endPutFile: File was modified during transport (either on source or destination side): expectedSha1='{}' foundSha1='{}' file='{}'",
							sha1, normalFile.getSha1(), file);
				}

			} finally {
				ParentFileLastModifiedManager.getInstance().restoreParentFileLastModified(parentFile);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void deleteTempChunkFiles(final Collection<TempChunkFileWithDTOFile> tempChunkFileWithDTOFiles) {
		for (final TempChunkFileWithDTOFile tempChunkFileWithDTOFile : tempChunkFileWithDTOFiles) {
			final File tempChunkFile = tempChunkFileWithDTOFile.getTempChunkFile(); // tempChunkFile may be null!!!
			final File tempChunkFileDTOFile = tempChunkFileWithDTOFile.getTempChunkFileDTOFile();

			if (tempChunkFile != null && tempChunkFile.exists())
				deleteOrFail(tempChunkFile);

			if (tempChunkFileDTOFile != null && tempChunkFileDTOFile.exists())
				deleteOrFail(tempChunkFileDTOFile);
		}
	}

	private void deleteOrFail(File file) {
		file.delete();
		if (isSymlink(file) || file.exists())
			throw new IllegalStateException("Could not delete file (it still exists after deletion): " + file);
	}

	/**
	 * Skip the given {@code length} number of bytes.
	 * <p>
	 * Because {@link InputStream#skip(long)} and {@link FileInputStream#skip(long)} are both documented to skip
	 * over less than the requested number of bytes "for a number of reasons", this method invokes the underlying
	 * skip(...) method multiple times until either EOF is reached or the requested number of bytes was skipped.
	 * In case of EOF, an
	 * @param in the {@link InputStream} to be skipped. Must not be <code>null</code>.
	 * @param length the number of bytes to be skipped. Must not be negative (i.e. <code>length &gt;= 0</code>).
	 */
	private void skipOrFail(InputStream in, long length) {
		assertNotNull("in", in);
		if (length < 0)
			throw new IllegalArgumentException("length < 0");

		long skipped = 0;
		int skippedNowWas0Counter = 0;
		while (skipped < length) {
			final long toSkip = length - skipped;
			try {
				final long skippedNow = in.skip(toSkip);
				if (skippedNow < 0)
					throw new IOException("in.skip(" + toSkip + ") returned " + skippedNow);

				if (skippedNow == 0) {
					if (++skippedNowWas0Counter >= 5) {
						throw new IOException(String.format(
								"Could not skip %s consecutive times!", skippedNowWas0Counter));
					}
				}
				else
					skippedNowWas0Counter = 0;

				skipped += skippedNow;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void writeFileDataToDestFile(final File destFile, final long offset, final InputStream in, final long length) {
		assertNotNull("destFile", destFile);
		assertNotNull("in", in);
		if (offset < 0)
			throw new IllegalArgumentException("offset < 0");

		if (length == 0)
			return;

		if (length < 0)
			throw new IllegalArgumentException("length < 0");

		long lengthDone = 0;

		try {
			final RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
			try {
				raf.seek(offset);

				final byte[] buf = new byte[200 * 1024];

				while (lengthDone < length) {
					final long len = Math.min(length - lengthDone, buf.length);
					final int bytesRead = in.read(buf, 0, (int)len);
					if (bytesRead > 0) {
						raf.write(buf, 0, bytesRead);
						lengthDone += bytesRead;
					}
					else if (bytesRead < 0)
						throw new IOException("Premature end of stream!");
				}
			} finally {
				raf.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void endSyncFromRepository() {
		UUID clientRepositoryId = getClientRepositoryIdOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			PersistenceManager pm = ((co.codewizards.cloudstore.local.LocalRepoTransactionImpl)transaction).getPersistenceManager();
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);
			ModificationDAO modificationDAO = transaction.getDAO(ModificationDAO.class);

			RemoteRepository toRemoteRepository = remoteRepositoryDAO.getRemoteRepositoryOrFail(clientRepositoryId);

			LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepoOrFail(toRemoteRepository);
			if (lastSyncToRemoteRepo.getLocalRepositoryRevisionInProgress() < 0)
				throw new IllegalStateException(String.format("lastSyncToRemoteRepo.localRepositoryRevisionInProgress < 0 :: There is no sync in progress for the RemoteRepository with entityID=%s", clientRepositoryId));

			lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(lastSyncToRemoteRepo.getLocalRepositoryRevisionInProgress());
			lastSyncToRemoteRepo.setLocalRepositoryRevisionInProgress(-1);

			pm.flush(); // prevent problems caused by batching, deletion and foreign keys
			Collection<Modification> modifications = modificationDAO.getModificationsBeforeOrEqual(
					toRemoteRepository, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
			modificationDAO.deletePersistentAll(modifications);
			pm.flush();

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void endSyncToRepository(long fromLocalRevision) {
		UUID clientRepositoryId = getClientRepositoryIdOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			RemoteRepository remoteRepository = remoteRepositoryDAO.getRemoteRepositoryOrFail(clientRepositoryId);
			remoteRepository.setRevision(fromLocalRevision);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

}
