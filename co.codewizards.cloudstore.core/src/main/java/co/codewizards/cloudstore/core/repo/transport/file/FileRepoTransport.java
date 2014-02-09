package co.codewizards.cloudstore.core.repo.transport.file;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.CopyModificationDTO;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunkDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.persistence.CopyModification;
import co.codewizards.cloudstore.core.persistence.DeleteModification;
import co.codewizards.cloudstore.core.persistence.DeleteModificationDAO;
import co.codewizards.cloudstore.core.persistence.Directory;
import co.codewizards.cloudstore.core.persistence.FileChunk;
import co.codewizards.cloudstore.core.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.core.persistence.LastSyncToRemoteRepoDAO;
import co.codewizards.cloudstore.core.persistence.LocalRepository;
import co.codewizards.cloudstore.core.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.Modification;
import co.codewizards.cloudstore.core.persistence.ModificationDAO;
import co.codewizards.cloudstore.core.persistence.NormalFile;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequestDAO;
import co.codewizards.cloudstore.core.persistence.RepoFile;
import co.codewizards.cloudstore.core.persistence.RepoFileDAO;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.FilenameFilterSkipMetaDir;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoSync;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.repo.transport.DeleteModificationCollisionException;
import co.codewizards.cloudstore.core.util.IOUtil;

public class FileRepoTransport extends AbstractRepoTransport {
	private static final Logger logger = LoggerFactory.getLogger(FileRepoTransport.class);

	private static final long MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY = 100; // TODO make configurable!

	private LocalRepoManager localRepoManager;

	@Override
	public void close() {
		if (localRepoManager != null)
			localRepoManager.close();
	}

	@Override
	public EntityID getRepositoryID() {
		return getLocalRepoManager().getRepositoryID();
	}

	@Override
	public byte[] getPublicKey() {
		return getLocalRepoManager().getPublicKey();
	}

	@Override
	public void requestRepoConnection(byte[] publicKey) {
		assertNotNull("publicKey", publicKey);
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrNull(clientRepositoryID);
			if (remoteRepository != null)
				throw new IllegalArgumentException("RemoteRepository already connected! repositoryID=" + clientRepositoryID);

			String localPathPrefix = getPathPrefix();
			RemoteRepositoryRequestDAO remoteRepositoryRequestDAO = transaction.getDAO(RemoteRepositoryRequestDAO.class);
			RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDAO.getRemoteRepositoryRequest(clientRepositoryID);
			if (remoteRepositoryRequest != null) {
				logger.info("RemoteRepository already requested to be connected. repositoryID={}", clientRepositoryID);
				remoteRepositoryRequest.setChanged(new Date()); // make sure it is not deleted soon (the request expires after a while)
				remoteRepositoryRequest.setPublicKey(publicKey);
				remoteRepositoryRequest.setLocalPathPrefix(localPathPrefix);
			}
			else {
				long remoteRepositoryRequestsCount = remoteRepositoryRequestDAO.getObjectsCount();
				if (remoteRepositoryRequestsCount >= MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY)
					throw new IllegalStateException(String.format(
							"The maximum number of connection requests (%s) is reached or exceeded! Please retry later, when old requests were accepted or expired.", MAX_REMOTE_REPOSITORY_REQUESTS_QUANTITY));

				remoteRepositoryRequest = new RemoteRepositoryRequest();
				remoteRepositoryRequest.setRepositoryID(clientRepositoryID);
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

		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
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

			RemoteRepository toRemoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(clientRepositoryID);

			LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepo(toRemoteRepository);
			if (lastSyncToRemoteRepo == null) {
				lastSyncToRemoteRepo = new LastSyncToRemoteRepo();
				lastSyncToRemoteRepo.setRemoteRepository(toRemoteRepository);
				lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(-1);
			}
			lastSyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());
			lastSyncToRemoteRepoDAO.makePersistent(lastSyncToRemoteRepo);

			Collection<Modification> modifications = modificationDAO.getModificationsAfter(toRemoteRepository, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
			changeSetDTO.setModificationDTOs(toModificationDTOs(modifications));

			if (!getPathPrefix().isEmpty()) {
				Collection<DeleteModification> deleteModifications = transaction.getDAO(DeleteModificationDAO.class).getDeleteModificationsForPathOrParentOfPathAfter(
						getPathPrefix(), lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced(), toRemoteRepository);
				if (!deleteModifications.isEmpty()) { // our virtual root was deleted => create synthetic DeleteModificationDTO for virtual root
					DeleteModificationDTO deleteModificationDTO = new DeleteModificationDTO();
					deleteModificationDTO.setEntityID(new EntityID(0, 0));
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
			Map<EntityID, RepoFileDTO> entityID2RepoFileDTO = getEntityID2RepoFileDTOWithParents(pathPrefixRepoFile, repoFiles, repoFileDAO);
			changeSetDTO.setRepoFileDTOs(new ArrayList<RepoFileDTO>(entityID2RepoFileDTO.values()));

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
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			assertNoDeleteModificationCollision(transaction, clientRepositoryID, path);
			mkDir(transaction, file, lastModified);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void assertNoDeleteModificationCollision(LocalRepoTransaction transaction, EntityID fromRepositoryID, String path) throws DeleteModificationCollisionException {
		RemoteRepository fromRemoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getObjectByIdOrFail(fromRepositoryID);
		long lastSyncFromRemoteRepositoryLocalRevision = fromRemoteRepository.getLocalRevision();

		if (!path.startsWith("/"))
			path = '/' + path;

		DeleteModificationDAO deleteModificationDAO = transaction.getDAO(DeleteModificationDAO.class);
		Collection<DeleteModification> deleteModifications = deleteModificationDAO.getDeleteModificationsForPathOrParentOfPathAfter(
				path, lastSyncFromRemoteRepositoryLocalRevision, fromRemoteRepository);

		if (!deleteModifications.isEmpty())
			throw new DeleteModificationCollisionException(
					String.format("There is at least one DeleteModification for repositoryID=%s path='%s'", fromRepositoryID, path));
	}

	@Override
	public void copy(String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);

		File fromFile = getFile(fromPath);
		File toFile = getFile(toPath);

		if (!fromFile.exists()) // TODO throw an exception and catch in RepoToRepoSync!
			return;

		if (toFile.exists()) // TODO either simply throw an exception or implement proper collision check.
			return;

		File toParentFile = toFile.getParentFile();
		long toParentFileLastModified = toParentFile.exists() ? toParentFile.lastModified() : Long.MIN_VALUE;
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			try {
				IOUtil.copyFile(fromFile, toFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
			localRepoSync.sync(toFile, new NullProgressMonitor());

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();

			if (toParentFileLastModified != Long.MIN_VALUE)
				toParentFile.setLastModified(toParentFileLastModified);
		}
	}

	@Override
	public void move(String fromPath, String toPath) {
		fromPath = prefixPath(fromPath);
		toPath = prefixPath(toPath);

		File fromFile = getFile(fromPath);
		File toFile = getFile(toPath);

		if (!fromFile.exists()) // TODO throw an exception and catch in RepoToRepoSync!
			return;

		if (toFile.exists()) // TODO either simply throw an exception or implement proper collision check.
			return;

		File fromParentFile = fromFile.getParentFile();
		File toParentFile = toFile.getParentFile();
		long fromParentFileLastModified = fromParentFile.exists() ? fromParentFile.lastModified() : Long.MIN_VALUE;
		long toParentFileLastModified = toParentFile.exists() ? toParentFile.lastModified() : Long.MIN_VALUE;
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			try {
				Files.move(fromFile.toPath(), toFile.toPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
			localRepoSync.sync(toFile, new NullProgressMonitor());
			RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), fromFile);
			if (repoFile != null)
				localRepoSync.deleteRepoFile(repoFile);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();

			if (fromParentFileLastModified != Long.MIN_VALUE)
				fromParentFile.setLastModified(fromParentFileLastModified);

			if (toParentFileLastModified != Long.MIN_VALUE)
				toParentFile.setLastModified(toParentFileLastModified);
		}
	}

	@Override
	public void delete(String path) {
		path = prefixPath(path);
		File file = getFile(path);
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		boolean fileIsLocalRoot = localRepoManager.getLocalRoot().equals(file);
		File parentFile = file.getParentFile();
		long parentFileLastModified = parentFile.exists() ? parentFile.lastModified() : Long.MIN_VALUE;
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
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
						delete(transaction, localRepoSync, clientRepositoryID, child);
				} finally {
					file.setLastModified(fileLastModified);
				}
			}
			else
				delete(transaction, localRepoSync, clientRepositoryID, file);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();

			if (parentFileLastModified != Long.MIN_VALUE)
				parentFile.setLastModified(parentFileLastModified);
		}
	}

	private void delete(LocalRepoTransaction transaction, LocalRepoSync localRepoSync, EntityID fromRepositoryID, File file) {
		if (detectFileCollisionRecursively(transaction, fromRepositoryID, file)) {
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
		FileChunkDTO fileChunkDTO = new FileChunkDTO();
		fileChunkDTO.setOffset(fileChunk.getOffset());
		fileChunkDTO.setLength(fileChunk.getLength());
		fileChunkDTO.setSha1(fileChunk.getSha1());
		return fileChunkDTO;
	}

	protected LocalRepoManager getLocalRepoManager() {
		if (localRepoManager == null) {
			File remoteRootFile;
			try {
				remoteRootFile = new File(getRemoteRootWithoutPathPrefix().toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(remoteRootFile);
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
		List<ModificationDTO> result = new ArrayList<ModificationDTO>(assertNotNull("modifications", modifications).size());
		for (Modification modification : modifications) {
			ModificationDTO modificationDTO = toModificationDTO(modification);
			if (modificationDTO != null)
				result.add(modificationDTO);
		}
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

		modificationDTO.setEntityID(modification.getEntityID());
		modificationDTO.setLocalRevision(modification.getLocalRevision());

		return modificationDTO;
	}

	private RepositoryDTO toRepositoryDTO(LocalRepository localRepository) {
		RepositoryDTO repositoryDTO = new RepositoryDTO();
		repositoryDTO.setEntityID(localRepository.getEntityID());
		repositoryDTO.setRevision(localRepository.getRevision());
		repositoryDTO.setPublicKey(localRepository.getPublicKey());
		return repositoryDTO;
	}

	private Map<EntityID, RepoFileDTO> getEntityID2RepoFileDTOWithParents(RepoFile pathPrefixRepoFile, Collection<RepoFile> repoFiles, RepoFileDAO repoFileDAO) {
		assertNotNull("repoFileDAO", repoFileDAO);
		assertNotNull("repoFiles", repoFiles);
		Map<EntityID, RepoFileDTO> entityID2RepoFileDTO = new HashMap<EntityID, RepoFileDTO>();
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
				if (!entityID2RepoFileDTO.containsKey(rf.getEntityID())) {
					RepoFileDTO repoFileDTO = toRepoFileDTO(rf, repoFileDAO, 0);
					if (pathPrefixRepoFile != null && pathPrefixRepoFile.equals(rf)) {
						repoFileDTO.setParentEntityID(null); // virtual root has no parent!
						repoFileDTO.setName(""); // virtual root has no name!
					}

					entityID2RepoFileDTO.put(rf.getEntityID(), repoFileDTO);
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
		RepoFileDTO repoFileDTO;
		if (repoFile instanceof NormalFile) {
			NormalFile normalFile = (NormalFile) repoFile;
			NormalFileDTO normalFileDTO;
			repoFileDTO = normalFileDTO = new NormalFileDTO();
			normalFileDTO.setLength(normalFile.getLength());
			normalFileDTO.setSha1(normalFile.getSha1());
			if (depth > 0) {
				for (FileChunk fileChunk : normalFile.getFileChunks()) {
					normalFileDTO.getFileChunkDTOs().add(toFileChunkDTO(fileChunk));
				}
			}
		}
		else if (repoFile instanceof Directory) {
			repoFileDTO = new DirectoryDTO();
		}
		else // TODO support symlinks!
			throw new UnsupportedOperationException("RepoFile type not yet supported: " + repoFile);

		repoFileDTO.setEntityID(repoFile.getEntityID());
		repoFileDTO.setLocalRevision(repoFile.getLocalRevision());
		repoFileDTO.setName(repoFile.getName());
		repoFileDTO.setParentEntityID(repoFile.getParent() == null ? null : repoFile.getParent().getEntityID());
		repoFileDTO.setLastModified(repoFile.getLastModified());

		return repoFileDTO;
	}

	private void mkDir(LocalRepoTransaction transaction, File file, Date lastModified) {
		assertNotNull("transaction", transaction);
		assertNotNull("file", file);

		File localRoot = getLocalRepoManager().getLocalRoot();
		if (localRoot.equals(file)) {
			return;
		}

		File parentFile = file.getParentFile();
		long parentFileLastModified = parentFile.exists() ? parentFile.lastModified() : Long.MIN_VALUE;
		try {
			RepoFile parentRepoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, parentFile);

			if (!localRoot.equals(parentFile) && (!parentFile.isDirectory() || parentRepoFile == null))
				mkDir(transaction, parentFile, null);

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
				transaction.getPersistenceManager().flush();
				transaction.getDAO(RepoFileDAO.class).deletePersistent(repoFile);
				transaction.getPersistenceManager().flush();
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
		} finally {
			if (parentFileLastModified != Long.MIN_VALUE)
				parentFile.setLastModified(parentFileLastModified);
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
		File file = getFile(path); // null-check already inside getFile(...) - no need for another check here
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		File parentFile = file.getParentFile();
		long parentFileLastModified = parentFile.exists() ? parentFile.lastModified() : Long.MIN_VALUE;
		try {
			if (file.exists() && !file.isFile())
				file.renameTo(IOUtil.createCollisionFile(file));

			if (file.exists() && !file.isFile())
				throw new IllegalStateException("Could not rename file! It is still in the way: " + file);

			File localRoot = getLocalRepoManager().getLocalRoot();
			LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
			try {
				assertNoDeleteModificationCollision(transaction, clientRepositoryID, path);

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
				RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
				new LocalRepoSync(transaction).sync(file, new NullProgressMonitor());
				transaction.getPersistenceManager().flush();

				RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);
				if (repoFile == null)
					throw new IllegalStateException("LocalRepoSync.sync(...) did not create the RepoFile for file: " + file);

				if (!(repoFile instanceof NormalFile))
					throw new IllegalStateException("LocalRepoSync.sync(...) created an instance of " + repoFile.getClass().getName() + " instead  of a NormalFile for file: " + file);

				NormalFile normalFile = (NormalFile) repoFile;

				if (!newFile && !normalFile.isInProgress())
					detectAndHandleFileCollision(transaction, clientRepositoryID, file, normalFile);

				normalFile.setLastSyncFromRepositoryID(clientRepositoryID);
				normalFile.setInProgress(true);

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			if (parentFileLastModified != Long.MIN_VALUE)
				parentFile.setLastModified(parentFileLastModified);
		}
	}

	/**
	 * Detect if the file to be copied has been modified locally (or copied from another repository) after the last
	 * sync from the repository identified by {@code fromRepositoryID}.
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
	 * keep the lock until {@link #endPutFile(String, Date, long)} or a timeout occurs - and refresh the lock
	 * (i.e. postpone the timeout) with every {@link #putFileData(String, long, byte[])}. The reason for this
	 * quite complicated strategy is that we cannot guarantee that the {@link #endPutFile(String, Date, long)}
	 * is ever invoked (the client might crash inbetween). We don't want a locked file to linger forever.
	 *
	 * @param transaction the DB transaction. Must not be <code>null</code>.
	 * @param fromRepositoryID the ID of the source repository from which the file is about to be copied. Must not be <code>null</code>.
	 * @param file the file that is to be copied (i.e. overwritten). Must not be <code>null</code>.
	 * @param normalFile the DB entity corresponding to {@code file}. Must not be <code>null</code>.
	 */
	private void detectAndHandleFileCollision(LocalRepoTransaction transaction, EntityID fromRepositoryID, File file, NormalFile normalFile) {
		if (detectFileCollision(transaction, fromRepositoryID, file, normalFile)) {
			File collisionFile = IOUtil.createCollisionFile(file);
			file.renameTo(collisionFile);
			if (file.exists())
				throw new IllegalStateException("Could not rename file to resolve collision: " + file);

			try {
				IOUtil.copyFile(collisionFile, file);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private boolean detectFileCollisionRecursively(LocalRepoTransaction transaction, EntityID fromRepositoryID, File fileOrDirectory) {
		assertNotNull("transaction", transaction);
		assertNotNull("fromRepositoryID", fromRepositoryID);
		assertNotNull("fileOrDirectory", fileOrDirectory);

		if (!fileOrDirectory.exists()) { // Is this correct? If it does not exist, then there is no collision? TODO what if it has been deleted locally and modified remotely and local is destination and that's our collision?!
			return false;
		}

		if (fileOrDirectory.isFile()) {
			RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), fileOrDirectory);
			if (!(repoFile instanceof NormalFile))
				return true; // We had a change after the last local sync (normal file => directory)!

			NormalFile normalFile = (NormalFile) repoFile;
			if (detectFileCollision(transaction, fromRepositoryID, fileOrDirectory, normalFile))
				return true;
			else
				return false;
		}

		File[] children = fileOrDirectory.listFiles();
		if (children == null)
			throw new IllegalStateException("listFiles() of directory returned null: " + fileOrDirectory);

		for (File child : children) {
			if (detectFileCollisionRecursively(transaction, fromRepositoryID, child))
				return true;
		}

		return false;
	}

	/**
	 * Detect if the file to be copied or deleted has been modified locally (or copied from another repository) after the last
	 * sync from the repository identified by {@code fromRepositoryID}.
	 * @param transaction
	 * @param fromRepositoryID
	 * @param file
	 * @param normalFile
	 * @return <code>true</code>, if there is a collision; <code>false</code>, if there is none.
	 */
	private boolean detectFileCollision(LocalRepoTransaction transaction, EntityID fromRepositoryID, File file, NormalFile normalFile) {
		assertNotNull("transaction", transaction);
		assertNotNull("fromRepositoryID", fromRepositoryID);
		assertNotNull("file", file);
		assertNotNull("normalFile", normalFile);

		RemoteRepository fromRemoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getObjectByIdOrFail(fromRepositoryID);
		long lastSyncFromRemoteRepositoryLocalRevision = fromRemoteRepository.getLocalRevision();
		if (normalFile.getLocalRevision() <= lastSyncFromRemoteRepositoryLocalRevision)
			return false;

		// The file was transferred from the same repository before and was thus not changed locally nor in another repo.
		// This can only happen, if the sync was interrupted (otherwise the check for the localRevision above
		// would have already caused this method to abort).
		if (fromRepositoryID.equals(normalFile.getLastSyncFromRepositoryID()))
			return false;

		return true;
	}

	@Override
	public void putFileData(String path, long offset, byte[] fileData) {
		path = prefixPath(path);
		File file = getFile(path);
		File localRoot = getLocalRepoManager().getLocalRoot();
		LocalRepoTransaction transaction = getLocalRepoManager().beginReadTransaction(); // It writes into the file system, but it only reads from the DB.
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

			try {
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				try {
					raf.seek(offset);
					raf.write(fileData);
				} finally {
					raf.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void endPutFile(String path, Date lastModified, long length) {
		path = prefixPath(path);
		assertNotNull("lastModified", lastModified);
		File file = getFile(path);
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {

			RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
			if (!(repoFile instanceof NormalFile)) {
				throw new IllegalStateException(String.format("RepoFile is not an instance of NormalFile! repoFile=%s file=%s",
						repoFile, file));
			}

			NormalFile normalFile = (NormalFile) repoFile;
			if (!normalFile.isInProgress())
				throw new IllegalStateException(String.format("NormalFile.inProgress == false! beginFile(...) not called?! repoFile=%s file=%s",
						repoFile, file));

			try {
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				try {
					raf.setLength(length);
				} finally {
					raf.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
			file.setLastModified(lastModified.getTime());
			localRepoSync.updateRepoFile(normalFile, file, new NullProgressMonitor());
			normalFile.setLastSyncFromRepositoryID(clientRepositoryID);
			normalFile.setInProgress(false);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void endSyncFromRepository() {
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			PersistenceManager pm = transaction.getPersistenceManager();
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);
			ModificationDAO modificationDAO = transaction.getDAO(ModificationDAO.class);

			RemoteRepository toRemoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(clientRepositoryID);

			LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepoOrFail(toRemoteRepository);
			if (lastSyncToRemoteRepo.getLocalRepositoryRevisionInProgress() < 0)
				throw new IllegalStateException(String.format("lastSyncToRemoteRepo.localRepositoryRevisionInProgress < 0 :: There is no sync in progress for the RemoteRepository with entityID=%s", clientRepositoryID));

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
		EntityID clientRepositoryID = getClientRepositoryIDOrFail();
		LocalRepoTransaction transaction = getLocalRepoManager().beginWriteTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(clientRepositoryID);
			remoteRepository.setRevision(fromLocalRevision);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}
}
