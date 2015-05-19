package co.codewizards.cloudstore.core.repo.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.collection.LazyUnmodifiableList;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

public class LocalRepoRegistryImpl implements LocalRepoRegistry
{
	private static final Logger logger = LoggerFactory.getLogger(LocalRepoRegistry.class);

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private static final String PROP_KEY_PREFIX_REPOSITORY_ID = "repositoryId:";
	private static final String PROP_KEY_PREFIX_REPOSITORY_ALIAS = "repositoryAlias:";
	private static final String PROP_EVICT_DEAD_ENTRIES_LAST_TIMESTAMP = "evictDeadEntriesLastTimestamp";
	/**
	 * @deprecated Replaced by {@link #CONFIG_KEY_EVICT_DEAD_ENTRIES_PERIOD}.
	 */
	@Deprecated
	private static final String PROP_EVICT_DEAD_ENTRIES_PERIOD = "evictDeadEntriesPeriod";
	private static final long LOCK_TIMEOUT_MS = 10000L; // 10 s

	private File registryFile;
	private long repoRegistryFileLastModified;
	private Properties repoRegistryProperties;
	private boolean repoRegistryPropertiesDirty;

	private static class LocalRepoRegistryHolder {
		public static final LocalRepoRegistry INSTANCE = new LocalRepoRegistryImpl();
	}

	public static LocalRepoRegistry getInstance() {
		return LocalRepoRegistryHolder.INSTANCE;
	}

	private LocalRepoRegistryImpl() { }

	private File getRegistryFile() {
		if (registryFile == null) {
			final File old = createFile(ConfigDir.getInstance().getFile(), "repositoryList.properties"); // old name until 0.9.0
			registryFile = createFile(ConfigDir.getInstance().getFile(), LOCAL_REPO_REGISTRY_FILE);
			if (old.exists() && !registryFile.exists())
				old.renameTo(registryFile);
		}
		return registryFile;
	}

	@Override
	public synchronized Collection<UUID> getRepositoryIds() {
		loadRepoRegistryIfNeeded();
		final List<UUID> result = new ArrayList<UUID>();
		for (final Entry<Object, Object> me : repoRegistryProperties.entrySet()) {
			final String key = String.valueOf(me.getKey());
			if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ID)) {
				final UUID repositoryId = UUID.fromString(key.substring(PROP_KEY_PREFIX_REPOSITORY_ID.length()));
				result.add(repositoryId);
			}
		}
		Collections.sort(result); // guarantee a stable order to prevent Heisenbugs
		return Collections.unmodifiableList(result);
	}

	@Override
	public synchronized UUID getRepositoryId(final String repositoryName) {
		AssertUtil.assertNotNull("repositoryName", repositoryName);
		loadRepoRegistryIfNeeded();
		final String repositoryIdString = repoRegistryProperties.getProperty(getPropertyKeyForAlias(repositoryName));
		if (repositoryIdString != null) {
			final UUID repositoryId = UUID.fromString(repositoryIdString);
			return repositoryId;
		}

		UUID repositoryId;
		try {
			repositoryId = UUID.fromString(repositoryName);
		} catch (final IllegalArgumentException x) {
			return null;
		}

		final String localRootString = repoRegistryProperties.getProperty(getPropertyKeyForID(repositoryId));
		if (localRootString == null)
			return null;

		return repositoryId;
	}

	@Override
	public UUID getRepositoryIdOrFail(final String repositoryName) {
		final UUID repositoryId = getRepositoryId(repositoryName);
		if (repositoryId == null)
			throw new IllegalArgumentException("Unknown repositoryName (neither a known ID nor a known alias): " + repositoryName);

		return repositoryId;
	}

	@Override
	public URL getLocalRootURLForRepositoryNameOrFail(final String repositoryName) {
		try {
			return getLocalRootForRepositoryNameOrFail(repositoryName).toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized URL getLocalRootURLForRepositoryName(final String repositoryName) {
		final File localRoot = getLocalRootForRepositoryName(repositoryName);
		if (localRoot == null)
			return null;

		try {
			return localRoot.toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public File getLocalRootForRepositoryNameOrFail(final String repositoryName) {
		final File localRoot = getLocalRootForRepositoryName(repositoryName);
		if (localRoot == null)
			throw new IllegalArgumentException("Unknown repositoryName (neither a known repositoryAlias, nor a known repositoryId): " + repositoryName);

		return localRoot;
	}

	@Override
	public synchronized File getLocalRootForRepositoryName(final String repositoryName) {
		AssertUtil.assertNotNull("repositoryName", repositoryName);

		// If the repositoryName is an alias, this should find the corresponding repositoryId.
		final UUID repositoryId = getRepositoryId(repositoryName);
		if (repositoryId == null)
			return null;

		return getLocalRoot(repositoryId);
	}

	@Override
	public synchronized File getLocalRoot(final UUID repositoryId) {
		AssertUtil.assertNotNull("repositoryId", repositoryId);
		loadRepoRegistryIfNeeded();
		final String localRootString = repoRegistryProperties.getProperty(getPropertyKeyForID(repositoryId));
		if (localRootString == null)
			return null;

		final File localRoot = createFile(localRootString);
		return localRoot;
	}

	@Override
	public File getLocalRootOrFail(final UUID repositoryId) {
		final File localRoot = getLocalRoot(repositoryId);
		if (localRoot == null)
			throw new IllegalArgumentException("Unknown repositoryId: " + repositoryId);

		return localRoot;
	}

	@Override
	public synchronized void putRepositoryAlias(final String repositoryAlias, final UUID repositoryId) {
		AssertUtil.assertNotNull("repositoryAlias", repositoryAlias);
		AssertUtil.assertNotNull("repositoryId", repositoryId);

		if (repositoryAlias.isEmpty())
			throw new IllegalArgumentException("repositoryAlias must not be empty!");

		if ("ALL".equals(repositoryAlias))
			throw new IllegalArgumentException("repositoryAlias cannot be named 'ALL'! This is a reserved key word.");

		if (repositoryAlias.startsWith("_"))
			throw new IllegalArgumentException("repositoryAlias must not start with '_': " + repositoryAlias);

		if (repositoryAlias.indexOf('/') >= 0)
			throw new IllegalArgumentException("repositoryAlias must not contain a '/': " + repositoryAlias);

		boolean modified = false;
		try ( final LockFile lockFile = acquireLockFile(); ) {
			loadRepoRegistryIfNeeded();
			getLocalRootOrFail(repositoryId); // make sure, this is a known repositoryId!
			final String propertyKey = getPropertyKeyForAlias(repositoryAlias);
			final String oldRepositoryIdString = repoRegistryProperties.getProperty(propertyKey);
			final String repositoryIdString = repositoryId.toString();
			if (!repositoryIdString.equals(oldRepositoryIdString)) {
				modified = true;
				setProperty(propertyKey, repositoryIdString);
			}
			storeRepoRegistryIfDirty();
		}
		if (modified)
			fireRepositoryAliasesChanged();
	}

	@Override
	public synchronized Collection<String> getRepositoryAliases() {
		final Set<String> result= new LinkedHashSet<>();
		final Collection<UUID> repositoryIds = getRepositoryIds();
		for (final UUID repositoryId : repositoryIds) {
			final Collection<String> repositoryAliases = getRepositoryAliasesOrFail(repositoryId.toString());
			result.addAll(repositoryAliases);
		}
		return result;
	}

	@Override
	public synchronized void removeRepositoryAlias(final String repositoryAlias) {
		AssertUtil.assertNotNull("repositoryAlias", repositoryAlias);
		boolean modified = false;
		try ( LockFile lockFile = acquireLockFile(); ) {
			loadRepoRegistryIfNeeded();
			final String propertyKey = getPropertyKeyForAlias(repositoryAlias);
			final String repositoryIdString = repoRegistryProperties.getProperty(propertyKey);
			if (repositoryIdString != null) {
				modified = true;
				removeProperty(propertyKey);
			}
			storeRepoRegistryIfDirty();
		}
		if (modified)
			fireRepositoryAliasesChanged();
	}

	@Override
	public synchronized void putRepository(final UUID repositoryId, final File localRoot) {
		AssertUtil.assertNotNull("repositoryId", repositoryId);
		AssertUtil.assertNotNull("localRoot", localRoot);

		if (!localRoot.isAbsolute())
			throw new IllegalArgumentException("localRoot is not absolute.");

		boolean modified = false;
		try ( final LockFile lockFile = acquireLockFile(); ) {
			loadRepoRegistryIfNeeded();
			final String propertyKey = getPropertyKeyForID(repositoryId);
			final String oldLocalRootPath = repoRegistryProperties.getProperty(propertyKey);
			final String localRootPath = localRoot.getPath();
			if (!localRootPath.equals(oldLocalRootPath)) {
				modified = true;
				setProperty(propertyKey, localRootPath);
			}
			storeRepoRegistryIfDirty();
		}
		if (modified)
			fireRepositoryIdsChanged();
	}

	protected Date getPropertyAsDate(final String key) {
		final String value = getProperty(key);
		if (value == null || value.trim().isEmpty())
			return null;

		return new DateTime(value).toDate();
	}

	private void setProperty(final String key, final Date value) {
		setProperty(key, new DateTime(AssertUtil.assertNotNull("value", value)).toString());
	}

	private String getProperty(final String key) {
		return repoRegistryProperties.getProperty(AssertUtil.assertNotNull("key", key));
	}

	private void setProperty(final String key, final String value) {
		repoRegistryPropertiesDirty = true;
		repoRegistryProperties.setProperty(AssertUtil.assertNotNull("key", key), AssertUtil.assertNotNull("value", value));
	}

	private void removeProperty(final String key) {
		repoRegistryPropertiesDirty = true;
		repoRegistryProperties.remove(AssertUtil.assertNotNull("key", key));
	}

	@Override
	public synchronized Collection<String> getRepositoryAliasesOrFail(final String repositoryName) throws IllegalArgumentException {
		return getRepositoryAliases(repositoryName, true);
	}

	@Override
	public synchronized Collection<String> getRepositoryAliases(final String repositoryName) {
		return getRepositoryAliases(repositoryName, false);
	}

	private Collection<String> getRepositoryAliases(final String repositoryName, final boolean fail) throws IllegalArgumentException {
		try ( final LockFile lockFile = acquireLockFile(); ) {
			final UUID repositoryId = fail ? getRepositoryIdOrFail(repositoryName) : getRepositoryId(repositoryName);
			if (repositoryId == null)
				return null;

			final List<String> result = new ArrayList<String>();
			for (final Entry<Object, Object> me : repoRegistryProperties.entrySet()) {
				final String key = String.valueOf(me.getKey());
				if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ALIAS)) {
					final String value = String.valueOf(me.getValue());
					final UUID mappedRepositoryId = UUID.fromString(value);
					if (mappedRepositoryId.equals(repositoryId))
						result.add(key.substring(PROP_KEY_PREFIX_REPOSITORY_ALIAS.length()));
				}
			}
			Collections.sort(result);
			return Collections.unmodifiableList(result);
		}
	}

	private String getPropertyKeyForAlias(final String repositoryAlias) {
		return PROP_KEY_PREFIX_REPOSITORY_ALIAS + AssertUtil.assertNotNull("repositoryAlias", repositoryAlias);
	}

	private String getPropertyKeyForID(final UUID repositoryId) {
		return PROP_KEY_PREFIX_REPOSITORY_ID + AssertUtil.assertNotNull("repositoryId", repositoryId).toString();
	}

	private void loadRepoRegistryIfNeeded() {
		boolean modified = false;
		try ( final LockFile lockFile = acquireLockFile(); ) {
			if (repoRegistryProperties == null || repoRegistryFileLastModified != getRegistryFile().lastModified()) {
				loadRepoRegistry();
				modified = true;
			}

			evictDeadEntriesPeriodically();
		}

		if (modified || repoRegistryPropertiesDirty) {
			// We don't know what exactly changed => fire all events ;-)
			fireRepositoryIdsChanged();
			fireRepositoryAliasesChanged();
		}
	}

	private void fireRepositoryIdsChanged() {
		firePropertyChange(PropertyEnum.repositoryIds, null, new LazyUnmodifiableList<UUID>() {
			@Override
			protected Collection<UUID> loadElements() {
				return getRepositoryIds();
			}
		});
	}

	private void fireRepositoryAliasesChanged() {
		firePropertyChange(PropertyEnum.repositoryAliases, null, new LazyUnmodifiableList<String>() {
			@Override
			protected java.util.Collection<String> loadElements() {
				return getRepositoryAliases();
			}
		});
	}

	private LockFile acquireLockFile() {
		return LockFileFactory.getInstance().acquire(getRegistryFile(), LOCK_TIMEOUT_MS);
	}

	private void loadRepoRegistry() {
		try {
			final File registryFile = getRegistryFile();
			if (registryFile.exists() && registryFile.length() > 0) {
				final Properties properties = new Properties();
				try ( final LockFile lockFile = acquireLockFile(); ) {
					final InputStream in = lockFile.createInputStream();
					try {
						properties.load(in);
					} finally {
						in.close();
					}
				}
				repoRegistryProperties = properties;
			}
			else
				repoRegistryProperties = new Properties();

			repoRegistryFileLastModified = registryFile.lastModified();
			repoRegistryPropertiesDirty = false;
		} catch (final IOException e) {
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
			final File registryFile = getRegistryFile();
			try ( final LockFile lockFile = acquireLockFile(); ) {
				final OutputStream out = lockFile.createOutputStream();
				try {
					repoRegistryProperties.store(out, null);
				} finally {
					out.close();
				}
			}
			repoRegistryFileLastModified = registryFile.lastModified();
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Checks, which entries point to non-existing directories or directories which are not (anymore) repositories
	 * and removes them.
	 */
	private void evictDeadEntriesPeriodically() {
		final Long period = Config.getInstance().getPropertyAsLong(CONFIG_KEY_EVICT_DEAD_ENTRIES_PERIOD, DEFAULT_EVICT_DEAD_ENTRIES_PERIOD);
		removeProperty(PROP_EVICT_DEAD_ENTRIES_PERIOD);
		final Date last = getPropertyAsDate(PROP_EVICT_DEAD_ENTRIES_LAST_TIMESTAMP);
		if (last != null) {
			final long millisAfterLast = System.currentTimeMillis() - last.getTime();
			if (millisAfterLast >= 0 && millisAfterLast <= period) // < 0 : travelled back in time
				return;
		}
		evictDeadEntries();
		setProperty(PROP_EVICT_DEAD_ENTRIES_LAST_TIMESTAMP, new Date());
	}


	private void evictDeadEntries() {
		for (final Entry<Object, Object> me : new ArrayList<Entry<Object, Object>>(repoRegistryProperties.entrySet())) {
			final String key = String.valueOf(me.getKey());
			final String value = String.valueOf(me.getValue());
			UUID repositoryIdFromRegistry;
			if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ALIAS)) {
				repositoryIdFromRegistry = UUID.fromString(value);
			} else if (key.startsWith(PROP_KEY_PREFIX_REPOSITORY_ID)) {
				repositoryIdFromRegistry = UUID.fromString(key.substring(PROP_KEY_PREFIX_REPOSITORY_ID.length()));
			} else
				continue;

			final String localRootString = repoRegistryProperties.getProperty(getPropertyKeyForID(repositoryIdFromRegistry));
			if (localRootString == null) {
				evictDeadEntry(key);
				continue;
			}

			final File localRoot = createFile(localRootString);
			if (!localRoot.isDirectory()) {
				evictDeadEntry(key);
				continue;
			}

			final File repoMetaDir = createFile(localRoot, LocalRepoManager.META_DIR_NAME);
			if (!repoMetaDir.isDirectory()) {
				evictDeadEntry(key);
				continue;
			}

			final File repositoryPropertiesFile = createFile(repoMetaDir, LocalRepoManager.REPOSITORY_PROPERTIES_FILE_NAME);
			if (!repositoryPropertiesFile.exists()) {
				logger.warn("evictDeadEntries: File does not exist (repo corrupt?!): {}", repositoryPropertiesFile);
				continue;
			}

			Properties repositoryProperties;
			try {
				repositoryProperties = PropertiesUtil.load(repositoryPropertiesFile);
			} catch (final IOException e) {
				logger.warn("evictDeadEntries: Could not read file (repo corrupt?!): {}", repositoryPropertiesFile);
				logger.warn("evictDeadEntries: " + e, e);
				continue;
			}

			final String repositoryIdFromRepo = repositoryProperties.getProperty(LocalRepoManager.PROP_REPOSITORY_ID);
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

	private void evictDeadEntry(final String key) {
		repoRegistryPropertiesDirty = true;
		final Object value = repoRegistryProperties.remove(key);
		logger.info("evictDeadEntry: key='{}' value='{}'", key, value);
	}


	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}
}