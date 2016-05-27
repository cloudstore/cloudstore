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

public class SpecialCharsRepoToRepoSyncIT extends AbstractRepoAwareIT
{
	private static final Logger logger = LoggerFactory.getLogger(SpecialCharsRepoToRepoSyncIT.class);

	@Test
	public void syncFileWithSpecialChars() throws Exception {
		createLocalAndRemoteRepo();
		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);

		final File child_1 = createDirectory(localRoot, "A ä Ä { + } x ( << ))]] [ #");

		final File child_1_a = createFileWithRandomContent(child_1, "Öü ß ? # + {{ d d } (( > << ]] )");
		createRelativeSymlink(createFile(child_1, "Öü ß ? # + {{ d d } (( > << ]] ).new"), child_1_a);

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
