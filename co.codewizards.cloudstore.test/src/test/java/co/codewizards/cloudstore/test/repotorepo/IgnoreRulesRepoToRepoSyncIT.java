package co.codewizards.cloudstore.test.repotorepo;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.progress.NullProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import co.codewizards.cloudstore.test.AbstractRepoAwareIT;

public class IgnoreRulesRepoToRepoSyncIT extends AbstractRepoAwareIT
{
	private static final Logger logger = LoggerFactory.getLogger(IgnoreRulesRepoToRepoSyncIT.class);

	@Test
	public void ignoreRulesExistBeforeAffectedFiles() throws Exception {
		createLocalAndRemoteRepo();
		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		final File child_1 = createDirectory(remoteRoot, "1");

		final File child_1_a = createFileWithRandomContent(child_1, "a");
		final File child_1_b = createFileWithRandomContent(child_1, "b");

		localRepoManagerRemote.localSync(new NullProgressMonitor());
		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());


		// Create and sync ignore rules.
		Properties properties = new Properties();
		properties.put("ignore[0].namePattern", "a"); // ignore a file that already exists and was already synced
		properties.put("ignore[1].namePattern", "c"); // ignore a new file
		PropertiesUtil.store(createFile(remoteRoot, ".cloudstore.properties"), properties, null);

		repoToRepoSync.sync(new LoggerProgressMonitor(logger));


		// Modify some files and see whether the ignore-rules are respected.
		try (OutputStream out = castStream(child_1_a.createOutputStream(true))) {
			out.write(new byte[] { 1, 2, 3 });
		}

		try (OutputStream out = castStream(child_1_b.createOutputStream(true))) {
			out.write(new byte[] { 4, 5, 6 });
		}

		final File child_1_c = createFileWithRandomContent(child_1, "c"); // new file!


		final File dest_child_1 = createFile(localRoot, "1");
		final File dest_child_1_a = createFile(dest_child_1, "a");
		final File dest_child_1_b = createFile(dest_child_1, "b");
		final File dest_child_1_c = createFile(dest_child_1, "c");

		byte[] src_child_1_a_data = readAll(child_1_a);
		byte[] dest_child_1_a_data = readAll(dest_child_1_a);
		assertThat(src_child_1_a_data).isNotEqualTo(dest_child_1_a_data);

		byte[] src_child_1_b_data = readAll(child_1_b);
		byte[] dest_child_1_b_data = readAll(dest_child_1_b);
		assertThat(src_child_1_b_data).isNotEqualTo(dest_child_1_b_data);

		byte[] src_child_1_c_data = readAll(child_1_c);
		assertThat(dest_child_1_c.getIoFile()).doesNotExist();

		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		// The file "/1/a" should *not* have been synced! It's ignored!
		byte[] src_child_1_a_data2 = readAll(child_1_a);
		byte[] dest_child_1_a_data2 = readAll(dest_child_1_a);
		assertThat(src_child_1_a_data2).isEqualTo(src_child_1_a_data);
		assertThat(dest_child_1_a_data2).isEqualTo(dest_child_1_a_data);

		// The file "/1/b" should have been synced, because it's *not* ignored!
		byte[] src_child_1_b_data2 = readAll(child_1_b);
		byte[] dest_child_1_b_data2 = readAll(dest_child_1_b);
		assertThat(src_child_1_b_data2).isEqualTo(src_child_1_b_data);
		assertThat(dest_child_1_b_data2).isNotEqualTo(dest_child_1_b_data);
		assertThat(dest_child_1_b_data2).isEqualTo(src_child_1_b_data);

		// The file "/1/c" should *not* have been synced! It's ignored!
		byte[] src_child_1_c_data2 = readAll(child_1_c);
		assertThat(src_child_1_c_data2).isEqualTo(src_child_1_c_data);
		assertThat(dest_child_1_c.getIoFile()).doesNotExist();

		repoToRepoSync.close();
		localRepoManagerRemote.close();
	}

	@Test
	public void ignoreRulesBecomeDisabled() throws Exception {
		createLocalAndRemoteRepo();
		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);

		final File child_1 = createDirectory(remoteRoot, "1");

		final File child_1_a = createFileWithRandomContent(child_1, "a");
		final File child_1_b = createFileWithRandomContent(child_1, "b");

		localRepoManagerRemote.localSync(new NullProgressMonitor());
		assertThatFilesInRepoAreCorrect(remoteRoot);

		final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(remoteRoot);
		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());


		// Create and sync ignore rules.
		Properties properties = new Properties();
		properties.put("ignore[a].namePattern", "a"); // ignore a file that already exists and was already synced
		properties.put("ignore[c].namePattern", "c"); // ignore a new file
		properties.put("ignore[dir1].namePattern", "dir1"); // ignore a new directory
		PropertiesUtil.store(createFile(remoteRoot, ".cloudstore.properties"), properties, null);

		repoToRepoSync.sync(new LoggerProgressMonitor(logger));


		// Modify some files and see whether the ignore-rules are respected.
		try (OutputStream out = castStream(child_1_a.createOutputStream(true))) {
			out.write(new byte[] { 1, 2, 3 });
		}

		try (OutputStream out = castStream(child_1_b.createOutputStream(true))) {
			out.write(new byte[] { 4, 5, 6 });
		}

		final File child_1_c = createFileWithRandomContent(child_1, "c"); // new file!
		final File src_dir1 = createDirectory(child_1, "dir1");
		final File src_dir1_aaa = createFileWithRandomContent(src_dir1, "aaa");
		byte[] src_dir1_aaa_data = readAll(src_dir1_aaa);

		final File dest_child_1 = createFile(localRoot, "1");
		final File dest_child_1_a = createFile(dest_child_1, "a");
		final File dest_child_1_b = createFile(dest_child_1, "b");
		final File dest_child_1_c = createFile(dest_child_1, "c");
		final File dest_dir1 = createFile(dest_child_1, "dir1");
		final File dest_dir1_aaa = createFile(dest_dir1, "aaa");


		byte[] src_child_1_a_data = readAll(child_1_a);
		byte[] dest_child_1_a_data = readAll(dest_child_1_a);
		assertThat(src_child_1_a_data).isNotEqualTo(dest_child_1_a_data);

		byte[] src_child_1_b_data = readAll(child_1_b);
		byte[] dest_child_1_b_data = readAll(dest_child_1_b);
		assertThat(src_child_1_b_data).isNotEqualTo(dest_child_1_b_data);

		byte[] src_child_1_c_data = readAll(child_1_c);
		assertThat(dest_child_1_c.getIoFile()).doesNotExist();


		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		// The file "/1/a" should *not* have been synced! It's ignored!
		byte[] src_child_1_a_data2 = readAll(child_1_a);
		byte[] dest_child_1_a_data2 = readAll(dest_child_1_a);
		assertThat(src_child_1_a_data2).isEqualTo(src_child_1_a_data);
		assertThat(dest_child_1_a_data2).isEqualTo(dest_child_1_a_data);

		// The file "/1/b" should have been synced, because it's *not* ignored!
		byte[] src_child_1_b_data2 = readAll(child_1_b);
		byte[] dest_child_1_b_data2 = readAll(dest_child_1_b);
		assertThat(src_child_1_b_data2).isEqualTo(src_child_1_b_data);
		assertThat(dest_child_1_b_data2).isNotEqualTo(dest_child_1_b_data);
		assertThat(dest_child_1_b_data2).isEqualTo(src_child_1_b_data);

		// The file "/1/c" should *not* have been synced! It's ignored!
		byte[] src_child_1_c_data2 = readAll(child_1_c);
		assertThat(src_child_1_c_data2).isEqualTo(src_child_1_c_data);
		assertThat(dest_child_1_c.getIoFile()).doesNotExist();

		// The directory dir1 should *not* have been synced!
		assertThat(src_dir1.getIoFile()).isDirectory();
		assertThat(src_dir1_aaa.getIoFile()).isFile();
		byte[] src_dir1_aaa_data1 = readAll(src_dir1_aaa);
		assertThat(src_dir1_aaa_data1).isEqualTo(src_dir1_aaa_data);
		assertThat(dest_dir1.getIoFile()).doesNotExist();
		assertThat(dest_dir1_aaa.getIoFile()).doesNotExist();


		// Disable ignore rules.
		properties = new Properties();
		properties.put("ignore[a].enabled", "false");
		properties.put("ignore[c].enabled", "false");
		properties.put("ignore[dir1].enabled", "false");
		PropertiesUtil.store(createFile(child_1, ".cloudstore.properties"), properties, null);

		repoToRepoSync.sync(new LoggerProgressMonitor(logger));

		// Now, the previously ignored file "/1/a" should have been synced!
		byte[] src_child_1_a_data3 = readAll(child_1_a);
		byte[] dest_child_1_a_data3 = readAll(dest_child_1_a);
		assertThat(dest_child_1_a_data3).isEqualTo(src_child_1_a_data3);

		// And the previously ignoed file "/1/c" should have been synced, too!
		byte[] src_child_1_c_data3 = readAll(child_1_c);
		assertThat(src_child_1_c_data3).isEqualTo(src_child_1_c_data);
		assertThat(dest_child_1_c.getIoFile()).exists();
		byte[] dest_child_1_c_data3 = readAll(child_1_c);
		assertThat(dest_child_1_c_data3).isEqualTo(src_child_1_c_data3);

		// The previously ignored directory dir1 should have been synced!
		assertThat(src_dir1.getIoFile()).isDirectory();
		assertThat(src_dir1_aaa.getIoFile()).isFile();
		byte[] src_dir1_aaa_data2 = readAll(src_dir1_aaa);
		assertThat(src_dir1_aaa_data2).isEqualTo(src_dir1_aaa_data);
		assertThat(dest_dir1.getIoFile()).isDirectory();
		assertThat(dest_dir1_aaa.getIoFile()).isFile();
		byte[] dest_dir1_aaa_data = readAll(dest_dir1_aaa);
		assertThat(dest_dir1_aaa_data).isEqualTo(src_dir1_aaa_data);

		repoToRepoSync.close();
		localRepoManagerRemote.close();
	}

	private static byte[] readAll(File f) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream in = castStream(f.createInputStream())) {
			IOUtil.transferStreamData(in, out);
		}
		return out.toByteArray();
	}

}
