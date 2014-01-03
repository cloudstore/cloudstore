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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetRequest;
import co.codewizards.cloudstore.core.dto.ChangeSetResponse;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.FileChunk;
import co.codewizards.cloudstore.core.dto.FileChunkSetRequest;
import co.codewizards.cloudstore.core.dto.FileChunkSetResponse;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.persistence.DeleteModification;
import co.codewizards.cloudstore.core.persistence.Directory;
import co.codewizards.cloudstore.core.persistence.LocalRepository;
import co.codewizards.cloudstore.core.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.Modification;
import co.codewizards.cloudstore.core.persistence.ModificationDAO;
import co.codewizards.cloudstore.core.persistence.NormalFile;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.RepoFile;
import co.codewizards.cloudstore.core.persistence.RepoFileDAO;
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
	public ChangeSetResponse getChangeSet(ChangeSetRequest changeSetRequest) {
		assertNotNull("changeSetRequest", changeSetRequest);
		assertNotNull("changeSetRequest.clientRepositoryID", changeSetRequest.getClientRepositoryID());
		ChangeSetResponse changeSetResponse = new ChangeSetResponse();
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
			LocalRepositoryDAO localRepositoryDAO = transaction.getDAO(LocalRepositoryDAO.class);
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			ModificationDAO modificationDAO = transaction.getDAO(ModificationDAO.class);
			RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);

			// We must *first* read the LocalRepository and afterwards all changes, because this way, we don't need to lock it in the DB.
			// If we *then* read RepoFiles with a newer localRevision, it doesn't do any harm - we'll simply read them again, in the
			// next run.
			changeSetResponse.setRepositoryDTO(toRepositoryDTO(localRepositoryDAO.getLocalRepositoryOrFail()));

			RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(changeSetRequest.getClientRepositoryID());
			Collection<Modification> modifications = modificationDAO.getModificationsAfter(remoteRepository, changeSetRequest.getServerRevision());
			changeSetResponse.setModificationDTOs(toModificationDTOs(modifications));

			Collection<RepoFile> repoFiles = repoFileDAO.getRepoFilesChangedAfter(changeSetRequest.getServerRevision());
			Map<EntityID, RepoFileDTO> entityID2RepoFileDTO = getEntityID2RepoFileDTOWithParents(repoFiles, repoFileDAO, changeSetRequest.getServerRevision());
			changeSetResponse.setRepoFileDTOs(new ArrayList<RepoFileDTO>(entityID2RepoFileDTO.values()));

			transaction.commit();
			return changeSetResponse;
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void makeDirectory(String path) {
		File file = getFile(assertNotNull("path", path));
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
			mkDir(transaction, file);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void delete(String path) {
		File file = getFile(assertNotNull("path", path));
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
		}
	}

	@Override
	public FileChunkSetResponse getFileChunkSet(FileChunkSetRequest fileChunkSetRequest) {
		assertNotNull("fileChunkSetRequest", fileChunkSetRequest);
		assertNotNull("fileChunkSetRequest.path", fileChunkSetRequest.getPath());
		FileChunkSetResponse response = new FileChunkSetResponse();
		response.setPath(fileChunkSetRequest.getPath());
		final int bufLength = 32 * 1024;
		final int chunkLength = 32 * bufLength; // 1 MiB chunk size
		File file = getFile(fileChunkSetRequest.getPath());
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

	@Override
	public void setLastModified(String path, Date lastModified) {
		File file = getFile(assertNotNull("path", path));
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {

			RepoFile repoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(getLocalRepoManager().getLocalRoot(), file);
			if (repoFile instanceof NormalFile) {
				NormalFile normalFile = (NormalFile) repoFile;
				if (!normalFile.isInProgress()
						&& normalFile.getLength() == file.length()
						&& normalFile.getLastModified().getTime() == file.lastModified()) {
					file.setLastModified(lastModified.getTime());
					normalFile.setLastModified(lastModified);
				}
				else {
					// The file was modified; it is not in sync with the repo's DB. Hence we must resync it to make
					// sure the SHA1 is correct. We cannot simply set the timestamp because this might cause the
					// file to never be synced again (and the stale SHA1 to linger forever).
					LocalRepoSync localRepoSync = new LocalRepoSync(transaction);
					file.setLastModified(lastModified.getTime());
					localRepoSync.updateRepoFile(normalFile, file);
					normalFile.setInProgress(false);
				}
			}
			else if (repoFile instanceof Directory) {
				file.setLastModified(lastModified.getTime());
				repoFile.setLastModified(lastModified);
			}
//			else if (repoFile instanceof Symlink) {
//				// If a Symlink has a lastModified, we can set it. This is a separate timestamp from the real file's timestamp. But currently it does not have one anyway, yet.
//			}
			else
				throw new IllegalStateException("Unknown repoFile type: " + repoFile);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
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
		return repositoryDTO;
	}

	private Map<EntityID, RepoFileDTO> getEntityID2RepoFileDTOWithParents(Collection<RepoFile> repoFiles, RepoFileDAO repoFileDAO, long localRevision) {
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
					entityID2RepoFileDTO.put(rf.getEntityID(), toRepoFileDTO(rf, repoFileDAO, localRevision));
				}
				rf = rf.getParent();
			}
		}
		return entityID2RepoFileDTO;
	}

	private RepoFileDTO toRepoFileDTO(RepoFile repoFile, RepoFileDAO repoFileDAO, long localRevision) {
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

	protected void mkDir(LocalRepoTransaction transaction, File file) {
		File localRoot = getLocalRepoManager().getLocalRoot();
		if (localRoot.equals(file)) {
			return;
		}

		File parentFile = file.getParentFile();
		RepoFile parentRepoFile = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, parentFile);

		if (!localRoot.equals(parentFile) && (!parentFile.isDirectory() || parentRepoFile == null))
			mkDir(transaction, parentFile);

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
			transaction.getDAO(RepoFileDAO.class).deletePersistent(repoFile);
			repoFile = null;
		}

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
	public void createFile(String path) {
		File file = getFile(path);
		File parentFile = file.getParentFile();
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

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
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
				throw new IllegalStateException("NormalFile.inProgress == false for file: " + file);

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
