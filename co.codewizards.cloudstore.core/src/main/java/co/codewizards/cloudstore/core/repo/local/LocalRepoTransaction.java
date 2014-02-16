package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.util.concurrent.locks.Lock;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import co.codewizards.cloudstore.core.persistence.DAO;
import co.codewizards.cloudstore.core.persistence.LocalRepository;
import co.codewizards.cloudstore.core.persistence.LocalRepositoryDAO;

public class LocalRepoTransaction {

	private final LocalRepoManager localRepoManager;
	private final PersistenceManagerFactory persistenceManagerFactory;
	private final boolean write;
	private PersistenceManager persistenceManager;
	private Transaction jdoTransaction;
	private Lock lock;
	private long localRevision = -1;

	public LocalRepoTransaction(LocalRepoManagerImpl localRepoManager, boolean write) {
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
	}

	private void hookLifecycleListeners() {
		persistenceManager.addInstanceLifecycleListener(new AutoTrackLifecycleListener(this), (Class[]) null);
	}

	public synchronized void commit() {
		if (!isActive())
			throw new IllegalStateException("Transaction is not active!");

		persistenceManager.flush();
		jdoTransaction.commit();
		persistenceManager.close();
		jdoTransaction = null;
		persistenceManager = null;
		localRevision = -1;
		unlock();
	}

	public synchronized boolean isActive() {
		return jdoTransaction != null && jdoTransaction.isActive();
	}

	public synchronized void rollback() {
		if (!isActive())
			throw new IllegalStateException("Transaction is not active!");

		jdoTransaction.rollback();
		persistenceManager.close();
		jdoTransaction = null;
		persistenceManager = null;
		localRevision = -1;
		unlock();
	}

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

	public long getLocalRevision() {
		if (localRevision < 0) {
			if (!write)
				throw new IllegalStateException("This is a read-only transaction!");

			jdoTransaction.setSerializeRead(true);
			LocalRepository lr = getDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail();
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

	public LocalRepoManager getLocalRepoManager() {
		return localRepoManager;
	}

	public <D extends DAO<?, ?>> D getDAO(Class<D> daoClass) {
		PersistenceManager pm = getPersistenceManager();
		D dao;
		try {
			dao = daoClass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		dao.persistenceManager(pm);
		return dao;
	}
}
