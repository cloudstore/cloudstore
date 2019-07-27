package co.codewizards.cloudstore.core.repo.local;

import static java.util.Objects.*;

public abstract class AbstractLocalRepoTransactionListener implements LocalRepoTransactionListener {

	private LocalRepoTransaction transaction;

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public LocalRepoTransaction getTransaction() {
		return transaction;
	}

	protected LocalRepoTransaction getTransactionOrFail() {
		final LocalRepoTransaction transaction = getTransaction();
		requireNonNull(transaction, "transaction");
		return transaction;
	}

	@Override
	public void setTransaction(final LocalRepoTransaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public void onBegin() {
		// override to react on this!
	}

	@Override
	public void onCommit() {
		// override to react on this!
	}

	@Override
	public void onRollback() {
		// override to react on this!
	}

}
