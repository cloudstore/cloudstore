package co.codewizards.cloudstore.ls.core;

import co.codewizards.cloudstore.core.config.Config;

public class LsConfig {

	public static final String CONFIG_KEY_LOCAL_SERVER_ENABLED = "localServer.enabled";
	public static final boolean DEFAULT_LOCAL_SERVER_ENABLED = true;

	private LsConfig() {
	}

	public static boolean isLocalServerEnabled() {
		return Config.getInstance().getPropertyAsBoolean(
				CONFIG_KEY_LOCAL_SERVER_ENABLED,
				DEFAULT_LOCAL_SERVER_ENABLED);
	}
}
