package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.DeleteModification;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.Modification;
import co.codewizards.cloudstore.local.persistence.ModificationDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;
import co.codewizards.cloudstore.oio.api.File;

public class LocalRepoManagerTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoManagerTest.class);

	private File localRoot;

	@Test
	public void syncExistingDirectoryGraph() throws Exception {
		localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		final File child_1 = createDirectory(localRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(localRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");

		final File child_3 = createDirectory(localRoot, "3");

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
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		final File child_1 = newFile(localRoot, "1");
		final File child_1_1 = createDirectory(child_1, "1");
		final File child_1_2 = createDirectory(child_1, "2");
		final File child_2 = newFile(localRoot, "2");

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
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		final File child_1 = newFile(localRoot, "1");
		assertThat(child_1.isDirectory()).isTrue();
		final File child_1_b = newFile(child_1, "b");
		assertThat(child_1_b.isFile()).isTrue();
		final File child_1_c = newFile(child_1, "c");
		assertThat(child_1_c.isFile()).isTrue();

		final File child_2 = newFile(localRoot, "2");
		assertThat(child_2.isDirectory()).isTrue();

		final File child_2_1 = newFile(child_2, "1");
		assertThat(child_2_1.isDirectory()).isTrue();

		final File child_2_1_a = newFile(child_2_1, "a");
		assertThat(child_2_1_a.isFile()).isTrue();

		final File child_2_a = newFile(child_2, "a");
		assertThat(child_2_a.isFile()).isTrue();

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
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		final File child_1 = newFile(localRoot, "1");
		assertThat(child_1.isDirectory()).isTrue();
		final File child_1_b = newFile(child_1, "b");
		assertThat(child_1_b.isFile()).isTrue();
		final File child_1_c = newFile(child_1, "c");
		assertThat(child_1_c.isFile()).isTrue();

		final File child_2 = newFile(localRoot, "2");
		assertThat(child_2.isDirectory()).isTrue();

		final File child_2_1 = newFile(child_2, "1");
		assertThat(child_2_1.isDirectory()).isTrue();

		final File child_2_1_a = newFile(child_2_1, "a");
		assertThat(child_2_1_a.isFile()).isTrue();

		final File child_2_a = newFile(child_2, "a");
		assertThat(child_2_a.isFile()).isTrue();

		deleteFile(child_1_b);
		deleteFile(child_1_c);
		deleteFile(child_2_a);
		deleteFile(child_2_1_a);
		deleteFile(child_2_1);
		deleteFile(child_2);

		// child_2 was a directory => switching it to a file now.
		createFileWithRandomContent(child_2);
		assertThat(child_2.isFile()).isTrue();

		// child_1_b was a file => switching it to a directory now.
		createDirectory(child_1_b);
		assertThat(child_1_b.isDirectory()).isTrue();

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		localRepoManager.close();
	}

	@Test
	public void checkParentLocalRevisionAfterChildDeletion() throws Exception {
		syncExistingDirectoryGraph();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		final File child_1 = newFile(localRoot, "1");
		assertThat(child_1.isDirectory()).isTrue();
		final File child_1_b = newFile(child_1, "b");
		assertThat(child_1_b.isFile()).isTrue();

		long localRepositoryRevisionBeforeSync;
		long child_1_localRevisionBeforeSync;
		LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			localRepositoryRevisionBeforeSync = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail().getRevision();
			final RepoFile childRepoFile_1 = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionBeforeSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		final long child_1LastModifiedBeforeModification = child_1.lastModified();

		deleteFile(child_1_b);

		// In GNU/Linux, the parent-directory's last-modified timestamp is changed, if a child is added or removed.
		// To make sure, this has no influence on our test, we reset this timestamp after our change.
		child_1.setLastModified(child_1LastModifiedBeforeModification);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		long localRepositoryRevisionAfterSync;
		long child_1_localRevisionAfterSync;
		transaction = localRepoManager.beginWriteTransaction();
		try {
			localRepositoryRevisionAfterSync = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail().getRevision();
			final RepoFile childRepoFile_1 = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, child_1);
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
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		// We must connect another repository, because there is otherwise no DeleteModification created.
		// Only if at least one DeleteModification is created, we'll have a change.
		final File localRoot2 = newTestRepositoryLocalRoot();
		localRoot2.mkdir();
		final LocalRepoManager localRepoManager2 = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot2);
		localRepoManager.putRemoteRepository(localRepoManager2.getRepositoryId(), null, localRepoManager2.getPublicKey(), "");

		final File child_1 = newFile(localRoot, "1");
		assertThat(child_1.isDirectory()).isTrue();
		final File child_1_b = newFile(child_1, "b");
		assertThat(child_1_b.isFile()).isTrue();

		LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			final Collection<Modification> modifications = transaction.getDao(ModificationDao.class).getObjects();
			assertThat(getDeleteModifications(modifications)).isEmpty();
		} finally {
			transaction.rollbackIfActive();
		}

		final long child_1LastModifiedBeforeModification = child_1.lastModified();

		deleteFile(child_1_b);

		// In GNU/Linux, the parent-directory's last-modified timestamp is changed, if a child is added or removed.
		// To make sure, this has no influence on our test, we reset this timestamp after our change.
		child_1.setLastModified(child_1LastModifiedBeforeModification);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		transaction = localRepoManager.beginWriteTransaction();
		try {
			final Collection<Modification> modifications = transaction.getDao(ModificationDao.class).getObjects();
			final List<DeleteModification> deleteModifications = getDeleteModifications(modifications);
			assertThat(deleteModifications).hasSize(1);
			final DeleteModification deleteModification = deleteModifications.get(0);
			assertThat(deleteModification).isNotNull();
			assertThat(deleteModification.getPath()).isEqualTo("/1/b");
			assertThat(deleteModification.getRemoteRepository()).isNotNull();
			assertThat(deleteModification.getRemoteRepository().getRepositoryId()).isEqualTo(localRepoManager2.getRepositoryId());
		} finally {
			transaction.rollbackIfActive();
		}

		localRepoManager2.close();
		localRepoManager.close();
	}

	private List<DeleteModification> getDeleteModifications(final Collection<Modification> modifications) {
		final List<DeleteModification> result = new ArrayList<DeleteModification>();
		for (final Modification modification : modifications) {
			if (modification instanceof DeleteModification)
				result.add((DeleteModification) modification);
		}
		return result;
	}

	@Test
	public void checkParentLocalRevisionAfterChildAddition() throws Exception {
		syncExistingDirectoryGraph();
		final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);
		assertThat(localRepoManager).isNotNull();

		final File child_1 = newFile(localRoot, "1");
		assertThat(child_1.isDirectory()).isTrue();

		long localRepositoryRevisionBeforeSync;
		long child_1_localRevisionBeforeSync;
		LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();
		try {
			localRepositoryRevisionBeforeSync = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail().getRevision();
			final RepoFile childRepoFile_1 = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionBeforeSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		final long child_1LastModifiedBeforeModification = child_1.lastModified();

		createFileWithRandomContent(child_1, "d");

		// In GNU/Linux, the parent-directory's last-modified timestamp is changed, if a child is added or removed.
		// To make sure, this has no influence on our test, we reset this timestamp after our change.
		child_1.setLastModified(child_1LastModifiedBeforeModification);

		localRepoManager.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		long localRepositoryRevisionAfterSync;
		long child_1_localRevisionAfterSync;
		transaction = localRepoManager.beginWriteTransaction();
		try {
			localRepositoryRevisionAfterSync = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail().getRevision();
			final RepoFile childRepoFile_1 = transaction.getDao(RepoFileDao.class).getRepoFile(localRoot, child_1);
			child_1_localRevisionAfterSync = childRepoFile_1.getLocalRevision();
		} finally {
			transaction.rollbackIfActive();
		}

		assertThat(localRepositoryRevisionAfterSync).isGreaterThan(localRepositoryRevisionBeforeSync);
		assertThat(child_1_localRevisionAfterSync).isEqualTo(child_1_localRevisionBeforeSync);

		localRepoManager.close();
	}

	private File newTestRepositoryLocalRoot() throws IOException {
		return newTestRepositoryLocalRoot("");
	}

}
