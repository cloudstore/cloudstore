package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.DerbyUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
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
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.io.TimeoutException;
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
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.core.persistence.RemoteRepositoryRequestDAO;
import co.codewizards.cloudstore.core.persistence.RepoFile;
import co.codewizards.cloudstore.core.persistence.Repository;
import co.codewizards.cloudstore.core.persistence.Symlink;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
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

	protected final String id = Integer.toHexString(System.identityHashCode(this));

	protected static volatile long closeDeferredMillis = 20 * 1000L; // TODO make properly configurable!
	private final long lockTimeoutMillis = 30000; // TODO make configurable!

	private static final String VAR_LOCAL_ROOT = "repository.localRoot";
	private static final String VAR_META_DIR = "repository.metaDir";

	private static final String PROP_VERSION = "repository.version";

	private static final String REPOSITORY_PROPERTIES_FILE_NAME = "cloudstore-repository.properties";
	private static final String PERSISTENCE_PROPERTIES_FILE_NAME = "cloudstore-persistence.properties";

	private static final String CONNECTION_URL_KEY = "javax.jdo.option.ConnectionURL";

	private final File localRoot;
	private LockFile lockFile;
	private EntityID repositoryID;
	private PersistenceManagerFactory persistenceManagerFactory;
	private final AtomicInteger openReferenceCounter = new AtomicInteger();
	private List<LocalRepoManagerCloseListener> localRepoManagerCloseListeners = new CopyOnWriteArrayList<LocalRepoManagerCloseListener>();
	private String connectionURL;

	private boolean deleteMetaDir;
	private Timer closeDeferredTimer = new Timer(true);
	private TimerTask closeDeferredTimerTask;

	private byte[] privateKey;
	private byte[] publicKey;

	private boolean closing;
	private boolean closeAbortable = true;

	protected LocalRepoManagerImpl(File localRoot, boolean createRepository) throws LocalRepoManagerException {
		logger.info("[{}]<init>: localRoot='{}'", id, localRoot);
		this.localRoot = assertValidLocalRoot(localRoot);
		boolean releaseLockFile = true;
		deleteMetaDir = false; // only delete, if it is created in initMetaDirectory(...)
		try {
			// TODO Make this more robust: If we have a power-outage between directory creation and the finally block,
			// we end in an inconsistent state. We can avoid this by tracking the creation process in a properties
			// file later (somehow making this really transactional).
			initMetaDir(createRepository);
			initPersistenceManagerFactory(createRepository);
			releaseLockFile = false;
			deleteMetaDir = false; // if we come here, creation is successful => NO deletion
		} finally {
			if (releaseLockFile && lockFile != null)
				lockFile.release();

			if (deleteMetaDir)
				IOUtil.deleteDirectoryRecursively(getMetaDir());
		}
		LocalRepoRegistry.getInstance().putRepository(repositoryID, localRoot);
	}

	protected boolean open() {
		boolean result;
		synchronized(this) {
			logger.debug("[{}]open: closing={} closeAbortable={}", id, closing, closeAbortable);
			result = !closing || closeAbortable;
			if (result) {
				closing = false;
				closeAbortable = true;
			}
			if (closeDeferredTimerTask != null) {
				closeDeferredTimerTask.cancel();
				closeDeferredTimerTask = null;
			}
		}
		openReferenceCounter.incrementAndGet();
		return result;
	}

	private File assertValidLocalRoot(File localRoot) {
		assertNotNull("localRoot", localRoot);

		if (!localRoot.isAbsolute())
			throw new IllegalArgumentException("localRoot is not absolute.");

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
		File metaDir = getMetaDir();
		if (createRepository) {
			if (metaDir.exists()) {
				throw new FileAlreadyRepositoryException(localRoot);
			}

			deleteMetaDir = true;
			metaDir.mkdir();

			initLockFile();
			createRepositoryPropertiesFile();
			try {
				IOUtil.copyResource(LocalRepoManagerImpl.class, "/" + PERSISTENCE_PROPERTIES_FILE_NAME, new File(metaDir, PERSISTENCE_PROPERTIES_FILE_NAME));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			if (!metaDir.exists())
				throw new FileNoRepositoryException(localRoot);

			initLockFile();
			checkRepositoryPropertiesFile();
		}
	}

	private void initLockFile() {
		File lock = new File(getMetaDir(), "cloudstore-repository.lock");
		try {
			lockFile = LockFileFactory.getInstance().acquire(lock, 100);
		} catch (TimeoutException x) {
			logger.warn("[{}]initLockFile: Repository '{}' is currently locked by another process. Will wait {} ms for it...",
					id, localRoot, lockTimeoutMillis);
		}
		if (lockFile == null) {
			lockFile = LockFileFactory.getInstance().acquire(lock, lockTimeoutMillis);
		}
		logger.info("[{}]initLockFile: Repository '{}' locked successfully.",
				id, localRoot);
	}

	private void createRepositoryPropertiesFile() {
		File repositoryPropertiesFile = new File(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		try {
			Properties repositoryProperties = new Properties();
			repositoryProperties.put(PROP_VERSION, Integer.valueOf(1).toString());
			PropertiesUtil.store(repositoryPropertiesFile, repositoryProperties, null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkRepositoryPropertiesFile() throws LocalRepoManagerException {
		File repositoryPropertiesFile = new File(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		if (!repositoryPropertiesFile.exists())
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-directory does not contain '%s'!", REPOSITORY_PROPERTIES_FILE_NAME));

		Properties repositoryProperties;
		try {
			repositoryProperties = PropertiesUtil.load(repositoryPropertiesFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		String version = repositoryProperties.getProperty(PROP_VERSION);
		if (version == null || version.isEmpty())
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-file '%s' does not contain property '%s'!", REPOSITORY_PROPERTIES_FILE_NAME, PROP_VERSION));

		version = version.trim();
		int ver;
		try {
			ver = Integer.parseInt(version);
		} catch (NumberFormatException x) {
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-file '%s' contains an illegal value (not a number) for property '%s'!", REPOSITORY_PROPERTIES_FILE_NAME, PROP_VERSION));
		}

		if (ver != 1) // There is only one single version, right now. This is just a sanity check for allowing automatic upgrades, later.
			throw new RepositoryCorruptException(localRoot, "Repository is not version 1!");
	}

	private void initPersistenceManagerFactory(boolean createRepository) throws LocalRepoManagerException {
		logger.debug("[{}]initPersistenceManagerFactory: Starting up PersistenceManagerFactory...", id);
		long beginTimestamp = System.currentTimeMillis();
		Map<String, String> persistenceProperties = getPersistenceProperties(createRepository);
		persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(persistenceProperties);
		PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
		try {
			try {
				initPersistenceCapableClasses(pm);
			} catch (Exception x) {
				logger.warn("[" + id + "]initPersistenceCapableClasses(...) failed. Will try again.", x);
				pm.close(); pm = null; persistenceManagerFactory.close(); persistenceManagerFactory = null;
				shutdownDerbyDatabase(connectionURL);
				persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(persistenceProperties);
				pm = persistenceManagerFactory.getPersistenceManager();
				initPersistenceCapableClasses(pm);
			}

			pm.currentTransaction().begin();

			if (createRepository) {
				createAndPersistLocalRepository(pm);
			} else {
				assertSinglePersistentLocalRepository(pm);
			}
			logger.info("[{}]initPersistenceManagerFactory: repositoryID={}", id, repositoryID);

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();

			pm.close();
		}
		logger.info("[{}]initPersistenceManagerFactory: Started up PersistenceManagerFactory successfully in {} ms.", id, System.currentTimeMillis() - beginTimestamp);
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
		pm.getExtent(RemoteRepositoryRequest.class);
		pm.getExtent(Repository.class);
		pm.getExtent(RepoFile.class);
		pm.getExtent(Symlink.class);
	}

	private void assertSinglePersistentLocalRepository(PersistenceManager pm) {
		try {
			LocalRepository localRepository = new LocalRepositoryDAO().persistenceManager(pm).getLocalRepositoryOrFail();
			readRepositoryMainProperties(localRepository);
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
		generatePublicPrivateKey(localRepository);

		localRepository = pm.makePersistent(localRepository);
		readRepositoryMainProperties(localRepository);
	}

	private void readRepositoryMainProperties(LocalRepository localRepository) {
		assertNotNull("localRepository", localRepository);
		repositoryID = assertNotNull("localRepository.entityID", localRepository.getEntityID());
		publicKey = assertNotNull("localRepository.publicKey", localRepository.getPublicKey());
		privateKey = assertNotNull("localRepository.privateKey", localRepository.getPrivateKey());
	}

	private static final String KEY_STORE_PASSWORD_STRING = "CloudStore-key-store";
	private static final char[] KEY_STORE_PASSWORD_CHAR_ARRAY = KEY_STORE_PASSWORD_STRING.toCharArray();
	private SecureRandom random = new SecureRandom();

	private void generatePublicPrivateKey(LocalRepository localRepository) {
		try {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null, KEY_STORE_PASSWORD_CHAR_ARRAY);

			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(getKeySize(), random);
			KeyPair pair = keyGen.generateKeyPair();

			localRepository.setPrivateKey(pair.getPrivate().getEncoded());
			localRepository.setPublicKey(pair.getPublic().getEncoded());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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

	private class CloseTimerTask extends TimerTask {
		private volatile boolean cancelled;

		@Override
		public void run() {
			if (cancelled)
				return;

			_close();
		}

		@Override
		public boolean cancel() {
			cancelled = true;
			boolean result = super.cancel();
			return result;
		}
	};

	@Override
	public void close() {
		synchronized(this) {
			closing = true;
		}

		int openReferenceCounterValue = openReferenceCounter.decrementAndGet();
		if (openReferenceCounterValue > 0) {
			logger.debug("[{}]close: leaving with openReferenceCounterValue={}", id, openReferenceCounterValue);
			return;
		}
		if (openReferenceCounterValue < 0) {
			throw new IllegalStateException("openReferenceCounterValue < 0");
		}

		long closeDeferredMillis = LocalRepoManagerImpl.closeDeferredMillis;
		if (closeDeferredMillis > 0) {
			logger.info("[{}]close: Deferring shut down of real LocalRepoManager {} ms.", id, closeDeferredMillis);
			synchronized(this) {
				if (closeDeferredTimerTask == null) {
					closeDeferredTimerTask = new CloseTimerTask();

					try {
						closeDeferredTimer.schedule(closeDeferredTimerTask, closeDeferredMillis);
					} catch (IllegalStateException x) {
						logger.warn("closeDeferredTimer.schedule(...) failed: " + x, x);
						// WORKAROUND: Creating a new timer.
//						Caused by: java.lang.IllegalStateException: Timer already cancelled.
//				        at java.util.Timer.sched(Timer.java:397) ~[na:1.7.0_45]
//				        at java.util.Timer.schedule(Timer.java:193) ~[na:1.7.0_45]
//				        at co.codewizards.cloudstore.core.repo.local.LocalRepoManagerImpl.close(LocalRepoManagerImpl.java:403) ~[co.codewizards.cloudstore.core-1.0.0-SNAPSHOT.jar:na]
						closeDeferredTimer.cancel();
						closeDeferredTimer = new Timer(true);
						closeDeferredTimer.schedule(closeDeferredTimerTask, closeDeferredMillis);
					}
				}
			}
		}
		else
			_close();
	}

	private void _close() {
		synchronized(this) {
			if (!closing) { // closing was aborted
				logger.info("[{}]_close: Closing was aborted. Returning immediately.", id);
				return;
			}
			closeAbortable = false;

			if (closeDeferredTimerTask != null) {
				closeDeferredTimerTask.cancel();
				closeDeferredTimerTask = null;
			}
		}

		logger.info("[{}]_close: Shutting down real LocalRepoManager.", id);
		// TODO defer this (don't immediately close)
		// TODO the timeout should be configurable
		LocalRepoManagerCloseEvent event = new LocalRepoManagerCloseEvent(this, this, true);
		for (LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.preClose(event);
		}

		synchronized (this) {
			if (persistenceManagerFactory != null) {
				try {
					persistenceManagerFactory.close();
				} catch (Exception x) {
					logger.warn("Closing PersistenceManagerFactory failed: " + x, x);
				}
				persistenceManagerFactory = null;
				try {
					shutdownDerbyDatabase(connectionURL);
				} catch (Exception x) {
					logger.warn("Shutting down Derby database failed: " + x, x);
				}
			}
			if (lockFile != null) {
				try {
					lockFile.release();
				} catch (Exception x) {
					logger.warn("Releasing LockFile failed: " + x, x);
				}
				lockFile = null;
			}
		}

		for (LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.postClose(event);
		}
	}

	@Override
	public EntityID getRepositoryID() {
		return repositoryID;
	}

	@Override
	public byte[] getPublicKey() {
		return publicKey;
	}

	@Override
	public byte[] getPrivateKey() {
		return privateKey;
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
	public void localSync(ProgressMonitor monitor) {
		monitor.beginTask("Local sync...", 100);
		try {
			LocalRepoTransaction transaction = beginTransaction();
			try {
				monitor.worked(1);
				new LocalRepoSync(transaction).sync(new SubProgressMonitor(monitor, 98));
				transaction.commit();
				monitor.worked(1);
			} finally {
				transaction.rollbackIfActive();
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public void putRemoteRepository(EntityID repositoryID, URL remoteRoot, byte[] publicKey) {
		assertNotNull("entityID", repositoryID);
		assertNotNull("publicKey", publicKey);
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
			remoteRepository.setPublicKey(publicKey);

			remoteRepositoryDAO.makePersistent(remoteRepository); // just in case, it is new (otherwise this has no effect, anyway).

			RemoteRepositoryRequestDAO remoteRepositoryRequestDAO = transaction.getDAO(RemoteRepositoryRequestDAO.class);
			RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDAO.getRemoteRepositoryRequest(repositoryID);
			if (remoteRepositoryRequest != null)
				remoteRepositoryRequestDAO.deletePersistent(remoteRepositoryRequest);

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

	protected int getKeySize() {
		String keySizeString = System.getProperty(SYSTEM_PROPERTY_KEY_SIZE);
		if (keySizeString == null) {
			return DEFAULT_KEY_SIZE;
		}
		try {
			int keySize = Integer.parseInt(keySizeString);
			if (keySize < 1024) {
				logger.warn("System property '{}': keySize {} is out of range! Using default {} instead!", SYSTEM_PROPERTY_KEY_SIZE, keySize, DEFAULT_KEY_SIZE);
				return DEFAULT_KEY_SIZE;
			}
			return keySize;
		} catch (NumberFormatException x) {
			logger.warn("System property '{}': keySize '{}' is not a valid number!" + x, SYSTEM_PROPERTY_KEY_SIZE, keySizeString);
			return DEFAULT_KEY_SIZE;
		}
	}
}
