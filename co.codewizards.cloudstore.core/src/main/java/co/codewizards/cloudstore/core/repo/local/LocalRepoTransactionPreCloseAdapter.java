package co.codewizards.cloudstore.core.repo.local;

public abstract class LocalRepoTransactionPreCloseAdapter implements LocalRepoTransactionPreCloseListener {

	@Override
	public void preCommit(LocalRepoTransactionPreCloseEvent event) {
	}

	@Override
	public void preRollback(LocalRepoTransactionPreCloseEvent event) {
	}
}
