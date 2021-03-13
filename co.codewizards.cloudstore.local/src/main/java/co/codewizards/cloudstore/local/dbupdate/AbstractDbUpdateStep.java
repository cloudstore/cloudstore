package co.codewizards.cloudstore.local.dbupdate;

import static co.codewizards.cloudstore.core.repo.local.LocalRepoManager.*;
import static java.util.Objects.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.local.db.DatabaseAdapter;

public abstract class AbstractDbUpdateStep implements DbUpdateStep {

	private static final Logger logger = LoggerFactory.getLogger(AbstractDbUpdateStep.class);

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

	/**
	 * Gets the database-type behind the given {@code connection}.
	 *
	 * @param connection the DB-connection. Must not be {@code null}.
	 * @return the database-type behind the given {@code connection}.
	 * @throws SQLException if determining the database-type failed.
	 */
	protected static DatabaseType getDatabaseType(Connection connection) throws SQLException {
		String driverName = connection.getMetaData().getDriverName();
		if (driverName == null) {
			logger.error("getDatabaseType: connection.getMetaData().getDriverName() returned null!");
			return DatabaseType.OTHER;
		}
		logger.info("getDatabaseType: driverName='{}'", driverName); // TODO make this debug in the future.
		String driverNameLowerCase = driverName.toLowerCase(Locale.UK);
		if (driverNameLowerCase.contains("derby")) {
			return DatabaseType.DERBY;
		}
		if (driverNameLowerCase.contains("postgre")) {
			return DatabaseType.POSTGRESQL;
		}
		return DatabaseType.OTHER;
	}

	/**
	 * Determines whether a table exists.
	 * @param connection the DB-connection. Must not be {@code null}.
	 * @param tableName the table-name. Must not be {@code null}.
	 * @return <code>true</code>, if the table exists. <code>false</code>, if it does not exist.
	 * @throws SQLException
	 */
	protected static boolean doesTableExist(Connection connection, String tableName) throws SQLException {
		try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toLowerCase(Locale.UK), new String[] { "TABLE" })) {
			boolean result = rs.next();
			logger.info("doesTableExist: tableName='{}' result={}", tableName, result);
			return result;
		}
	}

	/**
	 * Determines whether a column already exists.
	 * <p>
	 * <b>Important:</b> This method throws an exception, if the table does not exist! Check it before with {@link #doesTableExist(Connection, String)}!
	 *
	 * @param connection the DB-connection. Must not be {@code null}.
	 * @param tableName the table-name. Must not be {@code null}.
	 * @param columnName the column-name. Must not be {@code null}.
	 * @return <code>true</code>, if the column already exists in the specified table. <code>false</code> otherwise.
	 * @throws SQLException if talking to the DB failed.
	 */
	protected static boolean doesColumnExist(Connection connection, String tableName, String columnName) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			ResultSet rs = statement.executeQuery("select * from " + quoteIdentifier(tableName) + " where 1 = 0");
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();
			for (int columnIndex = 1; columnIndex <= columnCount; ++columnIndex) {
				String cn = metaData.getColumnName(columnIndex);
				if (columnName.equalsIgnoreCase(cn)) {
					logger.info("doesColumnExist: tableName='{}' columnName='{}' result=true", tableName, columnName);
					return true;
				}
			}
		}
		logger.info("doesColumnExist: tableName='{}' columnName='{}' result=false", tableName, columnName);
		return false;
	}

	/**
	 * Quotes identifiers like a table-name or a column-name. Also makes them lower-case since we use all-lower-case in the
	 * persistence-settings.
	 * @param identifier the identifier to be quoted. Must not be <code>null</code>.
	 * @return the quoted identifier.
	 */
	protected static String quoteIdentifier(String identifier) {
		requireNonNull(identifier, "identifier");
		return "\"" + identifier.toLowerCase(Locale.UK) + "\"";
	}

	/**
	 * Adds a column of type {@code boolean} which is not nullable.
	 * @param connection the DB-connection. Must not be {@code null}.
	 * @param tableName the table-name. Must not be {@code null}.
	 * @param columnName the column-name. Must not be {@code null}.
	 * @throws SQLException if talking to the DB failed.
	 */
	protected static void addColumnBooleanNotNull(Connection connection, String tableName, String columnName) throws SQLException {
		DatabaseType databaseType = getDatabaseType(connection);
		String sql;
		switch (databaseType) {
			case DERBY:
				// DN just tried the following
				//
				//   ALTER TABLE "lastsynctoremoterepo" ADD "resyncmode" CHAR(1) NOT NULL CHECK ("resyncmode" IN ('Y','N'))
				//
				// which failed, because there are rows and there is no default value specified. I thus now add the default manually
				// here, and only for Derby. PostgreSQL-support was lately added. No need to support PostgreSQL, yet.
				// We'll add support for PostgreSQL here, when we need it, later.
				sql = "ALTER TABLE " + quoteIdentifier(tableName) + " "
						+ "ADD COLUMN " + quoteIdentifier(columnName) + " CHAR(1) NOT NULL DEFAULT 'N' "
						+ "CHECK (" + quoteIdentifier(columnName) + " IN ('Y','N'))";
				break;

			default:
				throw new IllegalStateException("Not yet supported for databaseType=" + databaseType);
		}
		try (Statement statement = connection.createStatement()) {
			logger.info("addColumnBooleanNotNull: SQL=>>{}<<", sql);
			statement.executeUpdate(sql);
		}
	}
}
