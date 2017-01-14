package co.codewizards.cloudstore.local.db;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.local.db.DatabaseAdapterFactory.*;

import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigImpl;

public class DatabaseAdapterFactoryRegistry {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseAdapterFactoryRegistry.class);

	private DatabaseAdapterFactory databaseAdapterFactory;

	protected DatabaseAdapterFactoryRegistry() { }

	private static final class Holder {
		public static final DatabaseAdapterFactoryRegistry instance = new DatabaseAdapterFactoryRegistry();
	}

	public static DatabaseAdapterFactoryRegistry getInstance() {
		return Holder.instance;
	}

	/**
	 * @deprecated Should normally only be used by tests.
	 */
	@Deprecated
	public void clearCache() {
		logger.info("clearCache: entered.");
		databaseAdapterFactory = null;
	}

	public DatabaseAdapter createDatabaseAdapter() {
		final DatabaseAdapterFactory databaseAdapterFactory= getDatabaseAdapterFactoryOrFail();
		final DatabaseAdapter databaseAdapter = databaseAdapterFactory.createDatabaseAdapter();
		if (databaseAdapter == null)
			throw new IllegalStateException(String.format("databaseAdapterFactory.createDatabaseAdapter() returned null! Implementation error in %s!",
					databaseAdapterFactory.getClass().getName()));

		return databaseAdapter;
	}

	protected DatabaseAdapterFactory getDatabaseAdapterFactoryOrFail() {
		DatabaseAdapterFactory databaseAdapterFactory = this.databaseAdapterFactory;
		if (databaseAdapterFactory == null) {
			final String databaseAdaptorName = ConfigImpl.getInstance().getPropertyAsNonEmptyTrimmedString(
					CONFIG_KEY_DATABASE_ADAPTER_NAME, DEFAULT_DATABASE_ADAPTER_NAME);

			final SortedMap<String, DatabaseAdapterFactory> name2DatabaseAdapter = getName2DatabaseAdapterFactory();
			if (name2DatabaseAdapter.isEmpty())
				throw new IllegalStateException("There is no DatabaseAdapterFactory registered!");

			if (isEmpty(databaseAdaptorName))
				databaseAdapterFactory = getDatabaseAdapterFactoryWithHighestPriority(name2DatabaseAdapter);
			else {
				databaseAdapterFactory = name2DatabaseAdapter.get(databaseAdaptorName);
				if (databaseAdapterFactory == null)
					throw new IllegalArgumentException(String.format("There is no DatabaseAdapterFactory with name='%s'!", databaseAdaptorName));
			}

			this.databaseAdapterFactory = databaseAdapterFactory;
		}
		else
			logger.debug("getDatabaseAdapterFactoryOrFail: returning existing DatabaseAdapterFactory instance.");

		return databaseAdapterFactory;
	}

	private static SortedMap<String, DatabaseAdapterFactory> getName2DatabaseAdapterFactory() {
		SortedMap<String, DatabaseAdapterFactory> result = new TreeMap<String, DatabaseAdapterFactory>();
		for (final DatabaseAdapterFactory a : ServiceLoader.load(DatabaseAdapterFactory.class)) {
			final String name = a.getName();
			assertNotNull(name, String.format("%s.getName()", a.getClass().getName()));
			if (name.indexOf(' ') >= 0)
				throw new IllegalStateException(
						String.format("%s.getName() returned a symbolic name containing a space!", a.getClass().getName()));

			final DatabaseAdapterFactory old = result.put(name, a);
			if (old != null)
				throw new IllegalStateException(
							String.format("There are multiple DatabaseAdapterFactory classes with name='%s'! %s + %s",
									name, a.getClass().getName(), old.getClass().getName()));
		}
		return result;
	}

	private static DatabaseAdapterFactory getDatabaseAdapterFactoryWithHighestPriority(final SortedMap<String, DatabaseAdapterFactory> name2DatabaseAdapter) {
		DatabaseAdapterFactory databaseAdapterFactory = null;
		for (final DatabaseAdapterFactory a : name2DatabaseAdapter.values()) {
			if (databaseAdapterFactory == null || databaseAdapterFactory.getPriority() < a.getPriority())
				databaseAdapterFactory = a;
		}
		return databaseAdapterFactory;
	}
}
