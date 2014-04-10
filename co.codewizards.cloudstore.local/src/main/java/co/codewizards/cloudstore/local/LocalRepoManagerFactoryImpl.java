package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.util.Util.*;

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

import co.codewizards.cloudstore.core.repo.local.FileAlreadyRepositoryException;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerCloseEvent;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerCloseListener;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerException;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

/**
 * Registry of {@link LocalRepoManager}s.
 * <p>
 * There is one single instance of this registry. It serves as the central point to obtain
 * {@code LocalRepoManager}s.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class LocalRepoManagerFactoryImpl implements LocalRepoManagerFactory
{
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoManagerFactoryImpl.class);

	private Map<File, LocalRepoManagerImpl> localRoot2LocalRepoManagerImpl = new HashMap<File, LocalRepoManagerImpl>();
	private Set<LocalRepoManagerImpl> nonReOpenableLocalRepoManagerImpls = new HashSet<LocalRepoManagerImpl>();

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

			postLocalRepoManagerBackendClose((LocalRepoManagerImpl) event.getLocalRepoManager());
		}
	};

	@Override
	public synchronized Set<File> getLocalRoots() {
		return Collections.unmodifiableSet(new HashSet<File>(localRoot2LocalRepoManagerImpl.keySet()));
	}

	@Override
	public synchronized LocalRepoManager createLocalRepoManagerForExistingRepository(File localRoot) throws LocalRepoManagerException {
		localRoot = canonicalize(localRoot);

		LocalRepoManagerImpl localRepoManagerImpl = localRoot2LocalRepoManagerImpl.get(localRoot);
		if (localRepoManagerImpl != null && !localRepoManagerImpl.open()) {
			localRoot2LocalRepoManagerImpl.remove(localRoot);
			nonReOpenableLocalRepoManagerImpls.add(localRepoManagerImpl);
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

	@Override
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

	@Override
	public synchronized void close() {
		for (LocalRepoManagerImpl localRepoManagerImpl : new ArrayList<LocalRepoManagerImpl>(localRoot2LocalRepoManagerImpl.values())) {
			localRepoManagerImpl.close();
		}
	}

	@Override
	public void addLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener) {
		localRepoManagerCloseListeners.add(listener);
	}

	@Override
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

	private void postLocalRepoManagerBackendClose(LocalRepoManagerImpl localRepoManager) {
		assertNotNull("localRepoManager", localRepoManager);
		synchronized (this) {
			LocalRepoManagerImpl localRepoManager2 = localRoot2LocalRepoManagerImpl.remove(localRepoManager.getLocalRoot());
			if (localRepoManager != localRepoManager2) {
				if (nonReOpenableLocalRepoManagerImpls.remove(localRepoManager))
					logger.info("localRepoManager[{}] could not be re-opened and was unlisted before.", localRepoManager.id);
				else
					throw new IllegalStateException(String.format("localRepoManager[%s] is unknown!", localRepoManager.id));

				localRoot2LocalRepoManagerImpl.put(localRepoManager2.getLocalRoot(), localRepoManager2); // re-add!
			}
		}
		LocalRepoManagerCloseEvent event = new LocalRepoManagerCloseEvent(this, localRepoManager, true);
		for (LocalRepoManagerCloseListener listener : localRepoManagerCloseListeners) {
			listener.postClose(event);
		}
	}

	private static final void doNothing() { }
}
