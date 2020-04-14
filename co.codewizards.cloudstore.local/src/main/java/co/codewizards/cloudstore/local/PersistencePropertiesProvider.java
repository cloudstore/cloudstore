package co.codewizards.cloudstore.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class PersistencePropertiesProvider {

	private final UUID repositoryId;
	private final File localRoot;
	private File overridePersistencePropertiesFile;

	public PersistencePropertiesProvider(final UUID repositoryId, final File localRoot) {
		this.repositoryId = requireNonNull(repositoryId, "repositoryId");
		this.localRoot = requireNonNull(localRoot, "localRoot");
		if (!localRoot.isDirectory())
			throw new IllegalArgumentException("The given localRoot is not an existing directory: " + localRoot.getAbsolutePath());
	}
	private File getMetaDir() {
		return createFile(localRoot, LocalRepoManager.META_DIR_NAME);
	}

	public File getOverridePersistencePropertiesFile() {
		return overridePersistencePropertiesFile;
	}
	public void setOverridePersistencePropertiesFile(File persistencePropertiesFile) {
		this.overridePersistencePropertiesFile = persistencePropertiesFile;
	}

	public Map<String, String> getPersistenceProperties() {
		final File metaDirectory = overridePersistencePropertiesFile != null ? null : getMetaDir();
		if (metaDirectory != null && ! metaDirectory.isDirectory())
			throw new IllegalStateException("The localRoot does not contain the meta-directory: " + metaDirectory.getAbsolutePath());

		final File persistencePropertiesFile =
				overridePersistencePropertiesFile != null
				? overridePersistencePropertiesFile : createFile(metaDirectory, LocalRepoManager.PERSISTENCE_PROPERTIES_FILE_NAME);
		if (!persistencePropertiesFile.isFile())
			throw new IllegalStateException("The persistencePropertiesFile does not exist or is not a file: " + persistencePropertiesFile.getAbsolutePath());

		final Map<String, Object> variablesMap = new HashMap<>();
		variablesMap.put(LocalRepoManager.VAR_REPOSITORY_ID, repositoryId);
		variablesMap.put(LocalRepoManager.VAR_LOCAL_ROOT, localRoot.getPath());
		variablesMap.put(LocalRepoManager.VAR_META_DIR, getMetaDir().getPath());

		List<PersistencePropertiesVariableProvider> variableProviders = new LinkedList<>();
		for (PersistencePropertiesVariableProvider provider : ServiceLoader.load(PersistencePropertiesVariableProvider.class)) {
			variableProviders.add(provider);
		}
		Collections.sort(variableProviders, new Comparator<PersistencePropertiesVariableProvider>() {
			@Override
			public int compare(PersistencePropertiesVariableProvider o1, PersistencePropertiesVariableProvider o2) {
				int res = Integer.compare(o1.getPriority(), o2.getPriority());
				if (res == 0)
					res = o1.getClass().getName().compareTo(o2.getClass().getName());

				return res;
			}
		});
		for (PersistencePropertiesVariableProvider provider : variableProviders) {
			provider.populatePersistencePropertiesVariableMap(variablesMap);
		}

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
