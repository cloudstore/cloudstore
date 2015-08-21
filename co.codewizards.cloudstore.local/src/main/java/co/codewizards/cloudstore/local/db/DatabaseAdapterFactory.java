package co.codewizards.cloudstore.local.db;

public interface DatabaseAdapterFactory {

	/**
	 * Configuration property key used to select a certain database adapter. If this is not specified or set
	 * to an empty value, the adapter with the highest {@link #getPriority() priority} is used.
	 */
	String CONFIG_KEY_DATABASE_ADAPTER_NAME = "databaseAdapter.name";

	/**
	 * Default value for {@link #CONFIG_KEY_DATABASE_ADAPTER_NAME} (applicable, if not specified by user).
	 */
	String DEFAULT_DATABASE_ADAPTER_NAME = null;

	/**
	 * Gets the symbolic name of this adapter.
	 * <p>
	 * Usually, this matches the JDBC-sub-protocol-name of the database being used (e.g. "derby" or "mysql"), but
	 * in case there are multiple adapters for the same database type, they must have a different name.
	 * <p>
	 * This symbolic name must be unique. It can be used via the configuration property {@link #CONFIG_KEY_DATABASE_ADAPTER_NAME}
	 * to force a certain adapter.
	 * @return the symbolic name of this adapter. Must not be <code>null</code>, must not be empty and
	 * must not contain spaces.
	 */
	String getName();

	int getPriority();

	DatabaseAdapter createDatabaseAdapter();
}
