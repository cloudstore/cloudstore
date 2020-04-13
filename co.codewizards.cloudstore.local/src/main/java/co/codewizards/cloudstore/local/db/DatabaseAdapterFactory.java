package co.codewizards.cloudstore.local.db;

import co.codewizards.cloudstore.core.oio.File;

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

	/**
	 * Get the reason <i>not</i> to use this factory.
	 * <p>
	 * Checks the current configuration and environment. If the {@link DatabaseAdapter} created by this factory
	 * can operate in the current situation, this method returns <code>null</code> (or an empty {@code String}).
	 * Otherwise, it returns the reason, why this database-adapter cannot be used.
	 * @return <code>null</code> if this factory is active and its {@code DatabaseAdapter} can be used.
	 */
	String getDisableReason();

	DatabaseAdapter createDatabaseAdapter();

	boolean isLocalRootSupported(File localRoot);
}
