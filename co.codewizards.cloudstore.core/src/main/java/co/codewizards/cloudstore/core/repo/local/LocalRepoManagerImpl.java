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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.persistence.CopyModification;
import co.codewizards.cloudstore.core.persistence.DeleteModification;
import co.codewizards.cloudstore.core.persistence.Directory;
import co.codewizards.cloudstore.core.persistence.Entity;
import co.codewizards.cloudstore.core.persistence.FileChunk;
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

	private long closeDeferredMillis = Long.MIN_VALUE;
	private static final long lockTimeoutMillis = 30000; // TODO make configurable!

	private static final long remoteRepositoryRequestExpiryAge = 24 * 60 * 60 * 1000L;

	private final File localRoot;
	private LockFile lockFile;
	private UUID repositoryId;
	/**
	 * The properties read from {@link LocalRepoManager#REPOSITORY_PROPERTIES_FILE_NAME}.
	 * <p>
	 * <b>Important:</b> This is only assigned from {@link #createRepositoryPropertiesFile()} / {@link #checkRepositoryPropertiesFile()}
	 * until {@link #updateRepositoryPropertiesFile()}. Afterwards, it is <code>null</code> again!
	 */
	private Properties repositoryProperties;
	private PersistenceManagerFactory persistenceManagerFactory;
	private final AtomicInteger openReferenceCounter = new AtomicInteger();
	private List<LocalRepoManagerCloseListener> localRepoManagerCloseListeners = new CopyOnWriteArrayList<LocalRepoManagerCloseListener>();
	private String connectionURL;

	private boolean deleteMetaDir;
	private int closeDeferredTimerSerial;
	private Timer closeDeferredTimer;
	private TimerTask closeDeferredTimerTask;
	private final Lock lock = new ReentrantLock();

	private final Timer deleteExpiredRemoteRepositoryRequestsTimer = new Timer("deleteExpiredRemoteRepositoryRequestsTimer-" + id, true);
	private final TimerTask deleteExpiredRemoteRepositoryRequestsTimeTask = new TimerTask() {
		@Override
		public void run() {
			if (isOpen())
				deleteExpiredRemoteRepositoryRequests();
		}
	};
	{
		deleteExpiredRemoteRepositoryRequestsTimer.schedule(
				deleteExpiredRemoteRepositoryRequestsTimeTask,
				60 * 60 * 1000L, 60 * 60 * 1000L); // TODO make times configurable
	}


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
			deleteExpiredRemoteRepositoryRequests();
			updateRepositoryPropertiesFile();
			releaseLockFile = false;
			deleteMetaDir = false; // if we come here, creation is successful => NO deletion
		} finally {
			if (releaseLockFile && lockFile != null)
				lockFile.release();

			if (deleteMetaDir)
				IOUtil.deleteDirectoryRecursively(getMetaDir());
		}
		LocalRepoRegistry.getInstance().putRepository(repositoryId, localRoot);
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
		File localRootFound = LocalRepoHelper.getLocalRootContainingFile(localRoot);
		if (localRootFound != null && !localRootFound.equals(localRoot))
			throw new FileAlreadyRepositoryException(localRoot);
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
		final int version = 2;
		File repositoryPropertiesFile = new File(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		try {
			repositoryProperties = new Properties();
			repositoryProperties.put(PROP_VERSION, Integer.valueOf(version).toString());
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

		// Because version 1 was used by 0.9.0, we do not provide compatibility, yet. Maybe we add compatibility
		// code converting version 1 into 2, later.
		// Further, this check prevents old versions to work with a newer repo (and possibly corrupt it).
		if (ver != 2)
			throw new RepositoryCorruptException(localRoot, "Repository is not version 2!");
	}

	private void updateRepositoryPropertiesFile() {
		assertNotNull("repositoryProperties", repositoryProperties);
		File repositoryPropertiesFile = new File(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		try {
			String repositoryId = assertNotNull("repositoryId", getRepositoryId()).toString();
			if (!repositoryId.equals(repositoryProperties.getProperty(PROP_REPOSITORY_ID))) {
				repositoryProperties.setProperty(PROP_REPOSITORY_ID, repositoryId);
				PropertiesUtil.store(repositoryPropertiesFile, repositoryProperties, null);
			}
			repositoryProperties = null; // not needed anymore => gc
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void initPersistenceManagerFactory(boolean createRepository) throws LocalRepoManagerException {
		logger.debug("[{}]initPersistenceManagerFactory: Starting up PersistenceManagerFactory...", id);
		long beginTimestamp = System.currentTimeMillis();
		initPersistenceManagerFactoryAndPersistenceCapableClassesWithRetry(createRepository);
		PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
		try {
			pm.currentTransaction().begin();

			if (createRepository) {
				createAndPersistLocalRepository(pm);
			} else {
				assertSinglePersistentLocalRepository(pm);
			}
			logger.info("[{}]initPersistenceManagerFactory: repositoryId={}", id, repositoryId);

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();

			pm.close();
		}
		logger.info("[{}]initPersistenceManagerFactory: Started up PersistenceManagerFactory successfully in {} ms.", id, System.currentTimeMillis() - beginTimestamp);
	}

	private void deleteExpiredRemoteRepositoryRequests() {
		lock.lock();
		try {
			PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
			try {
				pm.currentTransaction().begin();

				RemoteRepositoryRequestDAO dao = new RemoteRepositoryRequestDAO().persistenceManager(pm);
				Collection<RemoteRepositoryRequest> expiredRequests = dao.getRemoteRepositoryRequestsChangedBefore(new Date(System.currentTimeMillis() - remoteRepositoryRequestExpiryAge));
				pm.deletePersistentAll(expiredRequests);

				pm.currentTransaction().commit();
			} finally {
				if (pm.currentTransaction().isActive())
					pm.currentTransaction().rollback();

				pm.close();
			}
		} finally {
			lock.unlock();
		}
	}

	private void initPersistenceManagerFactoryAndPersistenceCapableClassesWithRetry(final boolean createRepository) {
		final int maxRetryCount = 10;
		int tryCount = 0;
		Map<String, String> persistenceProperties = getPersistenceProperties(createRepository);
		do {
			++tryCount;
			persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(persistenceProperties);
			PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
			try {
				try {
					initPersistenceCapableClasses(pm);
				} catch (Exception x) {
					if (tryCount > maxRetryCount) {
						if (x instanceof RuntimeException)
							throw (RuntimeException)x;
						else
							throw new RuntimeException(x);
					}

					logger.warn("[" + id + "]initPersistenceCapableClasses(...) failed. Will try again.", x);
					pm.close(); pm = null; persistenceManagerFactory.close(); persistenceManagerFactory = null;
					shutdownDerbyDatabase(connectionURL);

// https://github.com/cloudstore/main/issues/10 :: java.sql.SQLNonTransientConnectionException: No current connection.
// http://stackoverflow.com/questions/6172930/sqlnontransientconnectionexception-no-current-connection-in-my-application-whi
// Forcing garbage collection.
					System.gc();
					for (int i = 0; i < 3; ++i) {
						try { Thread.sleep(500 + tryCount * 1000); } catch (InterruptedException ie) { doNothing(); }
						System.gc();
					}

					if (createRepository)
						deleteDerbyDatabaseDirectory();
					else {
						logger.info("[" + id + "]initPersistenceManagerFactoryAndPersistenceCapableClassesWithRetry: Trying to repair the database.");
						try {
							new RepairDatabase(getLocalRoot()).run();
						} catch (Exception repairDatabaseException) {
							logger.warn("[" + id + "]initPersistenceManagerFactoryAndPersistenceCapableClassesWithRetry: " + repairDatabaseException.toString(), repairDatabaseException);
						}
					}
				}
			} finally {
				if (pm != null)
					pm.close();
			}
		} while (persistenceManagerFactory == null);
	}

	/**
	 * @deprecated temporary workaround for https://github.com/cloudstore/main/issues/10
	 */
	@Deprecated
	private void deleteDerbyDatabaseDirectory() {
		final String connectionURL = this.connectionURL;
		if (connectionURL == null)
			throw new IllegalStateException("connectionURL == null");

		if (!connectionURL.startsWith("jdbc:derby:"))
			throw new IllegalStateException("connectionURL does not start with 'jdbc:': " + connectionURL);

		final File dir = new File(connectionURL.substring(5));
		logger.info("[%s]deleteDerbyDatabaseDirectory: {}", id, dir);
		IOUtil.deleteDirectoryRecursively(dir);
	}

	private static final void doNothing() { }

	private void initPersistenceCapableClasses(PersistenceManager pm) {
		pm.getExtent(CopyModification.class);
		pm.getExtent(DeleteModification.class);
		pm.getExtent(Directory.class);
		pm.getExtent(Entity.class);
		pm.getExtent(FileChunk.class);
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
		repositoryId = assertNotNull("localRepository.repositoryId", localRepository.getRepositoryId());
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
		Map<String, String> persistenceProperties = new PersistencePropertiesProvider(localRoot).getPersistenceProperties(createRepository);
		connectionURL = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_URL_ORIGINAL.key);
		return persistenceProperties;
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

	protected boolean open() {
		boolean result;
		synchronized(this) {
			logger.debug("[{}]open: closing={} closeAbortable={}", id, closing, closeAbortable);
			result = !closing || closeAbortable;
			if (result) {
				closing = false;
				closeAbortable = true;

				if (closeDeferredTimerTask != null) {
					closeDeferredTimerTask.cancel();
					closeDeferredTimerTask = null;
				}
				if (closeDeferredTimer != null) {
					closeDeferredTimer.cancel();
					closeDeferredTimer = null;
				}
			}
		}
		if (result)
			openReferenceCounter.incrementAndGet();

		return result;
	}

	protected long getCloseDeferredMillis() {
		if (closeDeferredMillis < 0) {
			long closeDeferredMillis = PropertiesUtil.getSystemPropertyValueAsLong(
					SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS, DEFAULT_CLOSE_DEFERRED_MILLIS);

			if (closeDeferredMillis < 0) {
				logger.warn("System property '{}': closeDeferredMillis {} is less than 0! Using default {} instead!",
						SYSTEM_PROPERTY_CLOSE_DEFERRED_MILLIS, closeDeferredMillis, DEFAULT_CLOSE_DEFERRED_MILLIS);
				closeDeferredMillis = DEFAULT_CLOSE_DEFERRED_MILLIS;
			}
			this.closeDeferredMillis = closeDeferredMillis;
		}
		return closeDeferredMillis;
	}

	@Override
	public void close() {
		synchronized(this) {
			closing = true;
		}

		final int openReferenceCounterValue = openReferenceCounter.decrementAndGet();
		if (openReferenceCounterValue > 0) {
			logger.debug("[{}]close: leaving with openReferenceCounterValue={}", id, openReferenceCounterValue);
			return;
		}
		if (openReferenceCounterValue < 0) {
			throw new IllegalStateException("openReferenceCounterValue < 0");
		}

		final long closeDeferredMillis = getCloseDeferredMillis();
		if (closeDeferredMillis > 0) {
			logger.info("[{}]close: Deferring shut down of real LocalRepoManager {} ms.", id, closeDeferredMillis);
			synchronized(this) {
// Because of this error:
//		Caused by: java.lang.IllegalStateException: Timer already cancelled.
//	       at java.util.Timer.sched(Timer.java:397) ~[na:1.7.0_45]
//	       at java.util.Timer.schedule(Timer.java:193) ~[na:1.7.0_45]
//	       at co.codewizards.cloudstore.core.repo.local.LocalRepoManagerImpl.close(LocalRepoManagerImpl.java:403) ~[co.codewizards.cloudstore.core-1.0.0-SNAPSHOT.jar:na]
// and because even when recreating the timer in a catch clause still did not prevent
// sometimes tasks to not be called, anymore, we now create a new timer every time.
				if (closeDeferredTimer == null)
					closeDeferredTimer = new Timer("closeDeferredTimer-" + id + '-' + Integer.toString(++closeDeferredTimerSerial, 36), true);

				if (closeDeferredTimerTask == null) {
					closeDeferredTimerTask = new CloseTimerTask();
					closeDeferredTimer.schedule(closeDeferredTimerTask, closeDeferredMillis);
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

		deleteExpiredRemoteRepositoryRequestsTimer.cancel();

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
	public UUID getRepositoryId() {
		return repositoryId;
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
	public synchronized LocalRepoTransaction beginReadTransaction() {
		assertOpen();
		return new LocalRepoTransaction(this, false);
	}

	@Override
	public synchronized LocalRepoTransaction beginWriteTransaction() {
		assertOpen();
		return new LocalRepoTransaction(this, true);
	}

	@Override
	public void localSync(ProgressMonitor monitor) {
		monitor.beginTask("Local sync...", 100);
		try {
			LocalRepoTransaction transaction = beginWriteTransaction();
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
	public void putRemoteRepository(UUID repositoryId, URL remoteRoot, byte[] publicKey, String localPathPrefix) {
		assertNotNull("entityID", repositoryId);
		assertNotNull("publicKey", publicKey);
		LocalRepoTransaction transaction = beginWriteTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);

			if (remoteRoot != null) {
				RemoteRepository otherRepoWithSameRemoteRoot = remoteRepositoryDAO.getRemoteRepository(remoteRoot);
				if (otherRepoWithSameRemoteRoot != null && !repositoryId.equals(otherRepoWithSameRemoteRoot.getRepositoryId()))
					throw new IllegalStateException(String.format("Duplicate remoteRoot! The RemoteRepository '%s' already has the same remoteRoot '%s'! The remoteRoot must be unique!", otherRepoWithSameRemoteRoot.getRepositoryId(), remoteRoot));
			}

			RemoteRepository remoteRepository = remoteRepositoryDAO.getRemoteRepository(repositoryId);
			if (remoteRepository == null) {
				remoteRepository = new RemoteRepository(repositoryId);
				remoteRepository.setRevision(-1);
			}
			remoteRepository.setRemoteRoot(remoteRoot);
			remoteRepository.setPublicKey(publicKey);

			remoteRepository.setLocalPathPrefix(localPathPrefix);

			remoteRepositoryDAO.makePersistent(remoteRepository); // just in case, it is new (otherwise this has no effect, anyway).

			RemoteRepositoryRequestDAO remoteRepositoryRequestDAO = transaction.getDAO(RemoteRepositoryRequestDAO.class);
			RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDAO.getRemoteRepositoryRequest(repositoryId);
			if (remoteRepositoryRequest != null)
				remoteRepositoryRequestDAO.deletePersistent(remoteRepositoryRequest);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void deleteRemoteRepository(UUID repositoryId) {
		assertNotNull("entityID", repositoryId);
		LocalRepoTransaction transaction = beginWriteTransaction();
		try {
			RemoteRepositoryDAO remoteRepositoryDAO = transaction.getDAO(RemoteRepositoryDAO.class);
			RemoteRepository remoteRepository = remoteRepositoryDAO.getRemoteRepository(repositoryId);
			if (remoteRepository != null)
				remoteRepositoryDAO.deletePersistent(remoteRepository);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	protected int getKeySize() {
		int keySize = PropertiesUtil.getSystemPropertyValueAsInt(SYSTEM_PROPERTY_KEY_SIZE, DEFAULT_KEY_SIZE);
		if (keySize < 1024) {
			logger.warn("System property '{}': keySize {} is out of range! Using default {} instead!", SYSTEM_PROPERTY_KEY_SIZE, keySize, DEFAULT_KEY_SIZE);
			return DEFAULT_KEY_SIZE;
		}
		return keySize;
	}

	@Override
	public Lock getLock() {
		return lock;
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
	}

}
