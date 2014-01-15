package co.codewizards.cloudstore.core.repo.transport.file;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSet;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunk;
import co.codewizards.cloudstore.core.dto.FileChunkSet;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.persistence.DeleteModification;
import co.codewizards.cloudstore.core.persistence.Directory;
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
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoSync;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.IOUtil;

public class FileRepoTransport extends AbstractRepoTransport {
	private static final Logger logger = LoggerFactory.getLogger(FileRepoTransport.class);

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
	public void requestRepoConnection(EntityID remoteRepositoryID, byte[] publicKey) {
		assertNotNull("remoteRepositoryID", remoteRepositoryID);
		assertNotNull("publicKey", publicKey);
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrNull(remoteRepositoryID);
			if (remoteRepository != null)
				throw new IllegalArgumentException("RemoteRepository already connected! remoteRepositoryID=" + remoteRepositoryID);

			RemoteRepositoryRequestDAO remoteRepositoryRequestDAO = transaction.getDAO(RemoteRepositoryRequestDAO.class);
			RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDAO.getRemoteRepositoryRequest(remoteRepositoryID);
			if (remoteRepositoryRequest != null) {
				logger.info("RemoteRepository already requested to be connected. remoteRepositoryID={}", remoteRepositoryID);
				remoteRepositoryRequest.setChanged(new Date()); // make sure it is not deleted soon (the request expires after a while)
				remoteRepositoryRequest.setPublicKey(publicKey);
			}
			else {
				remoteRepositoryRequest = new RemoteRepositoryRequest();
				remoteRepositoryRequest.setRepositoryID(remoteRepositoryID);
				remoteRepositoryRequest.setPublicKey(publicKey);
				remoteRepositoryRequestDAO.makePersistent(remoteRepositoryRequest);
			}

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public RepositoryDTO getRepositoryDTO() {
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
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
	public ChangeSet getChangeSet(EntityID toRepositoryID, boolean localSync) {
		assertNotNull("toRepositoryID", toRepositoryID);

		if (localSync)
			getLocalRepoManager().localSync(new LoggerProgressMonitor(logger));

		ChangeSet changeSet = new ChangeSet();
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
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
			changeSet.setRepositoryDTO(toRepositoryDTO(localRepository));

			RemoteRepository toRemoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(toRepositoryID);

			LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepo(toRemoteRepository);
			if (lastSyncToRemoteRepo == null) {
				lastSyncToRemoteRepo = new LastSyncToRemoteRepo();
				lastSyncToRemoteRepo.setRemoteRepository(toRemoteRepository);
				lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(-1);
			}
			lastSyncToRemoteRepo.setLocalRepositoryRevisionInProgress(localRepository.getRevision());
			lastSyncToRemoteRepoDAO.makePersistent(lastSyncToRemoteRepo);

			Collection<Modification> modifications = modificationDAO.getModificationsAfter(toRemoteRepository, lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
			changeSet.setModificationDTOs(toModificationDTOs(modifications));

			Collection<RepoFile> repoFiles = repoFileDAO.getRepoFilesChangedAfter(lastSyncToRemoteRepo.getLocalRepositoryRevisionSynced());
			Map<EntityID, RepoFileDTO> entityID2RepoFileDTO = getEntityID2RepoFileDTOWithParents(repoFiles, repoFileDAO);
			changeSet.setRepoFileDTOs(new ArrayList<RepoFileDTO>(entityID2RepoFileDTO.values()));

			transaction.commit();
			return changeSet;
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void makeDirectory(String path, Date lastModified) {
		File file = getFile(assertNotNull("path", path));
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
			mkDir(transaction, file, lastModified);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void delete(String path) {
		File file = getFile(assertNotNull("path", path));
		File parentFile = file.getParentFile();
		long parentFileLastModified = parentFile.exists() ? parentFile.lastModified() : Long.MIN_VALUE;
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
			if (!IOUtil.deleteDirectoryRecursively(file)) {
				throw new IllegalStateException("Deleting file or directory failed: " + file);
			}

			RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
			if (repoFile != null) {
				LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
				localRepoSync.deleteRepoFile(repoFile);
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();

			if (parentFileLastModified != Long.MIN_VALUE)
				parentFile.setLastModified(parentFileLastModified);
		}
	}

	@Override
	public FileChunkSet getFileChunkSet(String path) {
		assertNotNull("path", path);
		FileChunkSet response = new FileChunkSet();
		response.setPath(path);
		final int bufLength = 32 * 1024;
		final int chunkLength = 32 * bufLength; // 1 MiB chunk size
		File file = getFile(path);
		try {
			if (!file.isFile())
				response.setFileExists(false);
			else {
				response.setLastModified(new Date(file.lastModified()));

				MessageDigest mdAll = MessageDigest.getInstance(HashUtil.HASH_ALGORITHM_SHA);
				MessageDigest mdChunk = MessageDigest.getInstance(HashUtil.HASH_ALGORITHM_SHA);

				long offset = 0;
				InputStream in = new FileInputStream(file);
				try {
					FileChunk fileChunk = null;

					byte[] buf = new byte[bufLength];
					while (true) {
						if (fileChunk == null) {
							fileChunk = new FileChunk();
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
							response.getFileChunks().add(fileChunk);
							fileChunk = null;

							if (bytesRead < 0) {
								break;
							}
						}
					}
				} finally {
					in.close();
				}
				response.setSha1(HashUtil.encodeHexStr(mdAll.digest()));
				response.setLength(offset);
			}
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
		return response;
	}

	protected LocalRepoManager getLocalRepoManager() {
		if (localRepoManager == null) {
			File remoteRootFile;
			try {
				remoteRootFile = new File(getRemoteRoot().toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(remoteRootFile);
		}
		return localRepoManager;
	}

	private List<ModificationDTO> toModificationDTOs(Collection<Modification> modifications) {
		List<ModificationDTO> result = new ArrayList<ModificationDTO>(assertNotNull("modifications", modifications).size());
		for (Modification modification : modifications) {
			result.add(toModificationDTO(modification));
		}
		return result;
	}

	private ModificationDTO toModificationDTO(Modification modification) {
		ModificationDTO modificationDTO;
		if (modification instanceof DeleteModification) {
			DeleteModification deleteModification = (DeleteModification) modification;
			DeleteModificationDTO deleteModificationDTO;
			modificationDTO = deleteModificationDTO = new DeleteModificationDTO();
			deleteModificationDTO.setPath(deleteModification.getPath());
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

	private Map<EntityID, RepoFileDTO> getEntityID2RepoFileDTOWithParents(Collection<RepoFile> repoFiles, RepoFileDAO repoFileDAO) {
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

			while (rf != null) {
				if (!entityID2RepoFileDTO.containsKey(rf.getEntityID())) {
					entityID2RepoFileDTO.put(rf.getEntityID(), toRepoFileDTO(rf, repoFileDAO));
				}
				rf = rf.getParent();
			}
		}
		return entityID2RepoFileDTO;
	}

	private RepoFileDTO toRepoFileDTO(RepoFile repoFile, RepoFileDAO repoFileDAO) {
		assertNotNull("repoFileDAO", repoFileDAO);
		assertNotNull("repoFile", repoFile);
		RepoFileDTO repoFileDTO;
		if (repoFile instanceof NormalFile) {
			NormalFile normalFile = (NormalFile) repoFile;
			NormalFileDTO normalFileDTO;
			repoFileDTO = normalFileDTO = new NormalFileDTO();
			normalFileDTO.setLength(normalFile.getLength());
			normalFileDTO.setSha1(normalFile.getSha1());
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
				file.renameTo(new File(parentFile, String.format("%s.%s.collision", file.getName(), Long.toString(System.currentTimeMillis(), 36))));

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

	protected File getFile(String path) {
		path = assertNotNull("path", path).replace('/', File.separatorChar);
		File file = new File(getLocalRepoManager().getLocalRoot(), path);
		return file;
	}

	@Override
	public byte[] getFileData(String path, long offset, int length) {
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
		File file = getFile(path);
		File parentFile = file.getParentFile();
		long parentFileLastModified = parentFile.exists() ? parentFile.lastModified() : Long.MIN_VALUE;
		try {
			if (file.exists() && !file.isFile())
				file.renameTo(new File(parentFile, String.format("%s.%s.collision", file.getName(), Long.toString(System.currentTimeMillis(), 36))));

			if (!file.isFile()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			if (!file.isFile())
				throw new IllegalStateException("Could not create file (permissions?!): " + file);

			File localRoot = getLocalRepoManager().getLocalRoot();
			LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
			try {

				RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
				RepoFile parentRepoFile = repoFileDAO.getRepoFile(localRoot, file.getParentFile());
				RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);
				if (repoFile == null) {
					NormalFile normalFile;
					repoFile = normalFile = new NormalFile();
					normalFile.setName(file.getName());
					normalFile.setParent(parentRepoFile);
					normalFile.setLastModified(new Date(file.lastModified()));
					normalFile.setLength(file.length());
					normalFile.setSha1(sha(file));
					normalFile.setInProgress(true);
					repoFile = normalFile = repoFileDAO.makePersistent(normalFile);
				}
				else
					((NormalFile) repoFile).setInProgress(true);

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			if (parentFileLastModified != Long.MIN_VALUE)
				parentFile.setLastModified(parentFileLastModified);
		}
	}

	@Override
	public void putFileData(String path, long offset, byte[] fileData) {
		File file = getFile(path);
		File localRoot = getLocalRepoManager().getLocalRoot();
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
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
		File file = getFile(assertNotNull("path", path));
		assertNotNull("lastModified", lastModified);
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
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
			normalFile.setInProgress(false);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void endSyncFromRepository(EntityID toRepositoryID) {
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
			PersistenceManager pm = transaction.getPersistenceManager();
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);
			ModificationDAO modificationDAO = transaction.getDAO(ModificationDAO.class);

			RemoteRepository toRemoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(toRepositoryID);

			LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepoOrFail(toRemoteRepository);
			if (lastSyncToRemoteRepo.getLocalRepositoryRevisionInProgress() < 0)
				throw new IllegalStateException(String.format("lastSyncToRemoteRepo.localRepositoryRevisionInProgress < 0 :: There is no sync in progress for the RemoteRepository with entityID=%s", toRepositoryID));

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
	public void endSyncToRepository(EntityID fromRepositoryID, long fromLocalRevision) {
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(fromRepositoryID);
			remoteRepository.setRevision(fromLocalRevision);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
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
			return HashUtil.encodeHexStr(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
