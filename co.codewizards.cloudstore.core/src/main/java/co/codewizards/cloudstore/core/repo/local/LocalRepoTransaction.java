package co.codewizards.cloudstore.core.repo.local;

import co.codewizards.cloudstore.core.context.ExtensibleContext;

public interface LocalRepoTransaction extends AutoCloseable, DaoProvider, ExtensibleContext {

	void commit();

	boolean isActive();

	void rollback();

	void rollbackIfActive();

	/**
	 * Equivalent to {@link #rollbackIfActive()}.
	 * <p>
	 * Implementations must make sure that invoking {@code close()} means exactly the same as invoking
	 * {@code rollbackIfActive()}. This method was added to make the usage of {@code LocalRepoTransaction}
	 * possible in a try-with-resources-clause. See {@link AutoCloseable} for more details. Here's a code
	 * example:
	 * <pre>  try ( LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction(); ) {
	 *    // Write sth. into the database...
	 *
	 *    // And don't forget to commit!
	 *    transaction.commit();
	 *  }</pre>
	 * <p>
	 * @see #rollbackIfActive()
	 */
	@Override
	public void close();

	long getLocalRevision();

	LocalRepoManager getLocalRepoManager();

	void flush();
}
