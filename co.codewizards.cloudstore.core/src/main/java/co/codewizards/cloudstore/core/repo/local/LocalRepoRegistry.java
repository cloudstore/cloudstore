package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.util.PropertiesUtil;


public class LocalRepoRegistry
{
	public static final String LOCAL_REPO_REGISTRY_FILE = "repositoryList.properties";


	private static class LocalRepoRegistryHolder {
		public static final LocalRepoRegistry INSTANCE = new LocalRepoRegistry();
	}

	public static LocalRepoRegistry getInstance() {
		return LocalRepoRegistryHolder.INSTANCE;
	}

	private LocalRepoRegistry() { }

	private File getRegistryFile() {
		return new File(ConfigDir.getInstance().getFile(), LOCAL_REPO_REGISTRY_FILE);
	}

	private Map<EntityID, File> repositoryID2LocalRootMap;
	private Map<EntityID, File> repositoryID2LocalRootReadOnlyMap;

	public synchronized Map<EntityID, File> getRepositoryID2LocalRootMap() {
		// TODO check that the properties file was not modified by another process!
		Map<EntityID, File> repositoryID2LocalRootMap = this.repositoryID2LocalRootMap;
		if (repositoryID2LocalRootMap == null) {
			repositoryID2LocalRootMap = new HashMap<EntityID, File>();

			try {
				Properties props = PropertiesUtil.load(getRegistryFile());
				Set<Entry<Object, Object>> entrySet = props.entrySet();
				for (Entry<Object, Object> entry : entrySet) {
					EntityID entityID = new EntityID(entry.getKey().toString());
					File file = new File(entry.getValue().toString());
					repositoryID2LocalRootMap.put(entityID, file);
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}

			this.repositoryID2LocalRootMap = repositoryID2LocalRootMap;
		}

		Map<EntityID, File> result = this.repositoryID2LocalRootReadOnlyMap;
		if (result == null) {
			result = Collections.unmodifiableMap(new HashMap<EntityID, File>(repositoryID2LocalRootMap));
			this.repositoryID2LocalRootReadOnlyMap = result;
		}

		return result;
	}

	public File getLocalRoot(EntityID repositoryID) {
		assertNotNull("repositoryID", repositoryID);
		File localRoot = getRepositoryID2LocalRootMap().get(repositoryID);
		return localRoot;
	}

	public File getLocalRootOrFail(EntityID repositoryID) {
		File localRoot = getLocalRoot(repositoryID);
		if (localRoot == null)
			throw new IllegalArgumentException("Unknown repositoryID: " + repositoryID);

		return localRoot;
	}

	public synchronized void registerRepository(EntityID repositoryID, File localRoot) {
		assertNotNull("repositoryID", repositoryID);
		assertNotNull("localRoot", localRoot);

		if (!localRoot.isAbsolute())
			throw new IllegalArgumentException("localRoot is not absolute.");

		File registryFile = getRegistryFile();

		// TODO prevent multiple processes from modifying the properties file simultaneously by employing a lock-file!
		Properties props = new Properties();
		try {
			if (registryFile.exists())
				props = PropertiesUtil.load(registryFile);

			props.setProperty(repositoryID.toString(), localRoot.getPath());

			if (repositoryID2LocalRootMap != null) {
				repositoryID2LocalRootMap.put(repositoryID, localRoot);
			}
			repositoryID2LocalRootReadOnlyMap = null;

			PropertiesUtil.store(registryFile, props, null);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}