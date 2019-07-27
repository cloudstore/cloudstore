package co.codewizards.cloudstore.ls.core;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class LocalServerPropertiesManager {

	private static final Logger logger = LoggerFactory.getLogger(LocalServerPropertiesManager.class);

	public static final String PROPERTY_KEY_PORT = "port";
	public static final String PROPERTY_KEY_PASSWORD = "password";

	private static final class Holder {
		public static final LocalServerPropertiesManager instance = new LocalServerPropertiesManager();
	}

	private volatile Properties localServerProperties;
	private File localServerPropertiesFile;

	protected LocalServerPropertiesManager() {
	}

	public static LocalServerPropertiesManager getInstance() {
		return Holder.instance;
	}

	protected File getLocalServerPropertiesFile() {
		if (localServerPropertiesFile == null)
			localServerPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "localServer.properties");

		return localServerPropertiesFile;
	}

	protected Properties getLocalServerProperties() {
		Properties properties = localServerProperties;
		if (properties == null) {
			properties = new Properties();

			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLocalServerPropertiesFile(), 30000);) {
				try (InputStream in = castStream(lockFile.createInputStream())) {
					final Properties p = localServerProperties;
					if (p != null)
						return p;

					properties.load(in);
					localServerProperties = properties;
				}
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
		return properties;
	}

	public void writeLocalServerProperties() {
		final Properties properties = getLocalServerProperties();

		try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLocalServerPropertiesFile(), 30000);) {
			try (OutputStream out = castStream(lockFile.createOutputStream())) {
				properties.store(out, null);
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	public int getPort() {
		final String s = getLocalServerProperties().getProperty(PROPERTY_KEY_PORT);
		try {
			final int port = Integer.parseInt(s);
			if (port < 1 || port > 65535)
				throw new NumberFormatException("Port is out of range: " + port);

			return port;
		} catch (NumberFormatException x) {
			logger.warn("getPort: " + x, x);
			return -1;
		}
	}

	public void setPort(final int port) {
		if (port < 1 || port > 65535)
			throw new IllegalArgumentException("Port is out of range: " + port);

		getLocalServerProperties().setProperty(PROPERTY_KEY_PORT, Integer.toString(port));
	}

	public String getBaseUrl() {
		final int port = LocalServerPropertiesManager.getInstance().getPort();
		if (port < 0)
			return null;

		final String baseUrl = "http://127.0.0.1:" + port + '/';
		return baseUrl;
	}

	public String getPassword() {
		return getLocalServerProperties().getProperty(PROPERTY_KEY_PASSWORD);
	}

	public void setPassword(final String password) {
		requireNonNull(password, "password");
		getLocalServerProperties().setProperty(PROPERTY_KEY_PASSWORD, password);
	}

	public void clear() {
		localServerProperties = null;
	}
}
