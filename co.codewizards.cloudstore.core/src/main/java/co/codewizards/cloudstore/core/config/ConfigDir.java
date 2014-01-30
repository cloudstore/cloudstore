package co.codewizards.cloudstore.core.config;

import java.io.File;

import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * {@code ConfigDir} represents the central configuration directory.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class ConfigDir {

	/**
	 * System property controlling the location of the central configuration directory.
	 * <p>
	 * If this system property is not set, it defaults to: <code>${user.home}/.cloudstore</code>
	 */
	public static final String SYSTEM_PROPERTY = "cloudstore.configDir";

	private static final class ConfigDirHolder {
		public static ConfigDir instance = new ConfigDir();
	}

	private String value;
	private File file;
	private File logDir;

	private ConfigDir() {
		value = System.getProperty(SYSTEM_PROPERTY, "${user.home}/.cloudstore");
		String resolvedValue = IOUtil.replaceTemplateVariables(value, System.getProperties());
		file = new File(resolvedValue).getAbsoluteFile();
		if (!file.isDirectory())
			file.mkdirs();

		if (!file.isDirectory())
			throw new IllegalStateException("Could not create directory (permissions?!): " + file);
	}

	public static ConfigDir getInstance() {
		return ConfigDirHolder.instance;
	}

	public String getValue() {
		return value;
	}

	public File getFile() {
		return file;
	}

	public File getLogDir() {
		if (logDir == null) {
			logDir = new File(getFile(), "log");
			if (!logDir.isDirectory())
				logDir.mkdirs();

			if (!logDir.isDirectory())
				throw new IllegalStateException("Could not create directory (permissions?!): " + logDir);
		}
		return logDir;
	}
}
