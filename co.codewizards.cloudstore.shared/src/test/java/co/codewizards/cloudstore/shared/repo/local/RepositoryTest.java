package co.codewizards.cloudstore.shared.repo.local;

import static org.assertj.core.api.Assertions.*;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.progress.LoggerProgressMonitor;

public class RepositoryTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(RepositoryTest.class);

	private File localRoot;

	@Test
	public void syncExistingDirectoryGraph() throws Exception {
		localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();

		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(localRoot);
		assertThat(repositoryManager).isNotNull();

		File child_1 = createDirectory(localRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		File child_2 = createDirectory(localRoot, "2");

		createFileWithRandomContent(child_2, "a");

		File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");

		File child_3 = createDirectory(localRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		repositoryManager.sync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		repositoryManager.close();
	}

	@Test
	public void syncAddedFiles() throws Exception {
		syncExistingDirectoryGraph();
		RepositoryManager repositoryManager = repositoryManagerRegistry.getRepositoryManager(localRoot);
		assertThat(repositoryManager).isNotNull();

		File child_1 = new File(localRoot, "1");
		File child_1_1 = createDirectory(child_1, "1");
		File child_1_2 = createDirectory(child_1, "2");
		File child_2 = new File(localRoot, "2");

		createFileWithRandomContent(child_1, "d");

		createFileWithRandomContent(child_1_1, "aa");
		createFileWithRandomContent(child_1_1, "bb");

		createFileWithRandomContent(child_1_2, "aaa");
		createFileWithRandomContent(child_1_2, "bbb");
		createFileWithRandomContent(child_1_2, "ccc");
		createFileWithRandomContent(child_1_2, "ddd");

		createFileWithRandomContent(child_2, "b");
		createFileWithRandomContent(child_2, "c");

		repositoryManager.sync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		repositoryManager.close();
	}

}
