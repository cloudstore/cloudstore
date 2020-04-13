package co.codewizards.cloudstore.local.db;

import static java.util.Objects.*;

import java.util.Properties;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.local.PersistencePropertiesEnum;

public class DerbyDatabaseAdapterFactory extends AbstractDatabaseAdapterFactory {

	@Override
	public String getName() {
		return "derby";
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	protected DatabaseAdapter _createDatabaseAdapter() {
		return new DerbyDatabaseAdapter();
	}

	@Override
	public boolean isLocalRootSupported(File localRoot) {
		requireNonNull(localRoot, "localRoot");
		Properties properties = readRawPersistenceProperties(localRoot);
		String connectionDriverName = properties.getProperty(PersistencePropertiesEnum.CONNECTION_DRIVER_NAME.key);
		if (connectionDriverName == null)
			return false;

		return connectionDriverName.indexOf(".derby.") >= 0;
	}
}
