package co.codewizards.cloudstore.test.repotorepo;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.test.AbstractRepoAwareIT;

public class SymlinkRepoToRepoSyncIT extends AbstractRepoAwareIT
{
	private static final Logger logger = LoggerFactory.getLogger(SymlinkRepoToRepoSyncIT.class);

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

		final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();
		localRepoManagerRemote.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
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

		final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();
		localRepoManagerLocal.close();

		assertThatFilesInRepoAreCorrect(localRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}
}
