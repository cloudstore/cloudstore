package co.codewizards.cloudstore.local.db;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class DbMigrateFromDerbyToPostgresqlTest extends AbstractDbMigrateTest {
	private static final Logger logger = LoggerFactory.getLogger(DbMigrateFromDerbyToPostgresqlTest.class);

	private File localRoot;

	@Test
	public void migrateFromDerbyToPostgresql() throws Exception {

		localRoot = newTestRepositoryLocalRoot("local");
		assertThat(localRoot.exists()).isFalse();
		localRoot.mkdirs();
		assertThat(localRoot.isDirectory()).isTrue();

		LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		final File child_1 = createDirectory(localRoot, "1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(localRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b", 150000);

		final File child_3 = createDirectory(localRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));

		assertThatFilesInRepoAreCorrect(localRoot);

		UUID repositoryId = localRepoManagerLocal.getRepositoryId();
		logger.info("local repo: {}", repositoryId);

		localRepoManagerLocal.setCloseDeferredMillis(0); // close it immediately!
		localRepoManagerLocal.close(); localRepoManagerLocal = null;

		enablePostgresql();

		DatabaseMigrater databaseMigrater = new DatabaseMigrater(localRoot);
		databaseMigrater.deleteTriggerFile();
		databaseMigrater.migrateIfNeeded();
	}

}
