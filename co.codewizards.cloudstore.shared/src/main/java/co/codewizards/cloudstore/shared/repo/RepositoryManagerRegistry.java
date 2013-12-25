package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of {@link RepositoryManager}s.
 * <p>
 * There is one single instance of this registry. It serves as the central point to obtain
 * {@code RepositoryManager}s.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class RepositoryManagerRegistry
{
	private Map<File, RepositoryManager> localRoot2RepositoryManager = new HashMap<File, RepositoryManager>();

	private List<RepositoryManagerCloseListener> repositoryManagerCloseListeners = new CopyOnWriteArrayList<RepositoryManagerCloseListener>();

	private RepositoryManagerCloseListener repositoryManagerCloseListener = new RepositoryManagerCloseListener() {
		@Override
		public void preClose(RepositoryManagerCloseEvent event) {
			preRepositoryManagerClose(event.getRepositoryManager());
		}
		@Override
		public void postClose(RepositoryManagerCloseEvent event) {
			postRepositoryManagerClose(event.getRepositoryManager());
		}
	};

	private RepositoryManagerRegistry() {}

	private static class RepositoryManagerRegistryHolder {
		public static final RepositoryManagerRegistry INSTANCE = new RepositoryManagerRegistry();
	}

	public static RepositoryManagerRegistry getInstance() {
		return RepositoryManagerRegistryHolder.INSTANCE;
	}

	/**
	 * Get the {@link RepositoryManager} for the given {@code localRoot}.
	 * <p>
	 * If there is already a {@code RepositoryManager} instance for this {@code localRoot}, the same
	 * instance is returned. If there is none, yet, it is implicitly instantiated and enlisted.
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
	 * @return the {@link RepositoryManager} for the given {@code localRoot}. Never <code>null</code>.
	 * @see #createRepositoryManager(File)
	 * @throws RepositoryManagerException if the given {@code localRoot} does not denote the root-directory
	 * of an existing repository.
	 */
	public RepositoryManager getRepositoryManager(File localRoot) throws RepositoryManagerException { // TODO why is this not synchronized?!?
		localRoot = canonicalize(localRoot);

		RepositoryManager repositoryManager = localRoot2RepositoryManager.get(localRoot);
		if (repositoryManager == null) {
			repositoryManager = new RepositoryManager(localRoot, false);
			enlist(repositoryManager);
		}
		return repositoryManager;
	}

	/**
	 * Create the {@link RepositoryManager} for the given {@code localRoot}.
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
	 * @return the {@link RepositoryManager} for the given {@code localRoot}. Never <code>null</code>.
	 * @throws RepositoryManagerException if the given {@code localRoot} does not denote an existing directory
	 * or if it is a directory inside an existing repository.
	 */
	public RepositoryManager createRepositoryManager(File localRoot) throws RepositoryManagerException { // TODO why is this not synchronized?!?
		localRoot = canonicalize(localRoot);

		RepositoryManager repositoryManager = localRoot2RepositoryManager.get(localRoot);
		if (repositoryManager != null) {
			throw new FileAlreadyRepositoryException(localRoot);
		}
		repositoryManager = new RepositoryManager(localRoot, true);
		enlist(repositoryManager);
		return repositoryManager;
	}

	public synchronized void close() {
		for (RepositoryManager repositoryManager : new ArrayList<RepositoryManager>(localRoot2RepositoryManager.values())) {
			repositoryManager.close();
		}
	}

	public void addRepositoryManagerCloseListener(RepositoryManagerCloseListener listener) {
		repositoryManagerCloseListeners.add(listener);
	}

	public void removeRepositoryManagerCloseListener(RepositoryManagerCloseListener listener) {
		repositoryManagerCloseListeners.remove(listener);
	}

	private void enlist(RepositoryManager repositoryManager) {
		localRoot2RepositoryManager.put(repositoryManager.getLocalRoot(), repositoryManager);
		repositoryManager.addRepositoryManagerCloseListener(repositoryManagerCloseListener);
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

	private void preRepositoryManagerClose(RepositoryManager repositoryManager) {
		RepositoryManagerCloseEvent event = new RepositoryManagerCloseEvent(this, repositoryManager);
		for (RepositoryManagerCloseListener listener : repositoryManagerCloseListeners) {
			listener.preClose(event);
		}
	}

	private void postRepositoryManagerClose(RepositoryManager repositoryManager) {
		assertNotNull("repositoryManager", repositoryManager);
		synchronized (this) {
			RepositoryManager repositoryManager2 = localRoot2RepositoryManager.remove(repositoryManager.getLocalRoot());
			if (repositoryManager != repositoryManager2) {
				throw new IllegalArgumentException("repositoryManager is unknown!");
			}
		}
		RepositoryManagerCloseEvent event = new RepositoryManagerCloseEvent(this, repositoryManager);
		for (RepositoryManagerCloseListener listener : repositoryManagerCloseListeners) {
			listener.postClose(event);
		}
	}
}
