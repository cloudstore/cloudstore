package co.codewizards.cloudstore.core.updater;

import static co.codewizards.cloudstore.core.util.Util.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;

import java.io.BufferedReader;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;
import co.codewizards.cloudstore.oio.api.File;

public class CloudStoreUpdaterCore {
	private static final Logger logger = LoggerFactory.getLogger(CloudStoreUpdaterCore.class);

	public static final String PROTOCOL_FILE = "file";
	public static final String PROTOCOL_JAR = "jar";

	public static final String INSTALLATION_PROPERTIES_FILE_NAME = "installation.properties";
	public static final String INSTALLATION_PROPERTIES_ARTIFACT_ID = "artifactId";
	public static final String INSTALLATION_PROPERTIES_VERSION = "version";
	public static final String remoteVersionURL = "http://cloudstore.codewizards.co/update/${artifactId}/version";
	public static final String remoteUpdatePropertiesURL = "http://cloudstore.codewizards.co/update/${artifactId}/update.properties";

	/**
	 * Configuration property key controlling whether we do a downgrade. By default, only an upgrade is done. If this
	 * configuration property is set to <code>true</code> and the local version is newer than the version on the server
	 * a downgrade is done, too.
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 */
	public static final String CONFIG_KEY_DOWNGRADE = "updater.downgrade";

	/**
	 * Configuration property key controlling whether the updater is enabled.
	 * <p>
	 * If it is enabled, it {@linkplain #CONFIG_KEY_REMOTE_VERSION_CACHE_VALIDITY_PERIOD periodically checks} whether an update is
	 * available, and if so, performs the update. Note, that {@link #CONFIG_KEY_FORCE} (or its
	 * corresponding system property "cloudstore.updater.force") have no effect, if the updater is disabled!
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 */
	public static final String CONFIG_KEY_ENABLED = "updater.enabled";

	/**
	 * Configuration property key controlling whether to force the update. If this property is set, an update is
	 * done even if the versions locally and remotely are already the same.
	 * <p>
	 * This is only designed as configuration key for consistency reasons - usually, you likely don't want to write
	 * this into a configuration file! Instead, you probably want to pass this as a system property - see
	 * {@link Config#SYSTEM_PROPERTY_PREFIX} (and the example below).
	 * <p>
	 * Note, that forcing an update has no effect, if the updater is {@linkplain #CONFIG_KEY_ENABLED disabled}!
	 * Thus, if you want to force an update under all circumstances (whether the updater is enabled or not),
	 * you should pass both. As system properties, this looks as follows:
	 * <pre>-D<b>cloudstore.updater.force=true</b> -D<b>cloudstore.updater.enabled=true</b></pre>
	 */
	public static final String CONFIG_KEY_FORCE = "updater.force";

	public static final long DEFAULT_REMOTE_VERSION_CACHE_VALIDITY_PERIOD = 6 * 60 * 60 * 1000;

	/**
	 * Configuration property key controlling how long a queried remote version is cached (and thus how
	 * often the server is asked for it).
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 */
	public static final String CONFIG_KEY_REMOTE_VERSION_CACHE_VALIDITY_PERIOD = "updater.remoteVersionCache.validityPeriod";

	private Version localVersion;
	private Version remoteVersion;
	private Properties installationProperties;
	private File installationDir;
	private File updaterDir;
	private File backupDir;

	public CloudStoreUpdaterCore() { }

	protected Version getRemoteVersion() {
		Version remoteVersion = this.remoteVersion;
		if (remoteVersion == null) {
			final RemoteVersionCache remoteVersionCache = readRemoteVersionCacheFromProperties();
			final long cachePeriod = getRemoteVersionCacheValidityPeriod();
			if (remoteVersionCache != null && System.currentTimeMillis() - remoteVersionCache.remoteVersionTimestamp.getMillis() <= cachePeriod) {
				logger.debug("getRemoteVersion: Cached value '{}' is from {} and still valid (it expires {}). Using this value (not asking server).",
						remoteVersionCache.remoteVersion,
						remoteVersionCache.remoteVersionTimestamp.toDate(),
						new Date(remoteVersionCache.remoteVersionTimestamp.getMillis() + cachePeriod));
				this.remoteVersion = remoteVersion = remoteVersionCache.remoteVersion;
			}
			else {
				final String artifactId = getInstallationProperties().getProperty(INSTALLATION_PROPERTIES_ARTIFACT_ID);
				// cannot use resolve(...), because it invokes this method ;-)
				assertNotNull("artifactId", artifactId);
				final Map<String, Object> variables = new HashMap<>(1);
				variables.put("artifactId", artifactId);
				final String resolvedRemoteVersionURL = IOUtil.replaceTemplateVariables(remoteVersionURL, variables);
				try {
					final URL url = new URL(resolvedRemoteVersionURL);
					final InputStream in = url.openStream();
					try {
						final BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
						final String line = r.readLine();
						if (line == null || line.isEmpty())
							throw new IllegalStateException("Failed to read version from: " + resolvedRemoteVersionURL);

						final String trimmed = line.trim();
						if (trimmed.isEmpty())
							throw new IllegalStateException("Failed to read version from: " + resolvedRemoteVersionURL);

						this.remoteVersion = remoteVersion = new Version(trimmed);
						r.close();
					} finally {
						in.close();
					}
					writeRemoteVersionCacheToProperties(new RemoteVersionCache(remoteVersion, new DateTime(new Date())));
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return remoteVersion;
	}

	protected Version getLocalVersion() {
		if (localVersion == null) {
			final String value = getInstallationProperties().getProperty(INSTALLATION_PROPERTIES_VERSION);
			if (value == null || value.isEmpty())
				throw new IllegalStateException("Failed to read local version from installation-properties-file!");

			final String trimmed = value.trim();
			if (trimmed.isEmpty())
				throw new IllegalStateException("Failed to read local version from installation-properties-file!");

			localVersion = new Version(trimmed);
		}
		return localVersion;
	}

	protected Properties getInstallationProperties() {
		if (installationProperties == null) {
			final File installationPropertiesFile = newFile(getInstallationDir(), INSTALLATION_PROPERTIES_FILE_NAME);
			if (!installationPropertiesFile.exists())
				throw new IllegalArgumentException(String.format("installationPropertiesFile '%s' does not exist!", installationPropertiesFile.getAbsolutePath()));

			if (!installationPropertiesFile.isFile())
				throw new IllegalArgumentException(String.format("installationPropertiesFile '%s' is not a file!", installationPropertiesFile.getAbsolutePath()));

			try {
				final Properties properties = PropertiesUtil.load(installationPropertiesFile);
				installationProperties = properties;
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
		return installationProperties;
	}

	/**
	 * Resolves the given {@code template} by replacing all its variables with their actual values.
	 * <p>
	 * Variables are written as "${variable}" similarly to Ant and Maven. See
	 * {@link IOUtil#replaceTemplateVariables(String, Map)} for further details.
	 * <p>
	 * The variable values are obtained from the {@link #getInstallationProperties() installationProperties}.
	 * @param template the template to be resolved. Must not be <code>null</code>.
	 * @return
	 */
	protected String resolve(final String template) {
		assertNotNull("template", template);
		final String artifactId = getInstallationProperties().getProperty(INSTALLATION_PROPERTIES_ARTIFACT_ID);
		assertNotNull("artifactId", artifactId);

		final Version remoteVersion = getRemoteVersion();

		final Map<String, Object> variables = new HashMap<>(4);
		variables.put("artifactId", artifactId);
		variables.put("version", remoteVersion);
		variables.put("remoteVersion", remoteVersion);
		variables.put("localVersion", getLocalVersion());
		return IOUtil.replaceTemplateVariables(template, variables);
	}

	/**
	 * Gets the installation directory.
	 * <p>
	 * The implementation in {@link CloudStoreUpdaterCore} assumes that this class is located in a library
	 * (i.e. a JAR file) inside the installation directory.
	 * @return the installation directory. Never <code>null</code>.
	 * @throws IllegalStateException if the installation directory cannot be determined.
	 */
	protected File getInstallationDir() throws IllegalStateException {
		if (installationDir == null)
			installationDir = determineInstallationDirFromClass();

		return installationDir;
	}

	private File determineInstallationDirFromClass() {
		final URL resource = CloudStoreUpdaterCore.class.getResource("");
		logger.debug("determineInstallationDirFromClass: resource={}", resource);
		if (resource.getProtocol().equalsIgnoreCase(PROTOCOL_JAR)) {
			final URL fileUrl = removePrefixAndSuffixFromJarURL(resource);
			logger.debug("determineInstallationDirFromClass: fileUrl={}", fileUrl);
			final File file = createFileFromFileURL(fileUrl);
			logger.debug("determineInstallationDirFromClass: file={}", file);

			File dir = file;
			if (!dir.isDirectory())
				dir = dir.getParentFile();

			while (dir != null) {
				final File installationPropertiesFile = newFile(dir, INSTALLATION_PROPERTIES_FILE_NAME);
				if (installationPropertiesFile.exists()) {
					logger.debug("determineInstallationDirFromClass: Found installationPropertiesFile in this directory: {}", dir);
					return dir;
				}
				logger.debug("determineInstallationDirFromClass: installationPropertiesFile not found in this directory: {}", dir);
				dir = dir.getParentFile();
			}
			throw new IllegalStateException(String.format("File '%s' was not found in any expected location!", INSTALLATION_PROPERTIES_FILE_NAME));
		} else if (resource.getProtocol().equalsIgnoreCase(PROTOCOL_FILE)) {
			throw new UnsupportedOperationException("CloudStoreUpdaterCore was loaded inside the IDE! Load it from a real installation!");
		} else
			throw new IllegalStateException("Class 'CloudStoreUpdaterCore' was not loaded from a local JAR or class file!");
	}

	private File createFileFromFileURL(final URL url) {
		assertNotNull("url", url);
		if (!url.getProtocol().equalsIgnoreCase(PROTOCOL_FILE))
			throw new IllegalStateException("url does not reference a local file, i.e. it does not start with 'file:': " + url);

		try {
			final File file = newFile(url.toURI());
			return file;
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private URL removePrefixAndSuffixFromJarURL(final URL url) {
		assertNotNull("url", url);
		if (!url.getProtocol().equalsIgnoreCase(PROTOCOL_JAR))
			throw new IllegalArgumentException("url is not starting with 'jar:': " + url);

		String urlStrWithoutJarPrefix = url.getFile();
		final int exclamationMarkIndex = urlStrWithoutJarPrefix.indexOf('!');
		if (exclamationMarkIndex >= 0) {
			urlStrWithoutJarPrefix = urlStrWithoutJarPrefix.substring(0, exclamationMarkIndex);
		}
		try {
			final URL urlWithoutJarPrefixAndSuffix = new URL(urlStrWithoutJarPrefix);
			return urlWithoutJarPrefixAndSuffix;
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Is the configuration property {@link #CONFIG_KEY_DOWNGRADE} set to "true"?
	 * @return the value of the configuration property {@link #CONFIG_KEY_DOWNGRADE}.
	 */
	private boolean isDowngrade() {
		return Config.getInstance().getPropertyAsBoolean(CONFIG_KEY_DOWNGRADE, Boolean.FALSE);
	}

	private boolean isForce() {
		return Config.getInstance().getPropertyAsBoolean(CONFIG_KEY_FORCE, Boolean.FALSE);
	}

	private boolean isEnabled() {
		return Config.getInstance().getPropertyAsBoolean(CONFIG_KEY_ENABLED, Boolean.TRUE);
	}

	private long getRemoteVersionCacheValidityPeriod() {
		return Config.getInstance().getPropertyAsPositiveOrZeroLong(CONFIG_KEY_REMOTE_VERSION_CACHE_VALIDITY_PERIOD, DEFAULT_REMOTE_VERSION_CACHE_VALIDITY_PERIOD);
	}

	private File getUpdaterPropertiesFile() {
		return newFile(ConfigDir.getInstance().getFile(), "updater.properties");
	}

	private static final String PROPERTY_KEY_REMOTE_VERSION_TIMESTAMP = "remoteVersionTimestamp";
	private static final String PROPERTY_KEY_REMOTE_VERSION = "remoteVersion";

	private static class RemoteVersionCache {
		public final Version remoteVersion;
		public final DateTime remoteVersionTimestamp;

		public RemoteVersionCache(final Version remoteVersion, final DateTime remoteVersionTimestamp) {
			this.remoteVersion = assertNotNull("remoteVersion", remoteVersion);
			this.remoteVersionTimestamp = assertNotNull("remoteVersionTimestamp", remoteVersionTimestamp);
		}
	}

	private RemoteVersionCache readRemoteVersionCacheFromProperties() {
		try ( final LockFile lockFile = LockFileFactory.getInstance().acquire(getUpdaterPropertiesFile(), 30000); ) {
			final Properties properties = new Properties();
			try {
				final InputStream in = lockFile.createInputStream();
				try {
					properties.load(in);
				} finally {
					in.close();
				}

				final String versionStr = properties.getProperty(PROPERTY_KEY_REMOTE_VERSION);
				if (versionStr == null || versionStr.trim().isEmpty())
					return null;

				final String timestampStr = properties.getProperty(PROPERTY_KEY_REMOTE_VERSION_TIMESTAMP);
				if (timestampStr == null || timestampStr.trim().isEmpty())
					return null;

				final Version remoteVersion;
				try {
					remoteVersion = new Version(versionStr.trim());
				} catch (final Exception x) {
					logger.warn("readRemoteVersionFromProperties: Version-String '{}' could not be parsed into a Version! Returning null!", versionStr.trim());
					return null;
				}

				final DateTime remoteVersionTimestamp;
				try {
					remoteVersionTimestamp = new DateTime(timestampStr.trim());
				} catch (final Exception x) {
					logger.warn("readRemoteVersionFromProperties: Timestamp-String '{}' could not be parsed into a DateTime! Returning null!", timestampStr.trim());
					return null;
				}

				return new RemoteVersionCache(remoteVersion, remoteVersionTimestamp);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void writeRemoteVersionCacheToProperties(final RemoteVersionCache remoteVersionCache) {
		try ( final LockFile lockFile = LockFileFactory.getInstance().acquire(getUpdaterPropertiesFile(), 30000); ) {
			final Lock lock = lockFile.getLock();
			lock.lock();
			try {
				final Properties properties = new Properties();
				try {
					final InputStream in = lockFile.createInputStream();
					try {
						properties.load(in);
					} finally {
						in.close();
					}

					if (remoteVersionCache == null) {
						properties.remove(PROPERTY_KEY_REMOTE_VERSION);
						properties.remove(PROPERTY_KEY_REMOTE_VERSION_TIMESTAMP);
					}
					else {
						properties.setProperty(PROPERTY_KEY_REMOTE_VERSION, remoteVersionCache.remoteVersion.toString());
						properties.setProperty(PROPERTY_KEY_REMOTE_VERSION_TIMESTAMP, remoteVersionCache.remoteVersionTimestamp.toString());
					}

					final OutputStream out = lockFile.createOutputStream();
					try {
						properties.store(out, null);
					} finally {
						out.close();
					}
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Creates the {@link #getUpdaterDir() updaterDir}, if an update is necessary.
	 * <p>
	 * If an update is not necessary, this method returns silently without doing anything.
	 * <p>
	 * The check, whether an update is needed, is not done every time. The result is cached for
	 * {@linkplain #CONFIG_KEY_REMOTE_VERSION_CACHE_VALIDITY_PERIOD a certain time period} to reduce HTTP queries.
	 * <p>
	 * This method does not throw any exception. In case of an exception, it is only logged and this
	 * method returns normally.
	 */
	public void createUpdaterDirIfUpdateNeeded() {
		File updaterDir = null;
		try {
			if (!isEnabled()) {
				if (isForce())
					logger.warn("createUpdaterDirIfUpdateNeeded: The configuration key '{}' (or its corresponding system property) is set to force an update, but the updater is *not* enabled! You must set the configuration key '{}' (or its corresponding system property) additionally! Skipping!", CONFIG_KEY_FORCE, CONFIG_KEY_ENABLED);
				else
					logger.info("createUpdaterDirIfUpdateNeeded: Updater is *not* enabled! Skipping! See configuration key '{}'.", CONFIG_KEY_ENABLED);

				return;
			}

			updaterDir = getUpdaterDir();
			IOUtil.deleteDirectoryRecursively(updaterDir);

			if (isUpdateNeeded()) {
				if (!canWriteAll(getInstallationDir())) {
					logger.error("Installation directory '{}' is not writable or contains sub-directories/files that are not writable! Cannot perform auto-update to new version {}! Please update manually! Your local version is {}.",
							getInstallationDir(), getRemoteVersion(), getLocalVersion());
					return;
				}

				copyInstallationDirectoryForUpdater();
				logger.debug("createUpdaterDirIfUpdateNeeded: updaterDir='{}'", updaterDir);
			}
		} catch (final Exception x) {
			logger.error("createUpdaterDirIfUpdateNeeded: " + x, x);
			if (updaterDir != null) {
				try {
					IOUtil.deleteDirectoryRecursively(updaterDir);
				} catch (final Exception y) {
					logger.error("createUpdaterDirIfUpdateNeeded: " + y, y);
				}
			}
		}
	}

	private boolean canWriteAll(final File fileOrDir) {
		if (!fileOrDir.canWrite())
			return false;

		final File[] children = fileOrDir.listFiles(fileFilterIgnoringBackupDir);
		if (children != null) {
			for (final File child : children) {
				if (!canWriteAll(child))
					return false;
			}
		}
		return true;
	}

	protected File getUpdaterDir() {
		if (updaterDir == null)
			updaterDir = newFile(getInstallationDir(), "updater");

		return updaterDir;
	}

	protected File getBackupDir() {
		if (backupDir == null)
			backupDir = newFile(getInstallationDir(), "backup");

		return backupDir;
	}

	protected final FileFilter fileFilterIgnoringBackupDir = new FileFilter() {
		@Override
		public boolean accept(final java.io.File file) {
			return !getBackupDir().equals(file);
		}
	};

	protected final FileFilter fileFilterIgnoringBackupAndUpdaterDir = new FileFilter() {
		@Override
		public boolean accept(final java.io.File file) {
			return !(getBackupDir().equals(file) || getUpdaterDir().equals(file));
		}
	};

	private File copyInstallationDirectoryForUpdater() {
		try {
			final File updaterDir = getUpdaterDir();
			IOUtil.deleteDirectoryRecursively(updaterDir);
			IOUtil.copyDirectory(getInstallationDir(), updaterDir, fileFilterIgnoringBackupAndUpdaterDir);
			return updaterDir;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isUpdateNeeded() {
		final Version localVersion = getLocalVersion();
		final Version remoteVersion = getRemoteVersion();
		if (isForce()) {
			logger.warn("isUpdateNeeded: Update forced via system-property! localVersion='{}' remoteVersion='{}'", localVersion, remoteVersion);
			return true;
		}

		if (localVersion.equals(remoteVersion)) {
			logger.debug("isUpdateNeeded: No update, because localVersion equals remoteVersion='{}'", remoteVersion);
			return false;
		}

		if (localVersion.compareTo(remoteVersion) > 0) {
			if (isDowngrade()) {
				logger.warn("isUpdateNeeded: Downgrading enabled via system-property! localVersion='{}' remoteVersion='{}'", localVersion, remoteVersion);
				return true;
			}

			logger.info("isUpdateNeeded: No update, because localVersion='{}' is newer than remoteVersion='{}'", localVersion, remoteVersion);
			return false;
		}

		logger.warn("isUpdateNeeded: Update needed! localVersion='{}' remoteVersion='{}'", localVersion, remoteVersion);
		return true;
	}
}
