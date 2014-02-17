package co.codewizards.cloudstore.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.transport.RestRepoTransportFactory;

public class RepoToRepoSyncWithRestIT extends AbstractIT
{
	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncWithRestIT.class);

	private File localRoot;
	private File remoteRoot;

	private String localPathPrefix;
	private String remotePathPrefix;
	private URL remoteRootURLWithPathPrefix;

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

	public static class TestDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(CheckServerTrustedCertificateExceptionContext context) {
			CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			result.setTrusted(true);
			return result;
		}
	}

	private static RestRepoTransportFactory restRepoTransportFactory;

	@BeforeClass
	public static void beforeClass() throws Exception {
		restRepoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(RestRepoTransportFactory.class);
		restRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);
	}

	@AfterClass
	public static void afterClass() {
		restRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(null);
	}

	private URL getRemoteRootURLWithPathPrefix(UUID remoteRepositoryId) throws MalformedURLException {
		URL remoteRootURL = new URL("https://localhost:" + getSecurePort() + "/" + remoteRepositoryId + remotePathPrefix);
		return remoteRootURL;
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

		UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix(remoteRepositoryId);

		new CloudStoreClient().execute("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefix.toExternalForm());
		new CloudStoreClient().execute("acceptRepoConnection", getRemoteRootWithPathPrefix().getPath());

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

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
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

		UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix(remoteRepositoryId);

		new CloudStoreClient().execute("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefix.toExternalForm());
		new CloudStoreClient().execute("acceptRepoConnection", getRemoteRootWithPathPrefix().getPath());

		File child_1 = createDirectory(localRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		File child_2 = createDirectory(localRoot, "2");

		createFileWithRandomContent(child_2, "a");

		File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		File child_3 = createDirectory(localRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
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
			RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRootURLWithPathPrefix);
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
			RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localRoot, remoteRootURLWithPathPrefix);
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
	public void syncFromRemoteToLocalWithRemotePathPrefix() throws Exception {
		remotePathPrefix = "/2";
		syncFromRemoteToLocal();
	}



	@Test
	public void syncMovedFile() throws Exception {
		syncFromRemoteToLocal();

		File r_child_2 = new File(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		File r_child_2_1 = new File(r_child_2, "1");
		assertThat(r_child_2_1).isDirectory();

		File r_child_2_1_b = new File(r_child_2_1, "b");
		assertThat(r_child_2_1_b).isFile();

		File r_child_2_b = new File(r_child_2, "b");
		assertThat(r_child_2_b).doesNotExist();

		Files.move(r_child_2_1_b.toPath(), r_child_2_b.toPath());
		assertThat(r_child_2_1_b).doesNotExist();
		assertThat(r_child_2_b).isFile();

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThat(r_child_2_1_b).doesNotExist();
		assertThat(r_child_2_b).isFile();

		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

	@Test
	public void syncMovedFileToNewDir() throws Exception {
		syncFromRemoteToLocal();

		File r_child_2 = new File(remoteRoot, "2");
		assertThat(r_child_2).isDirectory();

		File r_child_2_1 = new File(r_child_2, "1");
		assertThat(r_child_2_1).isDirectory();

		File r_child_2_1_b = new File(r_child_2_1, "b");
		assertThat(r_child_2_1_b).isFile();

		File r_child_2_new = new File(r_child_2, "new");
		assertThat(r_child_2_new).doesNotExist();
		r_child_2_new.mkdir();
		assertThat(r_child_2_new).isDirectory();

		File r_child_2_new_xxx = new File(r_child_2_new, "xxx");

		Files.move(r_child_2_1_b.toPath(), r_child_2_new_xxx.toPath());
		assertThat(r_child_2_1_b).doesNotExist();
		assertThat(r_child_2_new_xxx).isFile();

		RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertThat(r_child_2_1_b).doesNotExist();
		assertThat(r_child_2_new_xxx).isFile();

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
