package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.objectfactory.ObjectFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.local.persistence.FileInProgressMarker;
import co.codewizards.cloudstore.local.persistence.FileInProgressMarkerDao;
import co.codewizards.cloudstore.local.transport.TempChunkFileManager;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import net.jcip.annotations.NotThreadSafe;

/**
 * TODO rewrite this entire test! It is currently based on pretty fragile multi-threading. It might be better to use a different approach. Marco :-)
 * Maybe we should using mocking or somehow replace the real services by some sub-classes that interact with the test. This should
 * be more reliable than watching the file system from the outside on a different thread than the actual sync.
 *
 * @author Sebastian Schefczyk
 */
@RunWith(JMockit.class)
@NotThreadSafe // seems to be necessary because mocking of ObjectFactory otherwise does not work :-(
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
	public void before() throws Exception {
		super.before();

		// I tried to directly mock the RepoToRepoSync in a downstream project and was not able to do so. Mocking the
		// object factory works well, though => mocking here the ObjectFactory instead to return our actual mock.
		new MockUp<ObjectFactory>() {
			@Mock
			<T> T createObject(Invocation invocation, Class<T> clazz, Class<?>[] parameterTypes, Object ... parameters) {
				if (TempChunkFileManager.class.isAssignableFrom(clazz)) {
					return clazz.cast(new MockTempChunkFileManager());
				}
				return invocation.proceed();
			}
		};

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

		// initially there should be no files in progress!
		assertNoFilesInProgress();
	}

	/**
	 * Special TempChunkFileManager slowing down operations in order to make watching them asynchronously
	 * more reliable.
	 * <p>
	 * I had a few times the situation that tests worked fine on one machine and didn't on another.
	 * This seemed to be depending on CPU and disk as the tests here are heavily relying on multi-threading.
	 */
	private static class MockTempChunkFileManager extends TempChunkFileManager {
		protected MockTempChunkFileManager() {
			System.err.println("MockTempChunkFileManager instantiated.");
		}

		@Override
		protected synchronized File createTempChunkFile(File destFile, long offset, boolean createNewFile) {
			File result = super.createTempChunkFile(destFile, offset, createNewFile);
			System.err.println("createTempChunkFile: " + destFile.getName() + "; createNewFile=" + createNewFile);
			if (createNewFile)
				sleep(200);

			return result;
		}

		@Override
		protected void moveOrFail(File oldFile, File newFile) throws IOException {
			super.moveOrFail(oldFile, newFile);
			System.err.println("moveOrFail: " + oldFile.getName() + " => " + newFile.getName());
			sleep(200);
		}

		@Override
		protected void deleteOrFail(File file) throws IOException {
			super.deleteOrFail(file);
			System.err.println("deleteOrFail: " + file.getName());
			sleep(200);
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.warn("sleep: " + e, e);
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
		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		assertFilesInProgress(Sync.DOWN, 1);

		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			fileWatcher.createDeleteChunks(repoToRepoSync, localRepoManagerLocal, new LoggerProgressMonitor(logger), 1, 2);
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
		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		assertFilesInProgress(Sync.UP, 1);

		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
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
		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		// delete the chunks; the sync algorithm should tolerate this
		fileWatcher.deleteTempDir();

		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
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
		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		// delete the chunks; the sync algorithm should tolerate this
		fileWatcher.deleteTempDir();

		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
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
		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		// modify the source file
		file = createFileWithChunks(remoteRoot, remoteRoot, fileName, 2);

		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
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
		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.DOWN, 1);

		// modify the source file
		final String newFileName = "ee-renamed";
		moveFile(remoteRoot, file, createFile(remoteRoot, newFileName));

		// Because of the newly introduced ChangeSetDto-cache, the next repo-to-repo-sync would not yet work on
		// the new data. Hence, we delete the cache, now.
		// TODO when rewriting this entire test-class, this should be handled correctly! See class-comment above.
		// See: https://github.com/subshare/subshare/issues/75
		getLocalRootWithPathPrefix().createFile(".cloudstore-repo", "tmp").deleteRecursively();

		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
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
		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
				remoteRootURLWithPathPrefix);) {
			// the sync will start and get interrupted inside the fileWatcher!
			fileWatcher.syncOneChunk(repoToRepoSync, new LoggerProgressMonitor(logger));
		}

		// check on file inProgress
		assertFilesInProgress(Sync.UP, 1);

		// modify the source file
		final String newFileName = "lrrs-renamed";
		moveFile(localRoot, file, createFile(localRoot, newFileName));

		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
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
		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
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

		// Because of the newly introduced ChangeSetDto-cache, the next repo-to-repo-sync would not yet work on
		// the new data. Hence, we delete the cache, now.
		// TODO when rewriting this entire test-class, this should be handled correctly! See class-comment above.
		// See: https://github.com/subshare/subshare/issues/75
		getLocalRootWithPathPrefix().createFile(".cloudstore-repo", "tmp").deleteRecursively();

		try (RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(),
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
