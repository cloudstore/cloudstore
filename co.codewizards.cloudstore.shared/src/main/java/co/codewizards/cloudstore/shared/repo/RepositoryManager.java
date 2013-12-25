package co.codewizards.cloudstore.shared.repo;

import static co.codewizards.cloudstore.shared.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import co.codewizards.cloudstore.shared.persistence.Repository;
import co.codewizards.cloudstore.shared.util.IOUtil;
import co.codewizards.cloudstore.shared.util.PropertiesUtil;


public class RepositoryManager {

	private final String VAR_LOCALROOT = "repository.localRoot";

	private final String META_DIRECTORY_NAME = ".cloudstore";
	private final String META_FILE_NAME = "cloudstore-persistence.properties";
	/*
	 * Canonical File
	 */
	private File localRoot;
	private PersistenceManagerFactory persistenceManagerFactory;

	public RepositoryManager(File localRoot, boolean createRepository) {
		this.localRoot = assertNotNull("localRoot", localRoot);

		initMetaDirectory(createRepository);
		initPersistenceManagerFactory(createRepository);
	}

	private void initMetaDirectory(boolean createRepository) {
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

	private void initPersistenceManagerFactory(boolean createRepository) {
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
	}

	public File getLocalRoot() {
		return localRoot;
	}

	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return persistenceManagerFactory;
	}
}
