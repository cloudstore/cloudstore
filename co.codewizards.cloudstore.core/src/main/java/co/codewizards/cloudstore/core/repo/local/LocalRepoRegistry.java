package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class LocalRepoRegistry
{
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoRegistry.class);

	public static final String LOCAL_REPO_REGISTRY_FILE = "repoRegistry.properties"; // new name since 0.9.1
	private static final String PROP_KEY_PREFIX_REPOSITORY_ID = "repositoryId:";
	private static final String PROP_KEY_PREFIX_REPOSITORY_ALIAS = "repositoryAlias:";
	private static final String PROP_EVICT_DEAD_ENTRIES_LAST_TIMESTAMP = "evictDeadEntriesLastTimestamp";
	private static final String PROP_EVICT_DEAD_ENTRIES_PERIOD = "evictDeadEntriesPeriod";
	private static final long LOCK_TIMEOUT_MS = 10000L; // 10 s

	private File registryFile;
	private long repoRegistryFileLastModified;
	private Properties repoRegistryProperties;
	private boolean repoRegistryPropertiesDirty;

	private static class LocalRepoRegistryHolder {
		public static final LocalRepoRegistry INSTANCE = new LocalRepoRegistry();
	}

	public static LocalRepoRegistry getInstance() {
		return LocalRepoRegistryHolder.INSTANCE;
	}

	private LocalRepoRegistry() { }

	private File getRegistryFile() {
		if (registryFile == null) {
			File old = new File(ConfigDir.getInstance().getFile(), "repositoryList.properties"); // old name until 0.9.0
			registryFile = new File(ConfigDir.getInstance().getFile(), LOCAL_REPO_REGISTRY_FILE);
			if (old.exists() && !registryFile.exists())
				old.renameTo(registryFile);
		}
		return registryFile;
	}

	public synchronized Collection<UUID> getRepositoryIds() {
		loadRepoRegistryIfNeeded();
		List<UUID> result = new ArrayList<UUID>();
		for (Entry<Object, Object> me : repoRegistryProperties.entrySet()) {
			String key = String.valueOf(me.getKey());
			if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ID)) {
				UUID repositoryId = UUID.fromString(key.substring(PROP_KEY_PREFIX_REPOSITORY_ID.length()));
				result.add(repositoryId);
			}
		}
		Collections.sort(result); // guarantee a stable order to prevent Heisenbugs
		return Collections.unmodifiableList(result);
	}

	public synchronized UUID getRepositoryId(String repositoryName) {
		assertNotNull("repositoryName", repositoryName);
		loadRepoRegistryIfNeeded();
		String repositoryIdString = repoRegistryProperties.getProperty(getPropertyKeyForAlias(repositoryName));
		if (repositoryIdString != null) {
			UUID repositoryId = UUID.fromString(repositoryIdString);
			return repositoryId;
		}

		UUID repositoryId;
		try {
			repositoryId = UUID.fromString(repositoryName);
		} catch (IllegalArgumentException x) {
			return null;
		}

		String localRootString = repoRegistryProperties.getProperty(getPropertyKeyForID(repositoryId));
		if (localRootString == null)
			return null;

		return repositoryId;
	}

	public UUID getRepositoryIdOrFail(String repositoryName) {
		UUID repositoryId = getRepositoryId(repositoryName);
		if (repositoryId == null)
			throw new IllegalArgumentException("Unknown repositoryName (neither a known ID nor a known alias): " + repositoryName);

		return repositoryId;
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
			throw new IllegalArgumentException("Unknown repositoryName (neither a known repositoryAlias, nor a known repositoryId): " + repositoryName);

		return localRoot;
	}

	/**
	 * Get the local root for the given {@code repositoryName}.
	 * @param repositoryName the String-representation of the repositoryId or
	 * a repositoryAlias. Must not be <code>null</code>.
	 * @return the repository's local root or <code>null</code>, if the given {@code repositoryName} is neither
	 * a repositoryId nor a repositoryAlias known to this registry.
	 */
	public synchronized File getLocalRootForRepositoryName(String repositoryName) {
		assertNotNull("repositoryName", repositoryName);

		// If the repositoryName is an alias, this should find the corresponding repositoryId.
		UUID repositoryId = getRepositoryId(repositoryName);
		if (repositoryId == null)
			return null;

		return getLocalRoot(repositoryId);
	}

	public synchronized File getLocalRoot(UUID repositoryId) {
		assertNotNull("repositoryId", repositoryId);
		loadRepoRegistryIfNeeded();
		String localRootString = repoRegistryProperties.getProperty(getPropertyKeyForID(repositoryId));
		if (localRootString == null)
			return null;

		File localRoot = new File(localRootString);
		return localRoot;
	}

	public File getLocalRootOrFail(UUID repositoryId) {
		File localRoot = getLocalRoot(repositoryId);
		if (localRoot == null)
			throw new IllegalArgumentException("Unknown repositoryId: " + repositoryId);

		return localRoot;
	}

	public synchronized void putRepositoryAlias(String repositoryAlias, UUID repositoryId) {
		assertNotNull("repositoryAlias", repositoryAlias);
		assertNotNull("repositoryId", repositoryId);

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
			getLocalRootOrFail(repositoryId); // make sure, this is a known repositoryId!
			String propertyKey = getPropertyKeyForAlias(repositoryAlias);
			String oldRepositoryIdString = repoRegistryProperties.getProperty(propertyKey);
			String repositoryIdString = repositoryId.toString();
			if (!repositoryIdString.equals(oldRepositoryIdString))
				setProperty(propertyKey, repositoryIdString);

			storeRepoRegistryIfDirty();
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
			String repositoryIdString = repoRegistryProperties.getProperty(propertyKey);
			if (repositoryIdString != null)
				removeProperty(propertyKey);

			storeRepoRegistryIfDirty();
		} finally {
			lockFile.release();
		}
	}

	public synchronized void putRepository(UUID repositoryId, File localRoot) {
		assertNotNull("repositoryId", repositoryId);
		assertNotNull("localRoot", localRoot);

		if (!localRoot.isAbsolute())
			throw new IllegalArgumentException("localRoot is not absolute.");

		LockFile lockFile = LockFileFactory.getInstance().acquire(getRegistryFile(), LOCK_TIMEOUT_MS);
		try {
			loadRepoRegistryIfNeeded();
			String propertyKey = getPropertyKeyForID(repositoryId);
			String oldLocalRootPath = repoRegistryProperties.getProperty(propertyKey);
			String localRootPath = localRoot.getPath();
			if (!localRootPath.equals(oldLocalRootPath))
				setProperty(propertyKey, localRootPath);

			storeRepoRegistryIfDirty();
		} finally {
			lockFile.release();
		}
	}

	private Date getPropertyAsDate(String key) {
		String value = getProperty(key);
		if (value == null || value.trim().isEmpty())
			return null;

		return new DateTime(value).toDate();
	}

	private void setProperty(String key, Date value) {
		setProperty(key, new DateTime(assertNotNull("value", value)).toString());
	}

	private Long getPropertyAsLong(String key) {
		String value = getProperty(key);
		if (value == null || value.trim().isEmpty())
			return null;

		return Long.valueOf(value);
	}

	private void setProperty(String key, long value) {
		setProperty(key, Long.toString(value));
	}

	private String getProperty(String key) {
		return repoRegistryProperties.getProperty(assertNotNull("key", key));
	}

	private void setProperty(String key, String value) {
		repoRegistryPropertiesDirty = true;
		repoRegistryProperties.setProperty(assertNotNull("key", key), assertNotNull("value", value));
	}


	private void removeProperty(String key) {
		repoRegistryPropertiesDirty = true;
		repoRegistryProperties.remove(assertNotNull("key", key));
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
			UUID repositoryId = getRepositoryIdOrFail(repositoryName);
			for (Entry<Object, Object> me : repoRegistryProperties.entrySet()) {
				String key = String.valueOf(me.getKey());
				if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ALIAS)) {
					String value = String.valueOf(me.getValue());
					UUID mappedRepositoryId = UUID.fromString(value);
					if (mappedRepositoryId.equals(repositoryId))
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

	private String getPropertyKeyForID(UUID repositoryId) {
		return PROP_KEY_PREFIX_REPOSITORY_ID + assertNotNull("repositoryId", repositoryId).toString();
	}

	private void loadRepoRegistryIfNeeded() {
		LockFile lockFile = LockFileFactory.getInstance().acquire(getRegistryFile(), LOCK_TIMEOUT_MS);
		try {
			if (repoRegistryProperties == null || repoRegistryFileLastModified != getRegistryFile().lastModified())
				loadRepoRegistry();

			evictDeadEntriesPeriodically();
		} finally {
			lockFile.release();
		}
	}

	private void loadRepoRegistry() {
		try {
			File registryFile = getRegistryFile();
			if (registryFile.exists() && registryFile.length() > 0)
				repoRegistryProperties = PropertiesUtil.load(registryFile);
			else
				repoRegistryProperties = new Properties();

			repoRegistryFileLastModified = registryFile.lastModified();
			repoRegistryPropertiesDirty = false;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void storeRepoRegistryIfDirty() {
		if (repoRegistryPropertiesDirty) {
			storeRepoRegistry();
			repoRegistryPropertiesDirty = false;
		}
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

	/**
	 * Checks, which entries point to non-existing directories or directories which are not (anymore) repositories
	 * and removes them.
	 */
	private void evictDeadEntriesPeriodically() {
		Long period = getPropertyAsLong(PROP_EVICT_DEAD_ENTRIES_PERIOD);
		if (period == null) {
			period = 24 * 60 * 60 * 1000L;
			setProperty(PROP_EVICT_DEAD_ENTRIES_PERIOD, period);
		}
		Date last = getPropertyAsDate(PROP_EVICT_DEAD_ENTRIES_LAST_TIMESTAMP);
		if (last != null) {
			long millisAfterLast = System.currentTimeMillis() - last.getTime();
			if (millisAfterLast >= 0 && millisAfterLast <= period) // < 0 : travelled back in time
				return;
		}
		evictDeadEntries();
		setProperty(PROP_EVICT_DEAD_ENTRIES_LAST_TIMESTAMP, new Date());
	}


	private void evictDeadEntries() {
		for (Entry<Object, Object> me : new ArrayList<Entry<Object, Object>>(repoRegistryProperties.entrySet())) {
			String key = String.valueOf(me.getKey());
			String value = String.valueOf(me.getValue());
			UUID repositoryIdFromRegistry;
			if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ALIAS)) {
				repositoryIdFromRegistry = UUID.fromString(value);
			} else if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ID)) {
				repositoryIdFromRegistry = UUID.fromString(key.substring(PROP_KEY_PREFIX_REPOSITORY_ID.length()));
			} else
				continue;

			String localRootString = repoRegistryProperties.getProperty(getPropertyKeyForID(repositoryIdFromRegistry));
			if (localRootString == null) {
				evictDeadEntry(key);
				continue;
			}

			File localRoot = new File(localRootString);
			if (!localRoot.isDirectory()) {
				evictDeadEntry(key);
				continue;
			}

			File repoMetaDir = new File(localRoot, LocalRepoManager.META_DIR_NAME);
			if (!repoMetaDir.isDirectory()) {
				evictDeadEntry(key);
				continue;
			}

			File repositoryPropertiesFile = new File(repoMetaDir, LocalRepoManager.REPOSITORY_PROPERTIES_FILE_NAME);
			if (!repositoryPropertiesFile.exists()) {
				logger.warn("evictDeadEntries: File does not exist (repo corrupt?!): {}", repositoryPropertiesFile);
				continue;
			}

			Properties repositoryProperties;
			try {
				repositoryProperties = PropertiesUtil.load(repositoryPropertiesFile);
			} catch (IOException e) {
				logger.warn("evictDeadEntries: Could not read file (repo corrupt?!): {}", repositoryPropertiesFile);
				logger.warn("evictDeadEntries: " + e, e);
				continue;
			}

			String repositoryIdFromRepo = repositoryProperties.getProperty(LocalRepoManager.PROP_REPOSITORY_ID);
			if (repositoryIdFromRepo == null) {
				logger.warn("evictDeadEntries: repositoryProperties '{}' do not contain key='{}'!", repositoryPropertiesFile, LocalRepoManager.PROP_REPOSITORY_ID);
				// Old repos don't have the repo-id in the properties, yet.
				// This is automatically added, when the LocalRepoManager is started up for this repo, the next time.
				// For now, we ignore it.
				continue;
			}

			if (!repositoryIdFromRegistry.toString().equals(repositoryIdFromRepo)) { // new repo was created at the same location
				evictDeadEntry(key);
				continue;
			}
		}
	}

	private void evictDeadEntry(String key) {
		repoRegistryPropertiesDirty = true;
		Object value = repoRegistryProperties.remove(key);
		logger.info("evictDeadEntry: key='{}' value='{}'", key, value);
	}
}