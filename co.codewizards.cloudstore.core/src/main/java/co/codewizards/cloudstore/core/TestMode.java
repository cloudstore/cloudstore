package co.codewizards.cloudstore.core;

public class TestMode {

	private static final String SYSTEM_PROPERTY_KEY_TEST_MODE = TestMode.class.getName() + ".enabled";

	private TestMode() {
	}

	public static void enableTestMode() {
		System.setProperty(SYSTEM_PROPERTY_KEY_TEST_MODE, Boolean.TRUE.toString());
	}

	public static boolean isTestModeEnabled() {
		final String sysPropVal = System.getProperty(SYSTEM_PROPERTY_KEY_TEST_MODE);
		return Boolean.valueOf(sysPropVal);
	}
}
