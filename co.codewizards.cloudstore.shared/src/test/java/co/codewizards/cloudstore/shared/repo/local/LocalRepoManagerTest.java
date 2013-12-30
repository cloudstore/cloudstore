package co.codewizards.cloudstore.shared.repo.local;

import static org.assertj.core.api.Assertions.*;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.AbstractTest;
import co.codewizards.cloudstore.shared.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.shared.persistence.RepoFile;
import co.codewizards.cloudstore.shared.persistence.RepoFileDAO;
import co.codewizards.cloudstore.shared.progress.LoggerProgressMonitor;

public class LocalRepoManagerTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoManagerTest.class);

	private File localRoot;

	@Test
	public void syncExistingDirectoryGraph() throws Exception {
		localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();

		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

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

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		localRepoManager.close();
	}

	@Test
	public void syncAddedFiles() throws Exception {
		syncExistingDirectoryGraph();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

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

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		localRepoManager.close();
	}

	@Test
	public void syncDeletedFiles() throws Exception {
		syncExistingDirectoryGraph();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		File child_1 = new File(localRoot, "1");
		assertThat(child_1).isDirectory();
		File child_1_b = new File(child_1, "b");
		assertThat(child_1_b).isFile();
		File child_1_c = new File(child_1, "c");
		assertThat(child_1_c).isFile();

		File child_2 = new File(localRoot, "2");
		assertThat(child_2).isDirectory();

		File child_2_1 = new File(child_2, "1");
		assertThat(child_2_1).isDirectory();

		File child_2_1_a = new File(child_2_1, "a");
		assertThat(child_2_1_a).isFile();

		File child_2_a = new File(child_2, "a");
		assertThat(child_2_a).isFile();

		deleteFile(child_1_b);
		deleteFile(child_1_c);
		deleteFile(child_2_a);
		deleteFile(child_2_1_a);
		deleteFile(child_2_1);
		deleteFile(child_2);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		localRepoManager.close();
	}

	@Test
	public void syncSwitchingFromFilesToDirectoriesAndViceVersa() throws Exception {
		syncExistingDirectoryGraph();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		File child_1 = new File(localRoot, "1");
		assertThat(child_1).isDirectory();
		File child_1_b = new File(child_1, "b");
		assertThat(child_1_b).isFile();
		File child_1_c = new File(child_1, "c");
		assertThat(child_1_c).isFile();

		File child_2 = new File(localRoot, "2");
		assertThat(child_2).isDirectory();

		File child_2_1 = new File(child_2, "1");
		assertThat(child_2_1).isDirectory();

		File child_2_1_a = new File(child_2_1, "a");
		assertThat(child_2_1_a).isFile();

		File child_2_a = new File(child_2, "a");
		assertThat(child_2_a).isFile();

		deleteFile(child_1_b);
		deleteFile(child_1_c);
		deleteFile(child_2_a);
		deleteFile(child_2_1_a);
		deleteFile(child_2_1);
		deleteFile(child_2);

		// child_2 was a directory => switching it to a file now.
		createFileWithRandomContent(child_2);
		assertThat(child_2).isFile();

		// child_1_b was a file => switching it to a directory now.
		createDirectory(child_1_b);
		assertThat(child_1_b).isDirectory();

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		localRepoManager.close();
	}

	@Test
	public void checkParentLocalRevisionAfterChildDeletion() throws Exception {
		syncExistingDirectoryGraph();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		File child_1 = new File(localRoot, "1");
		assertThat(child_1).isDirectory();
		File child_1_b = new File(child_1, "b");
		assertThat(child_1_b).isFile();

		long localRepositoryRevisionBeforeSync;
		long child_1_localRevisionBeforeSync;
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			localRepositoryRevisionBeforeSync = transaction.createDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail().getRevision();
			RepoFile childRepoFile_1 = transaction.createDAO(RepoFileDAO.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionBeforeSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		deleteFile(child_1_b);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		long localRepositoryRevisionAfterSync;
		long child_1_localRevisionAfterSync;
		transaction = localRepoManager.beginTransaction();
		try {
			localRepositoryRevisionAfterSync = transaction.createDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail().getRevision();
			RepoFile childRepoFile_1 = transaction.createDAO(RepoFileDAO.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionAfterSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		assertThat(localRepositoryRevisionAfterSync).isGreaterThan(localRepositoryRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isGreaterThan(child_1_localRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isGreaterThan(localRepositoryRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isEqualTo(localRepositoryRevisionAfterSync);

		localRepoManager.close();
	}

	@Test
	public void checkParentLocalRevisionAfterChildAddition() throws Exception {
		syncExistingDirectoryGraph();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		File child_1 = new File(localRoot, "1");
		assertThat(child_1).isDirectory();

		long localRepositoryRevisionBeforeSync;
		long child_1_localRevisionBeforeSync;
		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			localRepositoryRevisionBeforeSync = transaction.createDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail().getRevision();
			RepoFile childRepoFile_1 = transaction.createDAO(RepoFileDAO.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionBeforeSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		createFileWithRandomContent(child_1, "d");

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		long localRepositoryRevisionAfterSync;
		long child_1_localRevisionAfterSync;
		transaction = localRepoManager.beginTransaction();
		try {
			localRepositoryRevisionAfterSync = transaction.createDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail().getRevision();
			RepoFile childRepoFile_1 = transaction.createDAO(RepoFileDAO.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionAfterSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		assertThat(localRepositoryRevisionAfterSync).isGreaterThan(localRepositoryRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isGreaterThan(child_1_localRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isGreaterThan(localRepositoryRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isEqualTo(localRepositoryRevisionAfterSync);

		localRepoManager.close();
	}
}
