package co.codewizards.cloudstore.local.dbupdate;

import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;
import static java.util.Objects.*;

import java.sql.Connection;
import java.util.Objects;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.local.db.DatabaseAdapter;

public abstract class AbstractDbUpdateStep implements DbUpdateStep {

	private DatabaseAdapter databaseAdapter; 

	@Override
	public int getOrderHint() {
		return 1000;
	}

	@Override
	public DatabaseAdapter getDatabaseAdapter() {
		return databaseAdapter;
	}

	@Override
	public void setDatabaseAdapter(DatabaseAdapter databaseAdapter) {
		this.databaseAdapter = databaseAdapter;
	}
	
	protected DatabaseAdapter getDatabaseAdapterOrFail() {
		return requireNonNull(getDatabaseAdapter(), "databaseAdapter");
	}

	protected File getLocalRoot() {
		return getDatabaseAdapterOrFail().getLocalRoot();
	}

	protected File getMetaDir() {
		return getLocalRoot().createFile(META_DIR_NAME);
	}
	
	protected File getPersistencePropertiesFile() {
		final File persistencePropertiesFile = getMetaDir().createFile(LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);
		if (!persistencePropertiesFile.isFile())
			throw new IllegalStateException("The persistencePropertiesFile does not exist or is not a file: " + persistencePropertiesFile.getAbsolutePath());
		
		return persistencePropertiesFile;
	}

	protected Connection createConnection() throws Exception {
		return getDatabaseAdapterOrFail().createConnection();
	}
}
