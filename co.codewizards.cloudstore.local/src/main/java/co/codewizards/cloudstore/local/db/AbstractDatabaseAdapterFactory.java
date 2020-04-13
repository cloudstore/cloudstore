package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.util.Properties;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public abstract class AbstractDatabaseAdapterFactory implements DatabaseAdapterFactory {

	@Override
	public String getDisableReason() {
		return null;
	}

	@Override
	public DatabaseAdapter createDatabaseAdapter() {
		final DatabaseAdapter databaseAdapter = _createDatabaseAdapter();
		if (databaseAdapter == null)
			throw new IllegalStateException(String.format("databaseAdapterFactory._createDatabaseAdapter() returned null! Implementation error in %s!",
					this.getClass().getName()));

		if (databaseAdapter instanceof AbstractDatabaseAdapter)
			((AbstractDatabaseAdapter) databaseAdapter).setFactory(this);

		return databaseAdapter;
	}

	protected abstract DatabaseAdapter _createDatabaseAdapter();

	protected File getMetaDir(File localRoot) {
		requireNonNull(localRoot, "localRoot");
		return createFile(localRoot, LocalRepoManager.META_DIR_NAME);
	}

	protected Properties readRawPersistenceProperties(File localRoot) {
		requireNonNull(localRoot, "localRoot");
		final File persistencePropertiesFile = createFile(getMetaDir(localRoot), LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);
		if (!persistencePropertiesFile.isFile())
			throw new IllegalStateException("The persistencePropertiesFile does not exist or is not a file: " + persistencePropertiesFile.getAbsolutePath());

		Properties rawProperties;
		try {
			rawProperties = PropertiesUtil.load(persistencePropertiesFile);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return rawProperties;
	}
}
