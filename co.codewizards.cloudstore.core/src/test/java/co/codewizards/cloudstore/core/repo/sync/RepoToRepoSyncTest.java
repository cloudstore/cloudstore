package co.codewizards.cloudstore.core.repo.sync;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.AbstractTest;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class RepoToRepoSyncTest extends AbstractTest {
	private static Logger logger = LoggerFactory.getLogger(RepoToRepoSyncTest.class);

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

		localRepoManagerLocal.putRemoteRepository(localRepoManagerRemote.getLocalRepositoryID(), remoteRootURL);
		localRepoManagerRemote.putRemoteRepository(localRepoManagerLocal.getLocalRepositoryID(), null);

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

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRootURL);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

		assertDirectoriesAreEqualRecursively(localRoot, remoteRoot);
	}

	@Test
	public void syncRemoteRootToLocalRootWithAddedFilesAndDirectories() throws Exception {
		syncRemoteRootToLocalRootInitially();

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

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRoot.toURI().toURL());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertDirectoriesAreEqualRecursively(localRoot, remoteRoot);
	}

	@Test
	public void syncRemoteRootToLocalRootWithModifiedFile() throws Exception {
		syncRemoteRootToLocalRootInitially();

		LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		File child_2 = new File(remoteRoot, "2");
		assertThat(child_2).isDirectory();

		File child_2_1 = new File(child_2, "1");
		assertThat(child_2_1).isDirectory();

		File child_2_1_a = new File(child_2_1, "a");
		assertThat(child_2_1_a).isFile();

		RandomAccessFile raf = new RandomAccessFile(child_2_1_a, "rw");
		try {
			raf.seek(random.nextInt((int)child_2_1_a.length()));

			byte[] buf = new byte[1 + random.nextInt(10)];
			random.nextBytes(buf);

			raf.write(buf);
		} finally {
			raf.close();
		}

		localRepoManagerRemote.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRoot.toURI().toURL());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertDirectoriesAreEqualRecursively(localRoot, remoteRoot);
	}

	@Test
	public void syncRemoteRootToLocalRootWithDeletedFile() throws Exception {
		syncRemoteRootToLocalRootInitially();

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

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRoot.toURI().toURL());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertDirectoriesAreEqualRecursively(localRoot, remoteRoot);
	}

	@Test
	public void syncRemoteRootToLocalRootWithDeletedDir() throws Exception {
		syncRemoteRootToLocalRootInitially();

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

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRoot.toURI().toURL());
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThatFilesInRepoAreCorrect(remoteRoot);

		localRepoManagerRemote.close();

		assertDirectoriesAreEqualRecursively(localRoot, remoteRoot);
	}
}
