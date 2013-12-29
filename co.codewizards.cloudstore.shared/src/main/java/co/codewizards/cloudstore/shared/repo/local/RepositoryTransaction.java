package co.codewizards.cloudstore.shared.repo.local;

import static co.codewizards.cloudstore.shared.util.Util.*;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import co.codewizards.cloudstore.shared.persistence.DAO;
import co.codewizards.cloudstore.shared.persistence.LocalRepository;
import co.codewizards.cloudstore.shared.persistence.LocalRepositoryDAO;

public class RepositoryTransaction {

	private final RepositoryManager repositoryManager;
	private final PersistenceManagerFactory persistenceManagerFactory;
	private PersistenceManager persistenceManager;
	private Transaction jdoTransaction;
	private long localRevision = -1;

	public RepositoryTransaction(RepositoryManager repositoryManager) {
		this.repositoryManager = assertNotNull("repositoryManager", repositoryManager);
		this.persistenceManagerFactory = assertNotNull("repositoryManager.persistenceManagerFactory", repositoryManager.getPersistenceManagerFactory());
		begin();
	}

	private synchronized void begin() {
		if (isActive()) {
			throw new IllegalStateException("Transaction is already active!");
		}
		persistenceManager = persistenceManagerFactory.getPersistenceManager();
		hookLifecycleListeners();
		jdoTransaction = persistenceManager.currentTransaction();
		jdoTransaction.begin();
	}

	private void hookLifecycleListeners() {
		persistenceManager.addInstanceLifecycleListener(new AutoTrackLifecycleListener(this), (Class[]) null);
	}

	public synchronized void commit() {
		if (!isActive()) {
			throw new IllegalStateException("Transaction is not active!");
		}
		jdoTransaction.commit();
		persistenceManager.close();
		jdoTransaction = null;
		persistenceManager = null;
		localRevision = -1;
	}

	public synchronized boolean isActive() {
		return jdoTransaction != null && jdoTransaction.isActive();
	}

	public synchronized void rollback() {
		if (!isActive()) {
			throw new IllegalStateException("Transaction is not active!");
		}
		jdoTransaction.rollback();
		persistenceManager.close();
		jdoTransaction = null;
		persistenceManager = null;
		localRevision = -1;
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
			jdoTransaction.setSerializeRead(true);
			LocalRepository lr = createDAO(LocalRepositoryDAO.class).getLocalRepositoryOrFail();
			jdoTransaction.setSerializeRead(null);
			localRevision = lr.getRevision() + 1;
			lr.setRevision(localRevision);
			persistenceManager.flush();
		}
		return localRevision;
	}

	public RepositoryManager getRepositoryManager() {
		return repositoryManager;
	}

	public <D extends DAO<?, ?>> D createDAO(Class<D> daoClass) {
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
