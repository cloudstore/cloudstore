package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.EntityID;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileRegistry;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class LocalRepoRegistry
{
	public static final String LOCAL_REPO_REGISTRY_FILE = "repositoryList.properties";
	private static final String PROP_KEY_PREFIX_REPOSITORY_ID = "repositoryID:";
	private static final String PROP_KEY_PREFIX_REPOSITORY_ALIAS = "repositoryAlias:";
	private static final String LOCK_FILE_NAME = LOCAL_REPO_REGISTRY_FILE + ".lock";
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

	private File getLockFile() {
		return new File(ConfigDir.getInstance().getFile(), LOCK_FILE_NAME);
	}

	private long repoRegistryFileLastModified;
	private Properties repoRegistryProperties;

	public synchronized EntityID getRepositoryID(String repositoryAlias) {
		assertNotNull("repositoryAlias", repositoryAlias);
		loadRepoRegistryIfNeeded();
		String entityIDString = repoRegistryProperties.getProperty(getPropertyKey(repositoryAlias));
		if (entityIDString == null)
			return null;

		EntityID entityID = new EntityID(entityIDString);
		return entityID;
	}

	public EntityID getRepositoryIDOrFail(String repositoryAlias) {
		EntityID repositoryID = getRepositoryID(repositoryAlias);
		if (repositoryID == null)
			throw new IllegalArgumentException("Unknown repositoryAlias: " + repositoryAlias);

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
		if (repositoryID == null) {
			// If it is not an alias, we try to parse it into an EntityID.
			try {
				repositoryID = new EntityID(repositoryName);
			} catch (IllegalArgumentException x) {
				// If it cannot be parsed into an entityID, it is an unknown alias.
				return null;
			}
		}
		return getLocalRoot(repositoryID);
	}

	public synchronized File getLocalRoot(EntityID repositoryID) {
		assertNotNull("repositoryID", repositoryID);
		loadRepoRegistryIfNeeded();
		String localRootString = repoRegistryProperties.getProperty(getPropertyKey(repositoryID));
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

	public synchronized void registerRepositoryAlias(String repositoryAlias, EntityID repositoryID) {
		assertNotNull("repositoryAlias", repositoryAlias);
		assertNotNull("repositoryID", repositoryID);

		if (repositoryAlias.startsWith("_"))
			throw new IllegalArgumentException("repositoryAlias must not start with '_': " + repositoryAlias);

		if (repositoryAlias.indexOf('/') >= 0)
			throw new IllegalArgumentException("repositoryAlias must not contain a '/': " + repositoryAlias);

		LockFile lockFile = LockFileRegistry.getInstance().acquire(getLockFile(), LOCK_TIMEOUT_MS);
		try {
			loadRepoRegistryIfNeeded();
			getLocalRootOrFail(repositoryID); // make sure, this is a known repositoryID!
			String propertyKey = getPropertyKey(repositoryAlias);
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

	public synchronized void registerRepository(EntityID repositoryID, File localRoot) {
		assertNotNull("repositoryID", repositoryID);
		assertNotNull("localRoot", localRoot);

		if (!localRoot.isAbsolute())
			throw new IllegalArgumentException("localRoot is not absolute.");

		LockFile lockFile = LockFileRegistry.getInstance().acquire(getLockFile(), LOCK_TIMEOUT_MS);
		try {
			loadRepoRegistryIfNeeded();
			String propertyKey = getPropertyKey(repositoryID);
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

	private String getPropertyKey(String repositoryAlias) {
		return PROP_KEY_PREFIX_REPOSITORY_ALIAS + assertNotNull("repositoryAlias", repositoryAlias);
	}

	private String getPropertyKey(EntityID repositoryID) {
		return PROP_KEY_PREFIX_REPOSITORY_ID + assertNotNull("repositoryID", repositoryID).toString();
	}

	private void loadRepoRegistryIfNeeded() {
		if (repoRegistryProperties == null || repoRegistryFileLastModified != getRegistryFile().lastModified())
			loadRepoRegistry();
	}

	private void loadRepoRegistry() {
		try {
			File registryFile = getRegistryFile();
			if (registryFile.exists())
				repoRegistryProperties = PropertiesUtil.load(registryFile);
			else
				repoRegistryProperties = new Properties();

			repoRegistryFileLastModified = registryFile.lastModified();
		} catch (IOException e) {
			throw new IllegalStateException(e);
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
}