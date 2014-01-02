package co.codewizards.cloudstore.shared.repo.sync;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.AbstractTest;
import co.codewizards.cloudstore.shared.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.shared.repo.local.LocalRepoManager;

public class RepoSyncTest extends AbstractTest {
	private static Logger logger = LoggerFactory.getLogger(RepoSyncTest.class);

	private File localRoot;
	private File remoteRoot;

	@Test
	public void syncRemoteRootToLocalRootInitially() throws Exception {
		localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();

		remoteRoot = newTestRepositoryLocalRoot();
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();
		URL remoteRootURL = remoteRoot.toURI().toURL();

		LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		localRepoManagerLocal.addRemoteRepository(localRepoManagerRemote.getLocalRepositoryID(), remoteRootURL);

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

		RepoSync repoSync = new RepoSync(localRoot, remoteRootURL);
		repoSync.sync(new LoggerProgressMonitor(logger));

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		// TODO compare directory graphs of localRoot and remoteRoot!
	}


}
