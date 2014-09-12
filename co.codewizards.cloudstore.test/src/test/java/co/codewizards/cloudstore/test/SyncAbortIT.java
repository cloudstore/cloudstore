package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.local.persistence.FileInProgressMarker;
import co.codewizards.cloudstore.local.persistence.FileInProgressMarkerDao;

/**
 * @author Sebastian Schefczyk
 */
public class SyncAbortIT extends AbstractRepoAwareIT {

	private static final Logger logger = LoggerFactory.getLogger(SyncAbortIT.class);
	private LocalRepoManager localRepoManagerLocal;
	private LocalRepoManager localRepoManagerRemote;

	private enum Sync {
		/** local to remote */
		UP,
		/** remote to local */
		DOWN
	}

	@Override
	@Before
	public void before() {
		super.before();
		try {
			localPathPrefix = "";
			remotePathPrefix = "";

			localRoot = newTestRepositoryLocalRoot("local");
			assertThat(localRoot.exists()).isFalse();
			localRoot.mkdirs();
			assertThat(localRoot.isDirectory()).isTrue();

			remoteRoot = newTestRepositoryLocalRoot("remote");
			assertThat(remoteRoot.exists()).isFalse();
			remoteRoot.mkdirs();
			assertThat(remoteRoot.isDirectory()).isTrue();

			localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
			assertThat(localRepoManagerLocal).isNotNull();

			localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
			assertThat(localRepoManagerRemote).isNotNull();

			final UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
			remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix(remoteRepositoryId);

			new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(),
					remoteRootURLWithPathPrefix.toExternalForm()).execute();
			new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix().getPath()).execute();

			// initially there should be not files in progress!
			assertNoFilesInProgress();
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void syncAbortResume_remoteToLocal() throws Exception {
		// one file, on remote-side, made of exactly two chunks
		final String fileName = "a";
		final File file = createFileWithChunks(remoteRoot, remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		assertFilesInProgress(Sync.DOWN, 1);

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerLocal, new LoggerProgressMonitor(logger), 1,
					2);
		}

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncAbortResume_localToRemote() throws Exception {
		final String fileName = "b";
		final File file = createFileWithChunks(localRoot, localRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(remoteRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		assertFilesInProgress(Sync.UP, 1);

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerRemote, new LoggerProgressMonitor(logger),
					1, 2);
		}

		afterSyncCompleteAssertionsAndCloseOperations(localRoot);
	}

	@Test
	public void syncAbortResume_remoteToLocal_deleteChunk() throws Exception {
		final String fileName = "c";
		final File file = createFileWithChunks(remoteRoot, remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		// delete the chunks; the sync algorithm should tolerate this
		fileWatcher.deleteTempDir();

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerLocal, new LoggerProgressMonitor(logger), 2,
					2);
		}

		afterSyncCompleteAssertionsAndCloseOperations(remoteRoot);
	}

	@Test
	public void syncAbortResume_remoteToLocal_deleteSource() throws Exception {
		final String fileName = "d";
		final File file = createFileWithChunks(remoteRoot, remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		// delete the chunks; the sync algorithm should tolerate this
		fileWatcher.deleteTempDir();

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerLocal, new LoggerProgressMonitor(logger), 2,
					2);
		}

		afterSyncCompleteAssertionsAndCloseOperations(remoteRoot);
	}

	@Test
	public void syncAbortResume_remoteToLocal_modifySource() throws Exception {
		final String fileName = "e";
		File file = createFileWithChunks(remoteRoot, remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		// modify the source file
		file = createFileWithChunks(remoteRoot, remoteRoot, fileName, 2);

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// chunks that will differ after modification of the source file
			// will be overwritten without deletion; so no difference in amount
			// of creation/deletion.
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerLocal, new LoggerProgressMonitor(logger), 1,
					2, file.length());
		}

		afterSyncCompleteAssertionsAndCloseOperations(remoteRoot);
	}

	@Test
	public void syncAbortResume_remoteToLocal_renameSource() throws Exception {
		final String fileName = "ee";
		final File file = createFileWithChunks(remoteRoot, remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		// modify the source file
		final String newFileName = "ee-renamed";
		moveFile(remoteRoot, file, createFile(remoteRoot, newFileName));

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// because the chunk will be moved, the move operation is also observed as create/delete; so for the first
			// chunk (move: 1 create, 1 delete), for the second (1 create); after appending the chunks to the
			// destination file, 2 deletes.
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerLocal, new LoggerProgressMonitor(logger), 2,
					3, newFileName);
		}

		afterSyncCompleteAssertionsAndCloseOperations(remoteRoot);
	}

	@Test
	public void syncAbortResume_localToRemote_renameSource() throws Exception {
		final String fileName = "lrrs";
		final File file = createFileWithChunks(localRoot, localRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(remoteRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.UP, 1);

		// modify the source file
		final String newFileName = "lrrs-renamed";
		moveFile(localRoot, file, createFile(localRoot, newFileName));

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// because the chunk will be moved, the move operation is also observed as create/delete; so for the first
			// chunk (move: 1 create, 1 delete), for the second (1 create); after appending the chunks to the
			// destination file, 2 deletes.
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerRemote, new LoggerProgressMonitor(logger), 2,
					3, newFileName);
		}

		afterSyncCompleteAssertionsAndCloseOperations(localRoot);
	}

	@Test
	public void syncAbortResume_remoteToLocal_watchOrder() throws Exception {
		final String fileName1 = "f1";
		final File file1 = createFileWithChunks(remoteRoot, remoteRoot, fileName1, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName1, file1.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		final String fileName0 = "f0";
		final String fileName2 = "f2";
		final File file0 = createFileWithChunks(remoteRoot, remoteRoot, fileName0, 2);
		final File file2 = createFileWithChunks(remoteRoot, remoteRoot, fileName2, 2);

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// we expect the aborted file to resume at first, then syncing the rest (and not again the first).
			fileWatcher.watchSyncOrder(repoToRepoSync, localRepoManagerLocal, new LoggerProgressMonitor(logger),
					fileName1, file1.length(), fileName0, file0.length(), fileName2, file2.length());
		}

		afterSyncCompleteAssertionsAndCloseOperations(remoteRoot);
	}

	/**
	 * Assert and close operations needed on every test!
	 *
	 * @throws IOException
	 */
	private void afterSyncCompleteAssertionsAndCloseOperations(final File root) throws IOException {
		// re-check files inProgress:
		assertNoFilesInProgress();

		assertThatFilesInRepoAreCorrect(root);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	private void assertNoFilesInProgress() {
		// Primary check proves against zero, the rest will also be checked against zero:
		assertFilesInProgress(Sync.UP, 0);
	}

	/**
	 * Check files in progress (fileWatcher should have caused an interrupted sync state). The opposite direction and
	 * both directions on the other side must be zero.
	 * @param syncDirection
	 *            UP would be local to remote, and DOWN vice versa.
	 * @param size
	 *            The expected amount of files in progress on the local side.
	 */
	@SuppressWarnings("resource")
	private void assertFilesInProgress(final Sync syncDirection, final int size) {
		final LocalRepoManager fromRepoManager = (syncDirection.equals(Sync.UP)) ? localRepoManagerLocal
				: localRepoManagerRemote;
		final LocalRepoManager toRepoManager = (syncDirection.equals(Sync.DOWN)) ? localRepoManagerLocal
				: localRepoManagerRemote;

		try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();) {
			final FileInProgressMarkerDao fileInProgressMarkerDao = transaction.getDao(FileInProgressMarkerDao.class);
			Collection<FileInProgressMarker> fileInProgressMarkers = fileInProgressMarkerDao.getFileInProgressMarkers(fromRepoManager.getRepositoryId(),
					toRepoManager.getRepositoryId());
			assertThat(fileInProgressMarkers.size()).isEqualTo(size);
			// also assert files are not in Progress in the opposite direction:
			fileInProgressMarkers = fileInProgressMarkerDao.getFileInProgressMarkers(toRepoManager.getRepositoryId(),
					fromRepoManager.getRepositoryId());
			assertThat(fileInProgressMarkers.size()).isEqualTo(0);
		}
		// also assert there is nothing on the other side (local / remote):
		try (final LocalRepoTransaction transaction = localRepoManagerRemote.beginWriteTransaction();) {
			final FileInProgressMarkerDao fileInProgressMarkerDao = transaction.getDao(FileInProgressMarkerDao.class);
			Collection<FileInProgressMarker> fileInProgressMarkers = fileInProgressMarkerDao.getFileInProgressMarkers(fromRepoManager.getRepositoryId(),
					toRepoManager.getRepositoryId());
			assertThat(fileInProgressMarkers.size()).isEqualTo(0);
			fileInProgressMarkers = fileInProgressMarkerDao.getFileInProgressMarkers(toRepoManager.getRepositoryId(),
					fromRepoManager.getRepositoryId());
			assertThat(fileInProgressMarkers.size()).isEqualTo(0);
		}
	}

}
