package co.codewizards.cloudstore.core.repo.local;

import java.util.EventListener;

public interface LocalRepoTransactionPreCloseListener extends EventListener {

	void preCommit(LocalRepoTransactionPreCloseEvent event);

	void preRollback(LocalRepoTransactionPreCloseEvent event);

}
