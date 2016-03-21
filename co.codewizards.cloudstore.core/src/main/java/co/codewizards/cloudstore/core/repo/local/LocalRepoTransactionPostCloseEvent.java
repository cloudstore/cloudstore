package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.EventObject;

import co.codewizards.cloudstore.core.context.ExtensibleContext;

public class LocalRepoTransactionPostCloseEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private final LocalRepoManager localRepoManager;

	public LocalRepoTransactionPostCloseEvent(LocalRepoTransaction source) {
		super(source);
		localRepoManager = assertNotNull("source", source).getLocalRepoManager();
	}

	@Override
	public LocalRepoTransaction getSource() {
		return (LocalRepoTransaction) super.getSource();
	}

	/**
	 * Gets the <b>closed</b> {@link LocalRepoTransaction}.
	 * <p>
	 * Please note that this event is fired after the transaction was already closed. This object thus
	 * cannot be used for anything else than accessing its
	 * {@linkplain ExtensibleContext#getContextObject(Class) context-objects}.
	 * <p>
	 * Alternatively, you might want to access the {@link #getLocalRepoManager() localRepoManager}
	 * and create a new transaction.
	 * @return the <b>closed</b> {@link LocalRepoTransaction}. Never <code>null</code>.
	 */
	public LocalRepoTransaction getTransaction() {
		return getSource();
	}

	/**
	 * Gets the {@code LocalRepoManager}.
	 * @return the {@code LocalRepoManager}. Never <code>null</code>.
	 */
	public LocalRepoManager getLocalRepoManager() {
		return localRepoManager;
	}
}
