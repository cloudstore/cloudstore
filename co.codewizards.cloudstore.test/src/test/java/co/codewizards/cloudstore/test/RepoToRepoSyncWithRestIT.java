package co.codewizards.cloudstore.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;

public class RepoToRepoSyncWithRestIT extends AbstractRepoAwareIT
{
	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncWithRestIT.class);


	@Override
	@Before
	public void before() {
		localPathPrefix = "";
		remotePathPrefix = "";
	}

	@Test
	public void syncFromRemoteToLocal() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix(remoteRepositoryId);

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefix.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix().getPath()).execute();

		final File child_1 = createDirectory(remoteRoot, "1 {11 11ä11#+} 1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(remoteRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1 {11 11ä11#+} 1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		final File child_3 = createDirectory(remoteRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		// special characters: fileNames must not be encoded.
		final File child_4 = createDirectory(remoteRoot, "#4");

		createFileWithRandomContent(child_4, "a");
		createFileWithRandomContent(child_4, "#b");
		createFileWithRandomContent(child_4, "c+");
		createFileWithRandomContent(child_4, "d$");

		final File child_5 = createDirectory(remoteRoot, "5#");
		createFileWithRandomContent(child_5, "e");

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
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
	public void syncFromLocalToRemote() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix(remoteRepositoryId);

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefix.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix().getPath()).execute();

		final File child_1 = createDirectory(localRoot, "1 {11 11ä11#+} 1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(localRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1 {11 11ä11#+} 1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		final File child_3 = createDirectory(localRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(localRoot);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncWithFileModificationInsideDeletedDirectoryCollision() throws Exception {
		syncFromRemoteToLocal();

		final File r_child_2 = createFile(remoteRoot, "2");
		assertThat(r_child_2.isDirectory()).isTrue();

		final File l_child_2 = createFile(localRoot, "2");
		assertThat(l_child_2.isDirectory()).isTrue();

		final File l_child_2_1 = createFile(l_child_2, "1 {11 11ä11#+} 1");
		assertThat(l_child_2_1.isDirectory()).isTrue();

		final File l_child_2_1_a = createFile(l_child_2_1, "a");
		assertThat(l_child_2_1_a.isFile()).isTrue();

		modifyFileRandomly(l_child_2_1_a);
		r_child_2.deleteRecursively();

		for (int i = 0; i < 2; ++i) { // We have to sync twice to make sure the collision is synced, too (it is created during the first sync).
			final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRootURLWithPathPrefix);
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

		final File r_child_2 = createFile(remoteRoot, "2");
		assertThat(r_child_2.isDirectory()).isTrue();

		final File r_child_2_1 = createFile(r_child_2, "1 {11 11ä11#+} 1");
		assertThat(r_child_2_1.isDirectory()).isTrue();

		final File r_child_2_1_a = createFile(r_child_2_1, "a");
		assertThat(r_child_2_1_a.isFile()).isTrue();

		final File l_child_2 = createFile(localRoot, "2");
		assertThat(l_child_2.isDirectory()).isTrue();

		modifyFileRandomly(r_child_2_1_a);
		l_child_2.deleteRecursively();

		for (int i = 0; i < 2; ++i) { // We have to sync twice to make sure the collision is synced, too (it is created during the first sync).
			final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRootURLWithPathPrefix);
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

	private void modifyFileRandomly(final File file) throws IOException {
		final RandomAccessFile raf = file.createRandomAccessFile("rw");
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
	public void syncFromRemoteToLocalWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncFromRemoteToLocal();
	}

	@Test
	public void syncFromRemoteToLocalWithRemotePathPrefix_specialChar() throws Exception {
		remotePathPrefix = "/#4";
		syncFromRemoteToLocal();
	}
	@Test
	public void syncFromRemoteToLocalWithRemotePathPrefix_specialChar2() throws Exception {
		remotePathPrefix = "/5#";
		syncFromRemoteToLocal();
	}

	@Test
	public void syncMovedFile() throws Exception {
		syncFromRemoteToLocal();

		final File r_child_2 = createFile(remoteRoot, "2");
		assertThat(r_child_2.isDirectory()).isTrue();

		final File r_child_2_1 = createFile(r_child_2, "1 {11 11ä11#+} 1");
		assertThat(r_child_2_1.isDirectory()).isTrue();

		final File r_child_2_1_b = createFile(r_child_2_1, "b");
		assertThat(r_child_2_1_b.isFile()).isTrue();

		final File r_child_2_b = createFile(r_child_2, "b");
		assertThat(r_child_2_b.exists()).isFalse();

		r_child_2_1_b.move(r_child_2_b);
		assertThat(r_child_2_1_b.exists()).isFalse();
		assertThat(r_child_2_b.isFile()).isTrue();

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThat(r_child_2_1_b.exists()).isFalse();
		assertThat(r_child_2_b.isFile()).isTrue();

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncMovedFileToNewDir() throws Exception {
		syncFromRemoteToLocal();

		final File r_child_2 = createFile(remoteRoot, "2");
		assertThat(r_child_2.isDirectory()).isTrue();

		final File r_child_2_1 = createFile(r_child_2, "1 {11 11ä11#+} 1");
		assertThat(r_child_2_1.isDirectory()).isTrue();

		final File r_child_2_1_b = createFile(r_child_2_1, "b");
		assertThat(r_child_2_1_b.isFile()).isTrue();

		final File r_child_2_new = createFile(r_child_2, "new");
		assertThat(r_child_2_new.exists()).isFalse();
		r_child_2_new.mkdir();
		assertThat(r_child_2_new.isDirectory()).isTrue();

		final File r_child_2_new_xxx = createFile(r_child_2_new, "xxx");

		r_child_2_1_b.move(r_child_2_new_xxx);
		assertThat(r_child_2_1_b.exists()).isFalse();
		assertThat(r_child_2_new_xxx.isFile()).isTrue();

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThat(r_child_2_1_b.exists()).isFalse();
		assertThat(r_child_2_new_xxx.isFile()).isTrue();

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncSymlinkFileDown() throws Exception {
		createLocalAndRemoteRepo();
		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		final File child_1 = createDirectory(remoteRoot, "1");

		final File child_1_a = createFileWithRandomContent(child_1, "a");
		createRelativeSymlink(createFile(child_1, "b"), child_1_a);

		createRelativeSymlink(createFile(child_1, "broken"), createFile(child_1, "doesNotExist"));

		localRepoManagerRemote.localSync(new NullProgressMonitor());
		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();
		localRepoManagerRemote.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	private void createLocalAndRemoteRepo() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix(remoteRepositoryId);

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefix.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix().getPath()).execute();

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();
	}

	@Test
	public void syncSymlinkFileUp() throws Exception {
		createLocalAndRemoteRepo();
		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);

		final File child_1 = createDirectory(localRoot, "1");

		final File child_1_a = createFileWithRandomContent(child_1, "a");
		createRelativeSymlink(createFile(child_1, "b"), child_1_a);

		createRelativeSymlink(createFile(child_1, "broken"), createFile(child_1, "doesNotExist"));

		localRepoManagerLocal.localSync(new NullProgressMonitor());
		assertThatFilesInRepoAreCorrect(localRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();
		localRepoManagerLocal.close();

		assertThatFilesInRepoAreCorrect(localRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncFileWithSpecialChars() throws Exception {
		createLocalAndRemoteRepo();
		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);

		final File child_1 = createDirectory(localRoot, "A ä Ä { + } x ( << ))]] [ #");

		final File child_1_a = createFileWithRandomContent(child_1, "Öü ß ? # + {{ d d } (( > << ]] )");
		createRelativeSymlink(createFile(child_1, "Öü ß ? # + {{ d d } (( > << ]] ).new"), child_1_a);

		localRepoManagerLocal.localSync(new NullProgressMonitor());
		assertThatFilesInRepoAreCorrect(localRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();
		localRepoManagerLocal.close();

		assertThatFilesInRepoAreCorrect(localRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
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
