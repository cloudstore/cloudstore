package co.codewizards.cloudstore.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.rest.client.CloudStoreRESTClient;
import co.codewizards.cloudstore.rest.client.RemoteException;

public class TestServiceIT {
	private static final Logger logger = LoggerFactory.getLogger(TestServiceIT.class);

	private CloudStoreRESTClient cloudStoreRESTClient = new CloudStoreRESTClient("http", "localhost", 4000);

	@Test
	public void testSuccess() {
		cloudStoreRESTClient.testSuccess();
	}

	@Test(expected=RemoteException.class)
	public void testException() throws Exception {
		try {
			cloudStoreRESTClient.testException();
		} catch (Exception x) {
			logger.info(x.toString(), x);
			throw x;
		}
	}
}
