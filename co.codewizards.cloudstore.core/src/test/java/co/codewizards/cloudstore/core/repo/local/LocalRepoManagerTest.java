package co.codewizards.cloudstore.core.repo.local;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.AbstractTest;
import co.codewizards.cloudstore.core.persistence.DeleteModification;
import co.codewizards.cloudstore.core.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.Modification;
import co.codewizards.cloudstore.core.persistence.ModificationDAO;
import co.codewizards.cloudstore.core.persistence.RepoFile;
import co.codewizards.cloudstore.core.persistence.RepoFileDAO;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;

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
			localRepositoryRevisionBeforeSync = transaction.getDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail().getRevision();
			RepoFile childRepoFile_1 = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionBeforeSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		long child_1LastModifiedBeforeModification = child_1.lastModified();

		deleteFile(child_1_b);

		// In GNU/Linux, the parent-directory's last-modified timestamp is changed, if a child is added or removed.
		// To make sure, this has no influence on our test, we reset this timestamp after our change.
		child_1.setLastModified(child_1LastModifiedBeforeModification);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		long localRepositoryRevisionAfterSync;
		long child_1_localRevisionAfterSync;
		transaction = localRepoManager.beginTransaction();
		try {
			localRepositoryRevisionAfterSync = transaction.getDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail().getRevision();
			RepoFile childRepoFile_1 = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionAfterSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		assertThat(localRepositoryRevisionAfterSync).isGreaterThan(localRepositoryRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isEqualTo(child_1_localRevisionBeforeSync);

		localRepoManager.close();
	}

	@Test
	public void checkDeleteModificationAfterFileDeletion() throws Exception {
		syncExistingDirectoryGraph();
		LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		// We must connect another repository, because there is otherwise no DeleteModification created.
		// Only if at least one DeleteModification is created, we'll have a change.
		File localRoot2 = newTestRepositoryLocalRoot();
		localRoot2.mkdir();
		LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot2);
		localRepoManager.putRemoteRepository(localRepoManager2.getLocalRepositoryID(), null, localRepoManager2.getPublicKey());

		File child_1 = new File(localRoot, "1");
		assertThat(child_1).isDirectory();
		File child_1_b = new File(child_1, "b");
		assertThat(child_1_b).isFile();

		LocalRepoTransaction transaction = localRepoManager.beginTransaction();
		try {
			Collection<Modification> modifications = transaction.getDAO(ModificationDAO.class).getObjects();
			assertThat(getDeleteModifications(modifications)).isEmpty();
		} finally {
			transaction.rollbackIfActive();
		}

		long child_1LastModifiedBeforeModification = child_1.lastModified();

		deleteFile(child_1_b);

		// In GNU/Linux, the parent-directory's last-modified timestamp is changed, if a child is added or removed.
		// To make sure, this has no influence on our test, we reset this timestamp after our change.
		child_1.setLastModified(child_1LastModifiedBeforeModification);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		transaction = localRepoManager.beginTransaction();
		try {
			Collection<Modification> modifications = transaction.getDAO(ModificationDAO.class).getObjects();
			List<DeleteModification> deleteModifications = getDeleteModifications(modifications);
			assertThat(deleteModifications).hasSize(1);
			DeleteModification deleteModification = deleteModifications.get(0);
			assertThat(deleteModification).isNotNull();
			assertThat(deleteModification.getPath()).isEqualTo("/1/b");
			assertThat(deleteModification.getRemoteRepository()).isNotNull();
			assertThat(deleteModification.getRemoteRepository().getEntityID()).isEqualTo(localRepoManager2.getLocalRepositoryID());
		} finally {
			transaction.rollbackIfActive();
		}

		localRepoManager2.close();
		localRepoManager.close();
	}

	private List<DeleteModification> getDeleteModifications(Collection<Modification> modifications) {
		List<DeleteModification> result = new ArrayList<DeleteModification>();
		for (Modification modification : modifications) {
			if (modification instanceof DeleteModification)
				result.add((DeleteModification) modification);
		}
		return result;
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
			localRepositoryRevisionBeforeSync = transaction.getDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail().getRevision();
			RepoFile childRepoFile_1 = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionBeforeSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		long child_1LastModifiedBeforeModification = child_1.lastModified();

		createFileWithRandomContent(child_1, "d");

		// In GNU/Linux, the parent-directory's last-modified timestamp is changed, if a child is added or removed.
		// To make sure, this has no influence on our test, we reset this timestamp after our change.
		child_1.setLastModified(child_1LastModifiedBeforeModification);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		long localRepositoryRevisionAfterSync;
		long child_1_localRevisionAfterSync;
		transaction = localRepoManager.beginTransaction();
		try {
			localRepositoryRevisionAfterSync = transaction.getDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail().getRevision();
			RepoFile childRepoFile_1 = transaction.getDAO(RepoFileDAO.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionAfterSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		assertThat(localRepositoryRevisionAfterSync).isGreaterThan(localRepositoryRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isEqualTo(child_1_localRevisionBeforeSync);

		localRepoManager.close();
	}
}
