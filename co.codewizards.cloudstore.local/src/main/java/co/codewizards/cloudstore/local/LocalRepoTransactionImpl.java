package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.context.ExtensibleContextSupport;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionListenerRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPostCloseEvent;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPostCloseListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPreCloseEvent;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionPreCloseListener;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;

public class LocalRepoTransactionImpl implements LocalRepoTransaction, ContextWithLocalRepoManager, ContextWithPersistenceManager {
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoTransactionImpl.class);

	public static final long LOCK_TIMEOUT = 5 * 60 * 1000;

	private final LocalRepoManager localRepoManager;
	private final PersistenceManagerFactory persistenceManagerFactory;
	private final boolean write;
	private PersistenceManager persistenceManager;
	private Transaction jdoTransaction;
	private final Lock lock;
	private long localRevision = -1;
	private final Map<Class<?>, Object> daoClass2Dao = new HashMap<>();
	private final ExtensibleContextSupport extensibleContextSupport = new ExtensibleContextSupport();

	private final LocalRepoTransactionListenerRegistry listenerRegistry = new LocalRepoTransactionListenerRegistry(this);

	private final CopyOnWriteArrayList<LocalRepoTransactionPreCloseListener> preCloseListeners = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<LocalRepoTransactionPostCloseListener> postCloseListeners = new CopyOnWriteArrayList<>();

	public LocalRepoTransactionImpl(final LocalRepoManagerImpl localRepoManager, final boolean write) {
		this.localRepoManager = requireNonNull(localRepoManager, "localRepoManager");
		this.persistenceManagerFactory = requireNonNull(localRepoManager.getPersistenceManagerFactory(), "localRepoManager.persistenceManagerFactory");
		this.lock = localRepoManager.getLock();
		this.write = write;
		begin();
	}

	private void begin() {
		boolean locked = false;
		try {
			locked = lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// ignore
		}
		if (! locked)
			throw new TimeoutException(String.format("Starting %s transaction on '%s' within timeout (%s ms) failed! ", write ? "write" : "read", localRepoManager.getLocalRoot(), LOCK_TIMEOUT));
		try {
			if (isActive())
				throw new IllegalStateException("Transaction is already active!");

			lockIfWrite();

			persistenceManager = persistenceManagerFactory.getPersistenceManager();
			jdoTransaction = persistenceManager.currentTransaction();
			jdoTransaction.begin();
			listenerRegistry.onBegin();
		} finally {
			lock.unlock();
		}
	}

	private final void lockIfWrite() {
		if (write)
			lock.lock(); // UNbalance lock to keep it after method returns!
	}

	private final void unlockIfWrite() {
		if (write)
			lock.unlock(); // UNbalance unlock to counter the unbalanced lock in lockIfWrite().
	}

	@Override
	public void commit() {
		lock.lock();
		try {
			if (!isActive())
				throw new IllegalStateException("Transaction is not active!");

			listenerRegistry.onCommit();
			firePreCloseListeners(true);
			daoClass2Dao.clear();
			jdoTransaction.commit();
			persistenceManager.close();
			jdoTransaction = null;
			persistenceManager = null;
			localRevision = -1;

			unlockIfWrite();
		} finally {
			lock.unlock();
		}
		firePostCloseListeners(true);
	}

	@Override
	public boolean isActive() {
		lock.lock();
		try {
			return jdoTransaction != null && jdoTransaction.isActive();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void rollback() {
		_rollback();
		firePostCloseListeners(false);
	}

	@Override
	public void rollbackIfActive() {
		boolean active;
		lock.lock();
		try {
			active = isActive();
			if (active) {
				_rollback();
			}
		} finally {
			lock.unlock();
		}
		if (active) {
			firePostCloseListeners(false);
		}
	}

	protected void _rollback() {
		lock.lock();
		try {
			if (!isActive())
				throw new IllegalStateException("Transaction is not active!");

			listenerRegistry.onRollback();
			firePreCloseListeners(false);
			daoClass2Dao.clear();
			jdoTransaction.rollback();
			persistenceManager.close();
			jdoTransaction = null;
			persistenceManager = null;
			localRevision = -1;

			unlockIfWrite();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		rollbackIfActive();
	}

	@Override
	public PersistenceManager getPersistenceManager() {
		if (!isActive()) {
			throw new IllegalStateException("Transaction is not active!");
		}
		return persistenceManager;
	}

	@Override
	public long getLocalRevision() {
		if (localRevision < 0) {
			if (!write)
				throw new IllegalStateException("This is a read-only transaction!");

			jdoTransaction.setSerializeRead(true);
			final LocalRepository lr = getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
			jdoTransaction.setSerializeRead(null);
			localRevision = lr.getRevision() + 1;
			lr.setRevision(localRevision);
			persistenceManager.flush();
		}
		return localRevision;
	}

	@Override
	public LocalRepoManager getLocalRepoManager() {
		return localRepoManager;
	}

	@Override
	public <D> D getDao(final Class<D> daoClass) {
		requireNonNull(daoClass, "daoClass");

		@SuppressWarnings("unchecked")
		D dao = (D) daoClass2Dao.get(daoClass);

		if (dao == null) {
			final PersistenceManager pm = getPersistenceManager();
			dao = createObject(daoClass);

			if (!(dao instanceof Dao))
				throw new IllegalStateException(String.format("dao class %s does not extend Dao!", daoClass.getName()));

			((Dao<?, ?>)dao).setPersistenceManager(pm);
			((Dao<?, ?>)dao).setDaoProvider(this);

			daoClass2Dao.put(daoClass, dao);
		}
		return dao;
	}

	@Override
	public void flush() {
		final PersistenceManager pm = getPersistenceManager();
		pm.flush();
	}

	@Override
	public void setContextObject(final Object object) {
		extensibleContextSupport.setContextObject(object);
	}

	@Override
	public <T> T getContextObject(final Class<T> clazz) {
		return extensibleContextSupport.getContextObject(clazz);
	}

	@Override
	public void removeContextObject(Object object) {
		extensibleContextSupport.removeContextObject(object);
	}

	@Override
	public void removeContextObject(Class<?> clazz) {
		extensibleContextSupport.removeContextObject(clazz);
	}

	@Override
	public void addPreCloseListener(LocalRepoTransactionPreCloseListener listener) {
		preCloseListeners.add(requireNonNull(listener, "listener"));
	}
	@Override
	public void addPostCloseListener(LocalRepoTransactionPostCloseListener listener) {
		postCloseListeners.add(requireNonNull(listener, "listener"));
	}

	protected void firePreCloseListeners(final boolean commit) {
		LocalRepoTransactionPreCloseEvent event = null;
		for (final LocalRepoTransactionPreCloseListener listener : preCloseListeners) {
			try {
				if (event == null)
					event = new LocalRepoTransactionPreCloseEvent(this);

				if (commit)
					listener.preCommit(event);
				else
					listener.preRollback(event);
			} catch (Exception x) {
				logger.error("firePreCloseListeners: " + x, x);
			}
		}
	}
	protected void firePostCloseListeners(final boolean commit) {
		LocalRepoTransactionPostCloseEvent event = null;
		for (final LocalRepoTransactionPostCloseListener listener : postCloseListeners) {
			try {
				if (event == null)
					event = new LocalRepoTransactionPostCloseEvent(this);

				if (commit)
					listener.postCommit(event);
				else
					listener.postRollback(event);
			} catch (Exception x) {
				logger.error("firePostCloseListeners: " + x, x);
			}
		}
	}
}
