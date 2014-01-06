package co.codewizards.cloudstore.core.repo.local;

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

	private Map<EntityID, File> repositoryID2FileMap;

	public synchronized Map<EntityID, File> getRepositoryID2FileMap() {
		if (repositoryID2FileMap == null) {
			repositoryID2FileMap = new HashMap<EntityID, File>();
		}

		try {
		 	Properties props = PropertiesUtil.load(getRegistryFile());
			Set<Entry<Object, Object>> entrySet = props.entrySet();
			for (Entry<Object, Object> entry : entrySet) {
				EntityID entityID = new EntityID(entry.getKey().toString());
				File file = new File(entry.getValue().toString());
				repositoryID2FileMap.put(entityID, file);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return Collections.unmodifiableMap(repositoryID2FileMap);
	}

	public synchronized void registerRepository(EntityID repositoryID, File file) {
		File registryFile = getRegistryFile();

		Properties props = new Properties();
		try {
			if (registryFile.exists())
				props = PropertiesUtil.load(registryFile);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		props.put(repositoryID.toString(), file.getAbsolutePath());
		
		if (repositoryID2FileMap != null) {
			repositoryID2FileMap.put(repositoryID, file);
		}
		
		try {
			PropertiesUtil.store(registryFile, props, "Repository List");
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}