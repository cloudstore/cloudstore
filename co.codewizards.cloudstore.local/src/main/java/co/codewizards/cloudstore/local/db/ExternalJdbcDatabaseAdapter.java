package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.local.PersistencePropertiesEnum;
import co.codewizards.cloudstore.local.PersistencePropertiesProvider;

/**
 * Abstract common base for JDBC-based external (non-embedded) RDBMS.
 * @author mangu
 */
public abstract class ExternalJdbcDatabaseAdapter extends AbstractDatabaseAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ExternalJdbcDatabaseAdapter.class);

	/**
	 * Host-name, optionally with port.
	 */
	public static final String CONFIG_KEY_JDBC_HOST_NAME = "jdbc.hostName"; // required
	public static final String CONFIG_KEY_JDBC_USER_NAME = "jdbc.userName"; // optional (if the DB does not require it)
	public static final String CONFIG_KEY_JDBC_PASSWORD = "jdbc.password"; // optional (if the DB does not require it)

	public static final String CONFIG_KEY_JDBC_DB_NAME_PREFIX = "jdbc.dbNamePrefix"; // optional
	public static final String CONFIG_KEY_JDBC_DB_NAME_SUFFIX = "jdbc.dbNameSuffix"; // optional

	/**
	 * Which database should the JDBC-driver connect to, before a CloudStore-database
	 * was created. It must somehow connect to the database-server in order to issue the
	 * {@code CREATE DATABASE...} command.
	 * <p>
	 * If this setting is empty or missing, it connects using a JDBC-URL without any
	 * database, e.g.: {@code jdbc:postgresql://localhost/}
	 */
	public static final String CONFIG_KEY_JDBC_SYSDB_NAME = "jdbc.sysdbName"; // optional

	private Map<String, String> persistenceProperties;

	private String connectionURL;

	private String connectionDriverName;

	private String connectionUserName;

	private String connectionPassword;

	private String databaseName;

	@Override
	protected void setFactory(AbstractDatabaseAdapterFactory factory) {
		if (factory != null && ! (factory instanceof ExternalJdbcDatabaseAdapterFactory)) {
			throw new IllegalArgumentException("factory is not an instance of ExternalJdbcDatabaseAdapterFactory: " + factory);
		}
		super.setFactory(factory);
	}

	@Override
	protected void createDatabase() throws Exception {
		if (connectionURL == null) {
			initProperties();
			initDriverClass();
		}
		Config config = ConfigImpl.getInstance();
		ExternalJdbcDatabaseAdapterFactory factory = (ExternalJdbcDatabaseAdapterFactory) getFactoryOrFail();

		String sysdbUrl = factory.getJdbcSysdbUrl();
		String userName = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_USER_NAME, null);
		String password = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_PASSWORD, null);

		String dbNamePrefix = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_DB_NAME_PREFIX, "");
		String dbNameSuffix = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_DB_NAME_SUFFIX, "");
		databaseName = dbNamePrefix + getRepositoryIdOrFail() + dbNameSuffix;

		try (Connection connection = DriverManager.getConnection(sysdbUrl, userName, password)) {
			boolean dropDatabase = true;
			createDatabase(connection, databaseName);
			try {
				// Now try to connect to the new database...
				try {
					Connection newDbConnection = createConnection();
					newDbConnection.close();
				} catch (Exception x) {
					logger.error("Creating connection failed: " + x, x);
					throw x;
				}
				dropDatabase = false;
			} finally {
				try {
					if (dropDatabase)
						dropDatabase(connection, databaseName);
				} catch (Throwable error) {
					logger.error("Dropping database '" + databaseName + "' failed: " + error, error);
				}
			}
		}
	}

	/**
	 * Drops the database -- only used by tests!
	 * @throws Exception if dropping failed.
	 */
	protected void dropDatabase() throws Exception {
		if (isEmpty(databaseName)) {
			throw new IllegalStateException("createDatabase() not called immediately before!");
		}
		logger.warn("dropDatabase: Dropping '{}'...", databaseName);

		Config config = ConfigImpl.getInstance();
		ExternalJdbcDatabaseAdapterFactory factory = (ExternalJdbcDatabaseAdapterFactory) getFactoryOrFail();

		String userName = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_USER_NAME, null);
		String password = config.getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_JDBC_PASSWORD, null);

		String sysdbUrl = factory.getJdbcSysdbUrl();
		try (Connection connection = DriverManager.getConnection(sysdbUrl, userName, password)) {
			dropDatabase(connection, databaseName);
		}
		logger.warn("dropDatabase: Dropped '{}'!", databaseName);
		databaseName = null;
	}

	protected void createDatabase(Connection connection, String databaseName) throws Exception {
		requireNonNull(connection, "connection");
		requireNonNull(databaseName, "databaseName");
		if (databaseName.indexOf('"') >= 0)
			throw new IllegalStateException("databaseName contains illegal character '\"': " + databaseName);

		logger.info("createDatabase: Creating '{}'...", databaseName);
		try (Statement statement = connection.createStatement()) {
			String sql = String.format("create database \"%s\"", databaseName);
			statement.execute(sql);
		}
		logger.info("createDatabase: Created '{}'!", databaseName);
	}

	protected void dropDatabase(Connection connection, String databaseName) throws Exception {
		requireNonNull(connection, "connection");
		requireNonNull(databaseName, "databaseName");
		if (databaseName.indexOf('"') >= 0)
			throw new IllegalStateException("databaseName contains illegal character '\"': " + databaseName);

		logger.info("dropDatabase: Dropping '{}'...", databaseName);
		try (Statement statement = connection.createStatement()) {
			String sql = String.format("drop database \"%s\"", databaseName);
			statement.execute(sql);
		}
		logger.info("dropDatabase: Dropped '{}'!", databaseName);
	}

	private void initDriverClass() {
		if (isEmpty(connectionDriverName))
			return;

		try {
			Class.forName(connectionDriverName);
		} catch (Throwable e) { // Might theoretically be a link error (i.e. a sub-class of Error instead of Exception) => catch Throwable
			logger.warn("initDriverClass: " + e, e);
		}
	}

	@Override
	public Connection createConnection() throws SQLException {
		if (connectionURL == null) {
			initProperties();
			initDriverClass();
		}
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
