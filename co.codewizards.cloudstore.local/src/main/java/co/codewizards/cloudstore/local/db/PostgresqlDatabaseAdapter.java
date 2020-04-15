package co.codewizards.cloudstore.local.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.SortedMap;

public class PostgresqlDatabaseAdapter extends ExternalJdbcDatabaseAdapter {

	@Override
	public boolean passivateIdentityColumn(Connection connection, Table table, SortedMap<String, Column> columnName2Column) throws Exception {
		Column idCol = columnName2Column.get("ID");
		if (idCol == null)
			return false;

		if (! Boolean.TRUE.equals(idCol.autoIncrement))
			return false;

		// No need to change the structure. The sequence is used *optionally* and we can write
		// into the table without modifying the structure here.
		// We only have to update the sequence's next value -- see below.
		return true;
	}

	@Override
	public void activateIdentityColumn(Connection connection, Table table, SortedMap<String, Column> columnName2Column) throws Exception {
		String sequenceName = table.name + "_id_seq";

		try (Statement statement = connection.createStatement()) {
			long nextId;
			String sql = String.format("select max(id) from %s", table.name);
			try (ResultSet rs = statement.executeQuery(sql)) {
				if (! rs.next())
					throw new IllegalStateException("SELECT MAX(...) returned no row! Table: " + table.name);

				nextId = rs.getLong(1);
				if (rs.wasNull())
					nextId = 1;
				else
					nextId += 1;

				if (rs.next())
					throw new IllegalStateException("SELECT MAX(...) returned multiple rows! Table: " + table.name);
			}

			sql = String.format("alter sequence %s restart with %s", sequenceName, nextId);
			statement.executeUpdate(sql);
		}
	}

}
