package co.codewizards.cloudstore.ls.server.cproc;

import static co.codewizards.cloudstore.core.chronos.ChronosUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.io.TimeoutException;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.core.LocalServerPropertiesManager;
import co.codewizards.cloudstore.ls.core.LsConfig;

public class LocalServerProcessLauncher {
	private static final Logger logger = LoggerFactory.getLogger(LocalServerProcessLauncher.class);
	private static final String JAR_URL_PROTOCOL = "jar";
	private static final String JAR_URL_PREFIX = JAR_URL_PROTOCOL + ':';
	private static final String JAR_URL_CONTENT_PATH_SEPARATOR = "!/";
	private static final String FILE_PROTOCOL = "file";

	public LocalServerProcessLauncher() {
	}

	public boolean start() throws IOException {
		// Check the configuration 'localServerProcess.enabled'.
		if (! LsConfig.isLocalServerProcessEnabled())
			return false;

		// Even though 'localServerProcess.enabled' is 'true', we also check for 'localServer.enabled'.
		// If the 'localServer.enabled' is 'false', waitUntilServerOnline() fails anyway, because the
		// LocalServer is not started inside the separate VM process. Hence, we don't launch the VM at all.
		if (! LsConfig.isLocalServerEnabled())
			return false;

		final File javaExecutableFile = getJavaExecutableFile();
		if (javaExecutableFile == null)
			return false;

		final File thisJarFile = getThisJarFile();
		if (thisJarFile == null)
			return false;

		final List<String> command = new ArrayList<>();
		command.add(javaExecutableFile.getPath());

		populateJvmArguments(command);
		populateConfigSystemProperties(command);

		command.add("-jar");
		command.add(thisJarFile.getPath());

		logger.info("start: command={}", command);

		final ProcessBuilder pb = new ProcessBuilder(command);

		final File processRedirectInputFile = getProcessRedirectInputFile();
		final File processRedirectOutputFile = getProcessRedirectOutputFile();
		processRedirectInputFile.createNewFile(); // 0-byte-file

		pb.redirectInput(processRedirectInputFile.getIoFile());
		pb.redirectOutput(processRedirectOutputFile.getIoFile());
		pb.redirectError(processRedirectOutputFile.getIoFile());

		final Process process = pb.start();
		if (process == null) {
			logger.warn("start: process=null");
			return false;
		}

		waitUntilServerOnline();
		return true;
	}

	private void populateJvmArguments(final List<String> command) {
		String maxHeapSize = LsConfig.getLocalServerProcessMaxHeapSize();
		if (maxHeapSize != null) {
			command.add("-Xmx" + maxHeapSize); // Warning: This might not be supported by the JVM! The -X... options are not standard. But what should we do instead?!
		}
		List<String> additionalVmArgs = LsConfig.getLocalServerProcessVmArgs();
		command.addAll(additionalVmArgs);
	}

	private void populateConfigSystemProperties(final List<String> command) {
		for (final Map.Entry<Object, Object> me : System.getProperties().entrySet()) {
			final String k = me.getKey().toString();
			final String v = me.getValue().toString();

			if (k.startsWith(Config.SYSTEM_PROPERTY_PREFIX)) {
				final String arg = "-D" + k + "=" + v;
				command.add(arg);
			}
		}
	}

	private void waitUntilServerOnline() {
		final long startTimestamp = nowAsMillis();
		while (true) {
			final long timeoutMs = LsConfig.getLocalServerProcessStartTimeout();
			final boolean timeout = nowAsMillis() - startTimestamp > timeoutMs;

			LocalServerPropertiesManager.getInstance().clear();
			final String baseUrlString = LocalServerPropertiesManager.getInstance().getBaseUrl();
			if (baseUrlString != null) {
				final URL baseUrl;
				try {
					baseUrl = new URL(baseUrlString);
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}

				int port = baseUrl.getPort();
				if (port < 0)
					port = baseUrl.getDefaultPort();

				if (port < 0)
					port = 443;

				try {
					Socket socket = new Socket(baseUrl.getHost(), port);
					socket.close();
					logger.info("waitUntilServerOnline: Connecting to " + baseUrl + " succeeded!");
					return;
				} catch (IOException e) {
					if (timeout)
						logger.error("waitUntilServerOnline: Connecting to " + baseUrl + " failed (fatal): " + e, e);
					else
						logger.warn("waitUntilServerOnline: Connecting to " + baseUrl + " failed (retrying): " + e);
				}
			}

			if (timeout)
				throw new TimeoutException("LocalServer did not come online within timeout!");

			try { Thread.sleep(500); } catch (InterruptedException e) { doNothing(); }
		}
	}

	/**
	 * Gets the source file for system-in of the new process.
	 * <p>
	 * This file is created (with size 0) instead of the default behaviour {@link Redirect#PIPE PIPE},
	 * because we don't want the child-process to be linked with the current process.
	 *
	 * @return the source file for system-in of the new process. Never <code>null</code>.
	 */
	private File getProcessRedirectInputFile() {
		final File tempDir = getTempDir();
		final DateFormat df = new SimpleDateFormat("YYYY-MM-dd-HH-mm-ss");
		final String now = df.format(nowAsDate());
		final File file = tempDir.createFile(String.format("LocalServer.%s.in", now)).getAbsoluteFile();
		logger.debug("getProcessRedirectInputFile: file='{}'", file);
		return file;
	}

	/**
	 * Gets the destination file for system-out and system-error of the new process.
	 * @return the destination file for system-out and system-error of the new process. Never <code>null</code>.
	 */
	private File getProcessRedirectOutputFile() {
		final File tempDir = getTempDir();
		final DateFormat df = new SimpleDateFormat("YYYY-MM-dd-HH-mm-ss");
		final String now = df.format(nowAsDate());
		final File file = tempDir.createFile(String.format("LocalServer.%s.out", now)).getAbsoluteFile();
		logger.debug("getProcessRedirectOutputFile: file='{}'", file);
		return file;
	}

	private File getJavaExecutableFile() {
		final String javaHome = System.getProperty("java.home");
		requireNonNull(javaHome, "javaHome");

		File file = createFile(javaHome, "bin", "java").getAbsoluteFile();
		if (file.isFile()) {
			logger.debug("getJavaExecutableFile: file='{}'", file);
			return file;
		}

		file = createFile(javaHome, "bin", "java.exe").getAbsoluteFile();
		if (file.isFile()) {
			logger.debug("getJavaExecutableFile: file='{}'", file);
			return file;
		}

		logger.warn("getJavaExecutableFile: Could not locate 'java' executable!");
		return null;
	}

	/**
	 * Gets the JAR file containing this object's class.
	 * @return the JAR file containing this object's class. <code>null</code>, if this class is not contained in a JAR.
	 */
	private File getThisJarFile() {
		// Should return an URL like this:
		// jar:file:/home/mn/.../co.codewizards.cloudstore.ls.server.cproc-0.9.7-SNAPSHOT.jar!/co/codewizards/cloudstore/ls/server/cproc/
		final URL url = this.getClass().getResource("");
		requireNonNull(url, "url");

		final String urlString = url.toString();
		logger.debug("getThisJarFile: url='{}'", urlString);

		if (! urlString.startsWith(JAR_URL_PREFIX)) {
			logger.warn("getThisJarFile: This class ({}) is not located in a JAR file! url='{}'",
					this.getClass().getName(), urlString);

			return null;
		}

		final int indexOfContentPathSeparator = urlString.indexOf(JAR_URL_CONTENT_PATH_SEPARATOR);
		if (indexOfContentPathSeparator < 0)
			throw new IllegalStateException(String.format("JAR-URL '%s' does not contain separator '%s'!",
					urlString, JAR_URL_CONTENT_PATH_SEPARATOR));

		final String jarUrlString = urlString.substring(JAR_URL_PREFIX.length(), indexOfContentPathSeparator);
		logger.debug("getThisJarFile: url='{}'", urlString);

		final URL jarUrl;
		try {
			jarUrl = new URL(jarUrlString);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		if (! FILE_PROTOCOL.equals(jarUrl.getProtocol()))
			throw new IllegalStateException(String.format("Illegal protocol ('%s' expected): %s",
					FILE_PROTOCOL, jarUrlString));

		java.io.File f;
		try {
			f = new java.io.File(jarUrl.toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		logger.debug("getThisJarFile: file='{}'", f);
		return createFile(f);
	}
}
