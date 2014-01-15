package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class LocalRepoRegistry
{
	public static final String LOCAL_REPO_REGISTRY_FILE = "repositoryList.properties";
	private static final String PROP_KEY_PREFIX_REPOSITORY_ID = "repositoryID:";
	private static final String PROP_KEY_PREFIX_REPOSITORY_ALIAS = "repositoryAlias:";
	private static final long LOCK_TIMEOUT_MS = 10000L; // 10 s

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

	private long repoRegistryFileLastModified;
	private Properties repoRegistryProperties;

	public synchronized Collection<EntityID> getRepositoryIDs() {
		loadRepoRegistryIfNeeded();
		List<EntityID> result = new ArrayList<EntityID>();
		for (Entry<Object, Object> me : repoRegistryProperties.entrySet()) {
			String key = String.valueOf(me.getKey());
			if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ID)) {
				EntityID repositoryID = new EntityID(key.substring(PROP_KEY_PREFIX_REPOSITORY_ID.length()));
				result.add(repositoryID);
			}
		}
		return Collections.unmodifiableList(result);
	}

	public synchronized EntityID getRepositoryID(String repositoryName) {
		assertNotNull("repositoryName", repositoryName);
		loadRepoRegistryIfNeeded();
		String entityIDString = repoRegistryProperties.getProperty(getPropertyKeyForAlias(repositoryName));
		if (entityIDString != null) {
			EntityID entityID = new EntityID(entityIDString);
			return entityID;
		}

		EntityID repositoryID;
		try {
			repositoryID = new EntityID(repositoryName);
		} catch (IllegalArgumentException x) {
			return null;
		}

		String localRootString = repoRegistryProperties.getProperty(getPropertyKeyForID(repositoryID));
		if (localRootString == null)
			return null;

		return repositoryID;
	}

	public EntityID getRepositoryIDOrFail(String repositoryName) {
		EntityID repositoryID = getRepositoryID(repositoryName);
		if (repositoryID == null)
			throw new IllegalArgumentException("Unknown repositoryName (neither a known ID nor a known alias): " + repositoryName);

		return repositoryID;
	}

	public URL getLocalRootURLForRepositoryNameOrFail(String repositoryName) {
		try {
			return getLocalRootForRepositoryNameOrFail(repositoryName).toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized URL getLocalRootURLForRepositoryName(String repositoryName) {
		File localRoot = getLocalRootForRepositoryName(repositoryName);
		if (localRoot == null)
			return null;

		try {
			return localRoot.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public File getLocalRootForRepositoryNameOrFail(String repositoryName) {
		File localRoot = getLocalRootForRepositoryName(repositoryName);
		if (localRoot == null)
			throw new IllegalArgumentException("Unknown repositoryName (neither a known repositoryAlias, nor a known repositoryID): " + repositoryName);

		return localRoot;
	}

	/**
	 * Get the local root for the given {@code repositoryName}.
	 * @param repositoryName the String-representation of the repositoryID or
	 * a repositoryAlias. Must not be <code>null</code>.
	 * @return the repository's local root or <code>null</code>, if the given {@code repositoryName} is neither
	 * a repositoryID nor a repositoryAlias known to this registry.
	 */
	public synchronized File getLocalRootForRepositoryName(String repositoryName) {
		assertNotNull("repositoryName", repositoryName);

		// If the repositoryName is an alias, this should find the corresponding repositoryID.
		EntityID repositoryID = getRepositoryID(repositoryName);
		if (repositoryID == null)
			return null;

		return getLocalRoot(repositoryID);
	}

	public synchronized File getLocalRoot(EntityID repositoryID) {
		assertNotNull("repositoryID", repositoryID);
		loadRepoRegistryIfNeeded();
		String localRootString = repoRegistryProperties.getProperty(getPropertyKeyForID(repositoryID));
		if (localRootString == null)
			return null;

		File localRoot = new File(localRootString);
		return localRoot;
	}

	public File getLocalRootOrFail(EntityID repositoryID) {
		File localRoot = getLocalRoot(repositoryID);
		if (localRoot == null)
			throw new IllegalArgumentException("Unknown repositoryID: " + repositoryID);

		return localRoot;
	}

	public synchronized void putRepositoryAlias(String repositoryAlias, EntityID repositoryID) {
		assertNotNull("repositoryAlias", repositoryAlias);
		assertNotNull("repositoryID", repositoryID);

		if (repositoryAlias.isEmpty())
			throw new IllegalArgumentException("repositoryAlias must not be empty!");

		if ("ALL".equals(repositoryAlias))
			throw new IllegalArgumentException("repositoryAlias cannot be named 'ALL'! This is a reserved key word.");

		if (repositoryAlias.startsWith("_"))
			throw new IllegalArgumentException("repositoryAlias must not start with '_': " + repositoryAlias);

		if (repositoryAlias.indexOf('/') >= 0)
			throw new IllegalArgumentException("repositoryAlias must not contain a '/': " + repositoryAlias);

		LockFile lockFile = LockFileFactory.getInstance().acquire(getRegistryFile(), LOCK_TIMEOUT_MS);
		try {
			loadRepoRegistryIfNeeded();
			getLocalRootOrFail(repositoryID); // make sure, this is a known repositoryID!
			String propertyKey = getPropertyKeyForAlias(repositoryAlias);
			String oldRepositoryIDString = repoRegistryProperties.getProperty(propertyKey);
			String repositoryIDString = repositoryID.toString();
			if (!repositoryIDString.equals(oldRepositoryIDString)) {
				repoRegistryProperties.setProperty(propertyKey, repositoryIDString);
				storeRepoRegistry();
			}
		} finally {
			lockFile.release();
		}
	}

	public synchronized void removeRepositoryAlias(String repositoryAlias) {
		assertNotNull("repositoryAlias", repositoryAlias);

		LockFile lockFile = LockFileFactory.getInstance().acquire(getRegistryFile(), LOCK_TIMEOUT_MS);
		try {
			loadRepoRegistryIfNeeded();
			String propertyKey = getPropertyKeyForAlias(repositoryAlias);
			String repositoryIDString = repoRegistryProperties.getProperty(propertyKey);
			if (repositoryIDString != null) {
				repoRegistryProperties.remove(propertyKey);
				storeRepoRegistry();
			}
		} finally {
			lockFile.release();
		}
	}

	public synchronized void putRepository(EntityID repositoryID, File localRoot) {
		assertNotNull("repositoryID", repositoryID);
		assertNotNull("localRoot", localRoot);

		if (!localRoot.isAbsolute())
			throw new IllegalArgumentException("localRoot is not absolute.");

		LockFile lockFile = LockFileFactory.getInstance().acquire(getRegistryFile(), LOCK_TIMEOUT_MS);
		try {
			loadRepoRegistryIfNeeded();
			String propertyKey = getPropertyKeyForID(repositoryID);
			String oldLocalRootPath = repoRegistryProperties.getProperty(propertyKey);
			String localRootPath = localRoot.getPath();
			if (!localRootPath.equals(oldLocalRootPath)) {
				repoRegistryProperties.setProperty(propertyKey, localRootPath);
				storeRepoRegistry();
			}
		} finally {
			lockFile.release();
		}
	}

	/**
	 * Get all aliases known for the specified repository.
	 * @param repositoryName the repository-ID or -alias. Must not be <code>null</code>.
	 * @return the known aliases. Never <code>null</code>, but maybe empty (if there are no aliases for this repository).
	 * @throws IllegalArgumentException if the repository with the given {@code repositoryName} does not exist,
	 * i.e. it's neither a repository-ID nor a repository-alias of a known repository.
	 */
	public synchronized Collection<String> getRepositoryAliasesOrFail(String repositoryName) throws IllegalArgumentException {
		LockFile lockFile = LockFileFactory.getInstance().acquire(getRegistryFile(), LOCK_TIMEOUT_MS);
		try {
			List<String> result = new ArrayList<String>();
			EntityID repositoryID = getRepositoryIDOrFail(repositoryName);
			for (Entry<Object, Object> me : repoRegistryProperties.entrySet()) {
				String key = String.valueOf(me.getKey());
				if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ALIAS)) {
					String value = String.valueOf(me.getValue());
					EntityID mappedRepositoryID = new EntityID(value);
					if (mappedRepositoryID.equals(repositoryID))
						result.add(key.substring(PROP_KEY_PREFIX_REPOSITORY_ALIAS.length()));
				}
			}
			Collections.sort(result);
			return Collections.unmodifiableList(result);
		} finally {
			lockFile.release();
		}
	}


	private String getPropertyKeyForAlias(String repositoryAlias) {
		return PROP_KEY_PREFIX_REPOSITORY_ALIAS + assertNotNull("repositoryAlias", repositoryAlias);
	}

	private String getPropertyKeyForID(EntityID repositoryID) {
		return PROP_KEY_PREFIX_REPOSITORY_ID + assertNotNull("repositoryID", repositoryID).toString();
	}

	private void loadRepoRegistryIfNeeded() {
		if (repoRegistryProperties == null || repoRegistryFileLastModified != getRegistryFile().lastModified())
			loadRepoRegistry();
	}

	private void loadRepoRegistry() {
		try {
			File registryFile = getRegistryFile();
			if (registryFile.exists() && registryFile.length() > 0)
				repoRegistryProperties = PropertiesUtil.load(registryFile);
			else
				repoRegistryProperties = new Properties();

			repoRegistryFileLastModified = registryFile.lastModified();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		evictDeadEntriesPeriodically();
	}

	private void storeRepoRegistry() {
		if (repoRegistryProperties == null)
			throw new IllegalStateException("repoRegistryProperties not loaded, yet!");

		try {
			File registryFile = getRegistryFile();
			PropertiesUtil.store(registryFile, repoRegistryProperties, null);
			repoRegistryFileLastModified = registryFile.lastModified();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void evictDeadEntriesPeriodically() {
		// TODO implement this: We must periodically check which entries point to non-existing directories and remove them.

	}
}