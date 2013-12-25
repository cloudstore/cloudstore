package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
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

	private final String VAR_LOCALROOT = "repository.localRoot"; // TODO why is this not static?!

	private final String META_DIRECTORY_NAME = ".cloudstore"; // TODO why is this not static?!
	private final String META_FILE_NAME = "cloudstore-persistence.properties"; // TODO why is this not static?!

	// TODO why is this a non-javadoc-comment instead of proper javadoc at the getter-method?!
	/*
	 * Canonical File
	 */
	private File localRoot;
	private PersistenceManagerFactory persistenceManagerFactory;
	private List<RepositoryManagerCloseListener> repositoryManagerCloseListeners = new CopyOnWriteArrayList<RepositoryManagerCloseListener>();

	protected RepositoryManager(File localRoot, boolean createRepository) throws RepositoryManagerException {
		this.localRoot = assertNotNull("localRoot", localRoot);

		initMetaDirectory(createRepository);
		initPersistenceManagerFactory(createRepository);
	}

	private void initMetaDirectory(boolean createRepository) throws RepositoryManagerException {
		if (createRepository) {
			File metaDirectory = new File(localRoot, META_DIRECTORY_NAME);
			if (!metaDirectory.exists())
				metaDirectory.mkdir();

			File templateFile = new File(META_FILE_NAME);
			try {
				IOUtil.copyFile(templateFile, new File(metaDirectory, META_FILE_NAME));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void initPersistenceManagerFactory(boolean createRepository) throws RepositoryManagerException {
		if (createRepository) {
			Map<String, String> variablesMap = new HashMap<String, String>();
			variablesMap.put(VAR_LOCALROOT, localRoot.getAbsolutePath());

			Properties properties;
			Map<String, String> metaMap;
			try {
				File metaDirectory = new File(localRoot, META_DIRECTORY_NAME);
				File metaFile = new File(metaDirectory, META_FILE_NAME);

				properties = PropertiesUtil.load(metaFile);
				metaMap = PropertiesUtil.filterProperties(properties, variablesMap);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Repository repository = new Repository();
			repository.setUuid(UUID.randomUUID());

			persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(metaMap);
			PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
			try {
				pm.currentTransaction().begin();

				pm.getExtent(Repository.class);
				pm.makePersistent(repository);
				pm.currentTransaction().commit();
			} finally {
				if (pm.currentTransaction().isActive())
					pm.currentTransaction().rollback();

				pm.close();
			}
		}
		// TODO what about an existing repository?!
	}

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
