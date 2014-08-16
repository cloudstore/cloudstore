package co.codewizards.cloudstore.local.transport;

import static co.codewizards.cloudstore.core.oio.file.FileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.RepoFileDtoTreeNode;
import co.codewizards.cloudstore.core.oio.file.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.local.AbstractTest;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepoDao;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;

public class FileRepoTransportTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(FileRepoTransportTest.class);

	private File remoteRoot;
	private UUID remoteRepositoryId;
	private File localRoot;
	private UUID localRepositoryId;
	private ChangeSetDto changeSetResponse1;

	@Test
	public void getChangeSetForEntireRepository() throws Exception {
		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();;
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManager).isNotNull();
		remoteRepositoryId = localRepoManager.getRepositoryId();
		assertThat(remoteRepositoryId).isNotNull();

		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot.exists()).isFalse();;
		localRoot.mkdirs();
		final LocalRepoManager toLocalRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		localRepoManager.putRemoteRepository(toLocalRepoManager.getRepositoryId(), null, toLocalRepoManager.getPublicKey(), "");
		toLocalRepoManager.putRemoteRepository(localRepoManager.getRepositoryId(), null, localRepoManager.getPublicKey(), "");
		localRepositoryId = toLocalRepoManager.getRepositoryId();
		toLocalRepoManager.close();

		final File child_1 = createDirectory(remoteRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(remoteRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		final File child_3 = createDirectory(remoteRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c+");
		createFileWithRandomContent(child_3, "d#");

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final URL remoteRootURL = remoteRoot.toURI().toURL();
		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		final RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL, localRepositoryId);

		changeSetResponse1 = repoTransport.getChangeSetDto(false);

		repoTransport.close();

		assertThat(changeSetResponse1).isNotNull();
		assertThat(changeSetResponse1.getRepoFileDtos()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse1.getRepositoryDto()).isNotNull();
		assertThat(changeSetResponse1.getRepositoryDto().getRepositoryId()).isNotNull();

		// changeSetResponse1 should contain the entire repository - including the root -, because really
		// every localRevision must be > -1.
		assertThat(changeSetResponse1.getRepoFileDtos()).hasSize(15);

		final Set<String> paths = getPaths(changeSetResponse1.getRepoFileDtos());
		assertThat(paths).containsOnly("/1/a", "/1/b", "/1/c", "/2/a", "/2/1/a", "/2/1/b", "/3/a", "/3/b", "/3/c+", "/3/d#");

		final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			final LocalRepositoryDao localRepositoryDao = transaction.getDao(LocalRepositoryDao.class);
			final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
			final LastSyncToRemoteRepoDao lastSyncToRemoteRepoDao = transaction.getDao(LastSyncToRemoteRepoDao.class);
			final LocalRepository localRepository = localRepositoryDao.getLocalRepositoryOrFail();
			final RemoteRepository toRemoteRepository = remoteRepositoryDao.getRemoteRepositoryOrFail(localRepositoryId);
			final LastSyncToRemoteRepo lastSyncToRemoteRepo = lastSyncToRemoteRepoDao.getLastSyncToRemoteRepoOrFail(toRemoteRepository);
			lastSyncToRemoteRepo.setRemoteRepository(toRemoteRepository);
			lastSyncToRemoteRepo.setLocalRepositoryRevisionSynced(localRepository.getRevision());
			lastSyncToRemoteRepoDao.makePersistent(lastSyncToRemoteRepo);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForAddedFile() throws Exception {
		getChangeSetForEntireRepository();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		final File child_2 = newFile(remoteRoot, "2");
		final File child_2_1 = newFile(child_2, "1");
		createFileWithRandomContent(child_2_1, "c");

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final URL remoteRootURL = remoteRoot.toURI().toURL();
		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		final RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL, localRepositoryId);

		final ChangeSetDto changeSetResponse2 = repoTransport.getChangeSetDto(false);
		repoTransport.close();
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDtos()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDto()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDto().getRepositoryId()).isNotNull();

		// We expect the added file and its direct parent, because they are both modified (new localRevision).
		// Additionally, we expect all parent-directories (recursively) until (including) the root, because they
		// are required to have a complete relative path for each modified RepoFile.
		assertThat(changeSetResponse2.getRepoFileDtos()).hasSize(4);

		final Set<String> paths = getPaths(changeSetResponse2.getRepoFileDtos());
		assertThat(paths).hasSize(1);
		assertThat(paths.iterator().next()).isEqualTo("/2/1/c");

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForModifiedFile() throws Exception {
		getChangeSetForEntireRepository();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		final File child_2 = newFile(remoteRoot, "2");
		final File child_2_1 = newFile(child_2, "1");
		final File child_2_1_b = newFile(child_2_1, "b");
		final OutputStream out = child_2_1_b.createFileOutputStream(true);
		out.write(4);
		out.close();

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final URL remoteRootURL = remoteRoot.toURI().toURL();
		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		final RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL, localRepositoryId);

		final ChangeSetDto changeSetResponse2 = repoTransport.getChangeSetDto(false);
		repoTransport.close();

		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDtos()).isNotNull().isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDto()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDto().getRepositoryId()).isNotNull();

		// We expect the changed file and all parent-directories (recursively) until (including) the
		// root, because they are required to have a complete relative path for each modified RepoFile.
		assertThat(changeSetResponse2.getRepoFileDtos()).hasSize(4);

		final Set<String> paths = getPaths(changeSetResponse2.getRepoFileDtos());
		assertThat(paths).hasSize(1);
		assertThat(paths.iterator().next()).isEqualTo("/2/1/b");

		localRepoManager.close();
	}

	@Test
	public void getChangeSetForDeletedFile() throws Exception {
		getChangeSetForEntireRepository();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		final File child_2 = newFile(remoteRoot, "2");
		final File child_2_1 = newFile(child_2, "1");
		final File child_2_1_b = newFile(child_2_1, "b");

		final long child_2_1LastModifiedBeforeModification = child_2_1.lastModified();

		deleteFile(child_2_1_b);

		// In GNU/Linux, the parent-directory's last-modified timestamp is changed, if a child is added or removed.
		// To make sure, this has no influence on our test, we reset this timestamp after our change.
		child_2_1.setLastModified(child_2_1LastModifiedBeforeModification);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final URL remoteRootURL = remoteRoot.toURI().toURL();
		final RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		final RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL, localRepositoryId);

		final ChangeSetDto changeSetResponse2 = repoTransport.getChangeSetDto(false);
		repoTransport.close();
		assertThat(changeSetResponse2).isNotNull();
		assertThat(changeSetResponse2.getRepoFileDtos()).isNotNull().isEmpty();
		assertThat(changeSetResponse2.getRepositoryDto()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDto().getRepositoryId()).isNotNull();

		// We expect the DeleteModificationDto
		assertThat(changeSetResponse2.getModificationDtos()).hasSize(1);

		final ModificationDto modificationDto = changeSetResponse2.getModificationDtos().get(0);
		assertThat(modificationDto).isNotNull().isInstanceOf(DeleteModificationDto.class);
		final DeleteModificationDto deleteModificationDto = (DeleteModificationDto) modificationDto;
		assertThat(deleteModificationDto.getPath()).isEqualTo("/2/1/b");

		localRepoManager.close();
	}

	private Set<String> getPaths(final Collection<RepoFileDto> repoFileDtos) {
		assertThat(repoFileDtos).isNotNull().isNotEmpty();
		final RepoFileDtoTreeNode rootNode = RepoFileDtoTreeNode.createTree(repoFileDtos);
		assertThat(rootNode).isNotNull();
		assertThat(rootNode.getRepoFileDto().getName()).isEqualTo("");
		final List<RepoFileDtoTreeNode> leafs = rootNode.getLeafs();
		final Set<String> paths = new HashSet<String>(leafs.size());
		for (final RepoFileDtoTreeNode leaf : leafs) {
			paths.add(leaf.getPath());
		}
		return paths;
	}

}
