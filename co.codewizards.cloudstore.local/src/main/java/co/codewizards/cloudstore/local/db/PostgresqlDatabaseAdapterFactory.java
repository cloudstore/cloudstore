package co.codewizards.cloudstore.local.db;

import static java.util.Objects.*;

import java.util.Properties;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.local.PersistencePropertiesEnum;

public class PostgresqlDatabaseAdapterFactory extends ExternalJdbcDatabaseAdapterFactory {

	@Override
	public String getName() {
		return "postgresql";
	}

	@Override
	public int getPriority() {
		return 90;
	}

	@Override
	protected DatabaseAdapter _createDatabaseAdapter() {
		return new PostgresqlDatabaseAdapter();
	}

	@Override
	public boolean isLocalRootSupported(File localRoot) {
		requireNonNull(localRoot, "localRoot");
		Properties properties = readRawPersistenceProperties(localRoot);
		String connectionDriverName = properties.getProperty(PersistencePropertiesEnum.CONNECTION_DRIVER_NAME.key);
		if (connectionDriverName == null)
			return false;

		return connectionDriverName.indexOf(".postgresql.") >= 0;
	}
}
