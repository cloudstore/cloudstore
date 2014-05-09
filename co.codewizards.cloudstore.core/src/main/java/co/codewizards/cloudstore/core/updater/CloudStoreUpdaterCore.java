package co.codewizards.cloudstore.core.updater;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.core.util.PropertiesUtil;

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
	 * System property controlling whether to force the update. If this property is set, an update is
	 * done even if the versions locally and remotely are already the same.
	 * <p>
	 * The configuration can be overridden by a system property - see {@link Config#SYSTEM_PROPERTY_PREFIX}.
	 */
	public static final String CONFIG_KEY_FORCE = "updater.force";

	private Version localVersion;
	private Version remoteVersion;
	private Properties installationProperties;
	private File installationDir;
	private File updaterDir;
	private File backupDir;

	public CloudStoreUpdaterCore() { }

	public Version getRemoteVersion() {
		if (remoteVersion == null) {
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
					BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					final String line = r.readLine();
					if (line == null || line.isEmpty())
						throw new IllegalStateException("Failed to read version from: " + resolvedRemoteVersionURL);

					final String trimmed = line.trim();
					if (trimmed.isEmpty())
						throw new IllegalStateException("Failed to read version from: " + resolvedRemoteVersionURL);

					remoteVersion = new Version(trimmed);
					r.close();
				} finally {
					in.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return remoteVersion;
	}

	public Version getLocalVersion() {
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
			final File installationPropertiesFile = new File(getInstallationDir(), INSTALLATION_PROPERTIES_FILE_NAME);
			if (!installationPropertiesFile.exists())
				throw new IllegalArgumentException(String.format("installationPropertiesFile '%s' does not exist!", installationPropertiesFile.getAbsolutePath()));

			if (!installationPropertiesFile.isFile())
				throw new IllegalArgumentException(String.format("installationPropertiesFile '%s' is not a file!", installationPropertiesFile.getAbsolutePath()));

			try {
				final Properties properties = PropertiesUtil.load(installationPropertiesFile);
				installationProperties = properties;
			} catch (IOException x) {
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
	protected String resolve(String template) {
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
				final File installationPropertiesFile = new File(dir, INSTALLATION_PROPERTIES_FILE_NAME);
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

	private File createFileFromFileURL(URL url) {
		assertNotNull("url", url);
		if (!url.getProtocol().equalsIgnoreCase(PROTOCOL_FILE))
			throw new IllegalStateException("url does not reference a local file, i.e. it does not start with 'file:': " + url);

		try {
			final File file = Paths.get(url.toURI()).toFile();
			return file;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private URL removePrefixAndSuffixFromJarURL(URL url) {
		assertNotNull("url", url);
		if (!url.getProtocol().equalsIgnoreCase(PROTOCOL_JAR))
			throw new IllegalArgumentException("url is not starting with 'jar:': " + url);

		String urlStrWithoutJarPrefix = url.getFile();
		int exclamationMarkIndex = urlStrWithoutJarPrefix.indexOf('!');
		if (exclamationMarkIndex >= 0) {
			urlStrWithoutJarPrefix = urlStrWithoutJarPrefix.substring(0, exclamationMarkIndex);
		}
		try {
			final URL urlWithoutJarPrefixAndSuffix = new URL(urlStrWithoutJarPrefix);
			return urlWithoutJarPrefixAndSuffix;
		} catch (MalformedURLException e) {
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

	/**
	 * Creates the {@link #getUpdaterDir() updaterDir}, if an update is necessary.
	 * <p>
	 * If an update is not necessary, this method returns silently without doing anything.
	 * <p>
	 * The check, whether an update is needed, is not done every time. The result is cached to reduce
	 * HTTP queries. TODO this caching still needs to be implemented.
	 */
	public void createUpdaterDirIfUpdateNeeded() {
		final File updaterDir = getUpdaterDir();
		try {
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
		} catch (Exception x) {
			logger.error("createUpdaterDirIfUpdateNeeded: " + x, x);
			try {
				IOUtil.deleteDirectoryRecursively(updaterDir);
			} catch (Exception y) {
				logger.error("createUpdaterDirIfUpdateNeeded: " + y, y);
			}
		}
	}

	private boolean canWriteAll(File fileOrDir) {
		if (!fileOrDir.canWrite())
			return false;

		File[] children = fileOrDir.listFiles(fileFilterIgnoringBackupDir);
		if (children != null) {
			for (File child : children) {
				if (!canWriteAll(child))
					return false;
			}
		}
		return true;
	}

	protected File getUpdaterDir() {
		if (updaterDir == null)
			updaterDir = new File(getInstallationDir(), "updater");

		return updaterDir;
	}

	protected File getBackupDir() {
		if (backupDir == null)
			backupDir = new File(getInstallationDir(), "backup");

		return backupDir;
	}

	protected final FileFilter fileFilterIgnoringBackupDir = new FileFilter() {
		@Override
		public boolean accept(final File file) {
			return !getBackupDir().equals(file);
		}
	};

	protected final FileFilter fileFilterIgnoringBackupAndUpdaterDir = new FileFilter() {
		@Override
		public boolean accept(final File file) {
			return !(getBackupDir().equals(file) || getUpdaterDir().equals(file));
		}
	};

	private File copyInstallationDirectoryForUpdater() {
		try {
			final File updaterDir = getUpdaterDir();
			IOUtil.deleteDirectoryRecursively(updaterDir);
			IOUtil.copyDirectory(getInstallationDir(), updaterDir, fileFilterIgnoringBackupAndUpdaterDir);
			return updaterDir;
		} catch (IOException e) {
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
			logger.debug("isUpdateNeeded: Silently returning, because localVersion equals remoteVersion='{}'", remoteVersion);
			return false;
		}

		if (localVersion.compareTo(remoteVersion) > 0) {
			if (isDowngrade()) {
				logger.warn("isUpdateNeeded: Downgrading enabled via system-property! localVersion='{}' remoteVersion='{}'", localVersion, remoteVersion);
				return true;
			}

			logger.info("isUpdateNeeded: Silently returning, because localVersion='{}' is newer than remoteVersion='{}'", localVersion, remoteVersion);
			return false;
		}

		logger.warn("isUpdateNeeded: Update needed! localVersion='{}' remoteVersion='{}'", localVersion, remoteVersion);
		return true;
	}
}
