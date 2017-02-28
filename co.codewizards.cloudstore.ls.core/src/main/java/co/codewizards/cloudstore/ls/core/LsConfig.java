package co.codewizards.cloudstore.ls.core;

import static co.codewizards.cloudstore.core.util.StringUtil.*;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;

public class LsConfig {

	/**
	 * {@link Config}-key controlling whether the local-server enabled.
	 * @see #DEFAULT_LOCAL_SERVER_ENABLED
	 * @see #isLocalServerEnabled()
	 */
	public static final String CONFIG_KEY_LOCAL_SERVER_ENABLED = "localServer.enabled";
	/**
	 * Default value for {@link #CONFIG_KEY_LOCAL_SERVER_ENABLED}.
	 */
	public static final boolean DEFAULT_LOCAL_SERVER_ENABLED = true;

	/**
	 * {@link Config}-key controlling whether the separate local-server-<b>process</b> is launched.
	 * @see #DEFAULT_LOCAL_SERVER_PROCESS_ENABLED
	 * @see #isLocalServerProcessEnabled()
	 */
	public static final String CONFIG_KEY_LOCAL_SERVER_PROCESS_ENABLED = "localServerProcess.enabled";
	/**
	 * Default value for {@link #CONFIG_KEY_LOCAL_SERVER_PROCESS_ENABLED}
	 */
	public static final boolean DEFAULT_LOCAL_SERVER_PROCESS_ENABLED = true;

	/**
	 * {@link Config}-key controlling the timeout in milliseconds the primary (first launched) process waits for
	 * the separate local-server-process to become available.
	 * @see #DEFAULT_LOCAL_SERVER_PROCESS_START_TIMEOUT
	 * @see #getLocalServerProcessStartTimeout()
	 */
	public static final String CONFIG_KEY_LOCAL_SERVER_PROCESS_START_TIMEOUT = "localServerProcess.startTimeout";
	/**
	 * Default value for {@link #CONFIG_KEY_LOCAL_SERVER_PROCESS_START_TIMEOUT}
	 */
	public static final long DEFAULT_LOCAL_SERVER_PROCESS_START_TIMEOUT = 120000L;

	/**
	 * Controls the value passed as
	 * <a href="http://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html">{@code -Xmx}</a>
	 * to the child-process, thus specifying the maximum heap size of the local-server's JVM.
	 * <p>
	 * Possible values are everything understood by the JVM after the "-Xmx", for example:
	 * <ul>
	 * <li>"1G" for 1 <a href="https://en.wikipedia.org/wiki/Gibibyte">Gibibyte</a>
	 * <li>"2g" for 2 <a href="https://en.wikipedia.org/wiki/Gibibyte">Gibibyte</a>
	 * <li>"512M" for 512 <a href="https://en.wikipedia.org/wiki/Mebibyte">Mebibyte</a>
	 * <li>"256M" for 256 <a href="https://en.wikipedia.org/wiki/Mebibyte">Mebibyte</a>
	 * </ul>
	 * <p>
	 * This only has an effect, if {@link #CONFIG_KEY_LOCAL_SERVER_PROCESS_ENABLED} is <code>true</code>.
	 * @see #DEFAULT_LOCAL_SERVER_PROCESS_MAX_HEAP_SIZE
	 * @see #getLocalServerProcessMaxHeapSize()
	 */
	public static final String CONFIG_KEY_LOCAL_SERVER_PROCESS_MAX_HEAP_SIZE = "localServerProcess.maxHeapSize";

	/**
	 * Default value for {@link #CONFIG_KEY_LOCAL_SERVER_PROCESS_MAX_HEAP_SIZE}
	 */
	public static final String DEFAULT_LOCAL_SERVER_PROCESS_MAX_HEAP_SIZE = "";

	private LsConfig() {
	}

	/**
	 * Is the local-server enabled?
	 * <p>
	 * Controls, whether a TCP (HTTP+REST) server is started on localhost.
	 * <p>
	 * If <code>false</code>, it also prevents the local-server-<b>process</b> from being launched.
	 * Thus in order to launch the local-server-process, both {@code isLocalServerEnabled()}
	 * and {@link #isLocalServerProcessEnabled()} must be <code>true</code>.
	 * @return <code>true</code>, if the local-server should be listening; <code>false</code> otherwise.
	 * @see #CONFIG_KEY_LOCAL_SERVER_ENABLED
	 */
	public static boolean isLocalServerEnabled() {
		return ConfigImpl.getInstance().getPropertyAsBoolean(
				CONFIG_KEY_LOCAL_SERVER_ENABLED,
				DEFAULT_LOCAL_SERVER_ENABLED);
	}

	/**
	 * Should the separate local-server-<b>process</b> be launched?
	 * <p>
	 * Controls, whether a TCP (HTTP+REST) server is started in a separate process,
	 * i.e. whether the current process should launch a separate process.
	 * <p>
	 * If <code>false</code>, the local-server (if {@linkplain #isLocalServerEnabled() enabled})
	 * runs inside the primary (first-launched) VM process.
	 * <p>
	 * Note: In order to launch the local-server-process, both {@link #isLocalServerEnabled()}
	 * and {@code isLocalServerProcessEnabled()} must be <code>true</code>.
	 * @return <code>true</code>, if the local-server should be listening; <code>false</code> otherwise.
	 * @see #CONFIG_KEY_LOCAL_SERVER_PROCESS_ENABLED
	 */
	public static boolean isLocalServerProcessEnabled() {
		return ConfigImpl.getInstance().getPropertyAsBoolean(
				CONFIG_KEY_LOCAL_SERVER_PROCESS_ENABLED,
				DEFAULT_LOCAL_SERVER_PROCESS_ENABLED);
	}

	/**
	 * Gets the timeout in milliseconds the primary (first launched) process waits for
	 * the separate local-server-process to become available.
	 * <p>
	 * If the local-server does not get ready within this timeout, an exception is thrown.
	 * @return the timeout in milliseconds within which the local-server-process must be
	 * launched completely (i.e. the TCP server become available).
	 * @see #CONFIG_KEY_LOCAL_SERVER_PROCESS_START_TIMEOUT
	 */
	public static long getLocalServerProcessStartTimeout() {
		final long timeoutMs = ConfigImpl.getInstance().getPropertyAsPositiveOrZeroLong(
						CONFIG_KEY_LOCAL_SERVER_PROCESS_START_TIMEOUT,
						DEFAULT_LOCAL_SERVER_PROCESS_START_TIMEOUT);
		return timeoutMs;
	}

	public static String getLocalServerProcessMaxHeapSize() {
		final String maxHeapSize = ConfigImpl.getInstance().getProperty(
						CONFIG_KEY_LOCAL_SERVER_PROCESS_MAX_HEAP_SIZE,
						DEFAULT_LOCAL_SERVER_PROCESS_MAX_HEAP_SIZE);
		return emptyToNull(maxHeapSize);
	}
}
