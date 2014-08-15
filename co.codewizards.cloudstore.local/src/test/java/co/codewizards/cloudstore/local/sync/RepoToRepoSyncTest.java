package co.codewizards.cloudstore.local.sync;

import static org.assertj.core.api.Assertions.*;

import co.codewizards.cloudstore.core.oio.file.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.AbstractTest;

public class RepoToRepoSyncTest extends AbstractTest {
	private static Logger logger = LoggerFactory.getLogger(RepoToRepoSyncTest.class);

	private File localRoot;
	private File remoteRoot;

	private String localPathPrefix;
	private String remotePathPrefix;

	@Override
	@Before
	public void before() {
		localPathPrefix = "";
		remotePathPrefix = "";
	}

	private File getLocalRootWithPathPrefix() {
		if (localPathPrefix.isEmpty())
			return localRoot;

		return newFile(localRoot, localPathPrefix);
	}

	private File getRemoteRootWithPathPrefix() {
		if (remotePathPrefix.isEmpty())
			return remoteRoot;

		final File file = newFile(remoteRoot, remotePathPrefix);
		return file;
	}

	private URL getRemoteRootUrlWithPathPrefix() {
		try {
			final URL url = getRemoteRootWithPathPrefix().toURI().toURL();
			return url;
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void syncFromRemoteToLocal() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		localRepoManagerLocal.putRemoteRepository(localRepoManagerRemote.getRepositoryId(), getRemoteRootUrlWithPathPrefix(), localRepoManagerRemote.getPublicKey(), localPathPrefix);
		localRepoManagerRemote.putRemoteRepository(localRepoManagerLocal.getRepositoryId(), null, localRepoManagerLocal.getPublicKey(), remotePathPrefix);

		final File child_1 = createDirectory(remoteRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(remoteRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b", 150000);

		final File child_3 = createDirectory(remoteRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		logger.info("local repo: {}", localRepoManagerLocal.getRepositoryId());
		logger.info("remote repo: {}", localRepoManagerRemote.getRepositoryId());

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncFromRemoteToLocalWithAddedFilesAndDirectories() throws Exception {
		syncFromRemoteToLocal();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final File child_2 = newFile(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		final File child_2_1 = newFile(child_2, "1");
		assertThat(child_2_1).isDirectory();

		final File child_2_1_5 = createDirectory(child_2_1, "5");
		createFileWithRandomContent(child_2_1_5, "aaa");
		createFileWithRandomContent(child_2_1_5, "bbb");

		final File child_3 = newFile(remoteRoot, "3");
		assertThat(child_3).isDirectory();

		createFileWithRandomContent(child_3, "e");

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncFromRemoteToLocalWithModifiedFiles() throws Exception {
		syncFromRemoteToLocal();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final File child_2 = newFile(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		final File child_2_1 = newFile(child_2, "1");
		assertThat(child_2_1).isDirectory();

		final File child_2_1_a = newFile(child_2_1, "a");
		assertThat(child_2_1_a).isFile();

		final File child_2_1_b = newFile(child_2_1, "b");
		assertThat(child_2_1_b).isFile();

		modifyFileRandomly(child_2_1_a);

		logger.info("file='{}' length={}", child_2_1_b, child_2_1_b.length());

		final FileOutputStream out = new FileOutputStream(child_2_1_b);
		out.write(random.nextInt());
		out.close();

		logger.info("file='{}' length={}", child_2_1_b, child_2_1_b.length());

		final byte[] child_2_1_a_expected = IOUtil.getBytesFromFile(child_2_1_a);
		final byte[] child_2_1_b_expected = IOUtil.getBytesFromFile(child_2_1_b);

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());

		// ensure that nothing was synced backwards into the wrong direction ;-)
		final byte[] child_2_1_a_actual = IOUtil.getBytesFromFile(child_2_1_a);
		final byte[] child_2_1_b_actual = IOUtil.getBytesFromFile(child_2_1_b);
		assertThat(child_2_1_a_actual).isEqualTo(child_2_1_a_expected);
		assertThat(child_2_1_b_actual).isEqualTo(child_2_1_b_expected);
	}

	private static class RepoToRepoSync extends co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync {
		public RepoToRepoSync(final File localRoot, final URL remoteRoot) {
			super(localRoot, remoteRoot);
		}
		@Override
		protected void syncUp(final ProgressMonitor monitor) {
			super.syncUp(monitor);
		}
		@Override
		protected void syncDown(final boolean fromRepoLocalSync, final ProgressMonitor monitor) {
			super.syncDown(fromRepoLocalSync, monitor);
		}
	}

	@Test
	public void syncUpAndModifyFile() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		localRoot.mkdirs();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		remoteRoot.mkdirs();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);

		localRepoManagerLocal.putRemoteRepository(localRepoManagerRemote.getRepositoryId(), getRemoteRootUrlWithPathPrefix(), localRepoManagerRemote.getPublicKey(), localPathPrefix);
		localRepoManagerRemote.putRemoteRepository(localRepoManagerLocal.getRepositoryId(), null, localRepoManagerLocal.getPublicKey(), remotePathPrefix);

		final File child_1 = createDirectory(remoteRoot, "1");
		final File file_1a = createFileWithRandomContent(child_1, "a");

		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));
		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());

		// This sync does down+up+down to make sure, everything is synced bidirectionally.
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);
		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());

		// The issue https://github.com/cloudstore/cloudstore/issues/25 was that when a file was modified
		// between the down- and the up-sync (that happens inside the normal sync(...) method), a wrong
		// collision was detected. So we do the steps normally happening in sync(...) individually here
		// and change the file inbetween.

		// First change the *remote* file and perform a down-sync.
		modifyFileRandomly(file_1a);
		repoToRepoSync.syncDown(true, new LoggerProgressMonitor(logger));

		// Then change the same file again at the same (remote) location.
		modifyFileRandomly(file_1a);

		// Now perform the up-sync that would normally happen. This should cause the wrong collision of issue #25.
		localRepoManagerLocal.localSync(new NullProgressMonitor()); // We make 100% sure, the local DB is up-to-date, before up-sync.
		repoToRepoSync.syncUp(new LoggerProgressMonitor(logger));

		// Now we sync down again. This is not really important for this test, but it usually happens in the
		// ordinary sync(...) method. This btw. syncs the collision file down (if there is one).
		// Additionally, without this down-sync, we cannot use assertDirectoriesAreEqualRecursively(...).
		repoToRepoSync.syncDown(true, new LoggerProgressMonitor(logger));

		repoToRepoSync.close();

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(remoteRoot);
		assertThatNoCollisionInRepo(localRoot);

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	private void modifyFileRandomly(final File file) throws IOException {
		try {
			//TODO get rid of that sleep; needed, because *nix system are rounding to the next second.
			// Setting via file.setLastModified(time) did not solve the problem.
			Thread.sleep(1100);
		} catch (final InterruptedException e) {
			logger.error("Interrupted!", e);
		}
		final RandomAccessFile raf = new RandomAccessFile(file, "rw");
		try {
			if (file.length() > 0)
				raf.seek(random.nextInt((int)file.length()));

			final byte[] buf = new byte[1 + random.nextInt(10)];
			random.nextBytes(buf);

			raf.write(buf);
		} finally {
			raf.close();
		}
	}

	@Test
	public void syncFromRemoteToLocalWithDeletedFile() throws Exception {
		syncFromRemoteToLocal();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final File child_2 = newFile(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		final File child_2_1 = newFile(child_2, "1");
		assertThat(child_2_1).isDirectory();

		final File child_2_1_a = newFile(child_2_1, "a");
		assertThat(child_2_1_a).isFile();

		deleteFile(child_2_1_a);

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncFromRemoteToLocalWithDeletedDir() throws Exception {
		syncFromRemoteToLocal();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final File child_2 = newFile(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		final File child_2_1 = newFile(child_2, "1");
		assertThat(child_2_1).isDirectory();

		for (final File child : child_2_1.listFiles()) {
			deleteFile(child);
		}

		for (final File child : child_2.listFiles()) {
			deleteFile(child);
		}

		deleteFile(child_2);

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);

		File deleteThisFile = null;
		try {
			if (!getRemoteRootWithPathPrefix().exists()) {
				getRemoteRootWithPathPrefix().mkdirs();
				assertThat(getRemoteRootWithPathPrefix()).isDirectory();
				deleteThisFile = getRemoteRootWithPathPrefix();
			}

			assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
		} finally {
			// Just for manual checks of the directories: This should not exist. It's confusing, if it does.
			if (deleteThisFile != null)
				deleteThisFile.delete();
		}
	}

	@Test
	public void syncWithDirectFileModificationCollision() throws Exception {
		syncFromRemoteToLocal();

		final File r_child_2 = newFile(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		final File r_child_2_1 = newFile(r_child_2, "1");
		assertThat(r_child_2_1).isDirectory();

		final File r_child_2_1_a = newFile(r_child_2_1, "a");
		assertThat(r_child_2_1_a).isFile();

		final File l_child_2 = newFile(localRoot, "2");
		assertThat(l_child_2).isDirectory();

		final File l_child_2_1 = newFile(l_child_2, "1");
		assertThat(l_child_2_1).isDirectory();

		final File l_child_2_1_a = newFile(l_child_2_1, "a");
		assertThat(l_child_2_1_a).isFile();

		modifyFileRandomly(r_child_2_1_a);
		modifyFileRandomly(l_child_2_1_a);

		for (int i = 0; i < 2; ++i) { // We have to sync twice to make sure the collision file is synced, too (it is created during the first sync).
			final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			repoToRepoSync.close();
		}

		// Expect exactly one collision in remote repo (in directory r_child_2_1).
		final List<File> remoteCollisions = searchCollisions(remoteRoot);
		assertThat(remoteCollisions).isNotNull().hasSize(1);
		final File r_collision = remoteCollisions.get(0);
		assertThat(r_collision).isNotNull();
		assertThat(r_collision.getParentFile()).isEqualTo(r_child_2_1);

		// Expect exactly one collision in local repo (in directory l_child_2_1).
		final List<File> localCollisions = searchCollisions(localRoot);
		assertThat(localCollisions).isNotNull().hasSize(1);
		final File l_collision = localCollisions.get(0);
		assertThat(l_collision).isNotNull();
		assertThat(l_collision.getParentFile()).isEqualTo(l_child_2_1);

		addToFilesInRepo(remoteRoot, r_collision);

		assertThatFilesInRepoAreCorrect(remoteRoot);

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	private void assertThatNoCollisionInRepo(final File localRoot) {
		final List<File> collisions = searchCollisions(localRoot);
		if (!collisions.isEmpty())
			Assert.fail("Collision: " + collisions.get(0));
	}

	private List<File> searchCollisions(final File localRoot) {
		final List<File> collisions = new ArrayList<File>();
		searchCollisions_populate(localRoot, localRoot, collisions);
		return collisions;
	}

	private void searchCollisions_populate(final File localRoot, final File file, final Collection<File> collisions) {
		final File[] children = file.listFiles();
		if (children != null) {
			for (final File f : children) {
				if (f.getName().contains(IOUtil.COLLISION_FILE_NAME_INFIX))
					collisions.add(f);

				searchCollisions_populate(localRoot, f, collisions);
			}
		}
	}

	@Test
	public void syncWithFileModificationInsideDeletedDirectoryCollision() throws Exception {
		syncFromRemoteToLocal();

		final File r_child_2 = newFile(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		final File l_child_2 = newFile(localRoot, "2");
		assertThat(l_child_2).isDirectory();

		final File l_child_2_1 = newFile(l_child_2, "1");
		assertThat(l_child_2_1).isDirectory();

		final File l_child_2_1_a = newFile(l_child_2_1, "a");
		assertThat(l_child_2_1_a).isFile();

		modifyFileRandomly(l_child_2_1_a);
		IOUtil.deleteDirectoryRecursively(r_child_2);

		for (int i = 0; i < 2; ++i) { // We have to sync twice to make sure the collision is synced, too (it is created during the first sync).
			final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			repoToRepoSync.close();
		}

		final List<File> remoteCollisions = searchCollisions(remoteRoot);
		assertThat(remoteCollisions).isNotNull().hasSize(1);
		final File r_collision = remoteCollisions.get(0);
		assertThat(r_collision).isNotNull();
		assertThat(r_collision.getParentFile()).isEqualTo(remoteRoot);
		assertThat(r_collision.getName()).startsWith("2.");

		final List<File> localCollisions = searchCollisions(localRoot);
		assertThat(localCollisions).isNotNull().hasSize(1);
		final File l_collision = localCollisions.get(0);
		assertThat(l_collision).isNotNull();
		assertThat(l_collision.getParentFile()).isEqualTo(localRoot);
		assertThat(l_collision.getName()).startsWith("2.");
	}

	@Test
	public void syncWithFileModificationInsideDeletedDirectoryCollisionInverse() throws Exception {
		syncFromRemoteToLocal();

		final File r_child_2 = newFile(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		final File r_child_2_1 = newFile(r_child_2, "1");
		assertThat(r_child_2_1).isDirectory();

		final File r_child_2_1_a = newFile(r_child_2_1, "a");
		assertThat(r_child_2_1_a).isFile();

		final File l_child_2 = newFile(localRoot, "2");
		assertThat(l_child_2).isDirectory();

		modifyFileRandomly(r_child_2_1_a);
		IOUtil.deleteDirectoryRecursively(l_child_2);

		for (int i = 0; i < 2; ++i) { // We have to sync twice to make sure the collision is synced, too (it is created during the first sync).
			final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			repoToRepoSync.close();
		}

		final List<File> remoteCollisions = searchCollisions(remoteRoot);
		assertThat(remoteCollisions).isNotNull().hasSize(1);
		final File r_collision = remoteCollisions.get(0);
		assertThat(r_collision).isNotNull();
		assertThat(r_collision.getParentFile()).isEqualTo(remoteRoot);
		assertThat(r_collision.getName()).startsWith("2.");

		final List<File> localCollisions = searchCollisions(localRoot);
		assertThat(localCollisions).isNotNull().hasSize(1);
		final File l_collision = localCollisions.get(0);
		assertThat(l_collision).isNotNull();
		assertThat(l_collision.getParentFile()).isEqualTo(localRoot);
		assertThat(l_collision.getName()).startsWith("2.");
	}

// TODO test this collision:
//	@Test
//	public void syncWithDirectFileDeletionCollision() throws Exception {
//
//	}

	@Test
	public void syncMovedFile() throws Exception {
		syncFromRemoteToLocal();

		final File r_child_2 = newFile(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		final File r_child_2_1 = newFile(r_child_2, "1");
		assertThat(r_child_2_1).isDirectory();

		final File r_child_2_1_b = newFile(r_child_2_1, "b");
		assertThat(r_child_2_1_b).isFile();

		final File r_child_2_b = newFile(r_child_2, "b");
		assertThat(r_child_2_b).doesNotExist();

		Files.move(r_child_2_1_b.toPath(), r_child_2_b.toPath());
		assertThat(r_child_2_1_b).doesNotExist();
		assertThat(r_child_2_b).isFile();

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThat(r_child_2_1_b).doesNotExist();
		assertThat(r_child_2_b).isFile();

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncMovedFileToNewDir() throws Exception {
		syncFromRemoteToLocal();

		final File r_child_2 = newFile(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		final File r_child_2_1 = newFile(r_child_2, "1");
		assertThat(r_child_2_1).isDirectory();

		final File r_child_2_1_b = newFile(r_child_2_1, "b");
		assertThat(r_child_2_1_b).isFile();

		final File r_child_2_new = newFile(r_child_2, "new");
		assertThat(r_child_2_new).doesNotExist();
		r_child_2_new.mkdir();
		assertThat(r_child_2_new).isDirectory();

		final File r_child_2_new_xxx = newFile(r_child_2_new, "xxx");

		Files.move(r_child_2_1_b.toPath(), r_child_2_new_xxx.toPath());
		assertThat(r_child_2_1_b).doesNotExist();
		assertThat(r_child_2_new_xxx).isFile();

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThat(r_child_2_1_b).doesNotExist();
		assertThat(r_child_2_new_xxx).isFile();

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncSymlinkFile() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		localRepoManagerLocal.putRemoteRepository(localRepoManagerRemote.getRepositoryId(), getRemoteRootUrlWithPathPrefix(), localRepoManagerRemote.getPublicKey(), localPathPrefix);
		localRepoManagerRemote.putRemoteRepository(localRepoManagerLocal.getRepositoryId(), null, localRepoManagerLocal.getPublicKey(), remotePathPrefix);

		final File child_1 = createDirectory(remoteRoot, "1");

		final File child_1_a = createFileWithRandomContent(child_1, "a");
		final File b = createRelativeSymlink(newFile(child_1, "b"), child_1_a);

		final File broken = createRelativeSymlink(newFile(child_1, "broken"), newFile(child_1, "doesNotExist"));

		final long child_1_a_lastModified = System.currentTimeMillis() - (24L * 3600);
		final long symlink_b_lastModified = System.currentTimeMillis() - (3L * 3600);
		final long symlink_broken_lastModified = System.currentTimeMillis() - (7L * 3600);

		IOUtil.setLastModifiedNoFollow(child_1_a, child_1_a_lastModified);
		assertThat(IOUtil.getLastModifiedNoFollow(child_1_a)).isBetween(child_1_a_lastModified - 2000, child_1_a_lastModified + 2000);

		IOUtil.setLastModifiedNoFollow(b, symlink_b_lastModified);
		assertThat(IOUtil.getLastModifiedNoFollow(b)).isBetween(symlink_b_lastModified - 2000, symlink_b_lastModified + 2000);

		// Assert that changing the symlink's timestamp did not affect the real file.
		assertThat(IOUtil.getLastModifiedNoFollow(child_1_a)).isBetween(child_1_a_lastModified - 2000, child_1_a_lastModified + 2000);

		IOUtil.setLastModifiedNoFollow(broken, symlink_broken_lastModified);
		assertThat(IOUtil.getLastModifiedNoFollow(broken)).isBetween(symlink_broken_lastModified - 2000, symlink_broken_lastModified + 2000);

		localRepoManagerRemote.localSync(new NullProgressMonitor());
		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);

		assertThatFilesInRepoAreCorrect(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncFromRemoteToLocalWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncFromRemoteToLocal();
	}

	@Test
	public void syncFromRemoteToLocalWithAddedFilesAndDirectoriesWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncFromRemoteToLocalWithAddedFilesAndDirectories();
	}

	@Test
	public void syncFromRemoteToLocalWithModifiedFilesWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncFromRemoteToLocalWithModifiedFiles();
	}

	@Test
	public void syncFromRemoteToLocalWithDeletedFileWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncFromRemoteToLocalWithDeletedFile();
	}

	@Test
	public void syncFromRemoteToLocalWithDeletedDirWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncFromRemoteToLocalWithDeletedDir();
	}

	@Test
	public void syncRemoteRootToLocalRootWithDeletedDirWithRemotePathPrefix_parentOfVirtualRootDeleted() throws Exception {
		remotePathPrefix = "/2/1";
		syncFromRemoteToLocalWithDeletedDir();
	}

	@Test
	public void syncMovedFileWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncMovedFile();
	}

	@Test
	public void syncMovedFileToNewDirWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncMovedFileToNewDir();
	}
}
