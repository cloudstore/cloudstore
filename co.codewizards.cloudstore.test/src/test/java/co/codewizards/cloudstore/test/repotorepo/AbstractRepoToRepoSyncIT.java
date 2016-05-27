package co.codewizards.cloudstore.test.repotorepo;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.test.AbstractRepoAwareIT;

/**
 * In contrast to the {@code RepoToRepoSyncTest}, this test (and all other tests in this package)
 * uses the REST transport.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public abstract class AbstractRepoToRepoSyncIT extends AbstractRepoAwareIT
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractRepoToRepoSyncIT.class);

	protected void syncFromRemoteToLocal() throws Exception {
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

		final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	protected void syncFromLocalToRemote() throws Exception {
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

		final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(localRoot);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertThatNoCollisionInRepo(localRoot);
		assertThatNoCollisionInRepo(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	protected void syncMovedFile() throws Exception {
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

		final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThat(r_child_2_1_b.exists()).isFalse();
		assertThat(r_child_2_b.isFile()).isTrue();

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	protected void syncMovedFileToNewDir() throws Exception {
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

		final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThat(r_child_2_1_b.exists()).isFalse();
		assertThat(r_child_2_new_xxx.isFile()).isTrue();

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

}
