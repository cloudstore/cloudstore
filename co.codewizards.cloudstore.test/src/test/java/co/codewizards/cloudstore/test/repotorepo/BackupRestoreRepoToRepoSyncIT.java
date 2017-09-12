package co.codewizards.cloudstore.test.repotorepo;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.util.IOUtil;

public class BackupRestoreRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {
	private static final Logger logger = LoggerFactory.getLogger(BackupRestoreRepoToRepoSyncIT.class);

	@BeforeClass
	public static void backupRestoreRepoToRepoSyncIT_beforeClass() {
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS, "0");
	}

	@AfterClass
	public static void backupRestoreRepoToRepoSyncIT_afterClass() {
		System.clearProperty(LocalRepoManager.SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS);
	}

	@Test
	public void backupRestoreClientRepo() throws Exception {
		// First set up 2 repos (local = client, remote = server) using the super-class-scenario.
		syncFromRemoteToLocal();

		// Create a new file in the remote (= server) repository. This file is not yet synced,
		// hence it exists only remotely -- not locally.
		final File remote_child_3 = remoteRoot.createFile("3");
		createFileWithRandomContent(remote_child_3, "zzz");

		final File local_child_3 = localRoot.createFile("3");
		File local_zzz = local_child_3.createFile("zzz");

		// Close all LocalRepoManager instances, before creating the backup!
		// We must close this stuff after creating the test file, because the super-class' file creation code
		// checks, whether we're inside an open repository.
		repoToRepoSync.close();
		repoToRepoSync = null;

		localRepoManagerLocal.close();
		localRepoManagerLocal = null;

		localRepoManagerRemote.close();
		localRepoManagerRemote = null;

		// Create a backup by simply copying the directory recursively.
		File localRootBackup = createFile(getTestRepositoryBaseDir(), localRoot.getName() + ".bak");
		logger.info("************************************************************************************");
		logger.info("Creating backup: '{}' => '{}'", localRoot, localRootBackup);
		IOUtil.copyDirectory(localRoot, localRootBackup);
		logger.info("Created backup: '{}' => '{}'", localRoot, localRootBackup);
		logger.info("************************************************************************************");

		// Now, check whether the scenario is correct, before the sync, i.e.
		// the new test-file "/3/zzz" does not exist locally (yet)!
		assertThat(local_zzz.existsNoFollow()).isFalse();

		// Synchronize -- this should download the test-file "/3/zzz" from the server to the client.
		repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		// Close the LocalRepoManager instances so that we can safely restore the backup.
		repoToRepoSync.close();
		repoToRepoSync = null;

		// Check, whether the test-file "/3/zzz" was correctly synced down.
		assertDirectoriesAreEqualRecursively(localRoot, remoteRoot);

		// Restore the backup, so that "/3/zzz" does not exist locally, again.
		logger.info("************************************************************************************");
		logger.info("Restoring backup: '{}' => '{}'", localRootBackup, localRoot);
		localRoot.deleteRecursively();
		IOUtil.copyDirectory(localRootBackup, localRoot);
		logger.info("Restored backup: '{}' => '{}'", localRootBackup, localRoot);
		logger.info("************************************************************************************");

		// Check, whether the test-file "/3/zzz" really does not exist locally.
		assertThat(local_zzz.existsNoFollow()).isFalse();

		// Sync again -- the test-file should be downloaded again, after
		// https://github.com/cloudstore/cloudstore/issues/67 was implemented.
		repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		// Check, whether the test-file "/3/zzz" was correctly synced down.
		assertDirectoriesAreEqualRecursively(localRoot, remoteRoot);
	}

	@Test
	public void backupRestoreServerRepo() throws Exception {
		// First set up 2 repos (local = client, remote = server) using the super-class-scenario.
		syncFromRemoteToLocal();

		// Create a new file in the local (= client) repository. This file is not yet synced,
		// hence it exists only locally -- not remotely.
		final File remote_child_3 = remoteRoot.createFile("3");
		File remote_zzz = remote_child_3.createFile("zzz");

		final File local_child_3 = localRoot.createFile("3");
		createFileWithRandomContent(local_child_3, "zzz");

		// Close all LocalRepoManager instances, before creating the backup!
		// We must close this stuff after creating the test file, because the super-class' file creation code
		// checks, whether we're inside an open repository.
		repoToRepoSync.close();
		repoToRepoSync = null;

		localRepoManagerLocal.close();
		localRepoManagerLocal = null;

		localRepoManagerRemote.close();
		localRepoManagerRemote = null;

		// Create a backup by simply copying the directory recursively.
		File remoteRootBackup = createFile(getTestRepositoryBaseDir(), remoteRoot.getName() + ".bak");
		logger.info("************************************************************************************");
		logger.info("Creating backup: '{}' => '{}'", remoteRoot, remoteRootBackup);
		IOUtil.copyDirectory(remoteRoot, remoteRootBackup);
		logger.info("Created backup: '{}' => '{}'", remoteRoot, remoteRootBackup);
		logger.info("************************************************************************************");

		// Now, check whether the scenario is correct, before the sync, i.e.
		// the new test-file "/3/zzz" does not exist remotely (yet)!
		assertThat(remote_zzz.existsNoFollow()).isFalse();

		// Synchronize -- this should upload the test-file "/3/zzz" from the client to the server.
		repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		// Close the LocalRepoManager instances so that we can safely restore the backup.
		repoToRepoSync.close();
		repoToRepoSync = null;

		// Check, whether the test-file "/3/zzz" was correctly synced up.
		assertDirectoriesAreEqualRecursively(remoteRoot, localRoot);

		// Restore the backup, so that "/3/zzz" does not exist remotely, again.
		logger.info("************************************************************************************");
		logger.info("Restoring backup: '{}' => '{}'", remoteRootBackup, remoteRoot);
		remoteRoot.deleteRecursively();
		IOUtil.copyDirectory(remoteRootBackup, remoteRoot);
		logger.info("Restored backup: '{}' => '{}'", remoteRootBackup, remoteRoot);
		logger.info("************************************************************************************");

		// Check, whether the test-file "/3/zzz" really does not exist remotely.
		assertThat(remote_zzz.existsNoFollow()).isFalse();

		// Sync again -- the test-file should be uploaded again, after
		// https://github.com/cloudstore/cloudstore/issues/67 was implemented.
		repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		// Check, whether the test-file "/3/zzz" was correctly synced up.
		assertDirectoriesAreEqualRecursively(remoteRoot, localRoot);
	}
}
