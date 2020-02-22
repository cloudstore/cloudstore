package co.codewizards.cloudstore.local.dbupdate;

import co.codewizards.cloudstore.local.db.DatabaseAdapter;

/**
 * Database-update-step used to convert an older database to a newer version.
 * <p>
 * Usually, it is <i>not</i> necessary to implement anything as nearly all updates are done implicitly and automatically
 * by DataNucleus. For example, it automatically adds new tables, new columns, new indices etc. But sometimes, it may
 * be needed to explicitly and manually convert some data or DB-structure in order to update the DB.
 * <p>
 * Because it is so rare, we do not provide an elaborate update-system (e.g. we do not include Liquibase). Also we
 * sometimes need to change other stuff, not in the database (e.g. the update from version 1 to version 2 requires
 * changing the persistence-properties-files -- not the databases). 
 * <p>
 * <b>Important note for downstream-projects:</b> Every version allocated by subshare or another downstream-project must be
 * also allocated here in cloudstore to prevent collisions. Multiple implementations of a {@code DbUpdateStep}
 * are supported, hence you can place an empty implementation for a certain version here in cloudstore and the real
 * implementation in the downstream-project. This is not very nice, but we hardly ever need this and thus we do not
 * need a more elaborate DB-update-system; better accepting this more complex handling.
 * 
 * @author mangu
 */
public interface DbUpdateStep {

	/**
	 * Gets the target-version to which this step updates the DB.
	 * <p>
	 * Multiple {@code DbUpdateStep}-implementations may declare the same target-version. They are then invoked according
	 * to their {@link #getOrderHint() orderHint} (ascending) and their fully qualified class-name (in case the
	 * {@code orderHint} is the same), before the DB-version in the properties-file is incremented.
	 * <p>
	 * It is possible that a {@code DbUpdateStep}-implementation is invoked multiple times. The implementation must
	 * make sure that it can operate under these circumstances and does not fail (but silently skip the work), if
	 * invoked a 2nd or 3rd time.
	 * 
	 * @return the target-version to which this step updates the DB.
	 */
	int getVersion();

	int getOrderHint();

	void performUpdate() throws Exception;

	DatabaseAdapter getDatabaseAdapter();
	void setDatabaseAdapter(DatabaseAdapter databaseAdapter);

}
