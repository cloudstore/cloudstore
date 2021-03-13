package co.codewizards.cloudstore.local.dbupdate;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbUpdateStep004 extends AbstractDbUpdateStep {

	private static final Logger logger = LoggerFactory.getLogger(DbUpdateStep004.class);

	@Override
	public int getVersion() {
		return 4;
	}

	@Override
	public void performUpdate() throws Exception {
		logger.info("performUpdate: entered.");
		try (Connection connection = getDatabaseAdapterOrFail().createConnection()) {
			DatabaseType databaseType = getDatabaseType(connection);
			if (databaseType != DatabaseType.DERBY) {
				logger.info("performUpdate: This update-step is not supported for the database-type {}. Skipping!", databaseType);
				return;
			}
			logger.info("performUpdate: Database-type: {}", databaseType);

			final String tableName = "LastSyncToRemoteRepo";
			final String columnName = "resyncMode";
			if (! doesTableExist(connection, tableName)) {
				logger.info("performUpdate: Table '{}' does not exist!", tableName);
				return;
			}
			logger.info("performUpdate: Table '{}' does exist! Checking for existence of column '{}'.", tableName, columnName);

			// Maybe this column was already added by DataNucleus. We removed the >>@Column(defaultValue = "N")<< because of PostgreSQL. Hence,
			// most DBs likely already have the column, but those which have not must be manually adapted now.
			if (! doesColumnExist(connection, tableName, columnName)) {
				addColumnBooleanNotNull(connection, tableName, columnName);
			}
		}
	}

}
