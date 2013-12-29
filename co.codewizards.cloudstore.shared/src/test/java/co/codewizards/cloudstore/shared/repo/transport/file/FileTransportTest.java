package co.codewizards.cloudstore.shared.repo.transport.file;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.AbstractTest;
import co.codewizards.cloudstore.shared.dto.ChangeSetRequest;
import co.codewizards.cloudstore.shared.dto.ChangeSetResponse;
import co.codewizards.cloudstore.shared.dto.RepoFileDTO;
import co.codewizards.cloudstore.shared.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.shared.repo.local.RepositoryManager;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransport;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactory;
import co.codewizards.cloudstore.shared.repo.transport.RepoTransportFactoryRegistry;

public class FileTransportTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(FileTransportTest.class);

	private File remoteRoot;
	private ChangeSetResponse changeSetResponse1;

	@Test
	public void getChangeSetForEntireRepository() throws Exception {
		remoteRoot = newTestRepositoryLocalRoot();
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(remoteRoot);
		assertThat(repositoryManager).isNotNull();

		File child_1 = createDirectory(remoteRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		File child_2 = createDirectory(remoteRoot, "2");

		createFileWithRandomContent(child_2, "a");

		File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");

		File child_3 = createDirectory(remoteRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		repositoryManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL);

		ChangeSetRequest changeSetRequest1 = new ChangeSetRequest();
		changeSetRequest1.setRevision(-1);

		changeSetResponse1 = repoTransport.getChangeSet(changeSetRequest1);
		assertThat(changeSetResponse1).isNotNull();
		assertThat(changeSetResponse1.getRepoFileDTOs()).isNotNull();
		assertThat(changeSetResponse1.getRepoFileDTOs()).isNotEmpty();
		assertThat(changeSetResponse1.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse1.getRepositoryDTO().getEntityID()).isNotNull();

		// changeSetResponse1 should contain the entire repository - including the root -, because really
		// every localRevision must be > -1.
		assertThat(changeSetResponse1.getRepoFileDTOs()).hasSize(14);
		// TODO check the response thoroughly.

		repositoryManager.close();
	}

	@Test
	public void getChangeSetForAddedFile() throws Exception {
		getChangeSetForEntireRepository();

		RepositoryManager repositoryManager = repositoryManagerRegistry.getRepositoryManager(remoteRoot);

		File child_2 = new File(remoteRoot, "2");
		File child_2_1 = new File(child_2, "1");
		createFileWithRandomContent(child_2_1, "b");

		repositoryManager.localSync(new LoggerProgressMonitor(logger));

		URL remoteRootURL = remoteRoot.toURI().toURL();
		RepoTransportFactory repoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactory(remoteRootURL);
		RepoTransport repoTransport = repoTransportFactory.createRepoTransport(remoteRootURL);

		ChangeSetRequest changeSetRequest2 = new ChangeSetRequest();
		changeSetRequest2.setRevision(changeSetResponse1.getRepositoryDTO().getRevision());

		ChangeSetResponse changeSetResponse2 = repoTransport.getChangeSet(changeSetRequest2);
		assertThat(changeSetResponse2).isNotNull();
		List<RepoFileDTO> repoFileDTOs = changeSetResponse2.getRepoFileDTOs();
		assertThat(repoFileDTOs).isNotNull();
		assertThat(repoFileDTOs).isNotEmpty();
		assertThat(changeSetResponse2.getRepositoryDTO()).isNotNull();
		assertThat(changeSetResponse2.getRepositoryDTO().getEntityID()).isNotNull();

		// We expect the added file and its direct parent, because they are both modified (new localRevision).
		// Additionally, we expect all parent-directories (recursively) until (including) the root, because they
		// are required to have a complete relative path for each modified RepoFile.
		assertThat(repoFileDTOs).hasSize(4);
		assertThat(repoFileDTOs.get(0).getName()).isEqualTo("b");
		assertThat(repoFileDTOs.get(1).getName()).isEqualTo("1");
		// TODO check the response thoroughly.

		repositoryManager.close();
	}

}
