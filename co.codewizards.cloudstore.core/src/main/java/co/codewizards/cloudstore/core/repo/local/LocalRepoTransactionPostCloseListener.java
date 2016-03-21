package co.codewizards.cloudstore.core.repo.local;

import java.util.EventListener;

public interface LocalRepoTransactionPostCloseListener extends EventListener {

	void postCommit(LocalRepoTransactionPostCloseEvent event);

	void postRollback(LocalRepoTransactionPostCloseEvent event);

}
