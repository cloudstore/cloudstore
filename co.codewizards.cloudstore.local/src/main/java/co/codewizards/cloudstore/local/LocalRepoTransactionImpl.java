package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import co.codewizards.cloudstore.core.repo.local.ContextWithLocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionListenerRegistry;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;

public class LocalRepoTransactionImpl implements LocalRepoTransaction, ContextWithLocalRepoManager, ContextWithPersistenceManager {

	private final LocalRepoManager localRepoManager;
	private final PersistenceManagerFactory persistenceManagerFactory;
	private final boolean write;
	private PersistenceManager persistenceManager;
	private Transaction jdoTransaction;
	private final Lock lock;
	private long localRevision = -1;
	private final Map<Class<?>, Object> daoClass2Dao = new HashMap<>();

	private final LocalRepoTransactionListenerRegistry listenerRegistry = new LocalRepoTransactionListenerRegistry(this);

	public LocalRepoTransactionImpl(final LocalRepoManagerImpl localRepoManager, final boolean write) {
		this.localRepoManager = assertNotNull("localRepoManager", localRepoManager);
		this.persistenceManagerFactory = assertNotNull("localRepoManager.persistenceManagerFactory", localRepoManager.getPersistenceManagerFactory());
		this.lock = localRepoManager.getLock();
		this.write = write;
		begin();
	}

	private void begin() {
		lock.lock();
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
			daoClass2Dao.clear();
			persistenceManager.flush();
			jdoTransaction.commit();
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
		lock.lock();
		try {
			if (!isActive())
				throw new IllegalStateException("Transaction is not active!");

			listenerRegistry.onRollback();
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
	public void rollbackIfActive() {
		lock.lock();
		try {
			if (isActive())
				rollback();
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

		@SuppressWarnings("unchecked")
		D dao = (D) daoClass2Dao.get(daoClass);

		if (dao == null) {
			final PersistenceManager pm = getPersistenceManager();
			try {
				dao = daoClass.newInstance();
			} catch (final InstantiationException e) {
				throw new RuntimeException(e);
			} catch (final IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			if (!(dao instanceof Dao))
				throw new IllegalStateException(String.format("dao class %s does not extend Dao!", daoClass.getName()));

			((Dao<?, ?>)dao).setPersistenceManager(pm);

			daoClass2Dao.put(daoClass, dao);
		}
		return dao;
	}

	@Override
	public void flush() {
		final PersistenceManager pm = getPersistenceManager();
		pm.flush();
	}
}
