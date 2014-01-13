package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of {@link LocalRepoManager}s.
 * <p>
 * There is one single instance of this registry. It serves as the central point to obtain
 * {@code LocalRepoManager}s.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class LocalRepoManagerFactory
{
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoManagerFactory.class);

	private Map<File, LocalRepoManagerImpl> localRoot2LocalRepoManagerImpl = new HashMap<File, LocalRepoManagerImpl>();

	private List<LocalRepoManagerCloseListener> localRepoManagerCloseListeners = new CopyOnWriteArrayList<LocalRepoManagerCloseListener>();

	private LocalRepoManagerCloseListener localRepoManagerCloseListener = new LocalRepoManagerCloseListener() {
		@Override
		public void preClose(LocalRepoManagerCloseEvent event) {
			if (!event.isBackend())
				throw new IllegalStateException("Why are we notified by the proxy?!?");

			preLocalRepoManagerBackendClose(event.getLocalRepoManager());
		}
		@Override
		public void postClose(LocalRepoManagerCloseEvent event) {
			if (!event.isBackend())
				throw new IllegalStateException("Why are we notified by the proxy?!?");

			postLocalRepoManagerBackendClose(event.getLocalRepoManager());
		}
	};

	private LocalRepoManagerFactory() {}

	private static class LocalRepoManagerFactoryHolder {
		public static final LocalRepoManagerFactory INSTANCE = new LocalRepoManagerFactory();
	}

	public static LocalRepoManagerFactory getInstance() {
		return LocalRepoManagerFactoryHolder.INSTANCE;
	}

	public synchronized Set<File> getLocalRoots() {
		return Collections.unmodifiableSet(new HashSet<File>(localRoot2LocalRepoManagerImpl.keySet()));
	}

	/**
	 * Creates a {@link LocalRepoManager} for the given {@code localRoot}.
	 * <p>
	 * <b>Important:</b> You must call {@link LocalRepoManager#close()}. Use a try-finally block or a
	 * similar construction to prevent resource leakage!
	 * <p>
	 * If there is already a {@code LocalRepoManager} implementation instance for this {@code localRoot}, the same
	 * instance is re-used in the background. If there is none, yet, it is implicitly instantiated and enlisted.
	 * However, this method always returns a new proxy instance! It never returns the same instance twice.
	 * <p>
	 * If {@code localRoot} is not an existing repository in the file system, one of the following
	 * exceptions is thrown:
	 * <ul>
	 * <li>{@link FileNotFoundException}
	 * <li>{@link FileNoDirectoryException}
	 * <li>{@link FileNoRepositoryException}
	 * </ul>
	 * @param localRoot the root-directory of the repository. Must not be <code>null</code>. Can be
	 * relative or absolute.
	 * @return the {@link LocalRepoManagerImpl} for the given {@code localRoot}. Never <code>null</code>.
	 * @see #createLocalRepoManagerForNewRepository(File)
	 * @throws LocalRepoManagerException if the given {@code localRoot} does not denote the root-directory
	 * of an existing repository.
	 */
	public synchronized LocalRepoManager createLocalRepoManagerForExistingRepository(File localRoot) throws LocalRepoManagerException {
		localRoot = canonicalize(localRoot);

		LocalRepoManagerImpl localRepoManagerImpl = localRoot2LocalRepoManagerImpl.get(localRoot);
		if (localRepoManagerImpl != null && !localRepoManagerImpl.open()) {
			while (localRepoManagerImpl.isOpen()) {
				logger.info("createLocalRepoManagerForExistingRepository: Existing LocalRepoManagerImpl is currently closing and could not be re-opened. Waiting for it to be completely closed.");
				try { Thread.sleep(100); } catch (InterruptedException x) { doNothing(); }
			}
			localRepoManagerImpl = null;
		}

		if (localRepoManagerImpl == null) {
			localRepoManagerImpl = new LocalRepoManagerImpl(localRoot, false);
			if (!localRepoManagerImpl.open())
				throw new IllegalStateException("localRepoManagerImpl.open() of *new* instance returned false!");

			enlist(localRepoManagerImpl);
		}
		return createProxy(localRepoManagerImpl);
	}

	/**
	 * Creates a {@link LocalRepoManager} for the given {@code localRoot}.
	 * <p>
	 * <b>Important:</b> You must call {@link LocalRepoManager#close()}. Use a try-finally block or a
	 * similar construction to prevent resource leakage!
	 * <p>
	 * This method turns an existing directory into a repository. If {@code localRoot} already is a repository,
	 * a {@link FileAlreadyRepositoryException} is thrown.
	 * <p>
	 * If {@code localRoot} is not an existing directory in the file system, one of the following
	 * exceptions is thrown:
	 * <ul>
	 * <li>{@link FileNotFoundException}
	 * <li>{@link FileNoDirectoryException}
	 * </ul>
	 * @param localRoot the directory which is turned into the repository's root. Must not be <code>null</code>.
	 * Can be relative or absolute.
	 * @return the {@link LocalRepoManager} for the given {@code localRoot}. Never <code>null</code>.
	 * @throws LocalRepoManagerException if the given {@code localRoot} does not denote an existing directory
	 * or if it is a directory inside an existing repository.
	 */
	public synchronized LocalRepoManager createLocalRepoManagerForNewRepository(File localRoot) throws LocalRepoManagerException {
		localRoot = canonicalize(localRoot);

		LocalRepoManagerImpl localRepoManagerImpl = localRoot2LocalRepoManagerImpl.get(localRoot);
		if (localRepoManagerImpl != null) {
			throw new FileAlreadyRepositoryException(localRoot);
		}

		localRepoManagerImpl = new LocalRepoManagerImpl(localRoot, true);
		if (!localRepoManagerImpl.open())
			throw new IllegalStateException("localRepoManagerImpl.open() of *new* instance returned false!");

		enlist(localRepoManagerImpl);
		return createProxy(localRepoManagerImpl);
	}

	private LocalRepoManager createProxy(LocalRepoManagerImpl localRepoManagerImpl) {
		return (LocalRepoManager) Proxy.newProxyInstance(
				this.getClass().getClassLoader(),
				new Class<?>[] { LocalRepoManager.class },
				new LocalRepoManagerInvocationHandler(localRepoManagerImpl));
	}

	public synchronized void close() {
		for (LocalRepoManagerImpl localRepoManagerImpl : new ArrayList<LocalRepoManagerImpl>(localRoot2LocalRepoManagerImpl.values())) {
			localRepoManagerImpl.close();
		}
	}

	public void addLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener) {
		localRepoManagerCloseListeners.add(listener);
	}

	public void removeLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener) {
		localRepoManagerCloseListeners.remove(listener);
	}

	private void enlist(LocalRepoManagerImpl localRepoManager) {
		localRoot2LocalRepoManagerImpl.put(localRepoManager.getLocalRoot(), localRepoManager);
		localRepoManager.addLocalRepoManagerCloseListener(localRepoManagerCloseListener);
	}

	private File canonicalize(File localRoot) {
		assertNotNull("localRoot", localRoot);
		try {
			localRoot = localRoot.getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return localRoot;
	}

	private void preLocalRepoManagerBackendClose(LocalRepoManager localRepoManager) {
		LocalRepoManagerCloseEvent event = new LocalRepoManagerCloseEvent(this, localRepoManager, true);
		for (LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.preClose(event);
		}
	}

	private void postLocalRepoManagerBackendClose(LocalRepoManager localRepoManager) {
		assertNotNull("localRepoManager", localRepoManager);
		synchronized (this) {
			LocalRepoManager localRepoManager2 = localRoot2LocalRepoManagerImpl.remove(localRepoManager.getLocalRoot());
			if (localRepoManager != localRepoManager2) {
				throw new IllegalArgumentException("localRepoManager is unknown!");
			}
		}
		LocalRepoManagerCloseEvent event = new LocalRepoManagerCloseEvent(this, localRepoManager, true);
		for (LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.postClose(event);
		}
	}

	private static final void doNothing() { }
}
