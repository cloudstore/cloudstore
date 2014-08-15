package co.codewizards.cloudstore.client;

import static org.assertj.core.api.Assertions.*;

import co.codewizards.cloudstore.core.oio.file.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.FilenameFilterSkipMetaDir;

public abstract class AbstractTest {

	static {
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, "target/.cloudstore");
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_KEY_SIZE, "1024");
	}

	protected static final Random random = new Random();
//	protected static LocalRepoManagerFactory localRepoManagerFactory = LocalRepoManagerFactory.getInstance();

	protected File newTestRepositoryLocalRoot(String suffix) throws IOException {
		assertThat(suffix).isNotNull();
		long timestamp = System.currentTimeMillis();
		int randomNumber = random.nextInt(BigInteger.valueOf(36).pow(5).intValue());
		String repoName = Long.toString(timestamp, 36) + '-' + Integer.toString(randomNumber, 36) + (suffix.isEmpty() ? "" : "-") + suffix;
		File localRoot = newFile(getTestRepositoryBaseDir(), repoName);
		return localRoot;
	}

	protected File getTestRepositoryBaseDir() {
		File dir = newFile(new File("target"), "repo");
		dir.mkdirs();
		return dir;
	}

	protected void assertDirectoriesAreEqualRecursively(File dir1, File dir2) throws IOException {
		assertThat(dir1).isDirectory();
		assertThat(dir2).isDirectory();

		String[] children1 = dir1.list(new FilenameFilterSkipMetaDir());
		assertThat(children1).isNotNull();

		String[] children2 = dir2.list(new FilenameFilterSkipMetaDir());
		assertThat(children2).isNotNull();

		Arrays.sort(children1);
		Arrays.sort(children2);

		assertThat(children1).containsOnly(children2);

		for (String childName : children1) {
			File child1 = newFile(dir1, childName);
			File child2 = newFile(dir2, childName);

			if (child1.isFile()) {
				assertThat(child2.isFile());
				assertThat(IOUtil.compareFiles(child1, child2)).isTrue();
			}

			if (child1.isDirectory())
				assertDirectoriesAreEqualRecursively(child1, child2);
		}
	}

}
