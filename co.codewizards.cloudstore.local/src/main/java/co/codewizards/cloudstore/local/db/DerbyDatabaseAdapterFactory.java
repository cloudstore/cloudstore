package co.codewizards.cloudstore.local.db;

public class DerbyDatabaseAdapterFactory extends AbstractDatabaseAdapterFactory {

	@Override
	public String getName() {
		return "derby";
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	protected DatabaseAdapter _createDatabaseAdapter() {
		return new DerbyDatabaseAdapter();
	}
}
