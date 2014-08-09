package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class LocalRepoTransactionListenerRegistry {

	private final LocalRepoTransaction transaction;

	private final List<LocalRepoTransactionListener> listeners;
	private static List<Class<? extends LocalRepoTransactionListener>> listenerClasses;

	public LocalRepoTransactionListenerRegistry(final LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("transaction", transaction);
		this.listeners = createListeners();
	}

	public LocalRepoTransaction getTransaction() {
		return transaction;
	}

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being begun.
	 * @see #onCommit()
	 * @see #onRollback()
	 */
	public void onBegin() {
		for (final LocalRepoTransactionListener listener : listeners)
			listener.onBegin();
	}

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being committed.
	 * @see #onBegin()
	 * @see #onRollback()
	 */
	public void onCommit() {
		for (final LocalRepoTransactionListener listener : listeners)
			listener.onCommit();
	}

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being rolled back.
	 * @see #onBegin()
	 * @see #onCommit()
	 */
	public void onRollback() {
		for (final LocalRepoTransactionListener listener : listeners)
			listener.onRollback();
	}

	private List<LocalRepoTransactionListener> createListeners() {
		if (listenerClasses == null) {
			final List<LocalRepoTransactionListener> listeners = new LinkedList<>();
			final Iterator<LocalRepoTransactionListener> iterator = ServiceLoader.load(LocalRepoTransactionListener.class).iterator();
			while (iterator.hasNext()) {
				final LocalRepoTransactionListener listener = iterator.next();
				listener.setTransaction(transaction);
				listeners.add(listener);
			}

			sortListeners(listeners);

			final List<Class<? extends LocalRepoTransactionListener>> lcl = new ArrayList<>(listeners.size());
			for (final LocalRepoTransactionListener listener : listeners)
				lcl.add(listener.getClass());

			listenerClasses = lcl;
			return listeners;
		}
		else {
			final List<LocalRepoTransactionListener> listeners = new ArrayList<>(listenerClasses.size());
			for (final Class<? extends LocalRepoTransactionListener> lc : listenerClasses) {
				final LocalRepoTransactionListener listener = createInstance(lc);
				listener.setTransaction(transaction);
				listeners.add(listener);
			}

			return listeners;
		}
	}

	private void sortListeners(final List<LocalRepoTransactionListener> listeners) {
		Collections.sort(listeners, new Comparator<LocalRepoTransactionListener>() {
			@Override
			public int compare(final LocalRepoTransactionListener o1, final LocalRepoTransactionListener o2) {
				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});
	}

	private static <T> T createInstance(final Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}