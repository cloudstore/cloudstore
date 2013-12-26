package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import co.codewizards.cloudstore.shared.persistence.Repository;
import co.codewizards.cloudstore.shared.util.IOUtil;
import co.codewizards.cloudstore.shared.util.PropertiesUtil;

/**
 * Manager of a repository.
 * <p>
 * All operations on a repository are performed via this manager (or an object associated with it).
 * <p>
 * For every repository (identified by its root directory) there is one single instance. Use the
 * {@link RepositoryManagerRegistry} to obtain a {@code RepositoryManager}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class RepositoryManager {

	private static final String VAR_LOCAL_ROOT = "repository.localRoot";
	private static final String VAR_META_DIR = "repository.metaDir";

	private static final String META_DIR_NAME = ".cloudstore";
	private static final String PERSISTENCE_PROPERTIES_FILE_NAME = "cloudstore-persistence.properties";

	private static final String CONNECTION_URL_KEY = "javax.jdo.option.ConnectionURL";

	private final File localRoot;
	private PersistenceManagerFactory persistenceManagerFactory;
	private List<RepositoryManagerCloseListener> repositoryManagerCloseListeners = new CopyOnWriteArrayList<RepositoryManagerCloseListener>();

	private boolean deleteMetaDir;

	protected RepositoryManager(File localRoot, boolean createRepository) throws RepositoryManagerException {
		this.localRoot = assertValidLocalRoot(localRoot);
		deleteMetaDir = false; // only delete, if it is created in initMetaDirectory(...)
		try {
			// TODO Make this more robust: If we have a power-outage between directory creation and the finally block,
			// we end in an inconsistent state. We can avoid this, by tracking the creation process in a properties
			// file later (somehow making this really transactional).
			initMetaDir(createRepository);
			initPersistenceManagerFactory(createRepository);
			deleteMetaDir = false; // if we come here, creation is successful => NO deletion
		} finally {
			if (deleteMetaDir)
				IOUtil.deleteDirectoryRecursively(getMetaDir());
		}
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

	private void initMetaDir(boolean createRepository) throws RepositoryManagerException {
		File metaDirectory = getMetaDir();
		if (createRepository) {
			if (metaDirectory.exists()) {
				throw new FileAlreadyRepositoryException(localRoot);
			}

			deleteMetaDir = true;
			metaDirectory.mkdir();

			try {
				IOUtil.copyResource(RepositoryManager.class, "/" + PERSISTENCE_PROPERTIES_FILE_NAME, new File(metaDirectory, PERSISTENCE_PROPERTIES_FILE_NAME));
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

	private void initPersistenceManagerFactory(boolean createRepository) throws RepositoryManagerException {
		Map<String, String> persistenceProperties = getPersistenceProperties(createRepository);
		persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(persistenceProperties );
		PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
		try {
			pm.currentTransaction().begin();

			if (createRepository) {
				createAndPersistRepository(pm);
			} else {
				assertSinglePersistentRepository(pm);
			}

			pm.currentTransaction().commit();
		} finally {
			if (pm.currentTransaction().isActive())
				pm.currentTransaction().rollback();

			pm.close();
		}
	}

	private void assertSinglePersistentRepository(PersistenceManager pm) {
		Iterator<Repository> repositoryIterator = pm.getExtent(Repository.class).iterator();
		if (!repositoryIterator.hasNext()) {
			throw new RepositoryCorruptException(localRoot, "Repository entity not found in database.");
		}
		repositoryIterator.next();
		if (repositoryIterator.hasNext()) {
			throw new RepositoryCorruptException(localRoot, "Multiple Repository entities in database.");
		}
	}

	private void createAndPersistRepository(PersistenceManager pm) {
		Repository repository = new Repository();
		repository.setUuid(UUID.randomUUID());
		pm.makePersistent(repository);
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

		if (createRepository) {
			modifyConnectionURLForCreate(persistenceProperties);
		}
		return persistenceProperties;
	}

	private void modifyConnectionURLForCreate(Map<String, String> persistenceProperties) {
		String value = persistenceProperties.get(CONNECTION_URL_KEY);
		if (value == null || value.trim().isEmpty()) {
			throw new RepositoryCorruptException(localRoot,
					String.format("Property '%s' missing in '%s'.", CONNECTION_URL_KEY, PERSISTENCE_PROPERTIES_FILE_NAME));
		}

		String newValue = value.trim() + ";create=true";
		persistenceProperties.put(CONNECTION_URL_KEY, newValue);
	}

	/**
	 * Gets the repository's local root directory.
	 * <p>
	 * This file is canonical (absolute and symbolic links resolved).
	 * @return the repository's local root directory. Never <code>null</code>.
	 */
	public File getLocalRoot() {
		return localRoot;
	}

	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return persistenceManagerFactory;
	}

	public URL getRemoteRoot() {
		throw new UnsupportedOperationException("NYI"); // TODO implement
		// Read from the database!
	}

	public void setRemoteRoot(URL url) {
		throw new UnsupportedOperationException("NYI"); // TODO implement
	}

	public void addRepositoryManagerCloseListener(RepositoryManagerCloseListener listener) {
		repositoryManagerCloseListeners.add(listener);
	}

	public void removeRepositoryManagerCloseListener(RepositoryManagerCloseListener listener) {
		repositoryManagerCloseListeners.remove(listener);
	}

	public void close() {
		RepositoryManagerCloseEvent event = new RepositoryManagerCloseEvent(this, this);
		for (RepositoryManagerCloseListener listener : repositoryManagerCloseListeners) {
			listener.preClose(event);
		}

		synchronized (this) {
			if (persistenceManagerFactory != null) {
				persistenceManagerFactory.close();
				persistenceManagerFactory = null;
			}
		}

		for (RepositoryManagerCloseListener listener : repositoryManagerCloseListeners) {
			listener.postClose(event);
		}
	}
}
