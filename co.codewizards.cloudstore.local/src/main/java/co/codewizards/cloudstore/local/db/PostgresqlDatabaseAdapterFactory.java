package co.codewizards.cloudstore.local.db;

public class PostgresqlDatabaseAdapterFactory extends ExternalJdbcDatabaseAdapterFactory {

	@Override
	public String getName() {
		return "postgresql";
	}

	@Override
	public int getPriority() {
		return 90;
	}

	@Override
	protected DatabaseAdapter _createDatabaseAdapter() {
		return new PostgresqlDatabaseAdapter();
	}

}
