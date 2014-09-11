package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.DerbyUtil.*;

import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
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
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.progress.SubProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.FileAlreadyRepositoryException;
import co.codewizards.cloudstore.core.repo.local.FileNoDirectoryException;
import co.codewizards.cloudstore.core.repo.local.FileNoRepositoryException;
import co.codewizards.cloudstore.core.repo.local.FileNotFoundException;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerCloseEvent;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerCloseListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerException;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoRegistry;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.local.RepositoryCorruptException;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import co.codewizards.cloudstore.local.persistence.CopyModification;
import co.codewizards.cloudstore.local.persistence.DeleteModification;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.LastSyncToRemoteRepo;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.Modification;
import co.codewizards.cloudstore.local.persistence.NormalFile;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequest;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryRequestDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.Repository;
import co.codewizards.cloudstore.local.persistence.FileInProgressMarker;
import co.codewizards.cloudstore.local.persistence.Symlink;
import co.codewizards.cloudstore.local.persistence.TransferDoneMarker;

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
	private final List<LocalRepoManagerCloseListener> localRepoManagerCloseListeners = new CopyOnWriteArrayList<LocalRepoManagerCloseListener>();
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

	protected LocalRepoManagerImpl(final File localRoot, final boolean createRepository) throws LocalRepoManagerException {
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
			syncWithLocalRepoRegistry();
			updateRepositoryPropertiesFile();
			releaseLockFile = false;
			deleteMetaDir = false; // if we come here, creation is successful => NO deletion
		} finally {
			if (releaseLockFile && lockFile != null)
				lockFile.release();

			if (deleteMetaDir) {
				IOUtil.deleteDirectoryRecursively(getMetaDir());
//				if (repositoryId != null) // TODO should be removed - will be evicted after some time, anyway, but
//					LocalRepoRegistry.getInstance().removeRepository(repositoryId);
			}
		}
	}

	@Override
	public void putRepositoryAlias(final String repositoryAlias) {
		AssertUtil.assertNotNull("repositoryAlias", repositoryAlias);
		final LocalRepoTransactionImpl transaction = beginWriteTransaction();
		try {
			final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
			if (!localRepository.getAliases().contains(repositoryAlias))
				localRepository.getAliases().add(repositoryAlias);

			LocalRepoRegistry.getInstance().putRepositoryAlias(repositoryAlias, repositoryId);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void removeRepositoryAlias(final String repositoryAlias) {
		AssertUtil.assertNotNull("repositoryAlias", repositoryAlias);
		final LocalRepoTransactionImpl transaction = beginWriteTransaction();
		try {
			final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
			localRepository.getAliases().remove(repositoryAlias);
			LocalRepoRegistry.getInstance().removeRepositoryAlias(repositoryAlias);
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private File assertValidLocalRoot(final File localRoot) {
		AssertUtil.assertNotNull("localRoot", localRoot);

		if (!localRoot.isAbsolute())
			throw new IllegalArgumentException("localRoot is not absolute.");

		if (!localRoot.exists())
			throw new FileNotFoundException(localRoot);

		if (!localRoot.isDirectory())
			throw new FileNoDirectoryException(localRoot);

		assertNotInsideOtherRepository(localRoot);
		return localRoot;
	}

	private void assertNotInsideOtherRepository(final File localRoot) {
		final File localRootFound = LocalRepoHelper.getLocalRootContainingFile(localRoot);
		if (localRootFound != null && !localRootFound.equals(localRoot))
			throw new FileAlreadyRepositoryException(localRoot);
	}

	private void initMetaDir(final boolean createRepository) throws LocalRepoManagerException {
		final File metaDir = getMetaDir();
		if (createRepository) {
			if (metaDir.exists()) {
				throw new FileAlreadyRepositoryException(localRoot);
			}

			deleteMetaDir = true;
			metaDir.mkdir();

			initLockFile();
			createRepositoryPropertiesFile();
			try {
				IOUtil.copyResource(LocalRepoManagerImpl.class, "/" + PERSISTENCE_PROPERTIES_FILE_NAME, createFile(metaDir, PERSISTENCE_PROPERTIES_FILE_NAME));
			} catch (final IOException e) {
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
		final File lock = createFile(getMetaDir(), "cloudstore-repository.lock");
		try {
			lockFile = LockFileFactory.getInstance().acquire(lock, 100);
		} catch (final TimeoutException x) {
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
		final File repositoryPropertiesFile = createFile(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		try {
			repositoryProperties = new Properties();
			repositoryProperties.put(PROP_VERSION, Integer.valueOf(version).toString());
			PropertiesUtil.store(repositoryPropertiesFile, repositoryProperties, null);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkRepositoryPropertiesFile() throws LocalRepoManagerException {
		final File repositoryPropertiesFile = createFile(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		if (!repositoryPropertiesFile.exists())
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-directory does not contain '%s'!", REPOSITORY_PROPERTIES_FILE_NAME));

		try {
			repositoryProperties = PropertiesUtil.load(repositoryPropertiesFile);
		} catch (final IOException e) {
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
		} catch (final NumberFormatException x) {
			throw new RepositoryCorruptException(localRoot,
					String.format("Meta-file '%s' contains an illegal value (not a number) for property '%s'!", REPOSITORY_PROPERTIES_FILE_NAME, PROP_VERSION));
		}

		// Because version 1 was used by 0.9.0, we do not provide compatibility, yet. Maybe we add compatibility
		// code converting version 1 into 2, later.
		// Further, this check prevents old versions to work with a newer repo (and possibly corrupt it).
		if (ver != 2)
			throw new RepositoryCorruptException(localRoot, "Repository is not version 2!");
	}

	private void syncWithLocalRepoRegistry() {
		AssertUtil.assertNotNull("repositoryId", repositoryId);
		LocalRepoRegistry.getInstance().putRepository(repositoryId, localRoot);
		final LocalRepoTransactionImpl transaction = beginWriteTransaction();
		try {
			final LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
			final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
			for (final String repositoryAlias : new ArrayList<>(localRepository.getAliases())) {
				final UUID repositoryIdInRegistry = localRepoRegistry.getRepositoryId(repositoryAlias);
				if (repositoryIdInRegistry == null) {
					localRepoRegistry.putRepositoryAlias(repositoryAlias, repositoryId);
					logger.info("syncWithLocalRepoRegistry: Alias '{}' of repository '{}' copied from repo to repoRegistry.", repositoryAlias, repositoryId);
				}
				else if (repositoryId.equals(repositoryIdInRegistry)) {
					logger.debug("syncWithLocalRepoRegistry: Alias '{}' of repository '{}' already in-sync.", repositoryAlias, repositoryId);
				}
				else {
					localRepository.getAliases().remove(repositoryAlias);
					logger.warn("syncWithLocalRepoRegistry: Alias '{}' removed from repository '{}', because of conflicting entry (repository '{}') in localRepoRegistry.", repositoryAlias, repositoryId, repositoryIdInRegistry);
				}
			}

			final Collection<String> repositoryAliases = localRepoRegistry.getRepositoryAliases(repositoryId.toString());
			if (repositoryAliases != null) {
				for (final String repositoryAlias : repositoryAliases) {
					if (!localRepository.getAliases().contains(repositoryAlias)) {
						localRepository.getAliases().add(repositoryAlias);
						logger.info("syncWithLocalRepoRegistry: Alias '{}' of repository '{}' copied from repoRegistry to repo.", repositoryAlias, repositoryId);
					}
				}
			}

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	private void updateRepositoryPropertiesFile() {
		AssertUtil.assertNotNull("repositoryProperties", repositoryProperties);
		final File repositoryPropertiesFile = createFile(getMetaDir(), REPOSITORY_PROPERTIES_FILE_NAME);
		try {
			boolean store = false;
			final String repositoryId = AssertUtil.assertNotNull("repositoryId", getRepositoryId()).toString();
			if (!repositoryId.equals(repositoryProperties.getProperty(PROP_REPOSITORY_ID))) {
				repositoryProperties.setProperty(PROP_REPOSITORY_ID, repositoryId);
				store = true;
			}

			final LocalRepoTransactionImpl transaction = beginReadTransaction();
			try {
				final LocalRepository localRepository = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
				final SortedSet<String> repositoryAliases = new TreeSet<>(localRepository.getAliases());
				final String aliasesString = repositoryAliasesToString(repositoryAliases);
				if (!aliasesString.equals(repositoryProperties.getProperty(PROP_REPOSITORY_ALIASES))) {
					repositoryProperties.setProperty(PROP_REPOSITORY_ALIASES, aliasesString);
					store = true;
				}

				transaction.commit();
			} finally {
				transaction.rollbackIfActive();
			}

			if (store)
				PropertiesUtil.store(repositoryPropertiesFile, repositoryProperties, null);

			repositoryProperties = null; // not needed anymore => gc
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String repositoryAliasesToString(final Set<String> repositoryAliases) {
		AssertUtil.assertNotNull("repositoryAliases", repositoryAliases);
		final StringBuilder sb = new StringBuilder();
		sb.append('/');
		for (final String repositoryAlias : repositoryAliases) {
			sb.append(repositoryAlias);
			sb.append('/');
		}
		return sb.toString();
	}

	private void initPersistenceManagerFactory(final boolean createRepository) throws LocalRepoManagerException {
		logger.debug("[{}]initPersistenceManagerFactory: Starting up PersistenceManagerFactory...", id);
		final long beginTimestamp = System.currentTimeMillis();
//		initPersistenceManagerFactoryAndPersistenceCapableClassesWithRetry(createRepository);
		initPersistenceManagerFactoryAndPersistenceCapableClasses(createRepository);

		final PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
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
			final PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
			try {
				pm.currentTransaction().begin();

				final RemoteRepositoryRequestDao dao = new RemoteRepositoryRequestDao().persistenceManager(pm);
				final Collection<RemoteRepositoryRequest> expiredRequests = dao.getRemoteRepositoryRequestsChangedBefore(new Date(System.currentTimeMillis() - remoteRepositoryRequestExpiryAge));
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

	private void initPersistenceManagerFactoryAndPersistenceCapableClasses(final boolean createRepository) {
		final Map<String, String> persistenceProperties = getPersistenceProperties(createRepository);
		persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(persistenceProperties);
		final PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
		try {
			try {
				initPersistenceCapableClasses(pm);
			} catch (final Exception x) {
				if (x instanceof RuntimeException)
					throw (RuntimeException)x;
				else
					throw new RuntimeException(x);
			}
		} finally {
			if (pm != null)
				pm.close();
		}
	}

	private void initPersistenceCapableClasses(final PersistenceManager pm) {
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
		pm.getExtent(FileInProgressMarker.class);
		pm.getExtent(Symlink.class);
		pm.getExtent(TransferDoneMarker.class);
	}

	private void assertSinglePersistentLocalRepository(final PersistenceManager pm) {
		try {
			final LocalRepository localRepository = new LocalRepositoryDao().persistenceManager(pm).getLocalRepositoryOrFail();
			readRepositoryMainProperties(localRepository);
		} catch (final IllegalStateException x) {
			throw new RepositoryCorruptException(localRoot, x.getMessage());
		}
	}

	private void createAndPersistLocalRepository(final PersistenceManager pm) {
		LocalRepository localRepository = new LocalRepository();
		final Directory root = create(Directory.class);
		root.setName("");
		root.setLastModified(new Date(localRoot.lastModified()));
		localRepository.setRoot(root);
		generatePublicPrivateKey(localRepository);

		localRepository = pm.makePersistent(localRepository);
		readRepositoryMainProperties(localRepository);
	}

	private void readRepositoryMainProperties(final LocalRepository localRepository) {
		AssertUtil.assertNotNull("localRepository", localRepository);
		repositoryId = AssertUtil.assertNotNull("localRepository.repositoryId", localRepository.getRepositoryId());
		publicKey = AssertUtil.assertNotNull("localRepository.publicKey", localRepository.getPublicKey());
		privateKey = AssertUtil.assertNotNull("localRepository.privateKey", localRepository.getPrivateKey());
	}

	private static final String KEY_STORE_PASSWORD_STRING = "CloudStore-key-store";
	private static final char[] KEY_STORE_PASSWORD_CHAR_ARRAY = KEY_STORE_PASSWORD_STRING.toCharArray();
	private final SecureRandom random = new SecureRandom();

	private void generatePublicPrivateKey(final LocalRepository localRepository) {
		try {
			final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null, KEY_STORE_PASSWORD_CHAR_ARRAY);

			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(getKeySize(), random);
			final KeyPair pair = keyGen.generateKeyPair();

			localRepository.setPrivateKey(pair.getPrivate().getEncoded());
			localRepository.setPublicKey(pair.getPublic().getEncoded());
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private File getMetaDir() {
		return createFile(localRoot, META_DIR_NAME);
	}

	private Map<String, String> getPersistenceProperties(final boolean createRepository) {
		final Map<String, String> persistenceProperties = new PersistencePropertiesProvider(localRoot).getPersistenceProperties(createRepository);
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
	public void addLocalRepoManagerCloseListener(final LocalRepoManagerCloseListener listener) {
		localRepoManagerCloseListeners.add(listener);
	}

	@Override
	public void removeLocalRepoManagerCloseListener(final LocalRepoManagerCloseListener listener) {
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
			final boolean result = super.cancel();
			return result;
		}
	};

	protected boolean open() {
		boolean result;
		lock.lock();
		try {
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
		} finally {
			lock.unlock();
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
		lock.lock();
		try {
			closing = true;
		} finally {
			lock.unlock();
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
			lock.lock();
			try {
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
			} finally {
				lock.unlock();
			}
		}
		else
			_close();
	}

	private void _close() {
		lock.lock();
		try {
			if (!closing) { // closing was aborted
				logger.info("[{}]_close: Closing was aborted. Returning immediately.", id);
				return;
			}
			closeAbortable = false;

			if (closeDeferredTimerTask != null) {
				closeDeferredTimerTask.cancel();
				closeDeferredTimerTask = null;
			}
		} finally {
			lock.unlock();
		}

		logger.info("[{}]_close: Shutting down real LocalRepoManager.", id);
		// TODO defer this (don't immediately close)
		// TODO the timeout should be configurable
		final LocalRepoManagerCloseEvent event = new LocalRepoManagerCloseEvent(this, this, true);
		for (final LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.preClose(event);
		}

		deleteExpiredRemoteRepositoryRequestsTimer.cancel();

		lock.lock();
		try {
			if (persistenceManagerFactory != null) {
				try {
					persistenceManagerFactory.close();
				} catch (final Exception x) {
					logger.warn("Closing PersistenceManagerFactory failed: " + x, x);
				}
				persistenceManagerFactory = null;
				try {
					shutdownDerbyDatabase(connectionURL);
				} catch (final Exception x) {
					logger.warn("Shutting down Derby database failed: " + x, x);
				}
			}
			if (lockFile != null) {
				try {
					lockFile.release();
				} catch (final Exception x) {
					logger.warn("Releasing LockFile failed: " + x, x);
				}
				lockFile = null;
			}
		} finally {
			lock.unlock();
		}

		for (final LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
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
	public byte[] getRemoteRepositoryPublicKeyOrFail(final UUID repositoryId) {
		final byte[] result;
		final LocalRepoTransactionImpl transaction = beginReadTransaction();
		try {
			final RemoteRepository remoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(repositoryId);
			result = remoteRepository.getPublicKey();
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
		return result;
	}

	@Override
	public boolean isOpen() {
		lock.lock();
		try {
			return persistenceManagerFactory != null;
		} finally {
			lock.unlock();
		}
	}

	protected void assertOpen() {
		if (!isOpen())
			throw new IllegalStateException("This LocalRepoManagerImpl is closed!");
	}

	@Override
	public LocalRepoTransactionImpl beginReadTransaction() {
		lock.lock();
		try {
			assertOpen();
			return new LocalRepoTransactionImpl(this, false);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public LocalRepoTransactionImpl beginWriteTransaction() {
		lock.lock();
		try {
			assertOpen();
			return new LocalRepoTransactionImpl(this, true);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void localSync(final ProgressMonitor monitor) {
		monitor.beginTask("Local sync...", 100);
		try {
			final LocalRepoTransactionImpl transaction = beginWriteTransaction();
			try {
				monitor.worked(1);
				LocalRepoSync.create(transaction).sync(new SubProgressMonitor(monitor, 98));
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
	public void putRemoteRepository(final UUID repositoryId, final URL remoteRoot, final byte[] publicKey, final String localPathPrefix) {
		AssertUtil.assertNotNull("entityID", repositoryId);
		AssertUtil.assertNotNull("publicKey", publicKey);
		final LocalRepoTransactionImpl transaction = beginWriteTransaction();
		try {
			final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);

			if (remoteRoot != null) {
				final RemoteRepository otherRepoWithSameRemoteRoot = remoteRepositoryDao.getRemoteRepository(remoteRoot);
				if (otherRepoWithSameRemoteRoot != null && !repositoryId.equals(otherRepoWithSameRemoteRoot.getRepositoryId()))
					throw new IllegalStateException(String.format("Duplicate remoteRoot! The RemoteRepository '%s' already has the same remoteRoot '%s'! The remoteRoot must be unique!", otherRepoWithSameRemoteRoot.getRepositoryId(), remoteRoot));
			}

			RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepository(repositoryId);
			if (remoteRepository == null) {
				remoteRepository = new RemoteRepository(repositoryId);
				remoteRepository.setRevision(-1);
			}
			remoteRepository.setRemoteRoot(remoteRoot);
			remoteRepository.setPublicKey(publicKey);

			remoteRepository.setLocalPathPrefix(localPathPrefix);

			remoteRepositoryDao.makePersistent(remoteRepository); // just in case, it is new (otherwise this has no effect, anyway).

			final RemoteRepositoryRequestDao remoteRepositoryRequestDao = transaction.getDao(RemoteRepositoryRequestDao.class);
			final RemoteRepositoryRequest remoteRepositoryRequest = remoteRepositoryRequestDao.getRemoteRepositoryRequest(repositoryId);
			if (remoteRepositoryRequest != null)
				remoteRepositoryRequestDao.deletePersistent(remoteRepositoryRequest);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	@Override
	public void deleteRemoteRepository(final UUID repositoryId) {
		AssertUtil.assertNotNull("entityID", repositoryId);
		final LocalRepoTransactionImpl transaction = beginWriteTransaction();
		try {
			final RemoteRepositoryDao remoteRepositoryDao = transaction.getDao(RemoteRepositoryDao.class);
			final RemoteRepository remoteRepository = remoteRepositoryDao.getRemoteRepository(repositoryId);
			if (remoteRepository != null)
				remoteRepositoryDao.deletePersistent(remoteRepository);

			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
	}

	protected int getKeySize() {
		final int keySize = PropertiesUtil.getSystemPropertyValueAsInt(SYSTEM_PROPERTY_KEY_SIZE, DEFAULT_KEY_SIZE);
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
	public String getLocalPathPrefixOrFail(final URL remoteRoot) {
		final String localPathPrefix;
		final LocalRepoTransaction transaction = beginReadTransaction();
		try {
			final RemoteRepository remoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(remoteRoot);
			localPathPrefix = remoteRepository.getLocalPathPrefix();
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
		return localPathPrefix;
	}

	@Override
	public String getLocalPathPrefixOrFail(final UUID repositoryId) {
		final String localPathPrefix;
		final LocalRepoTransaction transaction = beginReadTransaction();
		try {
			final RemoteRepository clientRemoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(repositoryId);
			localPathPrefix = clientRemoteRepository.getLocalPathPrefix();
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
		return localPathPrefix;
	}

	@Override
	public UUID getRemoteRepositoryIdOrFail(final URL remoteRoot) {
		UUID remoteRepositoryId;
		final LocalRepoTransaction transaction = beginReadTransaction();
		try {
			final RemoteRepository remoteRepository = transaction.getDao(RemoteRepositoryDao.class).getRemoteRepositoryOrFail(remoteRoot);
			remoteRepositoryId = remoteRepository.getRepositoryId();
			transaction.commit();
		} finally {
			transaction.rollbackIfActive();
		}
		return remoteRepositoryId;
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
	}

}
