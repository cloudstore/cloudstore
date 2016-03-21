package co.codewizards.cloudstore.core.repo.local;

public abstract class LocalRepoTransactionPostCloseAdapter implements LocalRepoTransactionPostCloseListener {

	@Override
	public void postCommit(LocalRepoTransactionPostCloseEvent event) {
	}

	@Override
	public void postRollback(LocalRepoTransactionPostCloseEvent event) {
	}
}
