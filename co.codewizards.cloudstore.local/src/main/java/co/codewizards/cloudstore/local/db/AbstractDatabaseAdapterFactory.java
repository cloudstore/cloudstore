package co.codewizards.cloudstore.local.db;

public abstract class AbstractDatabaseAdapterFactory implements DatabaseAdapterFactory {
	
	@Override
	public String getDisableReason() {
		return null;
	}

	@Override
	public DatabaseAdapter createDatabaseAdapter() {
		final DatabaseAdapter databaseAdapter = _createDatabaseAdapter();
		if (databaseAdapter == null)
			throw new IllegalStateException(String.format("databaseAdapterFactory._createDatabaseAdapter() returned null! Implementation error in %s!",
					this.getClass().getName()));

		if (databaseAdapter instanceof AbstractDatabaseAdapter)
			((AbstractDatabaseAdapter) databaseAdapter).setFactory(this);

		return databaseAdapter;
	}

	protected abstract DatabaseAdapter _createDatabaseAdapter();
}
