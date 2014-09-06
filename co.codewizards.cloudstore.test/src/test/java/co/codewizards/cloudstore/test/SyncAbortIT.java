package co.codewizards.cloudstore.test;

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
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.NormalFileDao;

/**
 * TODO rename
 *
 * @author Sebastian Schefczyk
 */
public class SyncAbortIT extends AbstractRepoAwareIT {

	private static final Logger logger = LoggerFactory.getLogger(SyncAbortIT.class);
	private LocalRepoManager localRepoManagerLocal;
	private LocalRepoManager localRepoManagerRemote;

	// private static boolean ignoreTimeoutExceptions(final TimeoutException e)
	// {
	// // inform in any way
	// e.printStackTrace();
	// // return true;
	// // an alternative ending:
	// fail("In some executions a TimeoutException may occure. "
	// + "The FileWatchService seems to be not perfect in detecting events.");
	// return false;
	// }

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
			assertFilesInProgress(localRepoManagerLocal, 0);
			assertFilesInProgress(localRepoManagerRemote, 0);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void syncAbortResume_remoteToLocal() throws Exception {
		// one file, on remote-side, made of exactly two chunks
		final String fileName = "a";
		final File file = createFileWithChunks(remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		assertFilesInProgress(localRepoManagerRemote, 0);
		assertFilesInProgress(localRepoManagerLocal, 1);

		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerLocal, new LoggerProgressMonitor(logger), 1,
					2);
		}

		// re-check files inProgress:
		assertFilesInProgress(localRepoManagerRemote, 0);
		assertFilesInProgress(localRepoManagerLocal, 0);

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
		final File file = createFileWithChunks(localRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(remoteRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		assertFilesInProgress(localRepoManagerRemote, 1);
		assertFilesInProgress(localRepoManagerLocal, 0);

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
		final File file = createFileWithChunks(remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(localRepoManagerLocal, 1);
		assertFilesInProgress(localRepoManagerRemote, 0);

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
		final File file = createFileWithChunks(remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(localRepoManagerLocal, 1);
		assertFilesInProgress(localRepoManagerRemote, 0);

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
		File file = createFileWithChunks(remoteRoot, fileName, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName, file.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(localRepoManagerLocal, 1);
		assertFilesInProgress(localRepoManagerRemote, 0);

		// modify the source file
		file = createFileWithChunks(remoteRoot, fileName, 2);

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
	public void syncAbortResume_remoteToLocal_watchOrder() throws Exception {
		final String fileName1 = "f1";
		final File file1 = createFileWithChunks(remoteRoot, fileName1, 2);

		final FileWatcher fileWatcher = new FileWatcher(localRoot, fileName1, file1.length());

		// special: delegate the repoToRepoSync.sync into fileWatcher, to be
		// able to interrupt immediately.
		try (RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(localRepoManagerLocal, 1);
		assertFilesInProgress(localRepoManagerRemote, 0);

		final String fileName0 = "f0";
		final String fileName2 = "f2";
		final File file0 = createFileWithChunks(remoteRoot, fileName0, 2);
		final File file2 = createFileWithChunks(remoteRoot, fileName2, 2);

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
		assertFilesInProgress(localRepoManagerLocal, 0);
		assertFilesInProgress(localRepoManagerRemote, 0);

		assertThatFilesInRepoAreCorrect(root);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	/**
	 * Check files inProgress (fileWatcher should have caused an interrupted
	 * sync state)
	 *
	 * @param localRepoManager
	 *            The repo manager (local or remote?)
	 * @param size
	 *            The expected amount of files in progress.
	 */
	private void assertFilesInProgress(final LocalRepoManager localRepoManager, final int size) {
		try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
			final NormalFileDao normalFileDao = transaction.getDao(NormalFileDao.class);
			final Collection<NormalFile> filesInProgress = normalFileDao.getNormalFilesInProgress();
			assertThat(filesInProgress.size()).isEqualTo(size);
		}
	}

}
