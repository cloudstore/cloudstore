package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTOTreeNode;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.core.persistence.LastSyncToRemoteRepoDAO;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

/**
 * Logic for synchronising a local with a remote repository.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class RepoSync {
	private static final Logger logger = LoggerFactory.getLogger(RepoSync.class);

	private final URL remoteRoot;
	private final LocalRepoManager localRepoManager;
	private final RepoTransport localRepoTransport;
	private final RepoTransport remoteRepoTransport;
	private final EntityID localRepositoryID;
	private EntityID remoteRepositoryID;

	public RepoSync(File localRoot, URL remoteRoot) {
		this.remoteRoot = assertNotNull("remoteRoot", remoteRoot);
		localRepoManager = LocalRepoManagerFactory.getInstance().createLocalRepoManagerForExistingRepository(assertNotNull("localRoot", localRoot));
		localRepositoryID = localRepoManager.getLocalRepositoryID();
		localRepoTransport = createRepoTransport(localRoot);
		remoteRepoTransport = createRepoTransport(remoteRoot);
	}

	public void sync(ProgressMonitor monitor) {
		assertNotNull("monitor", monitor);
		monitor.beginTask("Synchronising remotely...", 101);
		try {
			readRemoteRepositoryID();
			monitor.worked(1);
			sync(remoteRepoTransport, localRepoTransport, new SubProgressMonitor(monitor, 50));
			sync(localRepoTransport, remoteRepoTransport, new SubProgressMonitor(monitor, 50));
		} finally {
			monitor.done();
		}
	}

	private void readRemoteRepositoryID() {
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			RemoteRepository remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getRemoteRepositoryOrFail(remoteRoot);
			remoteRepositoryID = remoteRepository.getEntityID();

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private RepoTransport createRepoTransport(File rootFile) {
		URL rootURL;
		try {
			rootURL = rootFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return createRepoTransport(rootURL);
	}

	private RepoTransport createRepoTransport(URL remoteRoot) {
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(remoteRoot);
		return repoTransportFactory.createRepoTransport(remoteRoot);
	}

	private void sync(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising remotely...", 100);
		try {
			ChangeSetRequest changeSetRequest = createChangeSetRequest(fromRepoTransport, toRepoTransport);
			monitor.worked(1);

			ChangeSetResponse changeSetResponse = fromRepoTransport.getChangeSet(changeSetRequest);
			monitor.worked(8);

			sync(fromRepoTransport, toRepoTransport, changeSetResponse, new SubProgressMonitor(monitor, 90));

			storeRepositoryDTOFromChangeSetResponse(changeSetResponse.getRepositoryDTO());
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}

	private void sync(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, ChangeSetResponse changeSetResponse, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising remotely...", changeSetResponse.getModificationDTOs().size() + changeSetResponse.getRepoFileDTOs().size());
		try {
			for (ModificationDTO modificationDTO : changeSetResponse.getModificationDTOs()) {
				syncModification(fromRepoTransport, toRepoTransport, modificationDTO, new SubProgressMonitor(monitor, 1));
			}

			RepoFileDTOTreeNode repoFileDTOTree = RepoFileDTOTreeNode.createTree(changeSetResponse.getRepoFileDTOs());
			for (RepoFileDTOTreeNode repoFileDTOTreeNode : repoFileDTOTree) {
				RepoFileDTO repoFileDTO = repoFileDTOTreeNode.getRepoFileDTO();
				if (repoFileDTO instanceof DirectoryDTO)
					syncDirectory(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, (DirectoryDTO) repoFileDTO, new SubProgressMonitor(monitor, 1));
				else if (repoFileDTO instanceof RepoFileDTO)
					syncFile(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, repoFileDTO, new SubProgressMonitor(monitor, 1));
				else
					throw new IllegalStateException("Unsupported RepoFileDTO type: " + repoFileDTO);
			}
		} finally {
			monitor.done();
		}
	}

	private void syncModification(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, ModificationDTO modificationDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising remotely...", 100);
		try {
			if (modificationDTO instanceof DeleteModificationDTO) {
				DeleteModificationDTO deleteModificationDTO = (DeleteModificationDTO) modificationDTO;
				toRepoTransport.delete(deleteModificationDTO.getPath());
			}
			else
				throw new IllegalStateException("Unknown modificationDTO type: " + modificationDTO);
		} finally {
			monitor.done();
		}
	}

	private void syncDirectory(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, RepoFileDTOTreeNode repoFileDTOTreeNode, DirectoryDTO directoryDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising remotely...", 100);
		try {
			// TODO maybe only one request?!
			toRepoTransport.makeDirectory(repoFileDTOTreeNode.getPath());
			toRepoTransport.setLastModified(repoFileDTOTreeNode.getPath(), directoryDTO.getLastModified());
		} finally {
			monitor.done();
		}
	}

	private void syncFile(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, RepoFileDTOTreeNode repoFileDTOTreeNode, RepoFileDTO normalFileDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising remotely...", 100);
		try {
			// TODO the check-sums should be obtained simultaneously with 2 threads.
			FileChunkSetResponse fromFileChunkSetResponse = fromRepoTransport.getFileChunkSet(createFileChunkSetRequest(repoFileDTOTreeNode));
			if (!assertNotNull("fromFileChunkSetResponse", fromFileChunkSetResponse).isFileExists()) {
				logger.warn("File was deleted during sync on source side: {}", repoFileDTOTreeNode.getPath());
				return;
			}
			assertNotNull("fromFileChunkSetResponse.lastModified", fromFileChunkSetResponse.getLastModified());

			FileChunkSetResponse toFileChunkSetResponse = toRepoTransport.getFileChunkSet(createFileChunkSetRequest(repoFileDTOTreeNode));
			if (areFilesExistingAndEqual(fromFileChunkSetResponse, assertNotNull("toFileChunkSetResponse", toFileChunkSetResponse))) {
				logger.info("File is already equal on destination side: {}", repoFileDTOTreeNode.getPath());
				return;
			}

			List<FileChunk> fromFileChunksDirty = new ArrayList<FileChunk>();
			Iterator<FileChunk> toFileChunkIterator = toFileChunkSetResponse.getFileChunks().iterator();
			int fileChunkIndex = -1;
			for (FileChunk fromFileChunk : fromFileChunkSetResponse.getFileChunks()) {
				FileChunk toFileChunk = toFileChunkIterator.hasNext() ? toFileChunkIterator.next() : null;
				++fileChunkIndex;
				if (toFileChunk != null
						&& equal(fromFileChunk.getLength(), toFileChunk.getLength())
						&& equal(fromFileChunk.getSha1(), toFileChunk.getSha1())) {
					logger.debug("Skipping FileChunk {} (already equal on destination side). File: {}", fileChunkIndex, repoFileDTOTreeNode.getPath());
					continue;
				}
				fromFileChunksDirty.add(fromFileChunk);
			}

			toRepoTransport.createFile(repoFileDTOTreeNode.getPath());

			for (FileChunk fileChunk : fromFileChunksDirty) {
				byte[] fileData = fromRepoTransport.getFileData(repoFileDTOTreeNode.getPath(), fileChunk.getOffset(), fileChunk.getLength());

				if (fileData == null || fileData.length != fileChunk.getLength()) {
					logger.warn("Source file was modified or deleted during sync: {}", repoFileDTOTreeNode.getPath());
					// The file is left in state 'inProgress'. Thus it should definitely not be synced back in the opposite
					// direction. The file should be synced again in the correct direction in the next run (after the source
					// repo did a local sync, too).
					return;
				}

				toRepoTransport.putFileData(repoFileDTOTreeNode.getPath(), fileChunk.getOffset(), fileData);
			}

			toRepoTransport.setLastModified(repoFileDTOTreeNode.getPath(), fromFileChunkSetResponse.getLastModified());
		} finally {
			monitor.done();
		}
	}

	private boolean areFilesExistingAndEqual(FileChunkSetResponse fromFileChunkSetResponse, FileChunkSetResponse toFileChunkSetResponse) {
		return (fromFileChunkSetResponse.isFileExists()
				&& toFileChunkSetResponse.isFileExists()
				&& equal(fromFileChunkSetResponse.getLength(), toFileChunkSetResponse.getLength())
				&& equal(fromFileChunkSetResponse.getLastModified(), toFileChunkSetResponse.getLastModified())
				&& equal(fromFileChunkSetResponse.getSha1(), toFileChunkSetResponse.getSha1()));
	}

	private FileChunkSetRequest createFileChunkSetRequest(RepoFileDTOTreeNode repoFileDTOTreeNode) {
		FileChunkSetRequest request = new FileChunkSetRequest();
		request.setPath(repoFileDTOTreeNode.getPath());
		return request;
	}

	private ChangeSetRequest createChangeSetRequest(RepoTransport fromRepoTransport, RepoTransport toRepoTransport) {
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			ChangeSetRequest changeSetRequest = new ChangeSetRequest();
			RemoteRepository remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getObjectByIdOrFail(remoteRepositoryID);

			if (localRepoTransport == fromRepoTransport && remoteRepoTransport == toRepoTransport) {
				// UPloading (changeSetRequest is sent to the *local* repo)
				changeSetRequest.setClientRepositoryID(remoteRepositoryID);

				LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);
				LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepo(remoteRepository);

				if (lastSyncToRemoteRepo == null)
					changeSetRequest.setServerRevision(-1);
				else
					changeSetRequest.setServerRevision(lastSyncToRemoteRepo.getLocalRepositoryRevision());

			} else if (localRepoTransport == toRepoTransport && remoteRepoTransport == fromRepoTransport) {
				// DOWNloading (changeSetRequest is sent to the *remote* repo)
				changeSetRequest.setClientRepositoryID(localRepositoryID);
				changeSetRequest.setServerRevision(remoteRepository.getRevision());
			}
			else
				throw new IllegalStateException("fromRepoTransport and toRepoTransport do not match localRepoTransport and remoteRepoTransport!");

			transaction.commit();
			return changeSetRequest;
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void storeRepositoryDTOFromChangeSetResponse(RepositoryDTO repositoryDTO) {
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			RemoteRepository remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getObjectByIdOrFail(remoteRepositoryID);

			if (localRepositoryID.equals(repositoryDTO.getEntityID())) {
				// UPloading (changeSetResponse came from the *local* repo)
				LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);
				LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepo(remoteRepository);

				if (lastSyncToRemoteRepo == null) {
					lastSyncToRemoteRepo = new LastSyncToRemoteRepo();
					lastSyncToRemoteRepo.setRemoteRepository(remoteRepository);
				}
				lastSyncToRemoteRepo.setLocalRepositoryRevision(repositoryDTO.getRevision());

				lastSyncToRemoteRepoDAO.makePersistent(lastSyncToRemoteRepo); // doesn't do anything, if it was already persistent ;-)
			} else if (remoteRepositoryID.equals(repositoryDTO.getEntityID())) {
				// DOWNloading (changeSetResponse came from the *remote* repo)
				remoteRepository.setRevision(repositoryDTO.getRevision());
			}
			else
				throw new IllegalStateException("fromRepoTransport and toRepoTransport do not match localRepoTransport and remoteRepoTransport!");

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	public void close() {
		localRepoManager.close();
		localRepoTransport.close();
		remoteRepoTransport.close();
	}
}
