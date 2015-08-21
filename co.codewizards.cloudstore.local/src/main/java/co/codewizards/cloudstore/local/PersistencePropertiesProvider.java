package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class PersistencePropertiesProvider {

	private final UUID repositoryId;
	private final File localRoot;

	public PersistencePropertiesProvider(final UUID repositoryId, final File localRoot) {
		this.repositoryId = assertNotNull("repositoryId", repositoryId);
		this.localRoot = assertNotNull("localRoot", localRoot);
		if (!localRoot.isDirectory())
			throw new IllegalArgumentException("The given localRoot is not an existing directory: " + localRoot.getAbsolutePath());
	}
	private File getMetaDir() {
		return createFile(localRoot, LocalRepoManager.META_DIR_NAME);
	}

	public Map<String, String> getPersistenceProperties() {
		final File metaDirectory = getMetaDir();
		if (!metaDirectory.isDirectory())
			throw new IllegalStateException("The localRoot does not contain the meta-directory: " + metaDirectory.getAbsolutePath());

		final File persistencePropertiesFile = createFile(metaDirectory, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);
		if (!persistencePropertiesFile.isFile())
			throw new IllegalStateException("The persistencePropertiesFile does not exist or is not a file: " + persistencePropertiesFile.getAbsolutePath());

		final Map<String, Object> variablesMap = new HashMap<>();
		variablesMap.put(LocalRepoManager.VAR_REPOSITORY_ID, repositoryId);
		variablesMap.put(LocalRepoManager.VAR_LOCAL_ROOT, localRoot.getPath());
		variablesMap.put(LocalRepoManager.VAR_META_DIR, getMetaDir().getPath());

		Properties rawProperties;
		try {
			rawProperties = PropertiesUtil.load(persistencePropertiesFile);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		final Map<String, String> persistenceProperties = PropertiesUtil.filterProperties(rawProperties, variablesMap);
//		final String connectionURL = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_URL.key);
//		persistenceProperties.put(PersistencePropertiesEnum.CONNECTION_URL_ORIGINAL.key, connectionURL);

//		if (createRepository) {
//			modifyConnectionURLForCreate(persistenceProperties);
//		}
		return persistenceProperties;
	}

//	private void modifyConnectionURLForCreate(final Map<String, String> persistenceProperties) {
//		final String value = persistenceProperties.get(PersistencePropertiesEnum.CONNECTION_URL.key);
//		if (value == null || value.trim().isEmpty()) {
//			throw new RepositoryCorruptException(localRoot,
//					String.format("Property '%s' missing in '%s'.", PersistencePropertiesEnum.CONNECTION_URL.key, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME));
//		}
//
//		final String newValue = value.trim() + ";create=true";
//		persistenceProperties.put(PersistencePropertiesEnum.CONNECTION_URL.key, newValue);
//	}
}
