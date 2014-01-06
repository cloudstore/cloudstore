package co.codewizards.cloudstore.client.ssl;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import co.codewizards.cloudstore.core.config.ConfigDir;

public class DynamicX509TrustManagerTest {
	static {
		System.setProperty(ConfigDir.SYSTEM_PROPERTY, "target/.cloudstore");
	}

	@BeforeClass
	public static void beforeClass() {
//		CloudStoreServ
	}

	@AfterClass
	public static void afterClass() {

	}


}
