open module co.codewizards.cloudstore.local {

	requires transitive javax.jdo;
	requires transitive org.datanucleus;

	requires transitive co.codewizards.cloudstore.core;
//	requires static co.codewizards.cloudstore.core.oio.nio; // not needed, because communication works exclusively over service.

	requires transitive log4j;

	exports co.codewizards.cloudstore.local;
	exports co.codewizards.cloudstore.local.db;
	exports co.codewizards.cloudstore.local.dbupdate;
	exports co.codewizards.cloudstore.local.dto;
	exports co.codewizards.cloudstore.local.persistence;
	exports co.codewizards.cloudstore.local.transport;

	uses co.codewizards.cloudstore.local.db.DatabaseAdapterFactory;
	uses co.codewizards.cloudstore.local.persistence.CloudStorePersistenceCapableClassesProvider;
	uses co.codewizards.cloudstore.local.dbupdate.DbUpdateStep;
	uses co.codewizards.cloudstore.local.PersistencePropertiesVariableProvider;

	provides co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory
		with co.codewizards.cloudstore.local.LocalRepoManagerFactoryImpl;

	provides co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionListener
		with co.codewizards.cloudstore.local.AutoTrackLifecycleListener;

	provides co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory
		with co.codewizards.cloudstore.local.transport.FileRepoTransportFactory;

	provides co.codewizards.cloudstore.local.db.DatabaseAdapterFactory
		with
			co.codewizards.cloudstore.local.db.DerbyDatabaseAdapterFactory,
			co.codewizards.cloudstore.local.db.PostgresqlDatabaseAdapterFactory;

	provides co.codewizards.cloudstore.local.persistence.CloudStorePersistenceCapableClassesProvider
		with co.codewizards.cloudstore.local.persistence.CloudStorePersistenceCapableClassesProviderImpl;

	provides co.codewizards.cloudstore.local.dbupdate.DbUpdateStep
		with
			co.codewizards.cloudstore.local.dbupdate.DbUpdateStep002,
			co.codewizards.cloudstore.local.dbupdate.DbUpdateStep003,
			co.codewizards.cloudstore.local.dbupdate.DbUpdateStep004;

	provides co.codewizards.cloudstore.local.PersistencePropertiesVariableProvider
		with co.codewizards.cloudstore.local.db.ExternalJdbcPersistencePropertiesVariableProvider;
}