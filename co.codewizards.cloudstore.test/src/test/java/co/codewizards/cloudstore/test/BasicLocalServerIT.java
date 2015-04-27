package co.codewizards.cloudstore.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;
import co.codewizards.cloudstore.ls.rest.client.request.TestRequest;
import co.codewizards.cloudstore.ls.server.LocalServer;

public class BasicLocalServerIT extends AbstractIT {

	private static LocalServer localServer;

	@BeforeClass
	public static void beforeBasicLocalServerIT() {
		localServer = new LocalServer();
		localServer.start();
	}

	@AfterClass
	public static void afterBasicLocalServerIT() {
		localServer.stop();
	}

	@Test
	public void invokeTestService() {
		LocalServerRestClient client = new LocalServerRestClient();
		client.execute(new TestRequest(false));
	}
}
