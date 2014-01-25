package co.codewizards.cloudstore.core.repo.sync;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.AbstractTest;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.IOUtil;

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

		return new File(localRoot, localPathPrefix);
	}

	private File getRemoteRootWithPathPrefix() {
		if (remotePathPrefix.isEmpty())
			return remoteRoot;

		File file = new File(remoteRoot, remotePathPrefix);
		return file;
	}

	private URL getRemoteRootUrlWithPathPrefix() {
		try {
			URL url = getRemoteRootWithPathPrefix().toURI().toURL();
			return url;
		} catch (MalformedURLException e) {
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

		LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		localRepoManagerLocal.putRemoteRepository(localRepoManagerRemote.getRepositoryID(), getRemoteRootUrlWithPathPrefix(), localRepoManagerRemote.getPublicKey(), localPathPrefix);
		localRepoManagerRemote.putRemoteRepository(localRepoManagerLocal.getRepositoryID(), null, localRepoManagerLocal.getPublicKey(), remotePathPrefix);

		File child_1 = createDirectory(remoteRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		File child_2 = createDirectory(remoteRoot, "2");

		createFileWithRandomContent(child_2, "a");

		File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		File child_3 = createDirectory(remoteRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		logger.info("local repo: {}", localRepoManagerLocal.getRepositoryID());
		logger.info("remote repo: {}", localRepoManagerRemote.getRepositoryID());

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
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

		LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		File child_2 = new File(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		File child_2_1 = new File(child_2, "1");
		assertThat(child_2_1).isDirectory();

		File child_2_1_5 = createDirectory(child_2_1, "5");
		createFileWithRandomContent(child_2_1_5, "aaa");
		createFileWithRandomContent(child_2_1_5, "bbb");

		File child_3 = new File(remoteRoot, "3");
		assertThat(child_3).isDirectory();

		createFileWithRandomContent(child_3, "e");

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
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

		LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		File child_2 = new File(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		File child_2_1 = new File(child_2, "1");
		assertThat(child_2_1).isDirectory();

		File child_2_1_a = new File(child_2_1, "a");
		assertThat(child_2_1_a).isFile();

		File child_2_1_b = new File(child_2_1, "b");
		assertThat(child_2_1_b).isFile();

		modifyFileRandomly(child_2_1_a);

		logger.info("file='{}' length={}", child_2_1_b, child_2_1_b.length());

		FileOutputStream out = new FileOutputStream(child_2_1_b);
		out.write(random.nextInt());
		out.close();

		logger.info("file='{}' length={}", child_2_1_b, child_2_1_b.length());

		byte[] child_2_1_a_expected = IOUtil.getBytesFromFile(child_2_1_a);
		byte[] child_2_1_b_expected = IOUtil.getBytesFromFile(child_2_1_b);

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());

		// ensure that nothing was synced backwards into the wrong direction ;-)
		byte[] child_2_1_a_actual = IOUtil.getBytesFromFile(child_2_1_a);
		byte[] child_2_1_b_actual = IOUtil.getBytesFromFile(child_2_1_b);
		assertThat(child_2_1_a_actual).isEqualTo(child_2_1_a_expected);
		assertThat(child_2_1_b_actual).isEqualTo(child_2_1_b_expected);
	}

	private void modifyFileRandomly(File file) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		try {
			if (file.length() > 0)
				raf.seek(random.nextInt((int)file.length()));

			byte[] buf = new byte[1 + random.nextInt(10)];
			random.nextBytes(buf);

			raf.write(buf);
		} finally {
			raf.close();
		}
	}

	@Test
	public void syncFromRemoteToLocalWithDeletedFile() throws Exception {
		syncFromRemoteToLocal();

		LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		File child_2 = new File(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		File child_2_1 = new File(child_2, "1");
		assertThat(child_2_1).isDirectory();

		File child_2_1_a = new File(child_2_1, "a");
		assertThat(child_2_1_a).isFile();

		deleteFile(child_2_1_a);

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
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

		LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		File child_2 = new File(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		File child_2_1 = new File(child_2, "1");
		assertThat(child_2_1).isDirectory();

		for (File child : child_2_1.listFiles()) {
			deleteFile(child);
		}

		for (File child : child_2.listFiles()) {
			deleteFile(child);
		}

		deleteFile(child_2);

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
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

		File r_child_2 = new File(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		File r_child_2_1 = new File(r_child_2, "1");
		assertThat(r_child_2_1).isDirectory();

		File r_child_2_1_a = new File(r_child_2_1, "a");
		assertThat(r_child_2_1_a).isFile();

		File l_child_2 = new File(localRoot, "2");
		assertThat(l_child_2).isDirectory();

		File l_child_2_1 = new File(l_child_2, "1");
		assertThat(l_child_2_1).isDirectory();

		File l_child_2_1_a = new File(l_child_2_1, "a");
		assertThat(l_child_2_1_a).isFile();

		modifyFileRandomly(r_child_2_1_a);
		modifyFileRandomly(l_child_2_1_a);

		for (int i = 0; i < 2; ++i) { // We have to sync twice to make sure the collision file is synced, too (it is created during the first sync).
			RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			repoToRepoSync.close();
		}

		// Expect exactly one collision in remote repo (in directory r_child_2_1).
		List<File> remoteCollisions = searchCollisions(remoteRoot);
		assertThat(remoteCollisions).isNotNull().hasSize(1);
		File r_collision = remoteCollisions.get(0);
		assertThat(r_collision).isNotNull();
		assertThat(r_collision.getParentFile()).isEqualTo(r_child_2_1);

		// Expect exactly one collision in local repo (in directory l_child_2_1).
		List<File> localCollisions = searchCollisions(localRoot);
		assertThat(localCollisions).isNotNull().hasSize(1);
		File l_collision = localCollisions.get(0);
		assertThat(l_collision).isNotNull();
		assertThat(l_collision.getParentFile()).isEqualTo(l_child_2_1);

		addToFilesInRepo(remoteRoot, r_collision);

		assertThatFilesInRepoAreCorrect(remoteRoot);

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	private void assertThatNoCollisionInRepo(File localRoot) {
		List<File> collisions = searchCollisions(localRoot);
		if (!collisions.isEmpty())
			Assert.fail("Collision: " + collisions.get(0));
	}

	private List<File> searchCollisions(File localRoot) {
		List<File> collisions = new ArrayList<File>();
		searchCollisions_populate(localRoot, localRoot, collisions);
		return collisions;
	}

	private void searchCollisions_populate(File localRoot, File file, Collection<File> collisions) {
		File[] children = file.listFiles();
		if (children != null) {
			for (File f : children) {
				if (f.getName().contains(IOUtil.COLLISION_FILE_NAME_INFIX))
					collisions.add(f);

				searchCollisions_populate(localRoot, f, collisions);
			}
		}
	}

	@Test
	public void syncWithFileModificationInsideDeletedDirectoryCollision() throws Exception {
		syncFromRemoteToLocal();

		File r_child_2 = new File(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		File l_child_2 = new File(localRoot, "2");
		assertThat(l_child_2).isDirectory();

		File l_child_2_1 = new File(l_child_2, "1");
		assertThat(l_child_2_1).isDirectory();

		File l_child_2_1_a = new File(l_child_2_1, "a");
		assertThat(l_child_2_1_a).isFile();

		modifyFileRandomly(l_child_2_1_a);
		IOUtil.deleteDirectoryRecursively(r_child_2);

		for (int i = 0; i < 2; ++i) { // We have to sync twice to make sure the collision is synced, too (it is created during the first sync).
			RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			repoToRepoSync.close();
		}

		List<File> remoteCollisions = searchCollisions(remoteRoot);
		assertThat(remoteCollisions).isNotNull().hasSize(1);
		File r_collision = remoteCollisions.get(0);
		assertThat(r_collision).isNotNull();
		assertThat(r_collision.getParentFile()).isEqualTo(remoteRoot);
		assertThat(r_collision.getName()).startsWith("2.");

		List<File> localCollisions = searchCollisions(localRoot);
		assertThat(localCollisions).isNotNull().hasSize(1);
		File l_collision = localCollisions.get(0);
		assertThat(l_collision).isNotNull();
		assertThat(l_collision.getParentFile()).isEqualTo(localRoot);
		assertThat(l_collision.getName()).startsWith("2.");
	}

	@Test
	public void syncWithFileModificationInsideDeletedDirectoryCollisionInverse() throws Exception {
		syncFromRemoteToLocal();

		File r_child_2 = new File(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		File r_child_2_1 = new File(r_child_2, "1");
		assertThat(r_child_2_1).isDirectory();

		File r_child_2_1_a = new File(r_child_2_1, "a");
		assertThat(r_child_2_1_a).isFile();

		File l_child_2 = new File(localRoot, "2");
		assertThat(l_child_2).isDirectory();

		modifyFileRandomly(r_child_2_1_a);
		IOUtil.deleteDirectoryRecursively(l_child_2);

		for (int i = 0; i < 2; ++i) { // We have to sync twice to make sure the collision is synced, too (it is created during the first sync).
			RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), getRemoteRootUrlWithPathPrefix());
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
			repoToRepoSync.close();
		}

		List<File> remoteCollisions = searchCollisions(remoteRoot);
		assertThat(remoteCollisions).isNotNull().hasSize(1);
		File r_collision = remoteCollisions.get(0);
		assertThat(r_collision).isNotNull();
		assertThat(r_collision.getParentFile()).isEqualTo(remoteRoot);
		assertThat(r_collision.getName()).startsWith("2.");

		List<File> localCollisions = searchCollisions(localRoot);
		assertThat(localCollisions).isNotNull().hasSize(1);
		File l_collision = localCollisions.get(0);
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

}
