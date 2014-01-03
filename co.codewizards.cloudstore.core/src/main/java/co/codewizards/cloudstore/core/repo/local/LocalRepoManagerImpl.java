package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.DerbyUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.persistence.DeleteModification;
import co.codewizards.cloudstore.core.persistence.Directory;
import co.codewizards.cloudstore.core.persistence.Entity;
import co.codewizards.cloudstore.core.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.core.persistence.LocalRepository;
import co.codewizards.cloudstore.core.persistence.LocalRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.Modification;
import co.codewizards.cloudstore.core.persistence.NormalFile;
import co.codewizards.cloudstore.core.persistence.RemoteRepository;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryDAO;
import co.codewizards.cloudstore.core.persistence.RepoFile;
import co.codewizards.cloudstore.core.persistence.Repository;
import co.codewizards.cloudstore.core.persistence.Symlink;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

/**
 * Manager of a repository.
 * <p>
 * All operations on a repository are performed via this manager (or an object associated with it).
 * <p>
 * For every repository (identified by its root directory) there is one single instance. Use the
 * {@link LocalRepoManagerFactory} to obtain a {@code LocalRepoManager}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
class LocalRepoManagerImpl implements LocalRepoManager {
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoManagerImpl.class);

	private static final String VAR_LOCAL_ROOT = "repository.localRoot";
	private static final String VAR_META_DIR = "repository.metaDir";

	private static final String PERSISTENCE_PROPERTIES_FILE_NAME = "cloudstore-persistence.properties";

	private static final String CONNECTION_URL_KEY = "javax.jdo.option.ConnectionURL";

	private final File localRoot;
	private EntityID repositoryID;
	private PersistenceManagerFactory persistenceManagerFactory;
	private final AtomicInteger openReferenceCounter = new AtomicInteger();
	private List<LocalRepoManagerCloseListener> localRepoManagerCloseListeners = new CopyOnWriteArrayList<LocalRepoManagerCloseListener>();
	private String connectionURL;

	private boolean deleteMetaDir;
	private static Timer closeDeferredTimer = new Timer(true);
	private TimerTask closeDeferredTimerTask;

	protected LocalRepoManagerImpl(File localRoot, boolean createRepository) throws LocalRepoManagerException {
		this.localRoot = assertValidLocalRoot(localRoot);
		deleteMetaDir = false; // only delete, if it is created in initMetaDirectory(...)
		try {
			// TODO Make this more robust: If we have a power-outage between directory creation and the finally block,
			// we end in an inconsistent state. We can avoid this by tracking the creation process in a properties
			// file later (somehow making this really transactional).
			initMetaDir(createRepository);
			initPersistenceManagerFactory(createRepository);
			deleteMetaDir = false; // if we come here, creation is successful => NO deletion
		} finally {
			if (deleteMetaDir)
				IOUtil.deleteDirectoryRecursively(getMetaDir());
		}

		LocalRepoRegistry.getInstance().registerRepository(repositoryID, localRoot);
	}

	protected void open() {
		synchronized(this) {
			if (closeDeferredTimerTask != null) {
				closeDeferredTimerTask.cancel();
				closeDeferredTimerTask = null;
			}
		}

		openReferenceCounter.incrementAndGet();
	}

	private File assertValidLocalRoot(File localRoot) {
		assertNotNull("localRoot", localRoot);

		if (!localRoot.exists())
			throw new FileNotFoundException(localRoot);

		if (!localRoot.isDirectory())
			throw new FileNoDirectoryException(localRoot);

		assertNotInsideOtherRepository(localRoot);
		return localRoot;
	}

	private void assertNotInsideOtherRepository(File localRoot) {
		File parentFile = localRoot.getParentFile();
		while (parentFile != null) {
			File parentMetaDir = new File(parentFile, META_DIR_NAME);
			if (parentMetaDir.exists()) {
				throw new FileAlreadyRepositoryException(localRoot);
			}
			parentFile = parentFile.getParentFile();
		}
	}

	private void initMetaDir(boolean createRepository) throws LocalRepoManagerException {
		File metaDirectory = getMetaDir();
		if (createRepository) {
			if (metaDirectory.exists()) {
				throw new FileAlreadyRepositoryException(localRoot);
			}

			deleteMetaDir = true;
			metaDirectory.mkdir();

			try {
				IOUtil.copyResource(LocalRepoManagerImpl.class, "/" + PERSISTENCE_PROPERTIES_FILE_NAME, new File(metaDirectory, PERSISTENCE_PROPERTIES_FILE_NAME));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			if (!metaDirectory.exists()) {
				throw new FileNoRepositoryException(localRoot);
			}
		}
	}

	private void initPersistenceManagerFactory(boolean createRepository) throws LocalRepoManagerException {
		Map<String, String> persistenceProperties = getPersistenceProperties(createRepository);
		persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(persistenceProperties );
		PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
		try {
			initPersistenceCapableClasses(pm);

			pm.currentTransaction().begin();

			if (createRepository) {
				createAndPersistLocalRepository(pm);
			} else {
				assertSinglePersistentLocalRepository(pm);
			}

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();

			pm.close();
		}
	}

	private void initPersistenceCapableClasses(PersistenceManager pm) {
		pm.getExtent(DeleteModification.class);
		pm.getExtent(Directory.class);
		pm.getExtent(Entity.class);
		pm.getExtent(LastSyncToRemoteRepo.class);
		pm.getExtent(LocalRepository.class);
		pm.getExtent(Modification.class);
		pm.getExtent(NormalFile.class);
		pm.getExtent(RemoteRepository.class);
		pm.getExtent(Repository.class);
		pm.getExtent(RepoFile.class);
		pm.getExtent(Symlink.class);
	}

	private void assertSinglePersistentLocalRepository(PersistenceManager pm) {
		try {
			LocalRepository localRepository = new LocalRepositoryDAO().persistenceManager(pm).getLocalRepositoryOrFail();
			repositoryID = localRepository.getEntityID();
		} catch (IllegalStateException x) {
			throw new RepositoryCorruptException(localRoot, x.getMessage());
		}
	}

	private void createAndPersistLocalRepository(PersistenceManager pm) {
		LocalRepository localRepository = new LocalRepository();
		Directory root = new Directory();
		root.setName("");
		root.setLastModified(new Date(localRoot.lastModified()));
		localRepository.setRoot(root);
		localRepository = pm.makePersistent(localRepository);
		repositoryID = localRepository.getEntityID();
	}

	private File getMetaDir() {
		return new File(localRoot, META_DIR_NAME);
	}

	private Map<String, String> getPersistenceProperties(boolean createRepository) {
		File metaDirectory = getMetaDir();
		File persistencePropertiesFile = new File(metaDirectory, PERSISTENCE_PROPERTIES_FILE_NAME);

		Map<String, String> variablesMap = new HashMap<String, String>();
		variablesMap.put(VAR_LOCAL_ROOT, localRoot.getPath());
		variablesMap.put(VAR_META_DIR, getMetaDir().getPath());

		Properties rawProperties;
		try {
			rawProperties = PropertiesUtil.load(persistencePropertiesFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Map<String, String> persistenceProperties = PropertiesUtil.filterProperties(rawProperties, variablesMap);
		connectionURL = persistenceProperties.get(CONNECTION_URL_KEY);

		if (createRepository) {
			modifyConnectionURLForCreate(persistenceProperties);
		}
		return persistenceProperties;
	}

	private void modifyConnectionURLForCreate(Map<String, String> persistenceProperties) {
		String value = connectionURL;
		if (value == null || value.trim().isEmpty()) {
			throw new RepositoryCorruptException(localRoot,
					String.format("Property '%s' missing in '%s'.", CONNECTION_URL_KEY, PERSISTENCE_PROPERTIES_FILE_NAME));
		}

		String newValue = value.trim() + ";create=true";
		persistenceProperties.put(CONNECTION_URL_KEY, newValue);
	}

	@Override
	public File getLocalRoot() {
		return localRoot;
	}

	protected PersistenceManagerFactory getPersistenceManagerFactory() {
		return persistenceManagerFactory;
	}

	@Override
	public void addLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener) {
		localRepoManagerCloseListeners.add(listener);
	}

	@Override
	public void removeLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener) {
		localRepoManagerCloseListeners.remove(listener);
	}

	@Override
	public void close() {
		int openReferenceCounterValue = openReferenceCounter.decrementAndGet();
		if (openReferenceCounterValue > 0) {
			logger.debug("[{}]close: leaving with openReferenceCounterValue={}", repositoryID, openReferenceCounterValue);
			return;
		}
		if (openReferenceCounterValue < 0) {
			throw new IllegalStateException("openReferenceCounterValue < 0");
		}

		long closeDeferredMillis = LocalRepoManagerImpl.closeDeferredMillis;
		if (closeDeferredMillis > 0) {
			synchronized(this) {
				if (closeDeferredTimerTask == null) {
					closeDeferredTimerTask = new TimerTask() {
						@Override
						public void run() {
							_close();
						}
					};
					closeDeferredTimer.schedule(closeDeferredTimerTask, closeDeferredMillis);
				}
			}
		}
		else
			_close();
	}

	protected static volatile long closeDeferredMillis = 60 * 1000L; // TODO make properly configurable!

	private void _close() {
		synchronized(this) {
			if (closeDeferredTimerTask != null) {
				closeDeferredTimerTask.cancel();
				closeDeferredTimerTask = null;
			}
		}

		logger.info("[{}]close: Shutting down real LocalRepoManager.", repositoryID);
		// TODO defer this (don't immediately close)
		// TODO the timeout should be configurable
		LocalRepoManagerCloseEvent event = new LocalRepoManagerCloseEvent(this, this, true);
		for (LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.preClose(event);
		}

		synchronized (this) {
			if (persistenceManagerFactory != null) {
				persistenceManagerFactory.close();
				persistenceManagerFactory = null;
				shutdownDerbyDatabase(connectionURL);
			}
		}

		for (LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.postClose(event);
		}
	}

	@Override
	public EntityID getLocalRepositoryID() {
		return repositoryID;
	}

	@Override
	public synchronized boolean isOpen() {
		return persistenceManagerFactory != null;
	}

	protected void assertOpen() {
		if (!isOpen())
			throw new IllegalStateException("This LocalRepoManagerImpl is closed!");
	}

	@Override
	public synchronized LocalRepoTransaction beginTransaction() {
		assertOpen();
		return new LocalRepoTransaction(this);
	}

	@Override
	public void localSync(ProgressMonitor monitor) { // TODO use this monitor properly (commit might take a bit)
		LocalRepoTransaction transaction = beginTransaction();
		try {
			new LocalRepoSync(transaction).sync(monitor);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void putRemoteRepository(EntityID repositoryID, URL remoteRoot) {
		assertNotNull("entityID", repositoryID);
		LocalRepoTransaction transaction = beginTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);

			if (remoteRoot != null) {
				RemoteRepository otherRepoWithSameRemoteRoot = remoteRepositoryDAO.getRemoteRepository(remoteRoot);
				if (otherRepoWithSameRemoteRoot != null && !repositoryID.equals(otherRepoWithSameRemoteRoot.getEntityID()))
					throw new IllegalStateException(String.format("Duplicate remoteRoot! The RemoteRepository '%s' already has the same remoteRoot '%s'! The remoteRoot must be unique!", otherRepoWithSameRemoteRoot.getEntityID(), remoteRoot));
			}

			RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrNull(repositoryID);
			if (remoteRepository == null) {
				remoteRepository = new RemoteRepository(repositoryID);
				remoteRepository.setRevision(-1);
			}
			remoteRepository.setRemoteRoot(remoteRoot);

			remoteRepositoryDAO.makePersistent(remoteRepository); // just in case, it is new (otherwise this has no effect, anyway).

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void deleteRemoteRepository(EntityID repositoryID) {
		assertNotNull("entityID", repositoryID);
		LocalRepoTransaction transaction = beginTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			RemoteRepository remoteRepository = remoteRepositoryDAO.getObjectByIdOrNull(repositoryID);
			if (remoteRepository != null)
				remoteRepositoryDAO.deletePersistent(remoteRepository);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}
}
