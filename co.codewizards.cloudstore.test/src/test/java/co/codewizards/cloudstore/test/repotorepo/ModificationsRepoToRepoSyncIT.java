package co.codewizards.cloudstore.test.repotorepo;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;

public class ModificationsRepoToRepoSyncIT extends AbstractRepoToRepoSyncIT
{
	private static final Logger logger = LoggerFactory.getLogger(ModificationsRepoToRepoSyncIT.class);

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
			final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(localRoot, remoteRootURLWithPathPrefix);
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
			final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(localRoot, remoteRootURLWithPathPrefix);
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
}
