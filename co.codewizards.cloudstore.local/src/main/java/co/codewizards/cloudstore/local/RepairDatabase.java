package co.codewizards.cloudstore.local;

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

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.AssertUtil;

public class RepairDatabase implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(RepairDatabase.class);

	private final File localRoot;

	private Connection connection;
	private Statement statement;

	public RepairDatabase(File localRoot) {
		this.localRoot = AssertUtil.assertNotNull(localRoot, "localRoot");
	}

	@Override
	public void run() {
		try {
			JdbcConnectionFactory jdbcConnectionFactory = new JdbcConnectionFactory(localRoot);
			connection = jdbcConnectionFactory.createConnection();
			try {
				statement = connection.createStatement();
				try {
//					testInsert();
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

//	private void testInsert() throws SQLException {
//		connection.setAutoCommit(false);
//		try {
//			long filechunkpayload_id_oid;
//			long histocryptorepofile_id_oid;
//			int length;
//			long offset;
//			Timestamp changed;
//			Timestamp created;
//
//			try (ResultSet rs = statement.executeQuery("select * from \"histofilechunk\" order by \"id\"")) {
//				if (! rs.next()) {
//					logger.warn("Table \"histofilechunk\" is empty! Cannot obtain test data!");
//					return;
//				}
//
//				filechunkpayload_id_oid = rs.getLong("filechunkpayload_id_oid");
//				histocryptorepofile_id_oid = rs.getLong("histocryptorepofile_id_oid");
//				length = rs.getInt("length");
//				offset = rs.getLong("offset");
//				changed = rs.getTimestamp("changed");
//				created = rs.getTimestamp("created");
//			}
//
//			++offset; // there is a unique key => must change the offset!
//
//			logger.info("testInsert: filechunkpayload_id_oid={}, histocryptorepofile_id_oid={}, length={}, offset={}, changed={}, created={}",
//					filechunkpayload_id_oid, histocryptorepofile_id_oid, length, offset, changed, created);
//
//			try (PreparedStatement ps = connection.prepareStatement(
//					"INSERT INTO \"histofilechunk\""
//				    + " (\"filechunkpayload_id_oid\",\"histocryptorepofile_id_oid\",\"length\",\"offset\",\"changed\",\"created\")"
//				    + " VALUES (?,?,?,?,?,?)")) {
//
//				int paramIdx = 0;
//				ps.setLong(++paramIdx, filechunkpayload_id_oid);
//				ps.setLong(++paramIdx, histocryptorepofile_id_oid);
//				ps.setInt(++paramIdx, length);
//				ps.setLong(++paramIdx, offset);
//				ps.setTimestamp(++paramIdx, changed);
//				ps.setTimestamp(++paramIdx, created);
//
//				try {
//					ps.execute();
//				} catch (Exception x) {
//					logger.error("testInsert: " + x, x);
//					return;
//				}
//			}
//			logger.info("testInsert: Success!");
//		} finally {
//			connection.rollback();
//			connection.setAutoCommit(true);
//		}
//	}

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
