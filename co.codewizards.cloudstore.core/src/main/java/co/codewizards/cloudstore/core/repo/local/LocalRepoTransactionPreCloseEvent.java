package co.codewizards.cloudstore.core.repo.local;

import java.util.EventObject;

public class LocalRepoTransactionPreCloseEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	public LocalRepoTransactionPreCloseEvent(LocalRepoTransaction source) {
		super(source);
	}

	@Override
	public LocalRepoTransaction getSource() {
		return (LocalRepoTransaction) super.getSource();
	}

	/**
	 * Gets the <b>active</b> {@link LocalRepoTransaction}.
	 * @return the <b>active</b> {@link LocalRepoTransaction}. Never <code>null</code>.
	 */
	public LocalRepoTransaction getTransaction() {
		return getSource();
	}

	/**
	 * Gets the {@code LocalRepoManager}.
	 * @return the {@code LocalRepoManager}. Never <code>null</code>.
	 */
	public LocalRepoManager getLocalRepoManager() {
		return getTransaction().getLocalRepoManager();
	}
}
