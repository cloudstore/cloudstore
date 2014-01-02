package co.codewizards.cloudstore.shared.repo.sync;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import co.codewizards.cloudstore.shared.dto.ChangeSetRequest;
import co.codewizards.cloudstore.shared.dto.ChangeSetResponse;
import co.codewizards.cloudstore.shared.dto.EntityID;
import co.codewizards.cloudstore.shared.dto.RepoFileDTO;
import co.codewizards.cloudstore.shared.dto.RepositoryDTO;
import co.codewizards.cloudstore.shared.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.shared.persistence.LastSyncToRemoteRepoDAO;
import co.codewizards.cloudstore.shared.persistence.RemoteRepository;
import co.codewizards.cloudstore.shared.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.shared.progress.ProgressMonitor;
import co.codewizards.cloudstore.shared.progress.SubProgressMonitor;
import co.codewizards.cloudstore.shared.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.shared.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.shared.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransport;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactoryRegistry;

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
		monitor.beginTask("Synchronising...", 101);
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
			RemoteRepository remoteRepository = transaction.createDAO(RemoteRepositoryDAO.class).getRemoteRepositoryOrFail(remoteRoot);
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
		monitor.beginTask("Synchronising...", 100);
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
		monitor.beginTask("Synchronising...", changeSetResponse.getRepoFileDTOs().size());
		try {
			for (RepoFileDTO repoFileDTO : changeSetResponse.getRepoFileDTOs()) {


				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	private ChangeSetRequest createChangeSetRequest(RepoTransport fromRepoTransport, RepoTransport toRepoTransport) {
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			ChangeSetRequest changeSetRequest = new ChangeSetRequest();
			RemoteRepository remoteRepository = transaction.createDAO(RemoteRepositoryDAO.class).getObjectByIdOrFail(remoteRepositoryID);

			if (localRepoTransport == fromRepoTransport && remoteRepoTransport == toRepoTransport) {
				// UPloading (changeSetRequest is sent to the *local* repo)
				LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.createDAO(LastSyncToRemoteRepoDAO.class);
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
			RemoteRepository remoteRepository = transaction.createDAO(RemoteRepositoryDAO.class).getObjectByIdOrFail(remoteRepositoryID);

			if (localRepositoryID.equals(repositoryDTO.getEntityID())) {
				// UPloading (changeSetResponse came from the *local* repo)
				LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.createDAO(LastSyncToRemoteRepoDAO.class);
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
