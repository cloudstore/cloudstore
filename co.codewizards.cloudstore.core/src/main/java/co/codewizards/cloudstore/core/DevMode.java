package co.codewizards.cloudstore.core;

public class DevMode {

	private static final String SYSTEM_PROPERTY_KEY_TEST_MODE = "DevMode.enabled";

	private DevMode() {
	}

	public static void enableDevMode() {
		System.setProperty(SYSTEM_PROPERTY_KEY_TEST_MODE, Boolean.TRUE.toString());
	}

	public static boolean isDevModeEnabled() {
		final String sysPropVal = System.getProperty(SYSTEM_PROPERTY_KEY_TEST_MODE);
		return Boolean.valueOf(sysPropVal);
	}
}
