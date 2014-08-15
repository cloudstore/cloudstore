package co.codewizards.cloudstore.core.repo.local;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import co.codewizards.cloudstore.core.oio.file.File;

public interface LocalRepoManagerFactory {
	class Helper {
		private static LocalRepoManagerFactory instance;

		public static synchronized LocalRepoManagerFactory getInstance() {
			if (instance == null) {
				final ServiceLoader<LocalRepoManagerFactory> serviceLoader = ServiceLoader.load(LocalRepoManagerFactory.class);
				final Iterator<LocalRepoManagerFactory> iterator = serviceLoader.iterator();
				if (!iterator.hasNext())
					throw new IllegalStateException("There is no LocalRepoManagerFactory implementation registered! Maybe the JAR 'co.codewizards.cloudstore.local' is missing in the classpath?!");

				final LocalRepoManagerFactory localRepoManagerFactory = iterator.next();

				if (iterator.hasNext())
					throw new IllegalStateException("There are multiple LocalRepoManagerFactory implementations registered! Maybe there are multiple versions of JAR 'co.codewizards.cloudstore.local' in the classpath?!");

				instance = localRepoManagerFactory;
			}
			return instance;
		}
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
	LocalRepoManager createLocalRepoManagerForExistingRepository(File localRoot) throws LocalRepoManagerException;

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
	LocalRepoManager createLocalRepoManagerForNewRepository(File localRoot) throws LocalRepoManagerException;

	void close();

	void addLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener);

	void removeLocalRepoManagerCloseListener(LocalRepoManagerCloseListener listener);

	Set<File> getLocalRoots();

}
