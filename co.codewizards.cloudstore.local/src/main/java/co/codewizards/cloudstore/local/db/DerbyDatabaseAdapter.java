package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.PersistencePropertiesEnum;
import co.codewizards.cloudstore.local.PersistencePropertiesProvider;

public class DerbyDatabaseAdapter extends AbstractDatabaseAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DerbyDatabaseAdapter.class);

	private Map<String, String> persistenceProperties;

	private String connectionURL;

	private String connectionDriverName;

	private String connectionUserName;

	private String connectionPassword;

	@Override
	protected void createDatabase() throws Exception {
		initProperties();
		initDriverClass();

		connectionURL = assertNotNull(connectionURL, "connectionURL").trim() + ";create=true";
		Connection connection = createConnection();
		connection.close();
	}

	private void initDriverClass() {
		if (isEmpty(connectionDriverName))
			return;

		try {
			Class.forName(connectionDriverName);
		} catch (Throwable e) { // Might theoretically be a link error (i.e. a sub-class of Error instead of Exception) => catch Throwable
			logger.warn("initDriverClass" + e, e);
		}
	}

	public Connection createConnection() throws SQLException {
		if (isEmpty(connectionUserName) && isEmpty(connectionPassword))
			return DriverManager.getConnection(connectionURL);
		else
			return DriverManager.getConnection(connectionURL, connectionUserName, connectionPassword);
	}

	private void initProperties() {
		PersistencePropertiesProvider persistencePropertiesProvider = new PersistencePropertiesProvider(getRepositoryIdOrFail(), getLocalRootOrFail());
		persistenceProperties = persistencePropertiesProvider.getPersistenceProperties();

		connectionDriverName = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_DRIVER_NAME.key);
		connectionURL = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_URL.key);
		connectionUserName = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_USER_NAME.key);
		connectionPassword = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_PASSWORD.key);
	}
}
