package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.local.db.DatabaseAdapterFactory.*;
import static co.codewizards.cloudstore.local.db.ExternalJdbcDatabaseAdapter.*;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.local.AbstractTest;

public class PostgresqlDatabaseAdapterTest extends AbstractTest {

	@BeforeClass
	public static void before_PostgresqlDatabaseAdapterTest() {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME, "postgresql");

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME, getEnvOrFail("TEST_PG_HOST_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME, getEnvOrFail("TEST_PG_USER_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD, getEnvOrFail("TEST_PG_PASSWORD"));

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX, "TEST_");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX, "_TEST");
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	@AfterClass
	public static void after_PostgresqlDatabaseAdapterTest() {
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME);

		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD);

		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX);
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	protected static String getEnvOrFail(String key) {
		String value = System.getenv(key);
		if (value == null)
			throw new IllegalStateException("Environment-variable not set: " + key);

		return value;
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
