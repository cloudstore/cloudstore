package co.codewizards.cloudstore.shared.repo;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.shared.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.shared.repo.local.RepositoryManager;

public class RepositoryTest extends AbstractTest {
	private static final Logger logger = LoggerFactory.getLogger(RepositoryTest.class);

	@Test
	public void syncExistingDirectoryGraph() throws Exception {
		File localRoot = newTestRepositoryLocalRoot();
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();

		File child_1 = new File(localRoot, "1");
		child_1.mkdir();

		File child_1_a = new File(child_1, "a");
		OutputStream out = new FileOutputStream(child_1_a);
		byte[] buf = new byte[1 + random.nextInt(10241)];
		int loops = 1 + random.nextInt(100);
		for (int i = 0; i < loops; ++i) {
			random.nextBytes(buf);
			out.write(buf);
		}
		out.close();

		File child_2 = new File(localRoot, "2");
		child_2.mkdir();
		File child_3 = new File(localRoot, "3");
		child_3.mkdir();

		RepositoryManager repositoryManager = repositoryManagerRegistry.createRepositoryManager(localRoot);
		assertThat(repositoryManager).isNotNull();
		repositoryManager.sync(new LoggerProgressMonitor(logger));

		// TODO check DB and assert all expected data is there as it should be. Maybe introduce and use appropriate API for this purpose.
	}
}
