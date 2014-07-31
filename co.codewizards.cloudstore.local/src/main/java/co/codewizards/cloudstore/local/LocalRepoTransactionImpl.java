package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;

public class LocalRepoTransactionImpl implements co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction {

	private final LocalRepoManager localRepoManager;
	private final PersistenceManagerFactory persistenceManagerFactory;
	private final boolean write;
	private PersistenceManager persistenceManager;
	private Transaction jdoTransaction;
	private Lock lock;
	private long localRevision = -1;
	private final Map<Class<?>, Object> daoClass2Dao = new HashMap<>();

	private final AutoTrackLifecycleListener autoTrackLifecycleListener = new AutoTrackLifecycleListener(this);

	public LocalRepoTransactionImpl(final LocalRepoManagerImpl localRepoManager, final boolean write) {
		this.localRepoManager = assertNotNull("localRepoManager", localRepoManager);
		this.persistenceManagerFactory = assertNotNull("localRepoManager.persistenceManagerFactory", localRepoManager.getPersistenceManagerFactory());
		this.write = write;
		begin();
	}

	private synchronized void begin() {
		if (isActive())
			throw new IllegalStateException("Transaction is already active!");

		if (write)
			lock();

		persistenceManager = persistenceManagerFactory.getPersistenceManager();
		hookLifecycleListeners();
		jdoTransaction = persistenceManager.currentTransaction();
		jdoTransaction.begin();
		autoTrackLifecycleListener.onBegin();
	}

	private void hookLifecycleListeners() {
		persistenceManager.addInstanceLifecycleListener(autoTrackLifecycleListener, (Class[]) null);
	}

	@Override
	public synchronized void commit() {
		if (!isActive())
			throw new IllegalStateException("Transaction is not active!");

		daoClass2Dao.clear();
		autoTrackLifecycleListener.onCommit();
		persistenceManager.flush();
		jdoTransaction.commit();
		persistenceManager.close();
		jdoTransaction = null;
		persistenceManager = null;
		localRevision = -1;
		unlock();
	}

	@Override
	public synchronized boolean isActive() {
		return jdoTransaction != null && jdoTransaction.isActive();
	}

	@Override
	public synchronized void rollback() {
		if (!isActive())
			throw new IllegalStateException("Transaction is not active!");

		daoClass2Dao.clear();
		autoTrackLifecycleListener.onRollback();
		jdoTransaction.rollback();
		persistenceManager.close();
		jdoTransaction = null;
		persistenceManager = null;
		localRevision = -1;
		unlock();
	}

	@Override
	public synchronized void rollbackIfActive() {
		if (isActive())
			rollback();
	}

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

	private synchronized void lock() {
		if (lock == null) {
			lock = localRepoManager.getLock();
			lock.lock();
		}
	}

	private synchronized void unlock() {
		if (lock != null) {
			lock.unlock();
			lock = null;
		}
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
