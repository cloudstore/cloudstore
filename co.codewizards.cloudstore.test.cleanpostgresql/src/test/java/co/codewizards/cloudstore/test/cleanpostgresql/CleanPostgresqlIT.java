package co.codewizards.cloudstore.test.cleanpostgresql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanPostgresqlIT {
	private static final Logger logger = LoggerFactory.getLogger(CleanPostgresqlIT.class);

	@Test
	public void cleanPostgresql() throws Exception {
		String hostName = getEnvOrFail("TEST_PG_HOST_NAME");
		String userName = getEnvOrFail("TEST_PG_USER_NAME");
		String password = getEnvOrFail("TEST_PG_PASSWORD");

		String url = "jdbc:postgresql://" + hostName + "/";

		try (Connection connection = DriverManager.getConnection(url, userName, password)) {
			SortedSet<String> databaseNames = new TreeSet<String>();
			try (Statement statement = connection.createStatement()) {
				ResultSet rs = statement.executeQuery("select DATNAME from PG_DATABASE");
				while (rs.next()) {
					String databaseName = rs.getString(1);
					if (databaseName != null
							&& databaseName.startsWith("TEST_")
							&& databaseName.endsWith("_TEST")) {
						databaseNames.add(databaseName);
					}
				}
			}

			for (String databaseName : databaseNames) {
				if (databaseName.indexOf('"') >= 0) {
					logger.warn("Database-name '{}' contains illegal character! Skipping this database.", databaseName);
					continue;
				}

				logger.info("*** DROPPING DATABASE '{}' ***", databaseName);

				try (Statement statement = connection.createStatement()) {
					String sql = String.format("drop database \"%s\"", databaseName);
					statement.executeUpdate(sql);
				}
			}
		}
	}

	protected static String getEnvOrFail(String key) {
		String value = System.getenv(key);
		if (value == null)
			throw new IllegalStateException("Environment-variable not set: " + key);

		return value;
	}
}
