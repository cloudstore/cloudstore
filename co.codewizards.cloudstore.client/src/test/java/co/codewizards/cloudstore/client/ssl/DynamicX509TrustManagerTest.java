package co.codewizards.cloudstore.client.ssl;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import co.codewizards.cloudstore.local.test.config.ConfigDir;

public class DynamicX509TrustManagerTest {
	static {
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, "target/.cloudstore");
	}

	@BeforeClass
	public static void beforeClass() {
//		CloudStoreServ
	}

	@AfterClass
	public static void afterClass() {

	}


}
