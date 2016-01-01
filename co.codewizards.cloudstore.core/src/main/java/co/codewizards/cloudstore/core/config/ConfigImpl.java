package co.codewizards.cloudstore.core.config;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.PropertiesUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.appid.AppIdRegistry;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoHelper;
import co.codewizards.cloudstore.core.util.AssertUtil;

/**
 * Configuration of CloudStore supporting inheritance of settings.
 * <p>
 * See {@link Config}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ConfigImpl implements Config {
	private static final Logger logger = LoggerFactory.getLogger(ConfigImpl.class);

	private static final long fileRefsCleanPeriod = 60000L;
	private static long fileRefsCleanLastTimestamp;

	private static final String PROPERTIES_FILE_NAME_FOR_DIRECTORY_LOCAL = '.' + APP_ID_SIMPLE_ID + ".local.properties";

	private static final String PROPERTIES_FILE_NAME_FOR_DIRECTORY_HIDDEN = '.' + APP_ID_SIMPLE_ID + ".properties";
	/**
	 * @deprecated We should only support one of these files - this is unnecessary!
	 */
	@Deprecated
	private static final String PROPERTIES_FILE_NAME_FOR_DIRECTORY_VISIBLE = APP_ID_SIMPLE_ID + ".properties";

	private static final String PROPERTIES_TEMPLATE_FILE_NAME = "cloudstore.properties"; // *NOT* dependent on AppId!

	private static final String PROPERTIES_FILE_FORMAT_FOR_FILE_HIDDEN = ".%s." + APP_ID_SIMPLE_ID + ".properties";

	/**
	 * @deprecated We should only support one of these files - this is unnecessary!
	 */
	@Deprecated
	private static final String PROPERTIES_FILE_FORMAT_FOR_FILE_VISIBLE = "%s." + APP_ID_SIMPLE_ID + ".properties";

	private static final String TRUE_STRING = Boolean.TRUE.toString();
	private static final String FALSE_STRING = Boolean.FALSE.toString();

	private static final LinkedHashSet<File> fileHardRefs = new LinkedHashSet<>();
	private static final int fileHardRefsMaxSize = 30;
	/**
	 * {@link SoftReference}s to the files used in {@link #file2Config}.
	 * <p>
	 * There is no {@code SoftHashMap}, hence we use a WeakHashMap combined with the {@code SoftReference}s here.
	 * @see #file2Config
	 */
	private static final LinkedList<SoftReference<File>> fileSoftRefs = new LinkedList<>();
	/**
	 * @see #fileSoftRefs
	 */
	private static final Map<File, ConfigImpl> file2Config = new WeakHashMap<File, ConfigImpl>();

	private static final class ConfigHolder {
		public static final ConfigImpl instance = new ConfigImpl(
				null, null,
				new File[] { createFile(ConfigDir.getInstance().getFile(), PROPERTIES_FILE_NAME_FOR_DIRECTORY_VISIBLE) });
	}

	private final ConfigImpl parentConfig;
	private final WeakReference<File> fileRef;
	protected final File[] propertiesFiles;
	private final long[] propertiesFilesLastModified;
	private final Properties properties;

	private static final Object classMutex = ConfigImpl.class;
	private final Object instanceMutex;

	protected ConfigImpl(final ConfigImpl parentConfig, final File file, final File [] propertiesFiles) {
		this.parentConfig = parentConfig;

		if (parentConfig == null)
			fileRef = null;
		else
			fileRef = new WeakReference<File>(AssertUtil.assertNotNull("file", file));

		this.propertiesFiles = AssertUtil.assertNotNullAndNoNullElement("propertiesFiles", propertiesFiles);
		properties = new Properties(parentConfig == null ? null : parentConfig.properties);
		propertiesFilesLastModified = new long[propertiesFiles.length];
		instanceMutex = properties;

		// Create the default global configuration (it's an empty template with some comments).
		if (parentConfig == null && !propertiesFiles[0].exists()) {
			try {
				AppIdRegistry.getInstance().copyResourceResolvingAppId(
						ConfigImpl.class, "/" + PROPERTIES_TEMPLATE_FILE_NAME, propertiesFiles[0]);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Get the directory or file for which this Config instance is responsible.
	 * @return the directory or file for which this Config instance is responsible. Might be <code>null</code>, if already
	 * garbage-collected or if this is the root-parent-Config. We try to make garbage-collection extremely unlikely
	 * as long as the Config is held in memory.
	 */
	protected File getFile() {
		return fileRef == null ? null : fileRef.get();
	}

	private static void cleanFileRefs() {
		synchronized (classMutex) {
			if (System.currentTimeMillis() - fileRefsCleanLastTimestamp < fileRefsCleanPeriod)
				return;

			for (final Iterator<SoftReference<File>> it = fileSoftRefs.iterator(); it.hasNext(); ) {
				final SoftReference<File> fileRef = it.next();
				if (fileRef.get() == null)
					it.remove();
			}
			fileRefsCleanLastTimestamp = System.currentTimeMillis();
		}
	}

	/**
	 * Gets the global {@code Config} for the current user.
	 * @return the global {@code Config} for the current user. Never <code>null</code>.
	 */
	public static Config getInstance() {
		return ConfigHolder.instance;
	}

	/**
	 * Gets the {@code Config} for the given {@code directory}.
	 * @param directory a directory inside a repository. Must not be <code>null</code>.
	 * The directory does not need to exist (it may be created later).
	 * @return the {@code Config} for the given {@code directory}. Never <code>null</code>.
	 */
	public static Config getInstanceForDirectory(final File directory) {
		return getInstance(directory, true);
	}

	/**
	 * Gets the {@code Config} for the given {@code file}.
	 * @param file a file inside a repository. Must not be <code>null</code>.
	 * The file does not need to exist (it may be created later).
	 * @return the {@code Config} for the given {@code file}. Never <code>null</code>.
	 */
	public static Config getInstanceForFile(final File file) {
		return getInstance(file, false);
	}

	private static Config getInstance(final File file, final boolean isDirectory) {
		AssertUtil.assertNotNull("file", file);
		cleanFileRefs();

		File config_file = null;
		ConfigImpl config;
		synchronized (classMutex) {
			config = file2Config.get(file);
			if (config != null) {
				config_file = config.getFile();
				if (config_file == null) // very unlikely, but it actually *can* happen.
					config = null; // we try to make it extremely probable that the Config we return does have a valid file reference.
			}

			if (config == null) {
				final File localRoot = LocalRepoHelper.getLocalRootContainingFile(file);
				if (localRoot == null)
					throw new IllegalArgumentException("file is not inside a repository: " + file.getAbsolutePath());

				final ConfigImpl parentConfig = (ConfigImpl) (localRoot == file ? getInstance() : getInstance(file.getParentFile(), true));
				config = new ConfigImpl(parentConfig, file, createPropertiesFiles(file, isDirectory));
				file2Config.put(file, config);
				fileSoftRefs.add(new SoftReference<File>(file));
				config_file = config.getFile();
			}
			AssertUtil.assertNotNull("config_file", config_file);
		}
		refreshFileHardRefAndCleanOldHardRefs(config_file);
		return config;
	}

	private static File[] createPropertiesFiles(final File file, final boolean isDirectory) {
		if (isDirectory) {
			return new File[] {
				createFile(file, PROPERTIES_FILE_NAME_FOR_DIRECTORY_HIDDEN),
				createFile(file, PROPERTIES_FILE_NAME_FOR_DIRECTORY_VISIBLE),
				createFile(file, PROPERTIES_FILE_NAME_FOR_DIRECTORY_LOCAL) // overrides the settings of the shared file!
			};
		}
		else {
			return new File[] {
				createFile(file.getParentFile(), String.format(PROPERTIES_FILE_FORMAT_FOR_FILE_HIDDEN, file.getName())),
				createFile(file.getParentFile(), String.format(PROPERTIES_FILE_FORMAT_FOR_FILE_VISIBLE, file.getName()))
			};
		}
	}

	private ConfigImpl readIfNeeded() {
		synchronized (instanceMutex) {
			for (int i = 0; i < propertiesFiles.length; i++) {
				final File propertiesFile = propertiesFiles[i];
				final long lastModified = propertiesFilesLastModified[i];
				if (propertiesFile.lastModified() != lastModified) {
					read();
					break;
				}
			}
		}

		if (parentConfig != null)
			parentConfig.readIfNeeded();

		return this;
	}

	private void read() {
		synchronized (instanceMutex) {
			logger.trace("read: Entered instanceMutex.");
			try {
				properties.clear();
				for (int i = 0; i < propertiesFiles.length; i++) {
					final File propertiesFile = propertiesFiles[i];
					logger.debug("read: Reading propertiesFile '{}'.", propertiesFile.getAbsolutePath());
					final long lastModified = getLastModifiedAndWaitIfNeeded(propertiesFile);
					if (propertiesFile.exists()) { // prevent the properties file from being modified while we're reading it.
						try ( LockFile lockFile = LockFileFactory.getInstance().acquire(propertiesFile, 10000); ) { // TODO maybe system property for timeout?
							final InputStream in = lockFile.createInputStream();
							try {
								properties.load(in);
							} finally {
								in.close();
							}
						}
					}
					propertiesFilesLastModified[i] = lastModified;
				}
			} catch (final IOException e) {
				properties.clear();
				throw new RuntimeException(e);
			}
		}
	}

	private void write() {
		synchronized (instanceMutex) {
			logger.trace("read: Entered instanceMutex.");
			try {
				// TODO We should switch to another Properties implementation (our own?! didn't I write one, already? where do I have this code?!)
				// Using java.util.Properties causes the entries' order to be randomized and all comments in the file to be lost :-(

				// Which of the multiple files is used? We overwrite this, if it's only one.

				File propertiesFile = getSinglePropertiesFile();
				if (propertiesFile == null)
					propertiesFile = propertiesFiles[propertiesFiles.length - 1]; // the last one has the last word ;-)

				logger.debug("write: Writing propertiesFile '{}'.", propertiesFile.getAbsolutePath());
				try ( LockFile lockFile = LockFileFactory.getInstance().acquire(propertiesFile, 10000); ) { // TODO maybe system property for timeout?
					final OutputStream out = lockFile.createOutputStream();
					try {
						properties.store(out, null);
					} finally {
						out.close();
					}
				}

				// TODO should we set propertiesFilesLastModified[...] to prevent re-reading?! would be more efficient - but then, we rarely ever write anyway.
			} catch (final IOException e) {
				properties.clear();
				throw new RuntimeException(e);
			}
		}
	}

	private File getSinglePropertiesFile() {
		File result = null;
		for (final File propertiesFile : propertiesFiles) {
			if (propertiesFile.exists()) {
				if (result == null)
					result = propertiesFile;
				else
					return null; // multiple in use
			}
		}

//		if (result == null) // none in use, yet => choose the .* one (the first)
//			result = propertiesFiles[0]; // now using the local file by default (the last)

		return result;
	}

	/**
	 * Gets the {@link File#lastModified() lastModified} timestamp of the given {@code file}
	 * and waits if needed.
	 * <p>
	 * Waiting is needed, if the modification's age is shorter than the file system's time granularity.
	 * Since we do not know the file system's time granularity, we assume 2 seconds. Thus, if the file
	 * was changed e.g. 600 ms before invoking this method, the method will wait for 1400 ms to make sure
	 * the modification is at least as old as the assumed file system's temporal granularity.
	 * <p>
	 * This waiting strategy makes sure that a future modification of the file, after the file was read,
	 * is reliably detected - causing the file to be read again.
	 * @param file the file whose {@link File#lastModified() lastModified} timestamp to obtain. Must not be <code>null</code>.
	 * @return the {@link File#lastModified() lastModified} timestamp. 0, if the specified {@code file}
	 * does not exist.
	 */
	private long getLastModifiedAndWaitIfNeeded(final File file) {
		assertNotNull("file", file);
		long lastModified = file.lastModified(); // is 0 for non-existing file
		final long now = System.currentTimeMillis();

		// Check and handle timestamp in the future.
		if (lastModified > now) {
			file.setLastModified(now);
			logger.warn("getLastModifiedAndWaitIfNeeded: lastModified of '{}' was in the future! Changed it to now!", file.getAbsolutePath());

			lastModified = file.lastModified();
			if (lastModified > now) {
				logger.error("getLastModifiedAndWaitIfNeeded: lastModified of '{}' is in the future! Changing it FAILED! Permissions?!", file.getAbsolutePath());
				return lastModified;
			}
		}

		// Wait, if the modification is not yet older than the file system's (assumed!) granularity.
		// No file system should have a granularity worse than 2 seconds. Waiting max. 2 seconds in this use-case
		// in this rare situation is acceptable. After all, this is a config file which isn't changed often.
		final long fileSystemTemporalGranularity = 2000; // TODO maybe make this configurable?! Warning: we are in the config here - accessing the config is thus not so easy (=> recursion).
		final long modificationAge = now - lastModified;
		final long waitPeriod = fileSystemTemporalGranularity - modificationAge;
		if (waitPeriod > 0) {
			logger.info("getLastModifiedAndWaitIfNeeded: Waiting {} ms.", waitPeriod);
			try { Thread.sleep(waitPeriod); } catch (InterruptedException e) { }
		}

		return lastModified;
	}

	@Override
	public String getProperty(final String key, final String defaultValue) {
		assertNotNull("key", key);
		refreshFileHardRefAndCleanOldHardRefs();

		final String sysPropKey = SYSTEM_PROPERTY_PREFIX + key;
		final String sysPropVal = System.getProperty(sysPropKey);
		if (sysPropVal != null) {
			logger.debug("getProperty: System property with key='{}' and value='{}' overrides config (config is not queried).", sysPropKey, sysPropVal);
			return sysPropVal;
		}

		final String envVarKey = systemPropertyToEnvironmentVariable(sysPropKey);
		final String envVarVal = System.getenv(envVarKey);
		if (envVarVal != null) {
			logger.debug("getProperty: Environment variable with key='{}' and value='{}' overrides config (config is not queried).", envVarKey, envVarVal);
			return envVarVal;
		}

		logger.debug("getProperty: System property with key='{}' is not set (config is queried next).", sysPropKey);

		synchronized (instanceMutex) {
			readIfNeeded();
			return properties.getProperty(key, defaultValue);
		}
	}

	@Override
	public String getDirectProperty(final String key) {
		assertNotNull("key", key);

		// TODO should we really take system properties and environment variables into account?!

		final String sysPropKey = SYSTEM_PROPERTY_PREFIX + key;
		final String sysPropVal = System.getProperty(sysPropKey);
		if (sysPropVal != null) {
			logger.debug("getProperty: System property with key='{}' and value='{}' overrides config (config is not queried).", sysPropKey, sysPropVal);
			return sysPropVal;
		}

		final String envVarKey = systemPropertyToEnvironmentVariable(sysPropKey);
		final String envVarVal = System.getenv(envVarKey);
		if (envVarVal != null) {
			logger.debug("getProperty: Environment variable with key='{}' and value='{}' overrides config (config is not queried).", envVarKey, envVarVal);
			return envVarVal;
		}

		refreshFileHardRefAndCleanOldHardRefs();
		synchronized (instanceMutex) {
			readIfNeeded();
			return (String) properties.get(key);
		}
	}

	@Override
	public void setDirectProperty(final String key, final String value) {
		assertNotNull("key", key);

		// TODO really prevent modifying values? Or handle system props + env-vars differently?

		final String sysPropKey = SYSTEM_PROPERTY_PREFIX + key;
		if (System.getProperty(sysPropKey) != null) {
			throw new IllegalStateException(String.format(
					"System property with key='%s' overrides config. The property '%s' can therefore not be modified.", sysPropKey, key));
		}

		final String envVarKey = systemPropertyToEnvironmentVariable(sysPropKey);
		if (System.getenv(envVarKey) != null) {
			throw new IllegalStateException(String.format(
					"Environment variable with key='%s' overrides config. The property '%s' can therefore not be modified.", envVarKey, key));
		}

		refreshFileHardRefAndCleanOldHardRefs();
		synchronized (instanceMutex) {
			readIfNeeded();
			if (value == null)
				properties.remove(key);
			else
				properties.put(key, value);

			write();
		}
	}

	@Override
	public String getPropertyAsNonEmptyTrimmedString(final String key, final String defaultValue) {
		AssertUtil.assertNotNull("key", key);
		refreshFileHardRefAndCleanOldHardRefs();

		final String sysPropKey = SYSTEM_PROPERTY_PREFIX + key;
		final String sysPropVal = trim(System.getProperty(sysPropKey));
		if (! isEmpty(sysPropVal)) {
			logger.debug("getPropertyAsNonEmptyTrimmedString: System property with key='{}' and value='{}' overrides config (config is not queried).", sysPropKey, sysPropVal);
			return sysPropVal;
		}

		final String envVarKey = systemPropertyToEnvironmentVariable(sysPropKey);
		final String envVarVal = trim(System.getenv(envVarKey));
		if (! isEmpty(envVarVal)) {
			logger.debug("getPropertyAsNonEmptyTrimmedString: Environment variable with key='{}' and value='{}' overrides config (config is not queried).", envVarKey, envVarVal);
			return envVarVal;
		}

		logger.debug("getPropertyAsNonEmptyTrimmedString: System property with key='{}' is not set (config is queried next).", sysPropKey);

		synchronized (instanceMutex) {
			readIfNeeded();
			String sval = trim(properties.getProperty(key));
			if (isEmpty(sval))
				return defaultValue;

			return sval;
		}
	}

	@Override
	public long getPropertyAsLong(final String key, final long defaultValue) {
		final String sval = getPropertyAsNonEmptyTrimmedString(key, null);
		if (sval == null)
			return defaultValue;

		try {
			final long lval = Long.parseLong(sval);
			return lval;
		} catch (final NumberFormatException x) {
			logger.warn("getPropertyAsLong: One of the properties files %s contains the key '%s' (or the system properties override it) with the illegal value '%s'. Falling back to default value '%s'!", propertiesFiles, key, sval, defaultValue);
			return defaultValue;
		}
	}

	@Override
	public long getPropertyAsPositiveOrZeroLong(final String key, final long defaultValue) {
		final long value = getPropertyAsLong(key, defaultValue);
		if (value < 0) {
			logger.warn("getPropertyAsPositiveOrZeroLong: One of the properties files %s contains the key '%s' (or the system properties override it) with the negative value '%s' (only values >= 0 are allowed). Falling back to default value '%s'!", propertiesFiles, key, value, defaultValue);
			return defaultValue;
		}
		return value;
	}

	@Override
	public int getPropertyAsInt(final String key, final int defaultValue) {
		final String sval = getPropertyAsNonEmptyTrimmedString(key, null);
		if (sval == null)
			return defaultValue;

		try {
			final int ival = Integer.parseInt(sval);
			return ival;
		} catch (final NumberFormatException x) {
			logger.warn("getPropertyAsInt: One of the properties files %s contains the key '%s' (or the system properties override it) with the illegal value '%s'. Falling back to default value '%s'!", propertiesFiles, key, sval, defaultValue);
			return defaultValue;
		}
	}

	@Override
	public int getPropertyAsPositiveOrZeroInt(final String key, final int defaultValue) {
		final int value = getPropertyAsInt(key, defaultValue);
		if (value < 0) {
			logger.warn("getPropertyAsPositiveOrZeroInt: One of the properties files %s contains the key '%s' (or the system properties override it) with the negative value '%s' (only values >= 0 are allowed). Falling back to default value '%s'!", propertiesFiles, key, value, defaultValue);
			return defaultValue;
		}
		return value;
	}

	@Override
	public <E extends Enum<E>> E getPropertyAsEnum(final String key, final E defaultValue) {
		AssertUtil.assertNotNull("defaultValue", defaultValue);
		@SuppressWarnings("unchecked")
		final Class<E> enumClass = (Class<E>) defaultValue.getClass();
		return getPropertyAsEnum(key, enumClass, defaultValue);
	}

	@Override
	public <E extends Enum<E>> E getPropertyAsEnum(final String key, final Class<E> enumClass, final E defaultValue) {
		AssertUtil.assertNotNull("enumClass", enumClass);
		final String sval = getPropertyAsNonEmptyTrimmedString(key, null);
		if (sval == null)
			return defaultValue;

		try {
			return Enum.valueOf(enumClass, sval);
		} catch (final IllegalArgumentException x) {
			logger.warn("getPropertyAsEnum: One of the properties files %s contains the key '%s' with the illegal value '%s'. Falling back to default value '%s'!", propertiesFiles, key, sval, defaultValue);
			return defaultValue;
		}
	}

	@Override
	public boolean getPropertyAsBoolean(final String key, final boolean defaultValue) {
		final String sval = getPropertyAsNonEmptyTrimmedString(key, null);
		if (sval == null)
			return defaultValue;

		if (TRUE_STRING.equalsIgnoreCase(sval))
			return true;
		else if (FALSE_STRING.equalsIgnoreCase(sval))
			return false;
		else {
			logger.warn("getPropertyAsBoolean: One of the properties files %s contains the key '%s' with the illegal value '%s'. Falling back to default value '%s'!", propertiesFiles, key, sval, defaultValue);
			return defaultValue;
		}
	}

	private static final void refreshFileHardRefAndCleanOldHardRefs(final ConfigImpl config) {
		final File config_file = AssertUtil.assertNotNull("config", config).getFile();
		if (config_file != null)
			refreshFileHardRefAndCleanOldHardRefs(config_file);
	}

	private final void refreshFileHardRefAndCleanOldHardRefs() {
		if (parentConfig != null)
			parentConfig.refreshFileHardRefAndCleanOldHardRefs();

		refreshFileHardRefAndCleanOldHardRefs(this);
	}

	private static final void refreshFileHardRefAndCleanOldHardRefs(final File config_file) {
		AssertUtil.assertNotNull("config_file", config_file);
		synchronized (fileHardRefs) {
			// make sure the config_file is at the end of fileHardRefs
			fileHardRefs.remove(config_file);
			fileHardRefs.add(config_file);

			// remove the first entry until size does not exceed limit anymore.
			while (fileHardRefs.size() > fileHardRefsMaxSize)
				fileHardRefs.remove(fileHardRefs.iterator().next());
		}
	}
}
