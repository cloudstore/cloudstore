package co.codewizards.cloudstore.test.repotorepo;

import static co.codewizards.cloudstore.local.db.DatabaseAdapterFactory.*;
import static co.codewizards.cloudstore.local.db.ExternalJdbcDatabaseAdapter.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.local.db.DatabaseAdapterFactoryRegistry;

public class PostgresqlBasicRepoToRepoSyncIT extends BasicRepoToRepoSyncIT {

	@BeforeClass
	public static void before_PostgresqlBasicRepoToRepoSyncIT() {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME, "postgresql");

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME, getEnvOrFail("TEST_PG_HOST_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME, getEnvOrFail("TEST_PG_USER_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD, getEnvOrFail("TEST_PG_PASSWORD"));

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX, "TEST_CS_");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX, "_TEST");
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	@AfterClass
	public static void after_PostgresqlBasicRepoToRepoSyncIT() {
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

	@Override
	@Test
	public void syncFromRemoteToLocal() throws Exception {
		super.syncFromRemoteToLocal();
	}

	@Override
	@Test
	public void syncFromLocalToRemote() throws Exception {
		super.syncFromLocalToRemote();
	}

	@Override
	@Test
	public void syncMovedFile() throws Exception {
		super.syncMovedFile();
	}

	@Override
	@Test
	public void syncMovedFileToNewDir() throws Exception {
		super.syncMovedFileToNewDir();
	}
}
