package co.codewizards.cloudstore.local.db;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.local.AbstractTest;

public class PostgresqlDatabaseAdapterTest extends AbstractTest {

	@BeforeClass
	public static void before_PostgresqlDatabaseAdapterTest() {
		enablePostgresql();
	}

	@AfterClass
	public static void after_PostgresqlDatabaseAdapterTest() {
		disablePostgresql();
	}

	@Test
	public void createTestDatabase() throws Exception {
		UUID repositoryId = UUID.randomUUID();
		File localRoot = newTestRepositoryLocalRoot(".tmp");
		localRoot.mkdir();
		if (! localRoot.isDirectory())
			throw new IllegalStateException("Creating directory failed: " + localRoot);

		File cloudStoreRepoDir = localRoot.createFile(LocalRepoManager.META_DIR_NAME);
		cloudStoreRepoDir.mkdir();
		if (! cloudStoreRepoDir.isDirectory())
			throw new IllegalStateException("Creating directory failed: " + cloudStoreRepoDir);

		try (DatabaseAdapter databaseAdapter = DatabaseAdapterFactoryRegistry.getInstance().createDatabaseAdapter()) {
			if (! (databaseAdapter instanceof PostgresqlDatabaseAdapter)) {
				throw new IllegalStateException("databaseAdapter is *not* an instance of PostgresqlDatabaseAdapter: " + databaseAdapter);
			}

			databaseAdapter.setLocalRoot(localRoot);
			databaseAdapter.setRepositoryId(repositoryId);
			databaseAdapter.createPersistencePropertiesFileAndDatabase();

			((ExternalJdbcDatabaseAdapter) databaseAdapter).dropDatabase();
		}
	}
}
