package co.codewizards.cloudstore.local.dbmigrate;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import co.codewizards.cloudstore.local.PersistencePropertiesProvider;
import co.codewizards.cloudstore.local.db.DatabaseAdapter;
import co.codewizards.cloudstore.local.db.DatabaseAdapterFactory;
import co.codewizards.cloudstore.local.db.DatabaseAdapterFactoryRegistry;
import co.codewizards.cloudstore.local.persistence.CloudStorePersistenceCapableClassesProvider;

public class DatabaseMigrater {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrater.class);

	public static final String DBMIGRATE_TRIGGER_FILE_NAME = "dbmigrate.deleteToRun";
	public static final String DBMIGRATE_STATUS_FILE_NAME = "dbmigrate.status.properties";
	public static final String DBMIGRATE_TARGET_DIR_NAME = "dbmigrate.target.tmp";

	protected final File localRoot;
	protected final UUID repositoryId;
	protected final File metaDir;
	protected final File targetLocalRoot;
	protected final File targetMetaDir;
	protected Properties status = new Properties();

	protected DatabaseAdapterFactory sourceDbAdapterFactory;
	protected DatabaseAdapterFactory targetDbAdapterFactory;

	protected PersistenceManagerFactory sourcePmf;
	protected PersistenceManagerFactory targetPmf;
	protected PersistenceManager sourcePm;
	protected PersistenceManager targetPm;

	public DatabaseMigrater(File localRoot, UUID repositoryId) {
		this.localRoot = requireNonNull(localRoot, "localRoot");
		this.repositoryId = requireNonNull(repositoryId, "repositoryId");
		this.metaDir = this.localRoot.createFile(META_DIR_NAME);

		this.targetLocalRoot = this.localRoot.createFile(DBMIGRATE_TARGET_DIR_NAME);
		this.targetMetaDir = this.targetLocalRoot.createFile(META_DIR_NAME);
	}

	public void migrateIfNeeded() {
		if (! isMigrationInProcess()) {
			logger.info("migrateIfNeeded: localRoot='{}': No migration => return immediately.", localRoot);
			return;
		}
		try {
			DatabaseAdapterFactoryRegistry databaseAdapterFactoryRegistry = DatabaseAdapterFactoryRegistry.getInstance();
			sourceDbAdapterFactory = databaseAdapterFactoryRegistry.getDatabaseAdapterFactoryOrFail(localRoot);
			targetDbAdapterFactory = databaseAdapterFactoryRegistry.getDatabaseAdapterFactoryOrFail();
			String sourceDbafName = sourceDbAdapterFactory.getName();
			String targetDbafName = targetDbAdapterFactory.getName();

			if (sourceDbafName.equals(targetDbafName)) {
				logger.info("migrateIfNeeded: localRoot='{}': sourceDatabaseAdapterName == targetDatabaseAdapterName == '{}' :: Nothing to do!",
						localRoot, sourceDbafName);
			} else {
				logger.info("migrateIfNeeded: localRoot='{}': sourceDatabaseAdapterName == '{}' != targetDatabaseAdapterName == '{}' :: Starting migration, now!",
						localRoot, sourceDbafName, targetDbafName);

				readStatus();

				File statusFile = getStatusFile();
				if (! statusFile.exists()) {
					writeStatus();
				}

				createTargetPersistenceProperties();

				createPersistenceManagerFactories();
				closePersistenceManagerFactories();

				dropTargetConstraints();
				copyTableData();

				createPersistenceManagerFactories();
			}
			// Finally, when we're completely done, we create the trigger-file, again.
			getTriggerFile().createNewFile();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			closePersistenceManagerFactories();
		}
	}

	protected void copyTableData() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	protected void dropTargetConstraints() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}

	protected void createPersistenceManagerFactories() {
		PersistencePropertiesProvider ppp = new PersistencePropertiesProvider(repositoryId, localRoot);
		Map<String, String> sourcePersistenceProperties = ppp.getPersistenceProperties();

		ppp = new PersistencePropertiesProvider(repositoryId, localRoot);
		ppp.setOverridePersistencePropertiesFile(getTargetPersistencePropertiesFile());
		Map<String, String> targetPersistenceProperties = ppp.getPersistenceProperties();

		sourcePmf = JDOHelper.getPersistenceManagerFactory(sourcePersistenceProperties);
		sourcePm = sourcePmf.getPersistenceManager();

		targetPmf = JDOHelper.getPersistenceManagerFactory(targetPersistenceProperties);
		targetPm = targetPmf.getPersistenceManager();

		CloudStorePersistenceCapableClassesProvider.Helper.initPersistenceCapableClasses(sourcePm);
		CloudStorePersistenceCapableClassesProvider.Helper.initPersistenceCapableClasses(targetPm);
	}

	protected void closePersistenceManagerFactories() {
		if (sourcePm != null) {
			if (sourcePm.currentTransaction().isActive())
				sourcePm.currentTransaction().rollback();

			sourcePm.close(); sourcePm = null;
		}
		if (targetPm != null) {
			if (targetPm.currentTransaction().isActive())
				targetPm.currentTransaction().rollback();

			targetPm.close(); targetPm = null;
		}
		if (sourcePmf != null) {
			sourcePmf.close(); sourcePmf = null;
		}
		if (targetPmf != null) {
			targetPmf.close(); targetPmf = null;
		}
	}

	protected void createTargetPersistenceProperties() throws Exception {
		requireNonNull(sourceDbAdapterFactory, "sourceDbAdapterFactory");
		requireNonNull(targetDbAdapterFactory, "targetDbAdapterFactory");

		targetLocalRoot.mkdir();
		targetMetaDir.mkdir();
		if (! targetMetaDir.isDirectory())
			throw new IllegalStateException("Creating directory failed: " + targetMetaDir.getAbsolutePath());

		if (getTargetPersistencePropertiesFile().exists()) {
			File directory = getTargetPersistencePropertiesFile().getParentFile();
			File backupFile = directory.createFile(
					getTargetPersistencePropertiesFile().getName() + ".bak_" + Long.toHexString(System.currentTimeMillis()));
			getTargetPersistencePropertiesFile().renameTo(backupFile);

			if (getTargetPersistencePropertiesFile().exists())
				throw new IOException(String.format("Renaming file '%s' to '%s' (in directory '%s') failed!",
						getTargetPersistencePropertiesFile().getName(), backupFile.getName(), directory.getAbsolutePath()));
		}

		try (DatabaseAdapter targetDatabaseAdapter = targetDbAdapterFactory.createDatabaseAdapter()) {
			targetDatabaseAdapter.setRepositoryId(repositoryId);
			targetDatabaseAdapter.setLocalRoot(targetLocalRoot);
			targetDatabaseAdapter.createPersistencePropertiesFileAndDatabase();
		}

		if (! getTargetPersistencePropertiesFile().exists())
			throw new IOException(String.format("Creating persistence-properties '%s' failed!",
					getTargetPersistencePropertiesFile().getAbsolutePath()));
	}

	protected File getSourcePersistencePropertiesFile() {
		return metaDir.createFile(PERSISTENCE_PROPERTIES_FILE_NAME);
	}

	protected File getTargetPersistencePropertiesFile() {
		return targetMetaDir.createFile(PERSISTENCE_PROPERTIES_FILE_NAME);
	}

	protected Properties readRawPersistenceProperties() throws IOException {
		final File persistencePropertiesFile = createFile(metaDir, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);
		if (!persistencePropertiesFile.isFile())
			throw new IllegalStateException("The persistencePropertiesFile does not exist or is not a file: " + persistencePropertiesFile.getAbsolutePath());

		Properties rawProperties = PropertiesUtil.load(persistencePropertiesFile);
		return rawProperties;
	}

	protected void readStatus() throws IOException {
		File statusFile = getStatusFile();
		if (statusFile.exists()) {
			try (IInputStream in = getStatusFile().createInputStream()) {
				status.load(castStream(in));
			}
		}
	}

	protected void writeStatus() throws IOException {
		try (IOutputStream out = getStatusFile().createOutputStream()) {
			status.store(castStream(out), null);
		}
	}

	protected File getTriggerFile() {
		return metaDir.createFile(DBMIGRATE_TRIGGER_FILE_NAME);
	}

	protected File getStatusFile() {
		return metaDir.createFile(DBMIGRATE_STATUS_FILE_NAME);
	}

	public boolean isMigrationInProcess() {
		File statusFile = getStatusFile();
		File triggerFile = getTriggerFile();
		return statusFile.exists() || ! triggerFile.exists();
	}
}
