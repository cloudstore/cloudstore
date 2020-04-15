package co.codewizards.cloudstore.local.db;

import java.sql.Connection;
import java.util.SortedMap;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;

public interface DatabaseAdapter extends AutoCloseable {
	UUID getRepositoryId();
	void setRepositoryId(UUID repositoryId);

	File getLocalRoot();
	void setLocalRoot(File localRoot);

	void createPersistencePropertiesFileAndDatabase() throws Exception;
	Connection createConnection() throws Exception;

	/**
	 * If the database is embedded, then shut it down. Do not shut down a remote database server!
	 */
	void shutdownEmbeddedDatabase();

	boolean passivateIdentityColumn(Connection connection, Table table, SortedMap<String, Column> columnName2Column) throws Exception;

	void activateIdentityColumn(Connection connection, Table table, SortedMap<String, Column> columnName2Column) throws Exception;
}
