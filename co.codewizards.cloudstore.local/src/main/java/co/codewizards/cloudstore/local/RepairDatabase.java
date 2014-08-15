package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import co.codewizards.cloudstore.core.oio.file.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepairDatabase implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(RepairDatabase.class);

	private final File localRoot;

	private Connection connection;
	private Statement statement;

	public RepairDatabase(File localRoot) {
		this.localRoot = assertNotNull("localRoot", localRoot);
	}

	@Override
	public void run() {
		try {
			JdbcConnectionFactory jdbcConnectionFactory = new JdbcConnectionFactory(localRoot);
			connection = jdbcConnectionFactory.createConnection();
			try {
				statement = connection.createStatement();
				try {
					executeDerbyCheckTable();
					dropForeignKeys();
					dropIndices();
					executeDerbyCheckTable();
				} finally {
					statement.close();
				}
			} finally {
				connection.close();
			}
		} catch (SQLException x) {
			throw new RuntimeException(x);
		}
	}

	private void executeDerbyCheckTable() throws SQLException {
		// http://objectmix.com/apache/646586-derby-db-files-get-corrupted-2.html
		statement.execute(
				"SELECT schemaname, tablename, SYSCS_UTIL.SYSCS_CHECK_TABLE(schemaname, tablename) "
				+ "FROM sys.sysschemas s, sys.systables t "
				+ "WHERE s.schemaid = t.schemaid");
	}

	private void dropForeignKeys() throws SQLException { // DataNucleus will re-create them.
		for (String tableName : getTableNames()) {
			for (String foreignKeyName : getForeignKeyNames(tableName)) {
				try {
					statement.execute(String.format("ALTER TABLE \"%s\" DROP CONSTRAINT \"%s\"", tableName, foreignKeyName));
					logger.info("dropForeignKeys: Dropped foreign-key '{}' of table '{}'.", foreignKeyName, tableName);
				} catch (SQLException x) {
					logger.warn("dropForeignKeys: Could not drop foreign-key '{}' of table '{}': {}", foreignKeyName, tableName, x.toString());
				}
			}
		}
	}

	private void dropIndices() throws SQLException { // DataNucleus will re-create them.
		for (String tableName : getTableNames()) {
			for (String indexName : getIndexNames(tableName)) {
				try {
					statement.execute(String.format("DROP INDEX \"%s\"", indexName));
					logger.info("dropIndices: Dropped index '{}'.", indexName);
				} catch (SQLException x) {
					logger.warn("dropIndices: Could not drop index '{}': {}", indexName, x.toString());
				}
			}
		}
	}

	private Collection<String> getTableNames() throws SQLException
	{
		ArrayList<String> res = new ArrayList<String>();

		final ResultSet rs = connection.getMetaData().getTables(null, null, null, null);
		while (rs.next()) {
			final String tableName = rs.getString("TABLE_NAME");
			final String tableType = rs.getString("TABLE_TYPE");

			if ("SEQUENCE".equals(tableType == null ? null : tableType.toUpperCase()))
				continue;

			if (tableName.toLowerCase().startsWith("sys"))
				continue;

			res.add(tableName);
		}
		rs.close();

		return res;
	}

	private Collection<String> getForeignKeyNames(String tableName) throws SQLException {
		Set<String> tableNameAndForeignKeyNameSet = new HashSet<>();
		ArrayList<String> res = new ArrayList<String>();

		for (String toTableName : getTableNames()) {
			ResultSet rs = connection.getMetaData().getCrossReference(null, null, toTableName, null, null, tableName);
			while (rs.next()) {
//				String parentKeyTableName = rs.getString("PKTABLE_NAME");
//				String foreignKeyTableName = rs.getString("FKTABLE_NAME");
				String foreignKeyName = rs.getString("FK_NAME");
				if (foreignKeyName == null)
					continue;

//				if (foreignKeyTableName != null && !tableName.equals(foreignKeyTableName))
//					continue;

				String tableNameAndForeignKeyName = tableName + '.' + foreignKeyName;
				if (tableNameAndForeignKeyNameSet.add(tableNameAndForeignKeyName))
					res.add(foreignKeyName);
			}
			rs.close();
		}

		return res;
	}

	private Collection<String> getIndexNames(String tableName) throws SQLException {
		ArrayList<String> res = new ArrayList<String>();

		ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName, false, true);
		while (rs.next()) {
			String indexName = rs.getString("INDEX_NAME");
			if (indexName == null)
				continue;

			res.add(indexName);
		}
		rs.close();

		return res;
	}
}
