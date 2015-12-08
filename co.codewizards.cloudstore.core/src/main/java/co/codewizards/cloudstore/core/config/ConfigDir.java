package co.codewizards.cloudstore.core.config;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.PropertiesUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import co.codewizards.cloudstore.core.appid.AppId;
import co.codewizards.cloudstore.core.appid.AppIdRegistry;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * {@code ConfigDir} represents the central configuration directory.
 * <p>
 * Besides the configuration, this directory holds all global (non-repository-related)
 * files, including log files.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ConfigDir {

	private static final String APP_ID_SIMPLE_ID = AppIdRegistry.getInstance().getAppIdOrFail().getSimpleId();

	/**
	 * System property controlling the location of the central configuration directory.
	 * <p>
	 * Also, the {@link #ENV_VAR_CONFIG_DIR} is checked, if the system property is not present. The system
	 * property thus overrides the OS environment variable.
	 * <p>
	 * If neither system property nor env var is set, it defaults to: <code>&#36;{user.home}/.cloudstore</code>
	 * <p>
	 * Note that this property is always set during runtime. If it is not set by the caller (via a <code>-D</code> JVM argument)
	 * from the outside, then it is set by the code inside the running application. Therefore, this property
	 * can be referenced in all configuration files where system properties are resolved (e.g. in the logback configuration).
	 * <p>
	 * Note that "cloudstore" might be replaced by another identifier using the {@link AppId}.
	 * @see #getValue()
	 * @see #getFile()
	 */
	public static final String SYSTEM_PROPERTY_CONFIG_DIR = APP_ID_SIMPLE_ID + ".configDir";

	/**
	 * Environment variable equivalent to {@link #SYSTEM_PROPERTY_CONFIG_DIR}.
	 */
	public static final String ENV_VAR_CONFIG_DIR = systemPropertyToEnvironmentVariable(SYSTEM_PROPERTY_CONFIG_DIR);

	/**
	 * System property controlling the location of the log directory.
	 * <p>
	 * If this system property is not set, it defaults to:
	 * <code>&#36;{user.home}/.cloudstore/log</code>
	 * <p>
	 * Note that this property is always set during runtime. If it is not set by the caller (via a <code>-D</code> JVM argument)
	 * from the outside, then it is set by the code inside the running application. Therefore, this property
	 * can be referenced in all configuration files where system properties are resolved (e.g. in the logback configuration).
	 * @see #getLogDir()
	 */
	public static final String SYSTEM_PROPERTY_LOG_DIR = APP_ID_SIMPLE_ID + ".logDir";

	private static final class ConfigDirHolder {
		public static ConfigDir instance = new ConfigDir();
	}

	private final String value;
	private final File file;
	private File logDir;

	/**
	 * Creates an instance of {@code ConfigDir}.
	 * <p>
	 * This method cannot and should not be called directly. Instead, {@link #getInstance()} should be
	 * used to obtain the singleton.
	 */
	protected ConfigDir() {
		String v = System.getProperty(SYSTEM_PROPERTY_CONFIG_DIR);

		if (v == null)
			v = System.getenv(ENV_VAR_CONFIG_DIR);

		if (v == null)
			v = "${user.home}/." + APP_ID_SIMPLE_ID;

		value = v;
		System.setProperty(SYSTEM_PROPERTY_CONFIG_DIR, value);
		final String resolvedValue = IOUtil.replaceTemplateVariables(value, System.getProperties());
		file = createFile(resolvedValue).getAbsoluteFile();
		if (!file.isDirectory())
			file.mkdirs();

		if (!file.isDirectory())
			throw new IllegalStateException("Could not create directory (permissions?!): " + file);
	}

	/**
	 * Gets the singleton instance of {@code ConfigDir}.
	 * @return the singleton instance of {@code ConfigDir}. Never <code>null</code>.
	 */
	public static ConfigDir getInstance() {
		return ConfigDirHolder.instance;
	}

	/**
	 * Gets the central configuration directory as {@code String}.
	 * <p>
	 * This is the <i>non-resolved</i> (as is) value of the system property {@link #SYSTEM_PROPERTY_CONFIG_DIR}.
	 * Even if this property was not set (from the outside), it is initialised by default to:
	 * <code>&#36;{user.home}/.cloudstore</code>
	 * @return the central configuration directory as {@code String}. Never <code>null</code>.
	 * @see #SYSTEM_PROPERTY_CONFIG_DIR
	 * @see #getFile()
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Gets the central configuration directory as <i>absolute</i> {@code File}.
	 * <p>
	 * In contrast to {@link #getValue()}, this file's path is <i>resolved</i>; i.e. all system properties
	 * occurring in it (e.g. "&#36;{user.home}") were replaced by their actual values.
	 * @return the central configuration directory as <i>absolute</i> {@code File}. Never <code>null</code>.
	 * @see #SYSTEM_PROPERTY_CONFIG_DIR
	 * @see #getValue()
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Gets the log directory (the directory where the log files are written to).
	 * <p>
	 * This directory can be configured or referenced (e.g. in a logback configuation file) via
	 * {@link #SYSTEM_PROPERTY_LOG_DIR}.
	 * @return the log directory. Never <code>null</code>.
	 * @see #SYSTEM_PROPERTY_LOG_DIR
	 */
	public File getLogDir() {
		if (logDir == null) {
			final String sysPropVal = System.getProperty(SYSTEM_PROPERTY_LOG_DIR);
			if (isEmpty(sysPropVal))
				logDir = createFile(getFile(), "log");
			else {
				final String resolvedSysPropVal = IOUtil.replaceTemplateVariables(sysPropVal, System.getProperties());
				logDir = createFile(resolvedSysPropVal).getAbsoluteFile();
			}

			System.setProperty(SYSTEM_PROPERTY_LOG_DIR, logDir.getPath());
			if (!logDir.isDirectory())
				logDir.mkdirs();

			if (!logDir.isDirectory())
				throw new IllegalStateException("Could not create directory (permissions?!): " + logDir);
		}
		return logDir;
	}
}
