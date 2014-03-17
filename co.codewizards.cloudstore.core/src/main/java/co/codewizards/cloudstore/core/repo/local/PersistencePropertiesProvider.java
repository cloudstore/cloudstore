package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class PersistencePropertiesProvider {

	private final File localRoot;

	public PersistencePropertiesProvider(File localRoot) {
		this.localRoot = assertNotNull("localRoot", localRoot);
	}
	private File getMetaDir() {
		return new File(localRoot, LocalRepoManager.META_DIR_NAME);
	}

	public Map<String, String> getPersistenceProperties(boolean createRepository) {
		File metaDirectory = getMetaDir();
		File persistencePropertiesFile = new File(metaDirectory, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);

		Map<String, String> variablesMap = new HashMap<String, String>();
		variablesMap.put(LocalRepoManager.VAR_LOCAL_ROOT, localRoot.getPath());
		variablesMap.put(LocalRepoManager.VAR_META_DIR, getMetaDir().getPath());

		Properties rawProperties;
		try {
			rawProperties = PropertiesUtil.load(persistencePropertiesFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Map<String, String> persistenceProperties = PropertiesUtil.filterProperties(rawProperties, variablesMap);
		String connectionURL = persistenceProperties.get(LocalRepoManager.CONNECTION_URL_KEY);
		persistenceProperties.put(LocalRepoManager.CONNECTION_URL_KEY_ORIGINAL, connectionURL);

		if (createRepository) {
			modifyConnectionURLForCreate(persistenceProperties);
		}
		return persistenceProperties;
	}

	private void modifyConnectionURLForCreate(Map<String, String> persistenceProperties) {
		String value = persistenceProperties.get(LocalRepoManager.CONNECTION_URL_KEY);
		if (value == null || value.trim().isEmpty()) {
			throw new RepositoryCorruptException(localRoot,
					String.format("Property '%s' missing in '%s'.", LocalRepoManager.CONNECTION_URL_KEY, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME));
		}

		String newValue = value.trim() + ";create=true";
		persistenceProperties.put(LocalRepoManager.CONNECTION_URL_KEY, newValue);
	}
}
