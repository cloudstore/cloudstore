package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import co.codewizards.cloudstore.core.dto.ChangeSetRequest;
import co.codewizards.cloudstore.core.dto.ChangeSetResponse;
import co.codewizards.cloudstore.core.dto.DirectoryDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.NormalFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTOTreeNode;
import co.codewizards.cloudstore.core.dto.RepositoryDTO;
import co.codewizards.cloudstore.core.dto.StringList;
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
		monitor.beginTask("Synchronising remotely...", changeSetResponse.getRepoFileDTOs().size());
		try {
			RepoFileDTOTreeNode repoFileDTOTree = RepoFileDTOTreeNode.createTree(changeSetResponse.getRepoFileDTOs());
			for (RepoFileDTOTreeNode repoFileDTOTreeNode : repoFileDTOTree) {
				RepoFileDTO repoFileDTO = repoFileDTOTreeNode.getRepoFileDTO();
				if (repoFileDTO instanceof DirectoryDTO)
					sync(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, (DirectoryDTO) repoFileDTO, new SubProgressMonitor(monitor, 1));
				else if (repoFileDTO instanceof NormalFileDTO)
					sync(fromRepoTransport, toRepoTransport, repoFileDTOTreeNode, (NormalFileDTO) repoFileDTO, new SubProgressMonitor(monitor, 1));
				else
					throw new IllegalStateException("Unsupported RepoFileDTO type: " + repoFileDTO);
			}
		} finally {
			monitor.done();
		}
	}

	private void sync(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, RepoFileDTOTreeNode repoFileDTOTreeNode, DirectoryDTO directoryDTO, ProgressMonitor monitor) {
		// TODO sync last-modified-timestamp, too!!!
		monitor.beginTask("Synchronising remotely...", 100);
		try {
			StringList childNamesToKeep = null;
			if (directoryDTO.isChildNamesLoaded()) {
				// All children not contained in childNamesToKeep are deleted!
				childNamesToKeep = new StringList();
				childNamesToKeep.setElements(directoryDTO.getChildNames());
			}
			toRepoTransport.makeDirectory(repoFileDTOTreeNode.getPath(), childNamesToKeep);
		} finally {
			monitor.done();
		}
	}

	private void sync(RepoTransport fromRepoTransport, RepoTransport toRepoTransport, RepoFileDTOTreeNode repoFileDTOTreeNode, NormalFileDTO normalFileDTO, ProgressMonitor monitor) {
		monitor.beginTask("Synchronising remotely...", 100);
		try {
			// TODO sync file in a chunked, efficient way!
			// TODO sync last-modified-timestamp, too!!!
			byte[] fileData = fromRepoTransport.getFileData(repoFileDTOTreeNode.getPath());
			toRepoTransport.putFileData(repoFileDTOTreeNode.getPath(), fileData);
		} finally {
			monitor.done();
		}
	}

	private ChangeSetRequest createChangeSetRequest(RepoTransport fromRepoTransport, RepoTransport toRepoTransport) {
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			ChangeSetRequest changeSetRequest = new ChangeSetRequest();
			RemoteRepository remoteRepository = transaction.getDAO(RemoteRepositoryDAO.class).getObjectByIdOrFail(remoteRepositoryID);

			if (localRepoTransport == fromRepoTransport && remoteRepoTransport == toRepoTransport) {
				// UPloading (changeSetRequest is sent to the *local* repo)
				LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);
				LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepo(remoteRepository);

				if (lastSyncToRemoteRepo == null)
					changeSetRequest.setRevision(-1);
				else
					changeSetRequest.setRevision(lastSyncToRemoteRepo.getLocalRepositoryRevision());

			} else if (localRepoTransport == toRepoTransport && remoteRepoTransport == fromRepoTransport) {
				// DOWNloading (changeSetRequest is sent to the *remote* repo)
				changeSetRequest.setRevision(remoteRepository.getRevision());
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
