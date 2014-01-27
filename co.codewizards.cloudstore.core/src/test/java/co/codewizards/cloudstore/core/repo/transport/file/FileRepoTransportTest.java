package co.codewizards.cloudstore.core.repo.transport.file;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.AbstractTest;
import co.codewizards.cloudstore.core.dto.ChangeSetDTO;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.dto.ModificationDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.RepoFileDTOTreeNode;
import co.codewizards.cloudstore.core.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.core.persistence.LastSyncToRemoteRepoDAO;
import co.codewizards.cloudstore.core.persistence.LocalRepository;
import co.codewizards.cloudstore.core.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

public class FileRepoTransportTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(FileRepoTransportTest.class);

	private File remoteRoot;
	private EntityID remoteRepositoryID;
	private File localRoot;
	private EntityID localRepositoryID;
	private ChangeSetDTO changeSetResponse1;

	@Test
	public void getChangeSetForEntireRepository() throws Exception {
		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManager).isNotNull();
		remoteRepositoryID = localRepoManager.getRepositoryID();

		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		LocalRepoManager toLocalRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		localRepoManager.putRemoteRepository(toLocalRepoManager.getRepositoryID(), null, toLocalRepoManager.getPublicKey(), "");
		toLocalRepoManager.putRemoteRepository(localRepoManager.getRepositoryID(), null, localRepoManager.getPublicKey(), "");
		localRepositoryID = toLocalRepoManager.getRepositoryID();
		toLocalRepoManager.close();

		File child_1 = createDirectory(remoteRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		File child_2 = createDirectory(remoteRoot, "2");

		createFileWithRandomContent(child_2, "a");

		File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		File child_3 = createDirectory(remoteRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL, localRepositoryID);

		changeSetResponse1 = repoTransport.getChangeSet(false);
		assertThat(changeSetResponse1).isNotNull();
		assertThat(changeSetResponse1.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse1.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse1.getRepositoryDTO().getEntityID()).isNotNull();

		// changeSetResponse1 should contain the entire repository - including the root -, because really
		// every localRevision must be > -1.
		assertThat(changeSetResponse1.getRepoFileDTOs()).hasSize(15);

		Set<String> paths = getPaths(changeSetResponse1.getRepoFileDTOs());
		assertThat(paths).containsOnly("/1/a", "/1/b", "/1/c", "/2/a", "/2/1/a", "/2/1/b", "/3/a", "/3/b", "/3/c", "/3/d");

		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			LocalRepositoryDAO localRepositoryDAO = transaction.getDAO(LocalRepositoryDAO.class);
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			LastSyncToRemoteRepoDAO lastSyncToRemoteRepoDAO = transaction.getDAO(LastSyncToRemoteRepoDAO.class);
			LocalRepository localRepository = localRepositoryDAO.getLocalRepositoryOrFail();
			RemoteRepository toRemoteRepository = remoteRepositoryDAO.getObjectByIdOrFail(localRepositoryID);
			LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDAO.getLastSyncToRemoteRepoOrFail(toRemoteRepository);
			lastSyncToRemoteRepo.setRemoteRepository(toRemoteRepository);
			lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(localRepository.getRevision());
			lastSyncToRemoteRepoDAO.makePersistent(lastSyncToRemoteRepo);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForAddedFile() throws Exception {
		getChangeSetForEntireRepository();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		File child_2 = new File(remoteRoot, "2");
		File child_2_1 = new File(child_2, "1");
		createFileWithRandomContent(child_2_1, "c");

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL, localRepositoryID);

		ChangeSetDTO changeSetResponse2 = repoTransport.getChangeSet(false);
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDTO().getEntityID()).isNotNull();

		// We expect the added file and its direct parent, because they are both modified (new localRevision).
		// Additionally, we expect all parent-directories (recursively) until (including) the root, because they
		// are required to have a complete relative path for each modified RepoFile.
		assertThat(changeSetResponse2.getRepoFileDTOs()).hasSize(4);

		Set<String> paths = getPaths(changeSetResponse2.getRepoFileDTOs());
		assertThat(paths).hasSize(1);
		assertThat(paths.iterator().next()).isEqualTo("/2/1/c");

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForModifiedFile() throws Exception {
		getChangeSetForEntireRepository();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		File child_2 = new File(remoteRoot, "2");
		File child_2_1 = new File(child_2, "1");
		File child_2_1_b = new File(child_2_1, "b");
		FileOutputStream out = new FileOutputStream(child_2_1_b, true);
		out.write(4);
		out.close();

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL, localRepositoryID);

		ChangeSetDTO changeSetResponse2 = repoTransport.getChangeSet(false);
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDTOs()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDTO().getEntityID()).isNotNull();

		// We expect the changed file and all parent-directories (recursively) until (including) the
		// root, because they are required to have a complete relative path for each modified RepoFile.
		assertThat(changeSetResponse2.getRepoFileDTOs()).hasSize(4);

		Set<String> paths = getPaths(changeSetResponse2.getRepoFileDTOs());
		assertThat(paths).hasSize(1);
		assertThat(paths.iterator().next()).isEqualTo("/2/1/b");

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForDeletedFile() throws Exception {
		getChangeSetForEntireRepository();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		File child_2 = new File(remoteRoot, "2");
		File child_2_1 = new File(child_2, "1");
		File child_2_1_b = new File(child_2_1, "b");

		long child_2_1LastModifiedBeforeModification = child_2_1.lastModified();

		deleteFile(child_2_1_b);

		// In GNU/Linux, the parent-directory's last-modified timestamp is changed, if a child is added or removed.
		// To make sure, this has no influence on our test, we reset this timestamp after our change.
		child_2_1.setLastModified(child_2_1LastModifiedBeforeModification);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL, localRepositoryID);

		ChangeSetDTO changeSetResponse2 = repoTransport.getChangeSet(false);
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDTOs()).isNotNull().isEmpty();
		assertThat(changeSetResponse2.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDTO().getEntityID()).isNotNull();

		// We expect the DeleteModificationDTO
		assertThat(changeSetResponse2.getModificationDTOs()).hasSize(1);

		ModificationDTO modificationDTO = changeSetResponse2.getModificationDTOs().get(0);
		assertThat(modificationDTO).isNotNull().isInstanceOf(DeleteModificationDTO.class);
		DeleteModificationDTO deleteModificationDTO = (DeleteModificationDTO) modificationDTO;
		assertThat(deleteModificationDTO.getPath()).isEqualTo("/2/1/b");

		localRepoManager.close();
	}

	private Set<String> getPaths(Collection<RepoFileDTO> repoFileDTOs) {
		assertThat(repoFileDTOs).isNotNull().isNotEmpty();
		RepoFileDTOTreeNode rootNode = RepoFileDTOTreeNode.createTree(repoFileDTOs);
		assertThat(rootNode).isNotNull();
		assertThat(rootNode.getRepoFileDTO().getName()).isEqualTo("");
		List<RepoFileDTOTreeNode> leafs = rootNode.getLeafs();
		Set<String> paths = new HashSet<String>(leafs.size());
		for (RepoFileDTOTreeNode leaf : leafs) {
			paths.add(leaf.getPath());
		}
		return paths;
	}

}
