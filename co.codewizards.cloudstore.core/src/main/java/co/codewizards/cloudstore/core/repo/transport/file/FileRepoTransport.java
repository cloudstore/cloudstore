package co.codewizards.cloudstore.core.repo.transport.file;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.codewizards.cloudstore.core.dto.ChangeSetRequest;
import co.codewizards.cloudstore.core.dto.ChangeSetResponse;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.dto.StringList;
import co.codewizards.cloudstore.core.persistence.Directory;
import co.codewizards.cloudstore.core.persistence.LocalRepository;
import co.codewizards.cloudstore.core.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.NormalFile;
import co.codewizards.cloudstore.core.persistence.RepoFile;
import co.codewizards.cloudstore.core.persistence.RepoFileDAO;
import co.codewizards.cloudstore.core.repo.local.FilenameFilterSkipMetaDir;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransport;
import co.codewizards.cloudstore.core.util.HashUtil;
import co.codewizards.cloudstore.core.util.IOUtil;

public class FileRepoTransport extends AbstractRepoTransport {

	private LocalRepoManager localRepoManager;

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

	@Override
	public ChangeSetResponse getChangeSet(ChangeSetRequest changeSetRequest) {
		assertNotNull("changeSetRequest", changeSetRequest);
		ChangeSetResponse changeSetResponse = new ChangeSetResponse();
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
			LocalRepositoryDAO localRepositoryDAO = transaction.getDAO(LocalRepositoryDAO.class);
			RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);

			// We must *first* read the LocalRepository and afterwards all changes, because this way, we don't need to lock it in the DB.
			// If we *then* read RepoFiles with a newer localRevision, it doesn't do any harm - we'll simply read them again, in the
			// next run.
			changeSetResponse.setRepositoryDTO(toRepositoryDTO(localRepositoryDAO.getLocalRepositoryOrFail()));

			Collection<RepoFile> repoFiles = repoFileDAO.getRepoFilesChangedAfter(changeSetRequest.getRevision());
			Map<EntityID, RepoFileDTO> entityID2RepoFileDTO = getEntityID2RepoFileDTOWithParents(repoFiles, repoFileDAO, changeSetRequest.getRevision());
			changeSetResponse.setRepoFileDTOs(new ArrayList<RepoFileDTO>(entityID2RepoFileDTO.values()));

			transaction.commit();
			return changeSetResponse;
		} finally {
			transaction.rollbackIfActive();
		}
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
			normalFileDTO.setLastModified(normalFile.getLastModified());
			normalFileDTO.setLength(normalFile.getLength());
			normalFileDTO.setSha1(normalFile.getSha1());
		}
		else if (repoFile instanceof Directory) {
			DirectoryDTO directoryDTO;
			repoFileDTO = directoryDTO = new DirectoryDTO();

			if (repoFile.getLocalRevision() <= localRevision)
				directoryDTO.setChildNamesLoaded(false);
			else {
				directoryDTO.setChildNamesLoaded(true);
				Collection<RepoFile> childRepoFiles = repoFileDAO.getChildRepoFiles(repoFile);
				List<String> childNames = directoryDTO.getChildNames();
				for (RepoFile childRepoFile : childRepoFiles) {
					childNames.add(childRepoFile.getName());
				}
			}
		}
		else // TODO support symlinks!
			throw new UnsupportedOperationException("RepoFile type not yet supported: " + repoFile);

		repoFileDTO.setEntityID(repoFile.getEntityID());
		repoFileDTO.setLocalRevision(repoFile.getLocalRevision());
		repoFileDTO.setName(repoFile.getName());
		repoFileDTO.setParentEntityID(repoFile.getParent() == null ? null : repoFile.getParent().getEntityID());

		return repoFileDTO;
	}

	@Override
	public void close() {
		if (localRepoManager != null)
			localRepoManager.close();
	}

	@Override
	public void makeDirectory(String path, StringList childNamesToKeep) {
		File file = getFile(assertNotNull("path", path));
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
//			transaction.setAutoTrackLifecycleListenerEnabled(false);
			mkDir(transaction, file);
			if (childNamesToKeep != null) {
				deleteChildrenExcept(transaction, file, new HashSet<String>(childNamesToKeep.getElements()));
			}
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void deleteChildrenExcept(LocalRepoTransaction transaction, File directory, Set<String> fileNamesToKeep) {
		assertNotNull("transaction", transaction);
		assertNotNull("directory", directory);
		assertNotNull("fileNamesToKeep", fileNamesToKeep);
		String[] fileNames = directory.list(new FilenameFilterSkipMetaDir());
		if (fileNames != null) {
			RepoFileDAO repoFileDAO = transaction.getDAO(RepoFileDAO.class);
			File localRoot = getLocalRepoManager().getLocalRoot();
			for (String fileName : fileNames) {
				if (fileNamesToKeep.contains(fileName))
					continue;

				File file = new File(directory, fileName);
				RepoFile repoFile = repoFileDAO.getRepoFile(localRoot, file);

				if (!IOUtil.deleteDirectoryRecursively(file))
					throw new IllegalStateException("Deleting file/directory failed: " + file);

				if (repoFile != null)
					deleteRepoFileWithAllChildrenRecursively(repoFileDAO, repoFile);
			}
		}
	}

	private void deleteRepoFileWithAllChildrenRecursively(RepoFileDAO repoFileDAO, RepoFile repoFile) {
		for (RepoFile childRepoFile : repoFileDAO.getChildRepoFiles(repoFile)) {
			deleteRepoFileWithAllChildrenRecursively(repoFileDAO, childRepoFile);
		}
		repoFileDAO.deletePersistent(repoFile);
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

		file.mkdir();
		if (!file.isDirectory())
			throw new IllegalStateException("Could not create directory: " + file);

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
			repoFile = directory = transaction.getDAO(RepoFileDAO.class).makePersistent(directory);
		}
	}

	protected File getFile(String path) {
		path = assertNotNull("path", path).replace('/', File.separatorChar);
		File file = new File(getLocalRepoManager().getLocalRoot(), path);
		return file;
	}

	@Override
	public byte[] getFileData(String path) {
		File file = getFile(path);
		try {
			return IOUtil.getBytesFromFile(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void putFileData(String path, byte[] fileData) {
		File localRoot = getLocalRepoManager().getLocalRoot();
		LocalRepoTransaction transaction = getLocalRepoManager().beginTransaction();
		try {
//			transaction.setAutoTrackLifecycleListenerEnabled(false);

			File file = getFile(path);
			try {
				FileOutputStream out = new FileOutputStream(file);
				out.write(fileData);
				out.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

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
				repoFile = normalFile = repoFileDAO.makePersistent(normalFile);
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
			return HashUtil.encodeHexStr(hash, 0, hash.length);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
