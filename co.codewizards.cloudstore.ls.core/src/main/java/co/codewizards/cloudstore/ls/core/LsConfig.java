package co.codewizards.cloudstore.ls.core;

import co.codewizards.cloudstore.core.config.ConfigImpl;

public class LsConfig {

	public static final String CONFIG_KEY_LOCAL_SERVER_ENABLED = "localServer.enabled";
	public static final boolean DEFAULT_LOCAL_SERVER_ENABLED = true;

	public static final String CONFIG_KEY_LOCAL_SERVER_PROCESS_ENABLED = "localServerProcess.enabled";
	public static final boolean DEFAULT_LOCAL_SERVER_PROCESS_ENABLED = true;

	public static final String CONFIG_KEY_LOCAL_SERVER_PROCESS_START_TIMEOUT = "localServerProcess.startTimeout";
	public static final long DEFAULT_LOCAL_SERVER_PROCESS_START_TIMEOUT = 120000L;

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
	 * If <code>false</code>, it also prevents the local-server-<b>process</b> from being launched.
	 * Thus in order to launch the local-server-process, both {@link #isLocalServerEnabled()}
	 * and {@code isLocalServerProcessEnabled()} must be <code>true</code>.
	 * @return <code>true</code>, if the local-server should be listening; <code>false</code> otherwise.
	 */
	public static boolean isLocalServerProcessEnabled() {
		return ConfigImpl.getInstance().getPropertyAsBoolean(
				CONFIG_KEY_LOCAL_SERVER_PROCESS_ENABLED,
				DEFAULT_LOCAL_SERVER_PROCESS_ENABLED);
	}

	public static long getLocalServerProcessStartTimeout() {
		final long timeoutMs = ConfigImpl.getInstance().getPropertyAsPositiveOrZeroLong(
						CONFIG_KEY_LOCAL_SERVER_PROCESS_START_TIMEOUT,
						DEFAULT_LOCAL_SERVER_PROCESS_START_TIMEOUT);
		return timeoutMs;
	}
}
