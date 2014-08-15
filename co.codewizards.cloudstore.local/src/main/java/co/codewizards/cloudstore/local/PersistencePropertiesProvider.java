package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import co.codewizards.cloudstore.core.oio.file.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.RepositoryCorruptException;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class PersistencePropertiesProvider {

	private final File localRoot;

	public PersistencePropertiesProvider(File localRoot) {
		this.localRoot = assertNotNull("localRoot", localRoot);
		if (!localRoot.isDirectory())
			throw new IllegalArgumentException("The given localRoot is not an existing directory: " + localRoot.getAbsolutePath());
	}
	private File getMetaDir() {
		return newFile(localRoot, LocalRepoManager.META_DIR_NAME);
	}

	public Map<String, String> getPersistenceProperties(boolean createRepository) {
		final File metaDirectory = getMetaDir();
		if (!metaDirectory.isDirectory())
			throw new IllegalStateException("The localRoot does not contain the meta-directory: " + metaDirectory.getAbsolutePath());

		final File persistencePropertiesFile = newFile(metaDirectory, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);
		if (!persistencePropertiesFile.isFile())
			throw new IllegalStateException("The persistencePropertiesFile does not exist or is not a file: " + persistencePropertiesFile.getAbsolutePath());

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
		String connectionURL = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_URL.key);
		persistenceProperties.put(PersistencePropertiesEnum.CONNECTION_URL_ORIGINAL.key, connectionURL);

		if (createRepository) {
			modifyConnectionURLForCreate(persistenceProperties);
		}
		return persistenceProperties;
	}

	private void modifyConnectionURLForCreate(Map<String, String> persistenceProperties) {
		String value = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_URL.key);
		if (value == null || value.trim().isEmpty()) {
			throw new RepositoryCorruptException(localRoot,
					String.format("Property '%s' missing in '%s'.", PersistencePropertiesEnum.CONNECTION_URL.key, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME));
		}

		String newValue = value.trim() + ";create=true";
		persistenceProperties.put(PersistencePropertiesEnum.CONNECTION_URL.key, newValue);
	}
}
